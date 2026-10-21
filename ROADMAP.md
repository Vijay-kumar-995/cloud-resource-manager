# Project Roadmap

## Milestone 1 — Understand the Problem

- [x] Understand cloud resources, allocation, pooling, monitoring, and reclamation

## Milestone 2 — Environment Setup

- [x] Identify Ubuntu, Java, Docker, and cgroups as the project environment
- [x] Define container resource limits for the test workloads

## Milestone 3 — Linux Resource Monitoring

- [x] Read host CPU and memory information from `/proc`
- [x] Read per-container usage through Docker stats

## Milestone 4 — Java Resource Monitor

- [x] Calculate host CPU usage from successive `/proc/stat` samples
- [x] Read available memory from `/proc/meminfo`

## Milestone 5 — Docker Workloads

- [x] Create bounded CPU and memory test workloads
- [x] Provide Docker build and startup configuration
- [x] Provide per-container usage monitoring
- [x] Run and observe the bounded workloads on Docker

## Milestone 6 — Static Allocation

- [x] Configure fixed CPU and memory limits for the workloads
- [x] Record host and container metrics to CSV for a static baseline
- [x] Run a short static allocation baseline

## Milestone 7 — Resource Pool and Scheduler

- [x] Model total and available pool capacity
- [x] Represent workload requests, minimums, and priorities
- [x] Allocate requested resources or fall back to minimums
- [x] Schedule higher-priority workloads first
- [x] Demonstrate allocation decisions from the command line

## Milestone 8 — Dynamic Allocation

- [x] Read live per-container CPU and memory usage directly from cgroups
- [x] Apply threshold/hysteresis policy and resize running container limits
- [x] Reclaim unused CPU and memory through a shared resource pool
- [x] Handle disappearing containers and emit live decisions to stdout/CSV
- [x] Add an opt-in burst workload for exercising scale-up behavior

## Milestone 9 — Observability (stretch goal)

- [ ] Add a terminal dashboard or lightweight REST endpoint for pool state
- [ ] Expand static-versus-dynamic experiments using `scripts/compare-runs.md`

## Milestone 10 — Multi-Node (future work)

- [ ] Explore coordinating resource pools across multiple Ubuntu/Docker nodes
- [ ] Define node discovery, placement, and failure handling before implementation

## Validation note

The Java sources compile with Java 17. `monitor` retains its original CSV columns, while `run` appends decision and new-limit fields. The M8 control loop was exercised against the Compose workloads using direct cgroup reads and writes; see `scripts/compare-runs.md` for repeatable comparison scenarios. Cgroup writes require sufficient privileges (typically `sudo`).
