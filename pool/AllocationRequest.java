package pool;

public class AllocationRequest {
    public final String name;
    public final int priority;
    public final double minimumCpu;
    public final double requestedCpu;
    public final double minimumMemoryMB;
    public final double requestedMemoryMB;
    public double currentUsagePercent;
    public int highStreak;
    public int lowStreak;

    public AllocationRequest(String name, int priority, double minimumCpu, double requestedCpu,
                             double minimumMemoryMB, double requestedMemoryMB) {
        if (!Double.isFinite(minimumCpu) || !Double.isFinite(requestedCpu)
                || !Double.isFinite(minimumMemoryMB) || !Double.isFinite(requestedMemoryMB)
                || minimumCpu < 0 || requestedCpu < minimumCpu || minimumMemoryMB < 0
                || requestedMemoryMB < minimumMemoryMB) {
            throw new IllegalArgumentException("Requested resources must be at least the minimum");
        }
        this.name = name;
        this.priority = priority;
        this.minimumCpu = minimumCpu;
        this.requestedCpu = requestedCpu;
        this.minimumMemoryMB = minimumMemoryMB;
        this.requestedMemoryMB = requestedMemoryMB;
    }
}
