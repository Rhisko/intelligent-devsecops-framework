from __future__ import annotations

from pathlib import Path
import shutil


def ensure_directory(path: str | Path) -> Path:
    if not path:
        raise ValueError("Directory path must not be empty.")

    directory = Path(path).expanduser().resolve()

    if directory.exists() and not directory.is_dir():
        raise NotADirectoryError(f"Path exists but is not a directory: {directory}")

    directory.mkdir(parents=True, exist_ok=True)
    
    # Copy logo.png to the created directory
    logo_source = Path(__file__).parent / "logo.png"
    if logo_source.exists():
        shutil.copy(logo_source, directory / "logo.png")
    
    return directory