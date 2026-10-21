import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import cgroup.CgroupPathResolver;
import cgroup.CgroupReader;
import metrics.MetricsCollector;
import enforcer.LiveController;
import enforcer.DisplayMetricsProvider;
import enforcer.DisplaySample;
import enforcer.HostMetrics;
import enforcer.HostMetricsProvider;
import policy.ContainerMetrics;
import pool.AllocationRequest;
import pool.ResourcePool;
import pool.ResourceAllocator;
import pool.Scheduler;

public class Main {
    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "help" : args[0];
        if (command.equals("monitor")) {
            monitor(args.length > 1 ? Integer.parseInt(args[1]) : 60,
                    args.length > 2 ? args[2] : "metrics.csv");
        } else if (command.equals("plan")) {
            plan(args);
        } else if (command.equals("cgroup-read")) {
            cgroupRead(args);
        } else if (command.equals("compare-metrics")) {
            compareMetrics(args);
        } else if (command.equals("run")) {
            runLive(args);
        } else {
            System.out.println("Commands:");
            System.out.println("  java Main monitor [seconds] [output.csv]");
            System.out.println("  java Main plan <cpu-cores> <memory-mb>");
            System.out.println("  java Main cgroup-read <container-id>");
            System.out.println("  java Main compare-metrics <seconds>");
            System.out.println("  java Main run <poll-seconds> <total-cpu-cores> <total-memory-mb> [output.csv]");
        }
    }

    private static void runLive(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            throw new IllegalArgumentException("Usage: java Main run <poll-seconds> <total-cpu-cores> <total-memory-mb> [output.csv]");
        }
        int pollSeconds = Integer.parseInt(args[1]);
        double cpuCores = Double.parseDouble(args[2]);
        double memoryMB = Double.parseDouble(args[3]);
        String csvPath = args.length == 5 ? args[4] : null;
        long[][] previousCpu = {ResourceMonitor.getCpuTimes()};
        HostMetricsProvider hostProvider = () -> {
            long[] currentCpu = ResourceMonitor.getCpuTimes();
            double cpu = ResourceMonitor.getCpuUsage(previousCpu[0], currentCpu);
            previousCpu[0] = currentCpu;
            return new HostMetrics(cpu, ResourceMonitor.getAvailableMemoryMB());
        };
        DisplayMetricsProvider displayProvider = () -> {
            List<DisplaySample> values = new ArrayList<>();
            for (DockerMonitor.Sample sample : DockerMonitor.readSamples()) {
                values.add(new DisplaySample(sample.name, sample.cpuPercent, sample.memoryMB,
                        sample.memoryLimitMB));
            }
            return values;
        };
        new LiveController().run(pollSeconds, cpuCores, memoryMB, csvPath, hostProvider, displayProvider);
    }

    private static void compareMetrics(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: java Main compare-metrics <seconds>");
        int seconds = Integer.parseInt(args[1]);
        CgroupPathResolver paths = new CgroupPathResolver();
        MetricsCollector collector = new MetricsCollector(paths, new CgroupReader(paths));
        long stopAt = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < stopAt) {
            List<DockerMonitor.Sample> docker = DockerMonitor.readSamples();
            for (ContainerMetrics metric : collector.collect().values()) {
                for (DockerMonitor.Sample sample : docker) {
                    if (!sample.name.equals(metric.name)) continue;
                    double cpuCores = (double) metric.cpuLimitMicros / 1_000_000;
                    double dockerCpuPercent = cpuCores == 0 ? 0 : sample.cpuPercent / cpuCores;
                    double cgroupMemoryMB = metric.memoryComparableBytes / (1024.0 * 1024);
                    double memoryDelta = sample.memoryMB == 0 ? 0
                            : Math.abs(cgroupMemoryMB - sample.memoryMB) / sample.memoryMB * 100;
                    System.out.printf(Locale.ROOT,
                            "%s cpu: cgroup=%.2f%% docker-normalized=%.2f%% memory: cgroup=%.2fMB docker=%.2fMB delta=%.2f%%%n",
                            metric.name, metric.cpuUsagePercent, dockerCpuPercent,
                            cgroupMemoryMB, sample.memoryMB, memoryDelta);
                }
            }
            Thread.sleep(1000);
        }
    }

    private static void cgroupRead(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: java Main cgroup-read <container-id>");
        CgroupPathResolver paths = new CgroupPathResolver();
        CgroupReader reader = new CgroupReader(paths);
        String containerId = args[1];
        System.out.println("cgroup version: " + (paths.isV2() ? 2 : 1));
        System.out.println("CPU usage: " + reader.readCpuUsageMicros(containerId) + " microseconds");
        System.out.println("Memory current: " + reader.readMemoryCurrentBytes(containerId) + " bytes");
        CgroupReader.CpuLimit cpu = reader.readCpuLimit(containerId);
        System.out.println("CPU limit: " + cpu.quotaMicros + " / " + cpu.periodMicros + " microseconds");
        System.out.println("Memory limit: " + reader.readMemoryMaxBytes(containerId) + " bytes");
    }

    private static void monitor(int seconds, String output) throws Exception {
        if (seconds < 1) throw new IllegalArgumentException("Monitor duration must be at least one second");
        long[] previousCpu = ResourceMonitor.getCpuTimes();
        long stopAt = System.currentTimeMillis() + seconds * 1000L;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write("timestamp,host_cpu_percent,host_available_memory_mb,container,cpu_percent,memory_usage_mb,memory_limit_mb");
            writer.newLine();
            while (System.currentTimeMillis() < stopAt) {
                Thread.sleep(1000);
                long[] currentCpu = ResourceMonitor.getCpuTimes();
                double hostCpu = ResourceMonitor.getCpuUsage(previousCpu, currentCpu);
                previousCpu = currentCpu;
                long availableMemory = ResourceMonitor.getAvailableMemoryMB();
                List<DockerMonitor.Sample> containers = DockerMonitor.readSamples();
                String timestamp = Instant.now().toString();
                for (DockerMonitor.Sample container : containers) {
                    String row = String.format(Locale.ROOT, "%s,%.2f,%d,%s,%.2f,%.2f,%.2f", timestamp,
                            hostCpu, availableMemory, container.name, container.cpuPercent,
                            container.memoryMB, container.memoryLimitMB);
                    writer.write(row);
                    writer.newLine();
                    System.out.println(row);
                }
                writer.flush();
            }
        }
        System.out.println("Metrics saved to " + output);
    }

    private static void plan(String[] args) {
        if (args.length != 3) throw new IllegalArgumentException("Usage: java Main plan <cpu-cores> <memory-mb>");
        ResourcePool pool = new ResourcePool(Double.parseDouble(args[1]), Double.parseDouble(args[2]));
        List<AllocationRequest> requests = new ArrayList<>(Arrays.asList(
                new AllocationRequest("cpu-worker", 2, 0.25, 0.50, 128, 192),
                new AllocationRequest("memory-worker", 1, 0.25, 0.50, 128, 192)));
        List<ResourceAllocator.Allocation> allocations = new Scheduler().schedule(requests, pool);
        System.out.println("Allocation decisions:");
        for (ResourceAllocator.Allocation allocation : allocations) System.out.println("  " + allocation);
        for (AllocationRequest request : requests) {
            boolean allocated = false;
            for (ResourceAllocator.Allocation allocation : allocations) {
                if (allocation.request == request) allocated = true;
            }
            if (!allocated) System.out.println("  " + request.name + ": not allocated (pool capacity is insufficient)");
        }
        System.out.printf("Pool remaining: %.2f CPU cores, %.0f MB memory%n",
                pool.getFreeCpu(), pool.getFreeMemoryMB());
    }
}
