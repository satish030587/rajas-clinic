from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import (
    appointments, auth, catalog, investigations, patients, recall, visits,
)

app = FastAPI(
    title="Rajas Clinic Backend",
    version="3.0.0",
    description=(
        "Backend for Rajas Homoeo Care. Two devices — reception and doctor — "
        "share one patient record; see homeopathy-clinic-app-spec-v3.md."
    ),
)

# The Android clients talk to this directly; a browser-based admin view may follow.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router)
app.include_router(patients.router)
app.include_router(visits.router)
app.include_router(recall.router)
app.include_router(appointments.router)
app.include_router(investigations.router)
app.include_router(catalog.router)


@app.get("/health", tags=["health"])
def health_check():
    return {"status": "ok"}
