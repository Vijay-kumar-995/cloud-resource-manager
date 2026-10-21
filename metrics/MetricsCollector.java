package metrics;

import cgroup.CgroupPathResolver;
import cgroup.CgroupReader;
import policy.ContainerMetrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.NoSuchFileException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetricsCollector {
    private static class RawSample {
        final long cpuUsec;
        final long memoryBytes;
        final long memoryComparableBytes;
        final long cpuLimitMicros;
        final long cpuPeriodMicros;
        final long memoryLimitBytes;
        final long timeMillis;

        RawSample(long cpuUsec, long memoryBytes, long memoryComparableBytes, long cpuLimitMicros,
                  long cpuPeriodMicros, long memoryLimitBytes, long timeMillis) {
            this.cpuUsec = cpuUsec;
            this.memoryBytes = memoryBytes;
            this.memoryComparableBytes = memoryComparableBytes;
            this.cpuLimitMicros = cpuLimitMicros;
            this.cpuPeriodMicros = cpuPeriodMicros;
            this.memoryLimitBytes = memoryLimitBytes;
            this.timeMillis = timeMillis;
        }
    }

    private final CgroupPathResolver paths;
    private final CgroupReader reader;
    private final Map<String, ArrayDeque<RawSample>> windows = new HashMap<>();

    public MetricsCollector(CgroupPathResolver paths, CgroupReader reader) {
        this.paths = paths;
        this.reader = reader;
    }

    public Map<String, ContainerMetrics> collect() throws IOException {
        Map<String, ContainerMetrics> metrics = new HashMap<>();
        for (String[] container : runningContainers()) {
            String id = container[0];
            String name = container[1];
            try {
                CgroupReader.CpuLimit cpuLimit = reader.readCpuLimit(id);
                long cpuLimitMicros = Double.isInfinite(cpuLimit.cores())
                        ? Runtime.getRuntime().availableProcessors() * 1_000_000L
                        : Math.round(cpuLimit.cores() * 1_000_000);
                long memoryLimit = reader.readMemoryMaxBytes(id);
                if (memoryLimit == Long.MAX_VALUE) memoryLimit = Runtime.getRuntime().maxMemory();
                long now = System.currentTimeMillis();
                RawSample sample = new RawSample(reader.readCpuUsageMicros(id),
                        reader.readMemoryCurrentBytes(id), reader.readMemoryCacheAdjustedBytes(id),
                        cpuLimitMicros, cpuLimit.periodMicros, memoryLimit, now);
                ArrayDeque<RawSample> window = windows.computeIfAbsent(id, ignored -> new ArrayDeque<>());
                window.addLast(sample);
                while (window.size() > 5) window.removeFirst();
                RawSample first = window.getFirst();
                long elapsedUsec = Math.max(1, (sample.timeMillis - first.timeMillis) * 1000);
                long usageDelta = Math.max(0, sample.cpuUsec - first.cpuUsec);
                double cpuCores = (double) sample.cpuLimitMicros / 1_000_000;
                double cpuPercent = window.size() < 2 || cpuCores <= 0 ? 0
                        : 100.0 * usageDelta / (elapsedUsec * cpuCores);
                double memoryPercent = sample.memoryLimitBytes <= 0 ? 0
                        : 100.0 * sample.memoryBytes / sample.memoryLimitBytes;
                metrics.put(id, new ContainerMetrics(id, name, cpuPercent, memoryPercent,
                        sample.memoryBytes, sample.memoryComparableBytes, sample.cpuLimitMicros,
                        sample.cpuPeriodMicros, sample.memoryLimitBytes, now, sample.cpuUsec));
            } catch (NoSuchFileException exception) {
                windows.remove(id);
            }
        }
        return metrics;
    }

    private List<String[]> runningContainers() throws IOException {
        Process process = new ProcessBuilder("docker", "ps", "--format", "{{.ID}}|{{.Names}}").start();
        List<String[]> containers = new ArrayList<>();
        try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = output.readLine()) != null) {
                String[] fields = line.split("\\|", 2);
                if (fields.length == 2 && (fields[1].contains("cpu-worker")
                        || fields[1].contains("memory-worker") || fields[1].contains("burst-workload"))) {
                    containers.add(fields);
                }
            }
        }
        String error = new String(process.getErrorStream().readAllBytes());
        try {
            if (process.waitFor() != 0) throw new IOException("docker ps failed: " + error.trim());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while listing containers", exception);
        }
        return containers;
    }
}
