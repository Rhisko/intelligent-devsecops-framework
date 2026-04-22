from __future__ import annotations


def infer_language_from_component(component: str) -> str:
    component = component.lower()
    if component.endswith('.py') or component.endswith('requirements.txt'):
        return 'python'
    if component.endswith('.ts') or component.endswith('.tsx'):
        return 'typescript'
    if component.endswith('.js') or component.endswith('.jsx'):
        return 'javascript'
    if component.endswith('.java') or 'pom.xml' in component or 'build.gradle' in component:
        return 'java'
    if component.endswith('.go') or component.endswith('go.mod'):
        return 'go'
    return 'generic'
