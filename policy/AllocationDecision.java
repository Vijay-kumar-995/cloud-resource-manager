package policy;

public class AllocationDecision {
    public enum Action { GROW, SHRINK, HOLD }

    public final Action action;
    public final long newCpuLimitMicros;
    public final long newMemoryLimitBytes;
    public final String reason;

    public AllocationDecision(Action action, long newCpuLimitMicros, long newMemoryLimitBytes, String reason) {
        this.action = action;
        this.newCpuLimitMicros = newCpuLimitMicros;
        this.newMemoryLimitBytes = newMemoryLimitBytes;
        this.reason = reason;
    }
}
