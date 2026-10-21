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

- [ ] Adjust Docker limits from observed demand
- [ ] Reallocate and reclaim resources safely

## Milestone 9 — Experiments

- [ ] Compare static and dynamic policies
- [ ] Compare utilization, wastage, and workload performance

## Milestone 10 — Final System

- [ ] Add broader automated checks, visualization, dashboard, and final report

## Validation note

The Java sources compiled with Java 17. Both bounded workloads ran in Docker, and `static-baseline.csv` captured host and per-container measurements. The pool demo printed priority-ordered allocations. Dynamic reallocation and static-versus-dynamic comparisons are Milestones 8 and 9.
