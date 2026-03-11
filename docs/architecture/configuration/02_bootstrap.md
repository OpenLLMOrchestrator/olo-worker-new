# Bootstrap

Part of the [olo-worker-configuration](olo-worker-configuration.md) documentation.

---

## Final worker flow

```
Worker Start
     │
     ▼
load defaults → env
     │
     ▼
initialize Redis client (if configured) → wait until Redis and snapshot available → build immutable configuration snapshot; atomically install snapshot into ConfigurationProvider
     │
     ▼
load tenant region map → start refresh scheduler (optional)
     │
     ▼
Worker runtime: workflow execution reads configuration from the local immutable snapshot
     │
     ▼
Every 60 sec:
  1. Read snapshot metadata from Redis
  2. Compare metadata version with local snapshot version
  If version changed: reload entire snapshot (core, pipelines, connections)
  Else: skip (no reload)
  (if Redis unavailable: keep current snapshot, retry next cycle)
```

---

## Worker startup flow (Bootstrap.run())

1. Load defaults  
2. Load environment variables  
3. Determine worker region  
4. Initialize Redis client (when Redis is configured)  
5. Wait until Redis and configuration snapshot are available  
6. Load snapshot from Redis  
7. Build immutable configuration snapshot; atomically install into ConfigurationProvider  
8. Load tenant–region mapping  
9. Start configuration refresh scheduler (optional)

The tenant–region mapping can change at runtime (e.g. a tenant moves region). **Tenant region mapping refresh** runs independently via **TenantRegionRefreshScheduler** (e.g. every 60 s or via event); otherwise the mapping would become stale. See [06_operational_guidelines](06_operational_guidelines.md).

**Determining region:** Region is determined using the configuration key **`olo.region`** (comma-separated list of regions this instance serves). Workers may serve one or more regions depending on deployment; each configured region has an independent configuration snapshot. This value is typically provided via ENV, e.g. **`OLO_REGION=us-east-1`**. If not set, the default is `default`.

**What “snapshot available” means:** The worker waits until the following exist in Redis **for its region**:
- **Snapshot metadata** (`olo:configuration:meta:<region>`)
- **Core configuration section** (`olo:configuration:core:<region>`)

If either is missing, the worker does not start. This avoids partial snapshots and stuck workers.

**Timeout behavior** (`olo.bootstrap.config.wait.timeout.seconds`):

- **If timeout &gt; 0 and exceeded:** Worker exits with non-zero status; the orchestrator (e.g. Kubernetes) restarts the container.  
- **If timeout = 0 or unset:** Worker waits indefinitely until Redis and snapshot are available.

If Redis is not configured (e.g. local dev), the worker uses defaults + env only and skips steps 4–6.

**Snapshot consistency:** Snapshots are written atomically by the admin service (e.g. Redis MULTI/EXEC). Workers load metadata first, then all referenced sections (core, pipelines, connections). If any required section is missing, the snapshot load fails and the worker retries on the next cycle. This avoids half-written snapshots.

---

## Default values file

- **Classpath resource**: `olo-defaults.properties`
- **Purpose**: Define default values for all keys (e.g. local dev).
- Infrastructure and local settings (Temporal, DB, cache, region list) live here and in ENV; they are **not** part of the Redis snapshot.

---

## Using configuration at runtime

**Bootstrap (once at startup):**

```java
Bootstrap.run();
```

**At runtime:** Read only from the in-memory configuration. Never query Redis or DB. Typical usage uses the layered config (global → region → tenant → resource):

```java
Configuration config = ConfigurationProvider.require();
String model = config.forContext(tenantId, "pipeline:chat").get("model", "gpt-4");
```

For tenant-only config: `config.forTenant(tenantId).get(...)`. For tenant + resource (e.g. pipeline): `config.forContext(tenantId, "pipeline:chat").get(...)`. Use **TenantRegionResolver.getRegion(tenantId)** when you need the tenant’s region.

---

## Flow diagram

```mermaid
flowchart LR
  A[olo-defaults.properties] --> B[defaults + env]
  C[Environment OLO_*] --> B
  B --> D[Bootstrap]
  D --> E{Redis?}
  E -->|yes| F[RedisSnapshotLoader: meta, core, pipelines, connections, resources]
  F --> G[ConfigurationProvider snapshot]
  E -->|no| H[DefaultConfiguration in impl]
  H --> G
  G --> I[Workers read via Configuration]
```
