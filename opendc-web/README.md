# OpenDC web

OpenDC as a service. People build topologies and experiments in the browser, and the server runs the
simulations beside itself, on a Kubernetes cluster or on a SLURM cluster.

| Module | Role |
|---|---|
| `opendc-web-server` | Quarkus service: the REST API under `/api/v1`, the execution queue, and the frontend's static files |
| `opendc-web-frontend` | Next.js app, exported as static files that the server serves |
| `opendc-web-dispatcher` | hands an execution to a platform (local processes, Kubernetes Jobs or SLURM jobs) and reports how it ended |
| `opendc-web-launcher` | the program one execution runs: it reads its manifest, simulates, reports progress and uploads the results |

An experiment is an `opendc-sdk-model` document. Each of its scenarios runs once per seed, and the
server packs these runs into *executions*, one launcher process each, sized to the dispatcher's
slot. It retries the runs of an execution that ran out of memory or time. Progress reported while an
execution runs is a preview that expires; outcomes come from the platform, and results from the
stored output files.

## Build and test

```sh
./gradlew :opendc-web:opendc-web-server:build      # also builds the launcher and the frontend
./gradlew :opendc-web:opendc-web-frontend:check    # Biome and Vitest
```

The tests need neither Docker nor PostgreSQL nor Redis: the server's tests run on H2 and keep live
progress in memory. In `opendc-web-frontend`, `pnpm lint` runs Biome only; `pnpm exec tsc --noEmit`
typechecks.

## Develop

```sh
./gradlew :opendc-web:opendc-web-server:quarkusDev   # http://localhost:8080, H2, nobody signs in
cd opendc-web/opendc-web-frontend
echo NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 > .env.development
pnpm install && pnpm dev                              # http://localhost:3000
```

[`deploy/`](../deploy/README.md) describes how to run and operate a deployment.
