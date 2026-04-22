from __future__ import annotations

from pathlib import Path


class PromptLoader:
    def __init__(self, prompts_root: str = 'prompts'):
        self.prompts_root = Path(prompts_root)

    def load(self, prompt_name: str) -> tuple[str, str]:
        system_text = (self.prompts_root / prompt_name / 'system.md').read_text(encoding='utf-8')
        user_text = (self.prompts_root / prompt_name / 'user.md').read_text(encoding='utf-8')
        return system_text, user_text
