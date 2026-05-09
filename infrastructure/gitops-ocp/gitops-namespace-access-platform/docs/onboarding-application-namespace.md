# Onboarding Application Namespace

To onboard a new application namespace:

1. Create a folder under `platform/namespaces/<environment>/<app-name>/`.
2. Add `Namespace`, `ResourceQuota`, `LimitRange`, and baseline `NetworkPolicy` manifests.
3. Add the folder to `platform/namespaces/<environment>/kustomization.yaml`.
4. Create an application access record under `platform/access-control/applications/`.
5. Add or update OpenShift group membership under `platform/access-control/groups/`.
6. Create namespace RoleBindings under `platform/access-control/bindings/<environment>/<app-name>/access.yaml`.
7. Create `platform/namespace-access/<environment>/<app-name>/kustomization.yaml` to compose namespace baseline and access binding.
8. Add the namespace access entrypoint to `platform/namespace-access/<environment>/kustomization.yaml`.
9. Add a matching Argo CD Application under `bootstrap/argocd-applications/`.
10. Add the app repository and namespace to the environment's Argo CD AppProject guardrail.
11. Run validation.

```bash
./tools/validate.sh
```

Application workload manifests remain in the application repository.
