package enforcer;

import cgroup.CgroupPathResolver;
import cgroup.CgroupReader;
import config.PolicyConfig;
import metrics.MetricsCollector;
import policy.AllocationDecision;
import policy.ContainerMetrics;
import policy.ContainerState;
import policy.ThresholdHysteresisPolicy;
import pool.ResourcePool;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.NoSuchFileException;

public class LiveController {
    public void run(int pollSeconds, double cpuCores, double memoryMB, String csvPath,
                    HostMetricsSource hostProvider,
                    DisplayMetricsSource displayProvider) throws Exception {
        if (pollSeconds < 1) throw new IllegalArgumentException("Poll interval must be at least one second");
        ResourcePool pool = new ResourcePool(cpuCores, memoryMB);
        CgroupPathResolver paths = new CgroupPathResolver();
        CgroupReader reader = new CgroupReader(paths);
        MetricsCollector collector = new MetricsCollector(paths, reader);
        ThresholdHysteresisPolicy policy = new ThresholdHysteresisPolicy();
        LimitEnforcer enforcer = new LimitEnforcer(paths);
        Map<String, ContainerState> states = new HashMap<>();
        try (BufferedWriter writer = csvPath == null ? null : new BufferedWriter(new FileWriter(csvPath))) {
            if (writer != null) {
                writer.write("timestamp,host_cpu_percent,host_available_memory_mb,container,cpu_percent,memory_usage_mb,memory_limit_mb,decision,new_limit");
                writer.newLine();
            }
            while (true) {
                HostMetrics host = hostProvider.read();
                Map<String, ContainerMetrics> samples = collector.collect();
                List<DisplaySample> displaySamples = writer == null ? List.of() : displayProvider.read();
                for (String id : List.copyOf(states.keySet())) {
                    if (!samples.containsKey(id)) reclaim(states.remove(id), pool, "container is no longer running");
                }
                List<ContainerMetrics> orderedSamples = new ArrayList<>(samples.values());
                orderedSamples.sort(Comparator.comparingInt(
                        (ContainerMetrics sample) -> priority(sample.name)).reversed());
                for (ContainerMetrics sample : orderedSamples) {
                    ContainerState state = states.get(sample.containerId);
                    if (state == null) {
                        long maxCpu = Math.max(PolicyConfig.DEFAULT_MAX_CPU_MICROS, sample.cpuLimitMicros);
                        long maxMemory = Math.max(PolicyConfig.DEFAULT_MAX_MEMORY_BYTES, sample.memoryLimitBytes);
                        state = new ContainerState(sample.containerId, sample.name, priority(sample.name), sample.cpuLimitMicros,
                                sample.cpuPeriodMicros, sample.memoryLimitBytes, maxCpu, maxMemory);
                        if (!pool.reserve((double) sample.cpuLimitMicros / 1_000_000,
                                (double) sample.memoryLimitBytes / (1024 * 1024))) {
                            throw new IllegalStateException("Pool capacity is below current container limits; increase run capacity");
                        }
                        states.put(sample.containerId, state);
                    } else {
                        reconcileLimits(state, sample, pool);
                    }
                    AllocationDecision decision = policy.decide(sample, state);
                    long oldCpu = state.cpuLimitMicros;
                    long oldMemory = state.memoryLimitBytes;
                    AllocationDecision applied;
                    try {
                        applied = applyDecision(state, sample, decision, pool, enforcer);
                    } catch (NoSuchFileException exception) {
                        reclaim(state, pool, "cgroup disappeared during enforcement");
                        states.remove(sample.containerId);
                        continue;
                    }
                    logDecision(state, oldCpu, oldMemory, sample, applied);
                    if (writer != null) writeCsv(writer, displaySamples, sample, state, applied,
                            host);
                }
                if (writer != null) writer.flush();
                Thread.sleep(pollSeconds * 1000L);
            }
        }
    }

    public record HostMetrics(double cpuPercent, long availableMemoryMB) { }

    public record DisplaySample(String name, double cpuPercent, double memoryMB, double memoryLimitMB) { }

    @FunctionalInterface
    public interface HostMetricsSource {
        HostMetrics read() throws Exception;
    }

    @FunctionalInterface
    public interface DisplayMetricsSource {
        List<DisplaySample> read() throws Exception;
    }

    private int priority(String name) {
        if (name.contains("burst-workload")) return 3;
        if (name.contains("cpu-worker")) return 2;
        return 1;
    }

