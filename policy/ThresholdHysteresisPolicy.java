package policy;

import config.PolicyConfig;

public class ThresholdHysteresisPolicy implements AllocationPolicy {
    @Override
    public AllocationDecision decide(ContainerMetrics metrics, ContainerState state) {
        state.currentUsagePercent = metrics.cpuUsagePercent;
        boolean high = metrics.cpuUsagePercent >= PolicyConfig.GROW_THRESHOLD * 100
                || metrics.memoryUsagePercent >= PolicyConfig.GROW_THRESHOLD * 100;
        boolean low = metrics.cpuUsagePercent <= PolicyConfig.SHRINK_THRESHOLD * 100
                || metrics.memoryUsagePercent <= PolicyConfig.SHRINK_THRESHOLD * 100;
        if (high) {
            state.highStreak++;
            state.lowStreak = 0;
        } else if (low) {
            state.lowStreak++;
            state.highStreak = 0;
        } else {
            state.highStreak = 0;
            state.lowStreak = 0;
        }

        if (state.highStreak >= PolicyConfig.GROW_CONFIRM_POLLS) {
            long cpu = metrics.cpuUsagePercent >= PolicyConfig.GROW_THRESHOLD * 100
                    ? grow(state.cpuLimitMicros, state.maxCpuMicros) : state.cpuLimitMicros;
            long memory = metrics.memoryUsagePercent >= PolicyConfig.GROW_THRESHOLD * 100
                    ? grow(state.memoryLimitBytes, state.maxMemoryBytes) : state.memoryLimitBytes;
            if (cpu > state.cpuLimitMicros || memory > state.memoryLimitBytes) {
                state.highStreak = 0;
                return new AllocationDecision(AllocationDecision.Action.GROW, cpu, memory,
                        "high usage confirmed for " + PolicyConfig.GROW_CONFIRM_POLLS + " polls");
            }
            state.highStreak = 0;
        }
        if (state.lowStreak >= PolicyConfig.SHRINK_CONFIRM_POLLS) {
            long cpu = metrics.cpuUsagePercent <= PolicyConfig.SHRINK_THRESHOLD * 100
                    ? shrink(state.cpuLimitMicros, PolicyConfig.FLOOR_CPU_MICROS) : state.cpuLimitMicros;
            long memory = metrics.memoryUsagePercent <= PolicyConfig.SHRINK_THRESHOLD * 100
                    ? shrink(state.memoryLimitBytes, PolicyConfig.FLOOR_MEM_BYTES) : state.memoryLimitBytes;
            if (cpu < state.cpuLimitMicros || memory < state.memoryLimitBytes) {
                state.lowStreak = 0;
                return new AllocationDecision(AllocationDecision.Action.SHRINK, cpu, memory,
                        "low usage confirmed for " + PolicyConfig.SHRINK_CONFIRM_POLLS + " polls");
            }
        }
        return new AllocationDecision(AllocationDecision.Action.HOLD, state.cpuLimitMicros,
                state.memoryLimitBytes, "usage is within the steady range or awaiting confirmation");
    }

    private long grow(long current, long maximum) {
        return current + Math.max(0, Math.round((maximum - current) * PolicyConfig.GROW_STEP_PCT));
    }

    private long shrink(long current, long floor) {
        return Math.max(floor, Math.round(current * (1 - PolicyConfig.SHRINK_STEP_PCT)));
    }
}
