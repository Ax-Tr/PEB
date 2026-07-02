# Blue/Green Kubernetes manifests (Sprint 19)

Reference manifests for the blue/green production topology. Shown for one representative service
(`api-gateway`); every deployable service follows the same pattern (Helm templates this per service —
see the release workflow). The active colour is selected by the Service's `selector.color`, flipped
during cutover.

- `deployment-blue.yaml` / `deployment-green.yaml` — the two colours; only one takes traffic at a time.
- `service.yaml` — stable ClusterIP whose `selector.color` is switched (blue↔green) at cutover.
- `hpa.yaml` — horizontal pod autoscaling on CPU + request latency.
- `keda-scaledobject.yaml` — event-driven autoscaling of a consumer on **Kafka consumer lag** (scale
  on backlog, not just CPU), so read-model projections keep up under bursts.

Migrations are expand/contract: additive migrations run before GREEN deploys (safe for BLUE);
destructive changes wait one release. This guarantees a seconds-level, traffic-only rollback
(flip the Service selector back to BLUE).
