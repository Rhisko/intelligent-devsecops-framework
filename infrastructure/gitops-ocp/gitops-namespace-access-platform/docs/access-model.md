# Access Model

Access is granted through namespace-local `RoleBinding` resources that reference reusable platform-managed `ClusterRole` profiles.

This enables a central RBAC catalog without granting teams cluster-wide access.

## User Membership Inventory

OpenShift `Group` resources are stored in Git under:

```text
platform/access-control/groups/
```

This makes membership changes auditable in Git. For example, adding `user-a` to `ocp-team-security` is a normal pull request against `platform/access-control/groups/ocp-team-security.yaml`.

If enterprise identity is synced from LDAP, Active Directory, Keycloak, or another IdP, that IdP should remain the source of truth instead. Do not let both GitOps and IdP sync own the same group membership.

## Access Matrix

The same team ownership pattern is applied to dev, staging, and prod namespaces unless a production-specific exception is approved.

| Application | Namespace | Owners | Operators | Viewers / Auditors |
| --- | --- | --- | --- | --- |
| dbank-app-01 | dbank-app-01-`<env>` | ocp-team-digital-banking | ocp-team-platform, ocp-team-sre | ocp-team-security |
| dbank-app-02 | dbank-app-02-`<env>` | ocp-team-card | ocp-team-platform | ocp-team-security, ocp-team-digital-banking |
| dbank-app-03 | dbank-app-03-`<env>` | ocp-team-loan | ocp-team-sre | ocp-team-security |
| dbank-app-04 | dbank-app-04-`<env>` | ocp-team-risk | ocp-team-platform, ocp-team-sre | ocp-team-security, ocp-team-audit |
| dbank-app-05 | dbank-app-05-`<env>` | ocp-team-integration | ocp-team-platform | ocp-team-security, ocp-team-digital-banking |

Application owners cannot manage RBAC, namespaces, cluster roles, cluster role bindings, or security context constraints.
