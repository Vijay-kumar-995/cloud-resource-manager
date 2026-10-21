package enforcer;

public interface HostMetricsProvider {
    HostMetrics read() throws Exception;
}
