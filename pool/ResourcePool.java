package pool;

public class ResourcePool {
    private static final long CPU_UNITS_PER_CORE = 1_000_000;
    private static final long BYTES_PER_MB = 1024 * 1024;
    private final double totalCpu;
    private final double totalMemoryMB;
    private final long cpuCapacityMicros;
    private final long memoryCapacityBytes;
    private long freeCpuMicros;
    private long freeMemoryBytes;

    public ResourcePool(double cpuCores, double memoryMB) {
        if (!Double.isFinite(cpuCores) || !Double.isFinite(memoryMB) || cpuCores <= 0 || memoryMB <= 0) {
            throw new IllegalArgumentException("Pool capacity must be positive and finite");
        }
        totalCpu = cpuCores;
        totalMemoryMB = memoryMB;
        cpuCapacityMicros = Math.round(cpuCores * CPU_UNITS_PER_CORE);
        memoryCapacityBytes = Math.round(memoryMB * BYTES_PER_MB);
        freeCpuMicros = cpuCapacityMicros;
        freeMemoryBytes = memoryCapacityBytes;
    }

    public synchronized boolean reserve(double cpuCores, double memoryMB) {
        long cpu = Math.round(cpuCores * CPU_UNITS_PER_CORE);
        long memory = Math.round(memoryMB * BYTES_PER_MB);
        if (cpu < 0 || memory < 0 || cpu > freeCpuMicros || memory > freeMemoryBytes) return false;
        freeCpuMicros -= cpu;
        freeMemoryBytes -= memory;
        return true;
    }

    public synchronized long tryWithdraw(long requestedCpuMicros) {
        if (requestedCpuMicros < 0) throw new IllegalArgumentException("CPU request cannot be negative");
        long granted = Math.min(requestedCpuMicros, freeCpuMicros);
        freeCpuMicros -= granted;
        return granted;
    }

    public synchronized long tryWithdrawMemory(long requestedBytes) {
        if (requestedBytes < 0) throw new IllegalArgumentException("Memory request cannot be negative");
        long granted = Math.min(requestedBytes, freeMemoryBytes);
        freeMemoryBytes -= granted;
        return granted;
    }

    public synchronized long tryWithdrawWithFallback(long requestedCpuMicros, long minimumCpuMicros) {
        if (minimumCpuMicros < 0 || minimumCpuMicros > requestedCpuMicros) {
            throw new IllegalArgumentException("Minimum CPU must be between zero and requested CPU");
        }
        long granted = tryWithdraw(requestedCpuMicros);
        if (granted < minimumCpuMicros) {
            deposit(granted);
            return 0;
        }
        return granted;
    }

    public synchronized long tryWithdrawMemoryWithFallback(long requestedBytes, long minimumBytes) {
        if (minimumBytes < 0 || minimumBytes > requestedBytes) {
            throw new IllegalArgumentException("Minimum memory must be between zero and requested memory");
        }
        long granted = tryWithdrawMemory(requestedBytes);
        if (granted < minimumBytes) {
            depositMemory(granted);
            return 0;
        }
        return granted;
    }

    public synchronized void deposit(long reclaimedCpuMicros) {
        if (reclaimedCpuMicros < 0) throw new IllegalArgumentException("CPU deposit cannot be negative");
        freeCpuMicros += Math.min(reclaimedCpuMicros, cpuCapacityMicros - freeCpuMicros);
    }

    public synchronized void depositMemory(long reclaimedBytes) {
        if (reclaimedBytes < 0) throw new IllegalArgumentException("Memory deposit cannot be negative");
        freeMemoryBytes += Math.min(reclaimedBytes, memoryCapacityBytes - freeMemoryBytes);
    }

    public double getTotalCpu() { return totalCpu; }
    public double getTotalMemoryMB() { return totalMemoryMB; }
    public synchronized double getFreeCpu() { return (double) freeCpuMicros / CPU_UNITS_PER_CORE; }
    public synchronized double getFreeMemoryMB() { return (double) freeMemoryBytes / BYTES_PER_MB; }
    public synchronized long getFreeCpuMicros() { return freeCpuMicros; }
    public synchronized long getFreeMemoryBytes() { return freeMemoryBytes; }
}
