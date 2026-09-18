from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str
    secret_key: str

    # Auth
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60 * 12  # a clinic session is one evening

    # Uploads — investigation scans and clinical photos
    upload_dir: str = "uploads"
    max_upload_bytes: int = 15 * 1024 * 1024

    class Config:
        env_file = ".env"


settings = Settings()
