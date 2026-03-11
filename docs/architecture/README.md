# Architecture documentation

- **[Configuration](configuration/olo-worker-configuration.md)** — olo-worker-configuration: bootstrap, snapshot model, refresh, admin service, operational guidelines.
- **[Bootstrap](bootstrap/README.md)** — olo-worker-bootstrap-loader (startup wiring) and olo-worker-bootstrap-runtime (execution infrastructure); BootstrapLoader.initialize() → WorkerRuntime; phases, contributors, plugin/feature registration.
- **[Execution tree](execution-tree/README.md)** — olo-execution-tree: execution trees per tenant/queue, PIPELINE_LOADING, relationship to configuration.
- **[Global context](global_context.md)** — Regions, tenants, region pipelines, tenant overrides; storage and seed (008).
- **[olo-workflow-input](olo-workflow-input.md)** — Workflow input and payload parsing.
