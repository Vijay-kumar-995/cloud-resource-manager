package cgroup;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class CgroupReader {
    public static class CpuLimit {
        public final long quotaMicros;
        public final long periodMicros;

        public CpuLimit(long quotaMicros, long periodMicros) {
            this.quotaMicros = quotaMicros;
            this.periodMicros = periodMicros;
        }

        public double cores() {
            return quotaMicros < 0 ? Double.POSITIVE_INFINITY : (double) quotaMicros / periodMicros;
        }
    }

    private final CgroupPathResolver paths;

    public CgroupReader(CgroupPathResolver paths) {
        this.paths = paths;
    }

    public long readCpuUsageMicros(String containerId) throws IOException {
        if (!paths.isV2()) return Long.parseLong(Files.readString(paths.cpuStatPath(containerId)).trim()) / 1000;
        for (String line : Files.readAllLines(paths.cpuStatPath(containerId))) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 2 && fields[0].equals("usage_usec")) return Long.parseLong(fields[1]);
        }
        throw new IOException("usage_usec not found in cpu.stat for " + containerId);
    }

    public long readMemoryCurrentBytes(String containerId) throws IOException {
        return Long.parseLong(Files.readString(paths.memoryCurrentPath(containerId)).trim());
    }

    public long readMemoryCacheAdjustedBytes(String containerId) throws IOException {
        long current = readMemoryCurrentBytes(containerId);
        String key = paths.isV2() ? "inactive_file" : "total_inactive_file";
        for (String line : Files.readAllLines(paths.memoryStatPath(containerId))) {
            String[] fields = line.trim().split("\\s+");
            if (fields.length == 2 && fields[0].equals(key)) {
                return Math.max(0, current - Long.parseLong(fields[1]));
            }
        }
        return current;
    }

    public CpuLimit readCpuLimit(String containerId) throws IOException {
        if (!paths.isV2()) {
            long quota = Long.parseLong(Files.readString(paths.cpuMaxPath(containerId)).trim());
            long period = Long.parseLong(Files.readString(paths.cpuPeriodPath(containerId)).trim());
            return new CpuLimit(quota, period);
        }
        String[] fields = Files.readString(paths.cpuMaxPath(containerId)).trim().split("\\s+");
        return new CpuLimit(fields[0].equals("max") ? -1 : Long.parseLong(fields[0]),
                Long.parseLong(fields[1]));
    }

    public long readMemoryMaxBytes(String containerId) throws IOException {
        String value = Files.readString(paths.memoryMaxPath(containerId)).trim();
        return value.equals("max") ? Long.MAX_VALUE : Long.parseLong(value);
    }
}
