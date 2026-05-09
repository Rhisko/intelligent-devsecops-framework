# RBAC Profiles

The RBAC catalog defines reusable `ClusterRole` profiles.

## namespace-readonly

Read-only access to common namespace resources, including pods, logs, events, services, config, workloads, routes, and ingress.

## namespace-operator

Operational access for SRE and platform teams. Operators can inspect workloads, read logs and events, patch workloads, and scale deployments or statefulsets.

## namespace-owner-limited

Application owner access to manage standard namespaced application resources. This profile intentionally excludes RBAC, namespaces, cluster roles, cluster role bindings, and security context constraints.

## namespace-deployer

Automation-oriented access for deployment pipelines or GitOps controllers that need to apply standard namespaced application resources.

## security-auditor

Read-oriented access for audit and security inspection, including network policies and workload metadata.

No profile uses wildcard verbs or wildcard resources.

