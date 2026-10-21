package policy;

public class ContainerMetrics {
    public final String containerId;
    public final String name;
    public final double cpuUsagePercent;
    public final double memoryUsagePercent;
    public final long memoryUsageBytes;
    public final long memoryComparableBytes;
    public final long cpuLimitMicros;
    public final long cpuPeriodMicros;
    public final long memoryLimitBytes;
    public final long sampledAtMillis;
    public final long cpuUsageMicros;

    public ContainerMetrics(String containerId, String name, double cpuUsagePercent,
                            double memoryUsagePercent, long memoryUsageBytes, long memoryComparableBytes,
                            long cpuLimitMicros, long cpuPeriodMicros, long memoryLimitBytes,
                            long sampledAtMillis, long cpuUsageMicros) {
        this.containerId = containerId;
        this.name = name;
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsagePercent = memoryUsagePercent;
        this.memoryUsageBytes = memoryUsageBytes;
        this.memoryComparableBytes = memoryComparableBytes;
        this.cpuLimitMicros = cpuLimitMicros;
        this.cpuPeriodMicros = cpuPeriodMicros;
        this.memoryLimitBytes = memoryLimitBytes;
        this.sampledAtMillis = sampledAtMillis;
        this.cpuUsageMicros = cpuUsageMicros;
    }
}
