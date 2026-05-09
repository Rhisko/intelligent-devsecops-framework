# Namespace Access Platform

This repository is a namespace access platform for OpenShift and Kubernetes. It manages namespace lifecycle, baseline controls, reusable RBAC, namespace access bindings, an application access matrix, and optional Argo CD AppProject guardrails.

Application teams manage workloads in separate application repositories. This repository intentionally does not manage application `Deployment`, `Service`, `Route`, `Ingress`, application `ConfigMap` data, application `Secret` data, `HPA`, `CronJob`, image tags, or replica settings.

## Ownership

Platform team manages:

- Namespace and OpenShift Project lifecycle
- `ResourceQuota`
- `LimitRange`
- baseline `NetworkPolicy`
- reusable RBAC `ClusterRole` catalog
- namespace-local `RoleBinding` access grants
- Argo CD AppProject guardrails

Application teams manage:

- application manifests in separate repositories
- image tags and release promotion
- application runtime configuration
- service-specific routes, ingress, jobs, scaling, and deployment settings

## Namespace Creation

Namespaces are defined under:

```text
platform/namespaces/dev/<application>/
```

The dev cluster entrypoint is:

```text
clusters/ocp-dev/kustomization.yaml
```

It includes namespace baselines, the RBAC catalog, access bindings, and Argo CD guardrails.

## Quota And Limits

Every namespace receives the same starter quota:

- CPU requests: `2`
- Memory requests: `4Gi`
- CPU limits: `4`
- Memory limits: `8Gi`
- Pods: `30`
- Services: `10`
- ConfigMaps: `30`
- Secrets: `30`
- PVCs: `5`

Every namespace also receives a `LimitRange` with default container requests and limits so workloads have predictable scheduling and cost boundaries.

## Network Baseline

Every namespace gets:

- `default-deny-all`: denies all ingress and egress by default.
- `allow-same-namespace`: allows pod-to-pod communication within the same namespace.

Cross-namespace, internet, database, and platform service access should be added through reviewed application network policy in the owning application repository or a dedicated platform exception process.

## RBAC Model

The repository defines reusable `ClusterRole` profiles and binds them into namespaces with `RoleBinding`.

This pattern keeps one central RBAC catalog while preserving namespace-scoped access. A `RoleBinding` that references a `ClusterRole` grants only namespaced permissions in the target namespace.

`ClusterRoleBinding` is avoided because it grants access cluster-wide and is too broad for application team access. This repository also does not use `cluster-admin`.

## Onboard `dbank-app-06`

1. Create `platform/namespaces/dev/dbank-app-06/` with namespace baseline manifests.
2. Add `dbank-app-06` to `platform/namespaces/dev/kustomization.yaml`.
3. Add `platform/access-control/applications/dbank-app-06.yaml` describing the owner, domain, namespace, and app repo.
4. Add `platform/access-control/bindings/dev/dbank-app-06-access.yaml`.
5. Add the binding file to `platform/access-control/bindings/dev/kustomization.yaml`.
6. Add the app repo and destination namespace to `platform/argocd-guardrails/appproject-dbank-dev.yaml`.
7. Run `./tools/validate.sh`.

## Change Team Access

Update the relevant file under:

```text
platform/access-control/bindings/dev/
```

RoleBinding mapping:

- owners -> `namespace-owner-limited`
- operators -> `namespace-operator`
- viewers -> `namespace-readonly`
- audit groups -> `security-auditor`
- deployers -> `namespace-deployer`

## Remove Access

Remove the group from the namespace RoleBinding subject list. If it is the last subject for that access level, remove the entire RoleBinding. OpenShift GitOps will prune it after merge.

## Validate Access

Examples:

```bash
kubectl auth can-i get pods \
  --namespace dbank-app-01-dev \
  --as access-reviewer \
  --as-group ocp-team-security

kubectl auth can-i patch deployment \
  --namespace dbank-app-01-dev \
  --as access-reviewer \
  --as-group ocp-team-sre

kubectl auth can-i create rolebindings.rbac.authorization.k8s.io \
  --namespace dbank-app-01-dev \
  --as access-reviewer \
  --as-group ocp-team-digital-banking
```

Expected result for the final command is `no`; application owners cannot create RoleBindings.

## Render

```bash
./tools/render.sh
./tools/validate.sh
```

## Apply To OpenShift

```bash
oc apply -k clusters/ocp-dev
```
