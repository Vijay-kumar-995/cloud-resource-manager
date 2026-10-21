import java.io.BufferedReader;
import java.io.FileReader;

public class ResourceMonitor {
    public static long getAvailableMemoryMB() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemAvailable:")) {
                    return Long.parseLong(line.split("\\s+")[1]) / 1024;
                }
            }
        }
        throw new IllegalStateException("MemAvailable was not found in /proc/meminfo");
    }

    public static long[] getCpuTimes() throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"))) {
            String[] fields = reader.readLine().trim().split("\\s+");
            int count = Math.min(fields.length, 9);
            long[] times = new long[count - 1];
            for (int i = 1; i < count; i++) {
                times[i - 1] = Long.parseLong(fields[i]);
            }
            return times;
        }
    }

    public static double getCpuUsage(long[] previous, long[] current) {
        long total = 0;
        for (int i = 0; i < current.length; i++) total += current[i] - previous[i];
        long idle = current[3] - previous[3];
        if (current.length > 4) idle += current[4] - previous[4];
        return total == 0 ? 0 : 100.0 * (total - idle) / total;
    }
}
