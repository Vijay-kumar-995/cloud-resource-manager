import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerMonitor {
    public static class Sample {
        public final String name;
        public final double cpuPercent;
        public final double memoryMB;
        public final double memoryLimitMB;

        Sample(String name, String cpu, String memory) {
            this.name = name;
            this.cpuPercent = Double.parseDouble(cpu.replace("%", "").trim());
            String[] values = memory.split("/");
            this.memoryMB = toMB(values[0]);
            this.memoryLimitMB = toMB(values[1]);
        }

        private static double toMB(String value) {
            String text = value.trim().toLowerCase();
            double number = Double.parseDouble(text.replaceAll("[^0-9.].*$", ""));
            if (text.contains("gib") || text.endsWith("gb")) return number * 1024;
            if (text.contains("kib") || text.endsWith("kb")) return number / 1024;
            if (text.contains("mib") || text.endsWith("mb")) return number;
            if (text.endsWith("b")) return number / (1024 * 1024);
            return number;
        }
    }

    public static List<Sample> readSamples() throws Exception {
        Process containers = new ProcessBuilder("docker", "ps", "--format", "{{.ID}}|{{.Names}}").start();
        List<String> ids = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(containers.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|", 2);
                if (fields.length == 2 && (fields[1].contains("cpu-worker")
                        || fields[1].contains("memory-worker")
                        || fields[1].contains("burst-workload"))) ids.add(fields[0]);
            }
        }
        String error = new String(containers.getErrorStream().readAllBytes());
        if (containers.waitFor() != 0) throw new IllegalStateException("docker ps failed: " + error.trim());
        List<Sample> samples = new ArrayList<>();
        if (ids.isEmpty()) return samples;
        List<String> command = new ArrayList<>(Arrays.asList("docker", "stats", "--no-stream",
                "--format", "{{.Name}}|{{.CPUPerc}}|{{.MemUsage}}"));
        command.addAll(ids);
        Process process = new ProcessBuilder(command).start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\|", 3);
                if (fields.length == 3) samples.add(new Sample(fields[0], fields[1], fields[2]));
            }
        }
        error = new String(process.getErrorStream().readAllBytes());
        if (process.waitFor() != 0) throw new IllegalStateException("docker stats failed: " + error.trim());
        return samples;
    }
}
