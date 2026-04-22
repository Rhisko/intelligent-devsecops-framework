from ruamel.yaml import YAML
from llm_client import get_llm

yaml = YAML()

def load_yaml(path: str) -> str:
    with open(path, "r") as f:
        return f.read()

def analyze_manifest(manifest_text: str):
    llm = get_llm()

    prompt = f"""
You are a DevSecOps Architecture Professional.

Analyze the following YAML manifest.
Identify security risks and classify overall risk level.

Manifest:
{manifest_text}
"""

    response = llm.invoke(prompt)
    return response

if __name__ == "__main__":
    manifest = load_yaml("samples/poc_deployment_hpa.yaml")
    result = analyze_manifest(manifest)

    print(result.model_dump_json(indent=2))