    private void reconcileLimits(ContainerState state, ContainerMetrics sample, ResourcePool pool) {
        long cpuDelta = sample.cpuLimitMicros - state.cpuLimitMicros;
        long memoryDelta = sample.memoryLimitBytes - state.memoryLimitBytes;
        if (cpuDelta > 0 && pool.tryWithdraw(cpuDelta) < cpuDelta) {
            throw new IllegalStateException("Observed CPU limit exceeds available pool capacity");
        }
        if (memoryDelta > 0 && pool.tryWithdrawMemory(memoryDelta) < memoryDelta) {
            throw new IllegalStateException("Observed memory limit exceeds available pool capacity");
        }
        if (cpuDelta < 0) pool.deposit(-cpuDelta);
        if (memoryDelta < 0) pool.depositMemory(-memoryDelta);
        state.cpuLimitMicros = sample.cpuLimitMicros;
        state.memoryLimitBytes = sample.memoryLimitBytes;
    }

    private AllocationDecision applyDecision(ContainerState state, ContainerMetrics sample,
                                             AllocationDecision decision, ResourcePool pool,
                                             LimitEnforcer enforcer) throws IOException {
        if (decision.action == AllocationDecision.Action.HOLD) return decision;
        long oldCpu = state.cpuLimitMicros;
        long oldMemory = state.memoryLimitBytes;
        long newCpu = decision.newCpuLimitMicros;
        long newMemory = decision.newMemoryLimitBytes;
        long cpuGrant = 0;
        long memoryGrant = 0;
        if (decision.action == AllocationDecision.Action.GROW) {
            long cpuRequest = Math.max(0, newCpu - oldCpu);
            long memoryRequest = Math.max(0, newMemory - oldMemory);
            if (cpuRequest > 0) cpuGrant = pool.tryWithdrawWithFallback(cpuRequest,
                    Math.min(cpuRequest, PolicyConfig.FLOOR_CPU_MICROS));
            if (memoryRequest > 0) memoryGrant = pool.tryWithdrawMemoryWithFallback(memoryRequest,
                    Math.min(memoryRequest, PolicyConfig.FLOOR_MEM_BYTES));
            newCpu = oldCpu + cpuGrant;
            newMemory = oldMemory + memoryGrant;
        } else {
            newCpu = Math.max(PolicyConfig.FLOOR_CPU_MICROS, newCpu);
            newMemory = Math.max(PolicyConfig.FLOOR_MEM_BYTES, newMemory);
            if (sample.memoryUsageBytes > newMemory * 0.80) newMemory = oldMemory;
        }
        AllocationDecision granted = new AllocationDecision(decision.action, newCpu, newMemory,
                decision.reason + (decision.action == AllocationDecision.Action.GROW
                        ? " (pool grant applied)" : ""));
        try {
            enforcer.apply(state, granted, state.cpuPeriodMicros);
        } catch (NoSuchFileException exception) {
            if (state.cpuLimitMicros == oldCpu) pool.deposit(cpuGrant);
            if (state.memoryLimitBytes == oldMemory) pool.depositMemory(memoryGrant);
            throw exception;
        } catch (IOException exception) {
            if (state.cpuLimitMicros == oldCpu) pool.deposit(cpuGrant);
            if (state.memoryLimitBytes == oldMemory) pool.depositMemory(memoryGrant);
            throw exception;
        }
        if (decision.action == AllocationDecision.Action.SHRINK) {
            pool.deposit(oldCpu - state.cpuLimitMicros);
            pool.depositMemory(oldMemory - state.memoryLimitBytes);
        }
        return granted;
    }

    private void logDecision(ContainerState state, long oldCpu, long oldMemory,
                             ContainerMetrics sample, AllocationDecision decision) {
        System.out.printf(Locale.ROOT,
                "%s old=%.3f cores/%d bytes new=%.3f cores/%d bytes decision=%s reason=%s%n",
                sample.name, (double) oldCpu / 1_000_000,
                oldMemory, (double) state.cpuLimitMicros / 1_000_000,
                state.memoryLimitBytes, decision.action, decision.reason);
    }

    private void writeCsv(BufferedWriter writer, List<DisplaySample> displaySamples,
                          ContainerMetrics metrics, ContainerState state, AllocationDecision decision,
                          HostMetrics host) throws IOException {
        for (DisplaySample display : displaySamples) {
            if (!display.name.equals(metrics.name)) continue;
            String limit = String.format(Locale.ROOT, "%.3f cores/%d bytes",
                    (double) state.cpuLimitMicros / 1_000_000, state.memoryLimitBytes);
            writer.write(String.format(Locale.ROOT,
                    "%s,%.2f,%d,%s,%.2f,%.2f,%.2f,%s,%s%n", Instant.now(), host.cpuPercent,
                    host.availableMemoryMB, metrics.name, display.cpuPercent, display.memoryMB,
                    display.memoryLimitMB, decision.action, limit));
        }
    }

    private void reclaim(ContainerState state, ResourcePool pool, String reason) {
        pool.deposit(state.cpuLimitMicros);
        pool.depositMemory(state.memoryLimitBytes);
        System.out.println(state.name + " returned its limits to the pool: " + reason);
    }
}
