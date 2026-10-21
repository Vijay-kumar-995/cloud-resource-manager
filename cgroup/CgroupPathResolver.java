package cgroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CgroupPathResolver {
    private static class ContainerInfo {
        final String id;
        final String processId;

        ContainerInfo(String id, String processId) {
            this.id = id;
            this.processId = processId;
        }
    }

    private final boolean v2;
    private final Path cgroupRoot;
    private final Map<String, Path> directories = new HashMap<>();

    public CgroupPathResolver() throws IOException {
        Path root = Paths.get("/sys/fs/cgroup");
        List<String> mountInfo = Files.readAllLines(Paths.get("/proc/self/mountinfo"));
        Path unifiedMount = findMount(mountInfo, "cgroup2", null);
        this.v2 = unifiedMount != null && Files.exists(unifiedMount.resolve("cgroup.controllers"));
        this.cgroupRoot = v2 ? unifiedMount : root;
    }

    public boolean isV2() {
        return v2;
    }

    public Path cpuStatPath(String containerId) throws IOException {
        return resolve(containerId, v2 ? null : "cpuacct").resolve(v2 ? "cpu.stat" : "cpuacct.usage");
    }

    public Path cpuMaxPath(String containerId) throws IOException {
        return resolve(containerId, v2 ? null : "cpu").resolve(v2 ? "cpu.max" : "cpu.cfs_quota_us");
    }

    public Path cpuPeriodPath(String containerId) throws IOException {
        return resolve(containerId, "cpu").resolve("cpu.cfs_period_us");
    }

    public Path memoryCurrentPath(String containerId) throws IOException {
        return resolve(containerId, v2 ? null : "memory")
                .resolve(v2 ? "memory.current" : "memory.usage_in_bytes");
    }

    public Path memoryMaxPath(String containerId) throws IOException {
        return resolve(containerId, v2 ? null : "memory")
                .resolve(v2 ? "memory.max" : "memory.limit_in_bytes");
    }

    public Path memoryStatPath(String containerId) throws IOException {
        return resolve(containerId, v2 ? null : "memory").resolve("memory.stat");
    }

    private Path resolve(String containerId, String controller) throws IOException {
        String key = containerId + ":" + controller;
        Path cached = directories.get(key);
        if (cached != null && Files.isDirectory(cached)) return cached;
        Path mount = v2 ? cgroupRoot : controllerMount(controller);
        ContainerInfo container = inspectContainer(containerId);
        Path membership = membershipPath(container.processId, controller);
        if (membership != null) {
            Path resolved = mountedPath(mount, membership);
            if (Files.isDirectory(resolved)) {
                directories.put(key, resolved);
                return resolved;
            }
        }
        for (Path candidate : fallbackPaths(mount, container.id, controller)) {
            if (Files.isDirectory(candidate)) {
                directories.put(key, candidate);
                return candidate;
            }
        }
        throw new NoSuchFileException(containerId, null, "container cgroup directory disappeared");
    }

    private ContainerInfo inspectContainer(String containerId) throws IOException {
        Process process = new ProcessBuilder("docker", "inspect", "--format", "{{.Id}}|{{.State.Pid}}", containerId)
                .start();
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            line = reader.readLine();
        }
        String error = new String(process.getErrorStream().readAllBytes());
        try {
            if (process.waitFor() != 0) throw new IOException("docker inspect failed: " + error.trim());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while inspecting container " + containerId, exception);
        }
        if (line == null) throw new NoSuchFileException(containerId, null, "container is not running");
        String[] fields = line.split("\\|", 2);
        if (fields.length != 2 || fields[1].trim().equals("0")) {
            throw new NoSuchFileException(containerId, null, "container is not running");
        }
        return new ContainerInfo(fields[0].trim(), fields[1].trim());
    }

    private Path membershipPath(String processId, String controller) {
        Path processCgroup = Paths.get("/proc", processId, "cgroup");
        if (!Files.isReadable(processCgroup)) return null;
        try {
            for (String line : Files.readAllLines(processCgroup)) {
                String[] fields = line.split(":", 3);
                if (fields.length != 3) continue;
                if (v2 && fields[0].equals("0") && fields[1].isEmpty()) return Paths.get(fields[2]);
                if (!v2 && hasController(fields[1], controller)) return Paths.get(fields[2]);
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private Path controllerMount(String controller) throws IOException {
        Path mount = findMount(Files.readAllLines(Paths.get("/proc/self/mountinfo")), "cgroup", controller);
        if (mount == null) throw new IOException("No cgroup mount found for controller " + controller);
        return mount;
    }

    private Path findMount(List<String> mountInfo, String fileSystem, String controller) {
        for (String line : mountInfo) {
            String[] halves = line.split(" - ", 2);
            if (halves.length != 2) continue;
            String[] left = halves[0].split(" ");
            String[] right = halves[1].split(" ");
            if (right.length < 3 || !right[0].equals(fileSystem)) continue;
            if (controller != null && !hasController(right[2], controller)
                    && !hasController(left[left.length - 1], controller)) continue;
            return Paths.get(left[4]);
        }
        return null;
    }

    private boolean hasController(String controllers, String controller) {
        for (String value : controllers.split(",")) if (value.equals(controller)) return true;
        return false;
    }

    private Path mountedPath(Path mount, Path membership) {
        Path normalized = membership.normalize();
        return mount.resolve(normalized.isAbsolute() ? normalized.toString().substring(1) : normalized.toString())
                .normalize();
    }

    private List<Path> fallbackPaths(Path mount, String containerId, String controller) {
        List<Path> paths = new ArrayList<>();
        if (v2) {
            paths.add(mount.resolve("system.slice").resolve("docker-" + containerId + ".scope"));
            paths.add(mount.resolve("docker").resolve(containerId));
            paths.add(mount.resolve(containerId));
        } else {
            paths.add(mount.resolve("docker").resolve(containerId));
            paths.add(mount.resolve("docker-" + containerId + ".scope"));
        }
        return paths;
    }
}
