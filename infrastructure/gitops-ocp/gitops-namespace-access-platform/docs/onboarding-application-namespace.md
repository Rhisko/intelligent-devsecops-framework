# Onboarding Application Namespace

To onboard a new application namespace:

1. Create a folder under `platform/namespaces/dev/<app-name>/`.
2. Add `Namespace`, `ResourceQuota`, `LimitRange`, and baseline `NetworkPolicy` manifests.
3. Add the folder to `platform/namespaces/dev/kustomization.yaml`.
4. Create an application access record under `platform/access-control/applications/`.
5. Create namespace RoleBindings under `platform/access-control/bindings/dev/`.
6. Add the app repository and namespace to the Argo CD AppProject guardrail.
7. Run validation.

```bash
./tools/validate.sh
```

Application workload manifests remain in the application repository.

