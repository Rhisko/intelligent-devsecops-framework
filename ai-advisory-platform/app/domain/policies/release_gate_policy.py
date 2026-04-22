from __future__ import annotations


def baseline_release_recommendation(critical_count: int, major_count: int) -> str:
    if critical_count > 0:
        return 'HOLD'
    if major_count >= 5:
        return 'REVIEW'
    return 'GO'
