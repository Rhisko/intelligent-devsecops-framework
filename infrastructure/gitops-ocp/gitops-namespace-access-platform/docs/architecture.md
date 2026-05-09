# Architecture

This repository separates namespace access platform concerns from application delivery concerns.

The platform repository owns cluster-facing guardrails:

- namespace lifecycle
- quota and limit defaults
- baseline network isolation
- reusable RBAC profiles
- namespace-local access bindings
- Argo CD AppProject boundaries

Application repositories own application manifests. This keeps platform governance stable while allowing application teams to ship independently.

## Cluster Entrypoint

Each cluster or environment entrypoint composes one environment:

- `clusters/ocp-dev/kustomization.yaml`
- `clusters/ocp-staging/kustomization.yaml`
- `clusters/ocp-prod/kustomization.yaml`

Each entrypoint includes:

- `platform/namespaces/<environment>`
- `platform/rbac-catalog`
- `platform/access-control/bindings/<environment>`
- one matching `platform/argocd-guardrails/<environment>`

This makes the cluster state auditable from one GitOps entrypoint.

## Argo CD Application Model

Bootstrap Applications are intentionally granular:

- `namespace-access-rbac-catalog` syncs shared `ClusterRole` profiles.
- `dbank-<environment>-argocd-guardrails` syncs AppProject guardrails.
- `dbank-app-<nn>-<environment>-access` syncs one namespace baseline and its RoleBindings.

This keeps Argo CD status visible at the application namespace level without duplicating the shared RBAC catalog in every namespace Application.
