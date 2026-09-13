# Dynamic Cloud Resource Pooling and Allocation System

## Overview

This project is a Java-based dynamic cloud resource pooling and allocation system.

The system aims to improve resource utilization by dynamically allocating and reclaiming CPU and memory resources according to workload requirements.

## Problem

Static resource allocation assigns fixed CPU and memory resources to workloads even when their actual requirements change.

This can lead to:

- Resource wastage
- Poor resource utilization
- Inefficient workload performance

## Proposed Solution

The project maintains a shared pool of CPU and memory resources.

A Java-based Resource Manager monitors resource usage and will dynamically allocate, reallocate, and reclaim resources based on workload demand.

## Technology Stack

- Java
- Docker
- Linux
- Linux cgroups
- Ubuntu
- VirtualBox

## Current Progress

- [x] Project requirements and cloud resource allocation concepts
- [x] Java and Docker environment setup
- [x] Linux resource monitoring concepts
- [x] Basic Java Resource Monitor
- [ ] Docker workload management
- [ ] Static allocation baseline
- [ ] Resource Pool
- [ ] Scheduler
- [ ] Resource Allocator
- [ ] Dynamic resource allocation
- [ ] Resource reallocation
- [ ] Resource reclamation
- [ ] Experiments and comparison
- [ ] Metrics and visualization
- [ ] Final dashboard and documentation

## Architecture

```text
Workloads / Users
        |
        v
Java Resource Manager
        |
        +---- Resource Monitor
        |
        +---- Scheduler
        |
        +---- Resource Allocator
        |
        v
Shared CPU & Memory Resource Pool
        |
        v
Docker Containers
        |
        v
Linux cgroups
        |
        v
Linux Kernel
