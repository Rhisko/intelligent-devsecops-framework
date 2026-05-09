# Namespace Access Platform

This repository is a namespace access platform for OpenShift and Kubernetes. It manages namespace lifecycle, baseline controls, reusable RBAC, namespace access bindings, an application access matrix, and optional Argo CD AppProject guardrails.

Application teams manage workloads in separate application repositories. This repository intentionally does not manage application `Deployment`, `Service`, `Route`, `Ingress`, application `ConfigMap` data, application `Secret` data, `HPA`, `CronJob`, image tags, or replica settings.

## Ownership

Platform team manages:

- Namespace and OpenShift Project lifecycle
- `ResourceQuota`
- `LimitRange`
- baseline `NetworkPolicy`
- `ApplicationAccess` CRD inventory for tenant ownership and access intent
- reusable RBAC `ClusterRole` catalog
- namespace-local `RoleBinding` access grants
- Argo CD AppProject guardrails

Application teams manage:

- application manifests in separate repositories
- image tags and release promotion
- application runtime configuration
- service-specific routes, ingress, jobs, scaling, and deployment settings

LDAP manages:

- user lifecycle
- user-to-group membership
- offboarding and disabled accounts

## Namespace Creation

Namespaces are defined under:

```text
platform/namespaces/<environment>/<application>/
```

Each application gets one namespace per environment:

```text
dbank-app-01-dev
dbank-app-01-staging
dbank-app-01-prod
```

Cluster entrypoints are:

```text
clusters/ocp-dev/kustomization.yaml
clusters/ocp-staging/kustomization.yaml
clusters/ocp-prod/kustomization.yaml
```

Each entrypoint includes that environment's namespace baselines, the shared RBAC catalog, that environment's access bindings, and the matching Argo CD guardrail.

For Argo CD visibility, the bootstrap uses smaller Applications:

- one shared `namespace-access-rbac-catalog` Application
- one guardrail Application per environment
- one access Application per application namespace, such as `dbank-app-01-dev-access`

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

## User Inventory

User membership is managed in LDAP, not in this GitOps repository.

This repository records which LDAP/OpenShift group gets which namespace role. LDAP records which users belong to each group.

```text
LDAP user -> LDAP group -> OpenShift group -> RoleBinding -> ClusterRole
```

To inventory users, query LDAP or the synced OpenShift group:

```bash
oc get groups
oc describe group ocp-team-security
```

## Application Inventory CRD

The repository defines a lightweight `ApplicationAccess` CRD for inventory and audit:

```text
platform/access-control/crds/applicationaccess-crd.yaml
platform/access-control/applications/
```

`ApplicationAccess` records application namespaces, domain, repository, owners, operators, viewers, auditors, and deployers. It does not create RoleBindings by itself. RoleBindings remain explicit under:

```text
platform/access-control/bindings/
```

This keeps cluster state queryable:

```bash
oc get applicationaccesses
oc get applicationaccess dbank-app-01 -o yaml
```

## Onboard `dbank-app-06`

1. Create `platform/namespaces/dev/dbank-app-06/` with namespace baseline manifests.
2. Create matching `staging` and `prod` namespace folders if the application is approved for those environments.
3. Add `dbank-app-06` to each environment `kustomization.yaml`.
4. Add `platform/access-control/applications/dbank-app-06.yaml` describing owner, domain, namespaces, and app repo.
5. Add environment RoleBindings such as `platform/access-control/bindings/dev/dbank-app-06/access.yaml`.
6. Add a namespace access entrypoint such as `platform/namespace-access/dev/dbank-app-06/kustomization.yaml`.
7. Add the namespace access entrypoint to `platform/namespace-access/dev/kustomization.yaml`.
8. Add a bootstrap Argo CD Application such as `bootstrap/argocd-applications/dbank-app-06-dev-access.yaml`.
9. Add the app repo and destination namespaces to the relevant `platform/argocd-guardrails/<environment>/appproject-dbank-<environment>.yaml`.
10. Run `./tools/validate.sh`.

## Change Team Access

Update the relevant file under:

```text
platform/access-control/bindings/<environment>/
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

Register the namespace access platform in Argo CD:

```bash
oc apply -k bootstrap/argocd-applications
```

All bootstrap Applications use this Git source:

```text
git@github.com:Rhisko/intelligent-devsecops-framework.git
```

Because this repository lives inside a monorepo, Argo CD source paths use:

```text
infrastructure/gitops-ocp/ocp-tenant-governance/
```

This creates Argo CD `Application` resources at platform and namespace granularity:

- `tenant-application-inventory` -> `platform/access-control/application-inventory`
- `namespace-access-rbac-catalog` -> `platform/rbac-catalog`
- `dbank-dev-argocd-guardrails` -> `platform/argocd-guardrails/dev`
- `dbank-staging-argocd-guardrails` -> `platform/argocd-guardrails/staging`
- `dbank-prod-argocd-guardrails` -> `platform/argocd-guardrails/prod`
- `dbank-app-01-dev-access` -> `platform/namespace-access/dev/dbank-app-01`
- one matching `*-access` Application for every other application namespace

For direct apply without Argo CD:

```bash
oc apply -k clusters/ocp-dev
oc apply -k clusters/ocp-staging
oc apply -k clusters/ocp-prod
```
