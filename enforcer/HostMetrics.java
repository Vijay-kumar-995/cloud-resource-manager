package enforcer;

public class HostMetrics {
    public final double cpuPercent;
    public final long availableMemoryMB;

    public HostMetrics(double cpuPercent, long availableMemoryMB) {
        this.cpuPercent = cpuPercent;
        this.availableMemoryMB = availableMemoryMB;
    }
}
