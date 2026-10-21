package enforcer;

public class DisplaySample {
    public final String name;
    public final double cpuPercent;
    public final double memoryMB;
    public final double memoryLimitMB;

    public DisplaySample(String name, double cpuPercent, double memoryMB, double memoryLimitMB) {
        this.name = name;
        this.cpuPercent = cpuPercent;
        this.memoryMB = memoryMB;
        this.memoryLimitMB = memoryLimitMB;
    }
}
