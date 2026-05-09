# Access Model

Access is granted through namespace-local `RoleBinding` resources that reference reusable platform-managed `ClusterRole` profiles.

This enables a central RBAC catalog without granting teams cluster-wide access.

## User Membership Inventory

LDAP is the source of truth for user lifecycle and group membership.

This GitOps repository is the source of truth for namespace access binding:

```text
LDAP group ocp-team-security -> RoleBinding -> ClusterRole namespace-readonly
```

To inventory users, query LDAP or the synced OpenShift groups:

```bash
oc get groups
oc describe group ocp-team-security
```

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
