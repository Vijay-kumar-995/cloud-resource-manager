public class ResourcePool {
    private final double totalCpu;
    private final double totalMemoryMB;
    private double freeCpu;
    private double freeMemoryMB;

    public ResourcePool(double cpuCores, double memoryMB) {
        if (!Double.isFinite(cpuCores) || !Double.isFinite(memoryMB) || cpuCores <= 0 || memoryMB <= 0) {
            throw new IllegalArgumentException("Pool capacity must be positive and finite");
        }
        totalCpu = freeCpu = cpuCores;
        totalMemoryMB = freeMemoryMB = memoryMB;
    }

    public boolean reserve(double cpuCores, double memoryMB) {
        if (cpuCores > freeCpu || memoryMB > freeMemoryMB) return false;
        freeCpu -= cpuCores;
        freeMemoryMB -= memoryMB;
        return true;
    }

    public double getTotalCpu() { return totalCpu; }
    public double getTotalMemoryMB() { return totalMemoryMB; }
    public double getFreeCpu() { return freeCpu; }
    public double getFreeMemoryMB() { return freeMemoryMB; }
}
