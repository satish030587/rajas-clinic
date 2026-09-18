from fastapi import FastAPI

app = FastAPI(title="Rajas Clinic Backend")

@app.get("/health")
def health_check():
    return {"status": "ok"}