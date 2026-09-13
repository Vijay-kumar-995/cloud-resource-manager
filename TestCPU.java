import java.io.BufferedReader;
import java.io.FileReader;

public class TestCPU {

    public static long[] getCpuTimes() throws Exception {

        BufferedReader reader =
                new BufferedReader(new FileReader("/proc/stat"));

        String line = reader.readLine();

        reader.close();

        String[] parts = line.split("\\s+");

        long user = Long.parseLong(parts[1]);
        long nice = Long.parseLong(parts[2]);
        long system = Long.parseLong(parts[3]);
        long idle = Long.parseLong(parts[4]);

        return new long[]{user, nice, system, idle};
    }

    public static double getCpuUsage() throws Exception {

        long[] first = getCpuTimes();

        Thread.sleep(1000);

        long[] second = getCpuTimes();

        long user = second[0] - first[0];
        long nice = second[1] - first[1];
        long system = second[2] - first[2];
        long idle = second[3] - first[3];

        long busy = user + nice + system;
        long total = busy + idle;

        return (double) busy / total * 100;
    }

    public static void main(String[] args) throws Exception {

        double cpuUsage = getCpuUsage();

        System.out.printf("CPU Usage: %.2f%%%n", cpuUsage);
    }
}