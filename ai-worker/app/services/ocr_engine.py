import numpy as np
from app.config import settings
from paddleocr import PaddleOCR
from typing import List, Tuple


class OcrEngine:
    def __init__(self):
        self.engine_type = "none"
        self.reader = None
        self._init_engine()

    def _init_engine(self):
        try:
            self.reader = PaddleOCR(use_angle_cls=True, lang='en', show_log = False)
            self.engine_type = "paddle"
            return
        except Exception:
            pass

        try:
            import easyocr
            self.reader = easyocr.Reader(['en', 'vi'], gpu=False)
            self.engine_type = "easyocr"
            return
        except Exception:
            pass

    def read_text(self, image: np.ndarray) -> List[Tuple[str, float]]:
        """Nhận dạng text từ ảnh, trả về danh sách [(text, confidence)]."""
        results = []
        if self.engine_type == "paddle":
            ocr_res = self.reader.ocr(image, cls=True)
            if ocr_res and ocr_res[0]:
                for line in ocr_res[0]:
                    text = line[1][0]
                    conf = float(line[1][1])
                    results.append((text, conf))
        elif self.engine_type == "easyocr":
            ocr_res = self.reader.readtext(image)
            for item in ocr_res:
                text = item[1]
                conf = float(item[2])
                results.append((text, conf))
        return results

ocr_engine = OcrEngine()
