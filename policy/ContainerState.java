package policy;

import config.PolicyConfig;
import pool.AllocationRequest;

public class ContainerState extends AllocationRequest {
    public final String containerId;
    public final long cpuPeriodMicros;
    public final long maxCpuMicros;
    public final long maxMemoryBytes;
    public long cpuLimitMicros;
    public long memoryLimitBytes;

    public ContainerState(String containerId, String name, long cpuLimitMicros, long cpuPeriodMicros,
                          long memoryLimitBytes, long maxCpuMicros, long maxMemoryBytes) {
        this(containerId, name, 1, cpuLimitMicros, cpuPeriodMicros,
                memoryLimitBytes, maxCpuMicros, maxMemoryBytes);
    }

    public ContainerState(String containerId, String name, int priority, long cpuLimitMicros,
                          long cpuPeriodMicros, long memoryLimitBytes, long maxCpuMicros,
                          long maxMemoryBytes) {
        super(name, priority, PolicyConfig.FLOOR_CPU_PCT, (double) maxCpuMicros / 1_000_000,
                (double) PolicyConfig.FLOOR_MEM_BYTES / (1024 * 1024),
                (double) maxMemoryBytes / (1024 * 1024));
        this.containerId = containerId;
        this.cpuLimitMicros = cpuLimitMicros;
        this.cpuPeriodMicros = cpuPeriodMicros;
        this.memoryLimitBytes = memoryLimitBytes;
        this.maxCpuMicros = maxCpuMicros;
        this.maxMemoryBytes = maxMemoryBytes;
    }
}
