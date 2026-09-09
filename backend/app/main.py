from fastapi import FastAPI

app = FastAPI(title="APP API", version="1.0.0")


@app.get("/")
def read_root() -> dict[str, str]:
    return {"message": "APP API ready", "status": "running"}


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok", "service": "app-api"}
