import os
from pathlib import Path
from dotenv import load_dotenv


ML_ROOT = Path(__file__).resolve().parents[1]

load_dotenv(ML_ROOT / ".env")


def get_database_url() -> str:
    host = os.getenv("DB_HOST", "localhost")
    port = os.getenv("DB_PORT", "5432")
    name = os.getenv("DB_NAME")
    user = os.getenv("DB_USER")
    password = os.getenv("DB_PASSWORD")

    if not name or not user or not password:
        raise ValueError("Database config is incomplete. Check your .env file.")

    return f"postgresql+psycopg2://{user}:{password}@{host}:{port}/{name}"
