from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="APP API", version="1.0.0")


class LoginRequest(BaseModel):
    email: str
    password: str


@app.get("/")
def read_root() -> dict[str, str]:
    return {"message": "APP API ready", "status": "running"}


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok", "service": "app-api"}


@app.post("/login")
def login(credentials: LoginRequest) -> dict[str, object]:
    valid_email = "admin@app.com"
    valid_password = "123456"

    if credentials.email == valid_email and credentials.password == valid_password:
        return {
            "token": "demo-token-123",
            "user": {
                "email": credentials.email,
                "name": "Administrador",
                "role": "admin",
            },
        }

    raise HTTPException(status_code=401, detail="Credenciales inválidas")
