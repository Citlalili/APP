from __future__ import annotations

from fastapi import FastAPI, HTTPException, Query
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

sports_teams = {
    "mlb": [
        {"id": 1, "name": "New York Yankees", "city": "New York", "conference": "AL", "league": "mlb"},
        {"id": 2, "name": "Los Angeles Dodgers", "city": "Los Angeles", "conference": "NL", "league": "mlb"},
        {"id": 3, "name": "Boston Red Sox", "city": "Boston", "conference": "AL", "league": "mlb"},
    ],
    "nfl": [
        {"id": 4, "name": "Kansas City Chiefs", "city": "Kansas City", "conference": "AFC", "league": "nfl"},
        {"id": 5, "name": "Dallas Cowboys", "city": "Dallas", "conference": "NFC", "league": "nfl"},
        {"id": 6, "name": "San Francisco 49ers", "city": "San Francisco", "conference": "NFC", "league": "nfl"},
    ],
    "soccer": [
        {"id": 7, "name": "Real Madrid", "city": "Madrid", "conference": "LaLiga", "league": "soccer"},
        {"id": 8, "name": "Barcelona", "city": "Barcelona", "conference": "LaLiga", "league": "soccer"},
        {"id": 9, "name": "Manchester City", "city": "Manchester", "conference": "Premier League", "league": "soccer"},
    ],
}


@app.get("/")
def read_root() -> dict[str, str]:
    return {"message": "APP API ready", "status": "running"}


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok", "service": "app-api"}


@app.get("/sports/leagues")
def sports_leagues() -> list[str]:
    return ["mlb", "nfl", "soccer"]


@app.get("/sports/teams")
def sports_teams_endpoint(
    league: str = Query(default="mlb", description="mlb | nfl | soccer")
) -> dict[str, object]:
    normalized = league.lower().strip()
    if normalized == "all":
        return {"league": "all", "teams": [team for league_key in sports_teams.values() for team in league_key]}

    if normalized not in sports_teams:
        raise HTTPException(status_code=404, detail="Liga no disponible. Usa: mlb, nfl o soccer")

    return {"league": normalized, "teams": sports_teams[normalized]}


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
