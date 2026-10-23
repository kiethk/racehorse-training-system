import numpy as np
from app.config import settings
from ultralytics import YOLO


class LayoutDetector:
    def __init__(self):
        try:
            self.model = YOLO(settings.YOLO_MODEL_PATH)
        except Exception:
            self.model = None

    def detect_regions(self, image: np.ndarray):
        """Dùng YOLOv8 để detect các vùng (header, table, field blocks)."""
        if self.model is None:
            return []
        try:
            results = self.model(image, conf=settings.MIN_CONFIDENCE, verbose=False)
            boxes = []
            for r in results:
                for b in r.boxes:
                    xyxy = b.xyxy[0].cpu().numpy().astype(int)
                    conf = float(b.conf[0].cpu().numpy())
                    cls_id = int(b.cls[0].cpu().numpy())
                    boxes.append({"bbox": xyxy, "conf": conf, "cls": cls_id})
            return boxes
        except Exception:
            return []

layout_detector = LayoutDetector()
