package config;

public final class PolicyConfig {
    public static final double GROW_THRESHOLD = 0.85;
    public static final int GROW_CONFIRM_POLLS = 3;
    public static final double SHRINK_THRESHOLD = 0.20;
    public static final int SHRINK_CONFIRM_POLLS = 5;
    public static final double GROW_STEP_PCT = 0.30;
    public static final double SHRINK_STEP_PCT = 0.20;
    public static final double FLOOR_CPU_PCT = 0.05;
    public static final long FLOOR_CPU_MICROS = 50_000;
    public static final long FLOOR_MEM_BYTES = 64L * 1024 * 1024;
    public static final long DEFAULT_MAX_CPU_MICROS = 1_000_000;
    public static final long DEFAULT_MAX_MEMORY_BYTES = 512L * 1024 * 1024;

    private PolicyConfig() { }
}
