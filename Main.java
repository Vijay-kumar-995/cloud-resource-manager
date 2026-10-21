import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "help" : args[0];
        if (command.equals("monitor")) {
            monitor(args.length > 1 ? Integer.parseInt(args[1]) : 60,
                    args.length > 2 ? args[2] : "metrics.csv");
        } else if (command.equals("plan")) {
            plan(args);
        } else {
            System.out.println("Commands:");
            System.out.println("  java Main monitor [seconds] [output.csv]");
            System.out.println("  java Main plan <cpu-cores> <memory-mb>");
        }
    }

    private static void monitor(int seconds, String output) throws Exception {
        long[] previousCpu = ResourceMonitor.getCpuTimes();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            writer.write("timestamp,host_cpu_percent,host_available_memory_mb,container,cpu_percent,memory_usage_mb,memory_limit_mb");
            writer.newLine();
            for (int sample = 0; sample < seconds; sample++) {
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
