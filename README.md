# Dynamic Cloud Resource Pooling and Allocation System

A compact Java demo of a shared CPU and memory pool for Docker workloads. It records a static baseline, then reads live cgroup usage and adjusts container limits using a threshold policy.

## Requirements

- Ubuntu Linux with Java 17 or newer
- Docker Engine and Docker Compose (plugin or standalone `docker-compose`)
- Permission to run Docker commands

## Run two bounded workloads

From the project directory, build and start the CPU and memory workloads:

```sh
docker compose up --build -d
# If using standalone Compose instead:
docker-compose up --build -d
```

The fixed CPU and memory workloads each have a 0.50-core and 192 MB limit. The additive burst workload has a 0.25-core and 128 MB limit and stays idle until triggered. The CPU workload keeps a processor busy; the memory workload retains about 80 MB. Check status and limits with:

```sh
docker compose ps
docker stats --no-stream
```

Start a burst without restarting the container, then turn it off by removing the trigger file:

```sh
docker exec cloud-resource-manager_burst-workload_1 touch /tmp/go
docker exec cloud-resource-manager_burst-workload_1 rm /tmp/go
```

## Capture the static baseline

Compile the Java files and record 60 seconds of host and per-container metrics:

```sh
javac *.java
java Main monitor 60 static-baseline.csv
```

The CSV contains a timestamp, host CPU usage and available memory, plus each project workload's CPU usage, memory usage, and configured memory limit. The monitor waits one second between samples; Docker command time adds to the actual interval. To stop the workloads:

```sh
docker compose down
# Or: docker-compose down
```

The CSV is ignored by Git. Keep a copy elsewhere if you need to submit experiment results.

## Run the live controller

Compile all packaged Java sources, then start the controller with the pool capacity reserved for workloads. The controller reads cgroup v1/v2 files, applies CPU and memory limits directly, and may need root permission on the Ubuntu host:

```sh
javac *.java cgroup/*.java pool/*.java policy/*.java config/*.java metrics/*.java enforcer/*.java
sudo java Main run 1 2.0 1024 live.csv
```

The live CSV preserves the static monitor's seven columns and appends `decision` and `new_limit`. To compare direct cgroup metrics with `docker stats`, run `java Main compare-metrics 30`. Use `java Main cgroup-read <container-id>` to inspect one container.

Detailed static/live comparison commands and scenarios are in [`scripts/compare-runs.md`](scripts/compare-runs.md).

## Try the resource pool and scheduler

Give the scheduler the CPU and memory capacity available to workloads after reserving enough for Ubuntu and Docker:

```sh
java Main plan 2.0 1024
```

It processes requests in priority order, grants the requested allocation when it fits, otherwise tries the minimum allocation, and reports requests that do not fit. This is a planning demonstration; it does not change Docker limits.

## Viva / presentation demo

Explain the system as one loop: **measure → decide → allocate → enforce**. `MetricsCollector` reads cgroups; `ThresholdHysteresisPolicy` waits for repeated high/low readings; `ResourcePool` grants or reclaims capacity; `LimitEnforcer` writes the new limits. `docker stats` is the visible verification path.

```sh
docker compose up --build -d
javac *.java cgroup/*.java pool/*.java policy/*.java config/*.java metrics/*.java enforcer/*.java
java Main plan 2.0 1024                  # show priority allocation
java Main cgroup-read <container-id>     # show real cgroup values
sudo java Main run 1 2.0 1024            # show live decisions and limit changes
docker stats --no-stream                 # verify changes without restarting
```

For the burst demonstration, run `docker exec cloud-resource-manager_burst-workload_1 touch /tmp/go` in another terminal. Explain that three consecutive high polls trigger growth, five low polls trigger gradual reclamation, and the pool prevents total allocation from exceeding its capacity. Stop the controller with Ctrl-C and clean up with `docker compose down`.

## Project structure

- `ResourceMonitor.java` reads host CPU counters and available memory from `/proc`.
- `DockerMonitor.java` reads per-container usage through `docker stats`.
- `Main.java` provides the metrics recorder, scheduler demo, cgroup inspection, and live-controller commands.
- `pool/` contains allocation requests, the shared pool, allocator, and scheduler.
- `cgroup/`, `metrics/`, `policy/`, `config/`, and `enforcer/` implement cgroup measurement, the allocation policy, and limit enforcement.
- `compose.yaml`, `Dockerfile`, and `Workload.java` define the bounded test workloads.
- `ROADMAP.md` tracks project milestones.
