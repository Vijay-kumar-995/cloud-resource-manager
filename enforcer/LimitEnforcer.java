package enforcer;

import cgroup.CgroupPathResolver;
import cgroup.CgroupWriter;
import config.PolicyConfig;
import policy.AllocationDecision;
import policy.ContainerState;

import java.io.IOException;

public class LimitEnforcer {
    private final CgroupWriter writer;

    public LimitEnforcer(CgroupPathResolver paths) {
        writer = new CgroupWriter(paths);
    }

    public void apply(String containerId, AllocationDecision decision) throws IOException {
        AllocationDecision bounded = new AllocationDecision(decision.action,
                Math.max(PolicyConfig.FLOOR_CPU_MICROS,
                        Math.min(PolicyConfig.DEFAULT_MAX_CPU_MICROS, decision.newCpuLimitMicros)),
                Math.max(PolicyConfig.FLOOR_MEM_BYTES,
                        Math.min(PolicyConfig.DEFAULT_MAX_MEMORY_BYTES, decision.newMemoryLimitBytes)),
                decision.reason);
        apply(containerId, bounded, 100_000);
    }

    public void apply(ContainerState state, AllocationDecision decision, long periodUsec) throws IOException {
        long cpuLimit = decision.action == AllocationDecision.Action.SHRINK
                ? Math.max(PolicyConfig.FLOOR_CPU_MICROS, decision.newCpuLimitMicros)
                : Math.min(state.maxCpuMicros, Math.max(PolicyConfig.FLOOR_CPU_MICROS,
                        decision.newCpuLimitMicros));
        long memoryLimit = decision.action == AllocationDecision.Action.SHRINK
                ? Math.max(PolicyConfig.FLOOR_MEM_BYTES, decision.newMemoryLimitBytes)
                : Math.min(state.maxMemoryBytes, Math.max(PolicyConfig.FLOOR_MEM_BYTES,
                        decision.newMemoryLimitBytes));
        long quotaUsec = Math.max(1, cpuLimit * periodUsec / 1_000_000);
        if (cpuLimit != state.cpuLimitMicros) {
            writer.setCpuMax(state.containerId, quotaUsec, periodUsec);
            state.cpuLimitMicros = cpuLimit;
        }
        if (memoryLimit != state.memoryLimitBytes) {
            writer.setMemoryMax(state.containerId, memoryLimit);
            state.memoryLimitBytes = memoryLimit;
        }
    }

    private void apply(String containerId, AllocationDecision decision, long periodUsec) throws IOException {
        if (decision.action == AllocationDecision.Action.HOLD) return;
        long quotaUsec = Math.max(1, decision.newCpuLimitMicros * periodUsec / 1_000_000);
        writer.setCpuMax(containerId, quotaUsec, periodUsec);
        writer.setMemoryMax(containerId, decision.newMemoryLimitBytes);
    }
}
