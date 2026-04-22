from __future__ import annotations


def component_to_path(component: str) -> str:
    if ":" not in component:
        return component
    return component.split(":", 1)[1]


def normalize_whitespace(value: str) -> str:
    return " ".join((value or "").split())
