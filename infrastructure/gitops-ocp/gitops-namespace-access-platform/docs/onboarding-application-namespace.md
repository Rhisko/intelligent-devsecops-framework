# Onboarding Application Namespace

To onboard a new application namespace:

1. Create a folder under `platform/namespaces/<environment>/<app-name>/`.
2. Add `Namespace`, `ResourceQuota`, `LimitRange`, and baseline `NetworkPolicy` manifests.
3. Add the folder to `platform/namespaces/<environment>/kustomization.yaml`.
4. Create an application access record under `platform/access-control/applications/`.
5. Create namespace RoleBindings under `platform/access-control/bindings/<environment>/<app-name>/access.yaml`.
6. Create `platform/namespace-access/<environment>/<app-name>/kustomization.yaml` to compose namespace baseline and access binding.
7. Add the namespace access entrypoint to `platform/namespace-access/<environment>/kustomization.yaml`.
8. Add a matching Argo CD Application under `bootstrap/argocd-applications/`.
9. Add the app repository and namespace to the environment's Argo CD AppProject guardrail.
10. Run validation.

```bash
./tools/validate.sh
```

Application workload manifests remain in the application repository.
