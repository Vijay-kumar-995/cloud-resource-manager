# Static and live comparison procedure

Compile the Java sources once, then start the Compose services. Keep `/tmp/go` absent for the static baseline.

```sh
javac *.java cgroup/*.java pool/*.java policy/*.java config/*.java metrics/*.java enforcer/*.java
docker-compose up --build -d
```

Run the static and live captures for the same wall-clock duration. Both use the same one-second sampling delay; Docker command time is added to each interval. `monitor` writes the original seven columns. `run` appends `decision` and `new_limit` after them.

```sh
java Main monitor 180 static-steady.csv
sudo timeout --signal=INT 180 java Main run 1 2.0 1024 live-steady.csv
```

Run the captures in separate passes with the same VM and workload state. The live command needs permission to write cgroup controls. If it cannot write them, it prints a `sudo` permission error and exits.

## Scenarios

| Scenario | Workloads | Commands and expected observation |
|---|---|---|
| Steady state | Fixed CPU and memory pair | Capture with `monitor`, then `run`. The CPU worker can grow from its initial 0.50 core; once usage falls below 85% of its raised limit, limits should hold. |
| Burst load | Fixed pair plus burst service | Start `run`, then `docker-compose exec -T burst-workload touch /tmp/go`. The burst container should reach GROW after three high samples; check `docker stats --no-stream` and its cgroup `cpu.max`. |
| Idle reclaim | Fixed pair plus burst service | Stop the burst with `docker-compose exec -T burst-workload rm /tmp/go`. After five low samples, the controller should lower the burst limits gradually and return capacity to the pool. |
| Contention | Fixed pair plus burst service | Use `java Main run 1 1.25 512`. This capacity covers the initial limits but leaves no spare pool. Trigger the burst; the idle burst and memory worker can surrender CPU after five low samples, and high-priority work receives available capacity first. |
| Mid-cycle crash | Fixed pair plus burst service | While `run` is active, execute `docker kill $(docker-compose ps -q burst-workload)`. The loop should log the vanished cgroup, return its last allocation, and continue. Compose may restart the service due to its restart policy. |
| Utilization comparison | Same selected workload mix for both passes | Save static and live CSVs with equal duration and interval. Compare container CPU, memory, decisions, and limits; use repeated runs before drawing conclusions. |

After experiments, stop the workloads:

```sh
docker-compose down
```
