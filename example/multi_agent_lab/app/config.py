import os
from dotenv import load_dotenv

load_dotenv()

class Settings:
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    MODEL_NAME = "gpt-4.1-mini"
    MAX_ROUNDS = 3
    SCORE_THRESHOLD = 8.0

settings = Settings()