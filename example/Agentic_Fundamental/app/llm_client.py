import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from schemas import SecurityAnalysis

load_dotenv()

def get_llm():
    llm = ChatOpenAI(
        model="gpt-4.1",
        temperature=0
    )
    
    structured_llm = llm.with_structured_output(SecurityAnalysis)
    return structured_llm
