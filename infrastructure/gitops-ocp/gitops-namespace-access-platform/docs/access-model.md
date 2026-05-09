# Access Model

Access is granted through namespace-local `RoleBinding` resources that reference reusable platform-managed `ClusterRole` profiles.

This enables a central RBAC catalog without granting teams cluster-wide access.

## Dev Access Matrix

| Application | Namespace | Owners | Operators | Viewers / Auditors |
| --- | --- | --- | --- | --- |
| dbank-app-01 | dbank-app-01-dev | ocp-team-digital-banking | ocp-team-platform, ocp-team-sre | ocp-team-security |
| dbank-app-02 | dbank-app-02-dev | ocp-team-card | ocp-team-platform | ocp-team-security, ocp-team-digital-banking |
| dbank-app-03 | dbank-app-03-dev | ocp-team-loan | ocp-team-sre | ocp-team-security |
| dbank-app-04 | dbank-app-04-dev | ocp-team-risk | ocp-team-platform, ocp-team-sre | ocp-team-security, ocp-team-audit |
| dbank-app-05 | dbank-app-05-dev | ocp-team-integration | ocp-team-platform | ocp-team-security, ocp-team-digital-banking |

Application owners cannot manage RBAC, namespaces, cluster roles, cluster role bindings, or security context constraints.

