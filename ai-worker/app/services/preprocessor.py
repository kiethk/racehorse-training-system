import cv2
import fitz
import numpy as np
from typing import List

def pdf_to_cv2_images(file_bytes: bytes, dpi: int = 300) -> List[np.ndarray]:
    """Chuyển đổi từng trang PDF sang ma trận ảnh OpenCV chuẩn 300 DPI."""
    images = []
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    zoom = dpi / 72.0
    matrix = fitz.Matrix(zoom, zoom)

    for page in doc:
        pix = page.get_pixmap(matrix=matrix, alpha=False)
        img_array = np.frombuffer(pix.samples, dtype=np.uint8).reshape((pix.height, pix.width, 3))
        img_bgr = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGRA)
        images.append(img_bgr)
    doc.close()
    return images

def bytes_to_cv2_image(file_bytes: bytes) -> np.ndarray:
    """Đọc ảnh từ raw bytes sang ma trận OpenCV."""
    nparr = np.frombuffer(file_bytes, dtype=np.uint8)
    image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if image is None:
        raise ValueError("Cannot decode image from bytes")
    return image

def deskew(image: np.ndarray) -> np.ndarray:
    """Khử góc nghiêng (deskew) của văn bản scan."""
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)[1]
    coords = np.column_stack(np.where(thresh > 0))
    if len(coords) == 0:
        return image
    angle = cv2.minAreaRect(coords)[-1]
    if angle < -45:
        angle = -(90 + angle)
    else:
        angle = -angle

    # Chỉ xoay nếu góc lệch đáng kể (> 0.2 độ)
    if 0.2 < abs(angle) < 45:
        (h, w) = image.shape[:2]
        center = (w // 2, h // 2)
        M = cv2.getRotationMatrix2D(center, angle, 1.0)
        image = cv2.warpAffine(image, M, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
    return image

def preprocess_image(image: np.ndarray) -> np.ndarray:
    """Chuẩn hóa ảnh: deskew và tăng tương phản CLAHE nhẹ."""
    deskewed = deskew(image)
    return deskewed


