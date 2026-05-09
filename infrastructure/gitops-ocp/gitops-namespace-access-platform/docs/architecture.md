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

`clusters/ocp-dev/kustomization.yaml` composes:

- `platform/namespaces/dev`
- `platform/rbac-catalog`
- `platform/access-control/bindings/dev`
- `platform/argocd-guardrails`

This makes the cluster state auditable from one GitOps entrypoint.

