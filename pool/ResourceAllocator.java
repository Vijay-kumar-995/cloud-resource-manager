package pool;

public class ResourceAllocator {
    public static class Allocation {
        public final AllocationRequest request;
        public final double cpu;
        public final double memoryMB;

        Allocation(AllocationRequest request, double cpu, double memoryMB) {
            this.request = request;
            this.cpu = cpu;
            this.memoryMB = memoryMB;
        }

        @Override
        public String toString() {
            return request.name + ": " + cpu + " CPU cores, " + memoryMB + " MB";
        }
    }

    public Allocation allocate(AllocationRequest request, ResourcePool pool) {
        if (pool.reserve(request.requestedCpu, request.requestedMemoryMB)) {
            return new Allocation(request, request.requestedCpu, request.requestedMemoryMB);
        }
        if (pool.reserve(request.minimumCpu, request.minimumMemoryMB)) {
            return new Allocation(request, request.minimumCpu, request.minimumMemoryMB);
        }
        return null;
    }
}
