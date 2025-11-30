import os, secrets
from dotenv import load_dotenv

load_dotenv()

DB_CONFIG = dict(
    host=os.getenv("DB_HOST", "localhost"),
    user=os.getenv("DB_USER", "door_user"),
    password=os.getenv("DB_PASS", "door_user_door_user"),
    database=os.getenv("DB_NAME", "smart_door_db"),
    port=int(os.getenv("DB_PORT", 3306)),
)

JWT_SECRET      = os.getenv("JWT_SECRET", secrets.token_hex(32))
JWT_ALGORITHM   = "HS256"
JWT_EXP_MINUTES = 60 * 24 * 7  # 1 week

FIREBASE_PROJECT_ID = "cm2211group8project"
FIRBEASE_URL = f"https://fcm.googleapis.com/v1/projects/{FIREBASE_PROJECT_ID}/messages:send"
FIREBASE_KEY_LOCATION = "config/service_account.json"