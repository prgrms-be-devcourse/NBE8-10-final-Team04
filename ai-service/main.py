from fastapi import FastAPI
from app.api.chat import router as chat_router 

app = FastAPI()

# chat API 등록
app.include_router(chat_router)

@app.get("/")
def root():
    return {"message": "AI Service is running"}