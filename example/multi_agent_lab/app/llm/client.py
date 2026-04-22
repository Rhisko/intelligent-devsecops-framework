from langchain_openai import ChatOpenAI
from app.config import settings

def get_llm():
    return ChatOpenAI(
        model=settings.MODEL_NAME,
        temperature=0.3,
    )