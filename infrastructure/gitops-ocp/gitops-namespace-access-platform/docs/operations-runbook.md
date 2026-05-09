# Operations Runbook

## Render Cluster State

```bash
kubectl kustomize clusters/ocp-dev
```

## Apply Cluster State

```bash
oc apply -k clusters/ocp-dev
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

- Missing namespace: check `platform/namespaces/dev/kustomization.yaml`.
- Access not granted: check the namespace RoleBinding and group name.
- Application cannot deploy RBAC: expected; app RBAC is owned by the platform team.
- Application cannot create cluster resources: expected; the AppProject blocks cluster-wide resources.
