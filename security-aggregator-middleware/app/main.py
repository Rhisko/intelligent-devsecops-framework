from fastapi import FastAPI

app = FastAPI(title="Security Aggregator Middleware")

@app.get("/health")
def health():
    return {"status": "ok"}
