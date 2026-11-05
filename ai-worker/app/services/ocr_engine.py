import logging
import warnings
import numpy as np
from app.config import settings
from typing import List, Tuple

# Suppress PyTorch quantization deprecation warnings in EasyOCR
warnings.filterwarnings("ignore", category=UserWarning, module="torch")

logger = logging.getLogger("uvicorn")


class OcrEngine:
    def __init__(self):
        self.engine_type = "none"
        self.reader = None
        self._init_engine()

    def _init_engine(self):
        # 1. Try PaddleOCR first (if supported on host OS/runtime)
        try:
            from paddleocr import PaddleOCR
            try:
                reader = PaddleOCR(use_angle_cls=True, lang='en', show_log=False)
            except TypeError:
                reader = PaddleOCR(lang='en')
            # Verify C++ runtime compatibility (e.g. PIR / oneDNN on Windows)
            test_img = np.zeros((32, 32, 3), dtype=np.uint8)
            reader.ocr(test_img)
            self.reader = reader
            self.engine_type = "paddle"
            logger.info("Initialized PaddleOCR engine successfully.")
            return
        except Exception as e:
            logger.info(f"PaddleOCR is not available on this platform ({e}). Falling back to EasyOCR...")

        # 2. Fallback to EasyOCR
        try:
            import easyocr
            self.reader = easyocr.Reader(['en'], gpu=False, verbose=False)
            self.engine_type = "easyocr"
            logger.info("Initialized EasyOCR engine successfully.")
            return
        except Exception as e:
            logger.warning(f"EasyOCR initialization failed: {e}. OCR text detection disabled.")
            self.engine_type = "none"

    def read_text(self, image: np.ndarray) -> List[Tuple[str, float]]:
        """Nhận dạng text từ ảnh, trả về danh sách [(text, confidence)]."""
        results = []
        if self.engine_type == "paddle" and self.reader is not None:
            ocr_res = self.reader.ocr(image, cls=True)
            if ocr_res and ocr_res[0]:
                for line in ocr_res[0]:
                    text = line[1][0]
                    conf = float(line[1][1])
                    results.append((text, conf))
        elif self.engine_type == "easyocr" and self.reader is not None:
            ocr_res = self.reader.readtext(image)
            for item in ocr_res:
                text = item[1]
                conf = float(item[2])
                results.append((text, conf))
        return results


ocr_engine = OcrEngine()
