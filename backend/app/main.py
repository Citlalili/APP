from __future__ import annotations

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="APP API", version="1.0.0")


class LoginRequest(BaseModel):
    email: str
    password: str


class RegisterRequest(BaseModel):
    email: str
    password: str


class TaskCreateRequest(BaseModel):
    text: str
    category: str = "General"
    done: bool = False


users_db: dict[str, dict[str, str]] = {}
tasks_db: list[dict[str, object]] = [
    {"id": 1, "text": "Diseñar la pantalla principal", "category": "Diseño", "done": False},
    {"id": 2, "text": "Revisar requisitos del backend", "category": "Trabajo", "done": True},
]


@app.get("/")
def read_root() -> dict[str, str]:
    return {"message": "APP API ready", "status": "running"}


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok", "service": "app-api"}


@app.post("/register")
def register(payload: RegisterRequest) -> dict[str, object]:
    email = payload.email.strip().lower()
    password = payload.password.strip()

    if not email or not password:
        raise HTTPException(status_code=400, detail="Email y contraseña son obligatorios")

    if "@" not in email:
        raise HTTPException(status_code=400, detail="Email inválido")

    if email in users_db:
        raise HTTPException(status_code=409, detail="Este usuario ya existe")

    users_db[email] = {"email": email, "password": password}

    return {
        "token": f"token-{email.split('@')[0]}-demo",
        "user": {
            "email": email,
            "name": email.split("@")[0].title(),
            "role": "user",
        },
    }


@app.post("/login")
def login(credentials: LoginRequest) -> dict[str, object]:
    email = credentials.email.strip().lower()
    password = credentials.password.strip()

    user = users_db.get(email)
    if user is None or user["password"] != password:
        if email == "admin@app.com" and password == "123456":
            return {
                "token": "demo-token-123",
                "user": {
                    "email": email,
                    "name": "Administrador",
                    "role": "admin",
                },
            }
        raise HTTPException(status_code=401, detail="Credenciales inválidas")

    return {
        "token": f"token-{email.split('@')[0]}-demo",
        "user": {
            "email": email,
            "name": email.split("@")[0].title(),
            "role": "user",
        },
    }


@app.get("/tasks")
def list_tasks() -> list[dict[str, object]]:
    return tasks_db


@app.post("/tasks")
def create_task(payload: TaskCreateRequest) -> dict[str, object]:
    task = {
        "id": (tasks_db[-1]["id"] if tasks_db else 0) + 1,
        "text": payload.text.strip(),
        "category": payload.category.strip() or "General",
        "done": payload.done,
    }
    if not task["text"]:
        raise HTTPException(status_code=400, detail="La tarea no puede estar vacía")
    tasks_db.append(task)
    return task
