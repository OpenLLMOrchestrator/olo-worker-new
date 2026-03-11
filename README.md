# olo-worker (Temporal)

Gradle multi-module Temporal worker:

- `olo-worker-configuration`: Load config at bootstrap from defaults file + ENV, keep in memory (see `olo-worker-configuration/README.md`)
- `olo-workflow-input`: JSON payload POJOs + deserializer (`OloPayloadParser`)
- `olo-worker`: Temporal worker entrypoint (depends on `olo-worker-configuration`, `olo-workflow-input`)

## Prerequisites

- Java 21+ (or any JDK that can compile with `--release 17`)
- Gradle (or use the wrapper: `gradlew` / `gradlew.bat`)
- A Temporal server (local dev default is `localhost:7233`)

If you want a local Temporal server quickly, you can use Temporalite or Temporal Server; this project assumes it’s reachable.

## Run the worker

```bash
./gradlew :olo-worker:run
```

Environment variables:

- `TEMPORAL_TARGET` (default `localhost:7233`)
- `TEMPORAL_NAMESPACE` (default `default`)
- `TASK_QUEUE` (default `olo-chat-queue-ollama`)

## Start a workflow (from JSON)

1) Save a valid JSON file (note: the JSON in your prompt is missing a comma inside `userContext`).

2) Run:

```bash
./gradlew :olo-worker:run -PappMain=com.olo.worker.StarterMain --args="--jsonFile sample.json"
```

The starter will:

- Parse JSON into `OloWorkerRequest`
- Start `OloChatWorkflow.run(request)`
- Print the workflow result

## Code layout

- **olo-workflow-input**: `model/` (OloWorkerRequest, Trace, Routing, Input, etc.), `model/enums/` (ExecutionMode, ExecutionPriority, InputType), `parser/` (OloPayloadParser), `validation/` (OloPayloadValidator), `util/` (InputResolver)
- **olo-worker**: `workflow/` (workflow + activities), `WorkerMain.java`, `StarterMain.java`

