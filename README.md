# Dynamic Cloud Resource Pooling and Allocation System

A small Java project that monitors Docker workloads, records a fixed-allocation baseline, and demonstrates priority-based allocation from a shared CPU and memory pool. Docker applies the fixed limits in Milestone 6. The Java scheduler in Milestone 7 plans allocations; applying changing limits is Milestone 8.

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

Both containers have a fixed limit of 0.50 CPU cores and 192 MB memory. The CPU workload keeps a processor busy; the memory workload retains about 80 MB. Check their status and limits with:

```sh
docker compose ps
docker stats --no-stream
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

## Try the resource pool and scheduler

Give the scheduler the CPU and memory capacity available to workloads after reserving enough for Ubuntu and Docker:

```sh
java Main plan 2.0 1024
```

It processes requests in priority order, grants the requested allocation when it fits, otherwise tries the minimum allocation, and reports requests that do not fit. This is a planning demonstration; it does not change Docker limits.

## Compare results

Run the same workload for the same duration with the same VM conditions, save the CSV, and use its samples to compare host and container CPU and memory usage. Record the pool capacity and workload limits with each run. Repeat runs for more reliable results. This project currently supplies a static baseline; dynamic Docker reallocation is the next milestone.

## Project structure

- `ResourceMonitor.java` reads host CPU counters and available memory from `/proc`.
- `DockerMonitor.java` reads per-container usage through `docker stats`.
- `Main.java` provides the metrics recorder and scheduler demonstration commands.
- `ResourcePool.java`, `Scheduler.java`, `ResourceAllocator.java`, and `AllocationRequest.java` model Milestone 7.
- `compose.yaml`, `Dockerfile`, and `Workload.java` define the bounded test workloads.
- `ROADMAP.md` tracks project milestones.
