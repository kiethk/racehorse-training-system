import cv2
try:
    import pymupdf as fitz
except ImportError:
    import fitz
import numpy as np
from typing import List

def pdf_to_cv2_images(file_bytes: bytes, dpi: int = 300) -> List[np.ndarray]:
    """Convert each PDF page to an OpenCV image matrix rendered at standard 300 DPI."""
    images = []
    try:
        doc = fitz.open(stream=file_bytes, filetype="pdf")
        zoom = dpi / 72.0
        matrix = fitz.Matrix(zoom, zoom)

        for page in doc:
            pix = page.get_pixmap(matrix=matrix, alpha=False)
            img_array = np.frombuffer(pix.samples, dtype=np.uint8).reshape((pix.height, pix.width, 3))
            img_bgr = cv2.cvtColor(img_array, cv2.COLOR_RGB2BGR)
            images.append(img_bgr)
        doc.close()
    except Exception:
        # Fallback: attempt to decode as a direct image file
        try:
            img = bytes_to_cv2_image(file_bytes)
            if img is not None:
                images.append(img)
        except Exception:
            pass
    return images

def bytes_to_cv2_image(file_bytes: bytes) -> np.ndarray:
    """Decode raw bytes into an OpenCV image matrix."""
    nparr = np.frombuffer(file_bytes, dtype=np.uint8)
    image = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if image is None:
        raise ValueError("Cannot decode image from bytes")
    return image

def deskew(image: np.ndarray) -> np.ndarray:
    """Deskew scanned document text."""
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

    # Only rotate if skew angle is significant (> 0.2 degrees)
    if 0.2 < abs(angle) < 45:
        (h, w) = image.shape[:2]
        center = (w // 2, h // 2)
        M = cv2.getRotationMatrix2D(center, angle, 1.0)
        image = cv2.warpAffine(image, M, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE)
    return image

def enhance_contrast(image: np.ndarray) -> np.ndarray:
    """Enhance text contrast using CLAHE on Luminance channel."""
    try:
        lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
        l_channel, a_channel, b_channel = cv2.split(lab)
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        cl = clahe.apply(l_channel)
        limg = cv2.merge((cl, a_channel, b_channel))
        return cv2.cvtColor(limg, cv2.COLOR_LAB2BGR)
    except Exception:
        return image

def preprocess_image(image: np.ndarray) -> np.ndarray:
    """Normalize image: deskew and apply gentle CLAHE contrast enhancement."""
    deskewed = deskew(image)
    enhanced = enhance_contrast(deskewed)
    return enhanced
