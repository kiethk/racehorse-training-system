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
                    label = self.model.names.get(cls_id, str(cls_id)) if hasattr(self.model, "names") else str(cls_id)
                    boxes.append({
                        "bbox": xyxy.tolist(),
                        "conf": round(conf, 4),
                        "cls": cls_id,
                        "label": label
                    })
            # Sắp xếp thứ tự đọc tự nhiên: ưu tiên từ trên xuống dưới (y1), sau đó từ trái sang phải (x1)
            boxes.sort(key=lambda item: (item["bbox"][1], item["bbox"][0]))
            return boxes
        except Exception:
            return []

layout_detector = LayoutDetector()
