package policy;

public class ThresholdHysteresisPolicyTest {
    public static void main(String[] args) {
        ThresholdHysteresisPolicy policy = new ThresholdHysteresisPolicy();
        ContainerState state = state();
        AllocationDecision result = null;
        for (int i = 0; i < 3; i++) result = policy.decide(metrics(90, 10), state);
        require(result.action == AllocationDecision.Action.GROW, "three high samples must grow");

        state = state();
        for (int i = 0; i < 5; i++) result = policy.decide(metrics(10, 10), state);
        require(result.action == AllocationDecision.Action.SHRINK, "five low samples must shrink");

        state = state();
        for (int i = 0; i < 10; i++) {
            result = policy.decide(metrics(i % 2 == 0 ? 49 : 51, 50), state);
            require(result.action == AllocationDecision.Action.HOLD, "mid-range usage must hold");
        }
    }

    private static ContainerState state() {
        return new ContainerState("id", "test", 500_000, 100_000, 192L * 1024 * 1024,
                1_000_000, 512L * 1024 * 1024);
    }

    private static ContainerMetrics metrics(double cpuPercent, double memoryPercent) {
        return new ContainerMetrics("id", "test", cpuPercent, memoryPercent,
                96L * 1024 * 1024, 96L * 1024 * 1024, 500_000, 100_000,
                192L * 1024 * 1024, System.currentTimeMillis(), 0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
