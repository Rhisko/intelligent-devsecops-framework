# Operations Runbook

## Render Cluster State

```bash
kubectl kustomize clusters/ocp-dev
kubectl kustomize clusters/ocp-staging
kubectl kustomize clusters/ocp-prod
```

## Apply Cluster State

Register the platform repository as Argo CD Applications:

```bash
oc apply -k bootstrap/argocd-applications
oc get applications -n argocd
```

The bootstrap creates a group inventory Application, a shared RBAC catalog Application, one guardrail Application per environment, and one `*-access` Application per application namespace.

## Inventory Users

```bash
oc get groups
oc describe group ocp-team-security
```

Git source:

```text
platform/access-control/groups/ocp-team-security.yaml
```

Direct apply option:

```bash
oc apply -k clusters/ocp-dev
oc apply -k clusters/ocp-staging
oc apply -k clusters/ocp-prod
```

## Inspect Namespace Baseline

```bash
oc get resourcequota,limitrange,networkpolicy -n dbank-app-01-dev
```

## Check Access

```bash
kubectl auth can-i get pods -n dbank-app-01-dev --as access-reviewer --as-group ocp-team-security
kubectl auth can-i patch deployment -n dbank-app-01-dev --as access-reviewer --as-group ocp-team-sre
kubectl auth can-i create rolebindings.rbac.authorization.k8s.io -n dbank-app-01-dev --as access-reviewer --as-group ocp-team-digital-banking
```

The RoleBinding creation check should return `no`.

## Common Issues

- Missing namespace: check `platform/namespace-access/<environment>/<application>/kustomization.yaml`.
- Access not granted: check the namespace RoleBinding and group name.
- Application cannot deploy RBAC: expected; app RBAC is owned by the platform team.
- Application cannot create cluster resources: expected; the AppProject blocks cluster-wide resources.
