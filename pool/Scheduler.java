package pool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scheduler {
    private final ResourceAllocator allocator = new ResourceAllocator();

    public List<ResourceAllocator.Allocation> schedule(List<AllocationRequest> requests, ResourcePool pool) {
        requests.sort(Comparator.comparingInt((AllocationRequest request) -> request.priority).reversed());
        List<ResourceAllocator.Allocation> allocations = new ArrayList<>();
        for (AllocationRequest request : requests) {
            ResourceAllocator.Allocation allocation = allocator.allocate(request, pool);
            if (allocation != null) allocations.add(allocation);
        }
        return allocations;
    }
}
