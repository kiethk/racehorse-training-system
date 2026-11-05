import os
from pydantic import BaseModel

class Settings(BaseModel):
    HOST: str = os.getenv("AI_WORKER_HOST", "0.0.0.0")
    PORT: int = int(os.getenv("AI_WORKER_PORT", "8000"))
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "info")
    YOLO_MODEL_PATH: str = os.getenv("YOLO_MODEL_PATH", "yolov8n.pt")
    OCR_ENGINE: str = os.getenv("OCR_ENGINE", "auto")  # 'paddle', 'easyocr', or 'auto'
    MIN_CONFIDENCE: float = float(os.getenv("MIN_CONFIDENCE", "0.6"))

settings = Settings()