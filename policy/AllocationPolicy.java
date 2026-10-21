package policy;

public interface AllocationPolicy {
    AllocationDecision decide(ContainerMetrics metrics, ContainerState state);
}
