try:
    import pymupdf as fitz
except ImportError:
    import fitz
import numpy as np
from app.schemas import OcrResponse, ExtractedData
from app.services.layout_detector import layout_detector
from app.services.ocr_engine import ocr_engine
from app.services.parser import extract_structured_fields
from app.services.preprocessor import pdf_to_cv2_images, bytes_to_cv2_image, preprocess_image
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional, List

app = FastAPI(title="RTMS AI OCR Worker", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def extract_direct_pdf_lines(file_bytes: bytes) -> List[str]:
    """Trích xuất text layer trực tiếp từ file PDF nếu có."""
    lines = []
    try:
        doc = fitz.open(stream=file_bytes, filetype="pdf")
        for page in doc:
            page_text = page.get_text()
            for line in page_text.split("\n"):
                cleaned = line.strip()
                if cleaned:
                    lines.append(cleaned)
    except Exception:
        pass
    return lines

@app.get("/health")
def health_check():
    return {"status": "UP", "ocrEngine": ocr_engine.engine_type}

@app.post("/internal/v1/ocr/extract", response_model=OcrResponse)
async def extract_document_info(
        file: UploadFile = File(...),
        documentType: Optional[str] = Form(None)
):
    try:
        file_bytes = await file.read()
        filename = (file.filename or "").lower()
        is_pdf = filename.endswith(".pdf") or file.content_type == "application/pdf"

        direct_lines = []
        if is_pdf:
            direct_lines = extract_direct_pdf_lines(file_bytes)

        # Nếu PDF có digital text layer chất lượng cao (> 5 dòng và chứa từ khóa định danh)
        has_rich_text_layer = (
            len(direct_lines) >= 5 and
            any(k in " ".join(direct_lines).lower() for k in ["horse", "ueln", "registration", "certificate", "pedigree"])
        )

        if has_rich_text_layer:
            # Ưu tiên digital text layer: tốc độ cực nhanh, chính xác 100%, không bị nhiễu OCR
            parsed = extract_structured_fields(direct_lines)
            avg_conf = 0.98
        else:
            # 1. Chuyển đổi thành ma trận ảnh
            if is_pdf:
                cv_images = pdf_to_cv2_images(file_bytes, dpi=300)
                target_image = cv_images[0] if cv_images else None
            else:
                target_image = bytes_to_cv2_image(file_bytes)

            if target_image is None:
                raise HTTPException(status_code=400, detail="Cannot read image from uploaded file")

            # 2. Tiền xử lý (Deskew, tăng độ nét)
            processed_image = preprocess_image(target_image)

            # 3. Layout Detection (YOLOv8)
            _ = layout_detector.detect_regions(processed_image)

            # 4. OCR nhận dạng chữ
            ocr_results = ocr_engine.read_text(processed_image)
            lines = [text for text, _ in ocr_results]
            confidences = [conf for _, conf in ocr_results]
            avg_conf = float(np.mean(confidences)) if confidences else 0.0

            # 5. Parsing & Regex chuẩn hóa trường dữ liệu
            parsed = extract_structured_fields(lines)

        extracted = ExtractedData(
            name=parsed.get("name"),
            sex=parsed.get("sex"),
            gender=parsed.get("gender"),
            breed=parsed.get("breed"),
            dateOfBirth=parsed.get("dateOfBirth"),
            registrationNumber=parsed.get("registrationNumber"),
            registryName=parsed.get("registryName"),
            sireName=parsed.get("sireName"),
            sireRegistrationNumber=parsed.get("sireRegistrationNumber"),
            damName=parsed.get("damName"),
            damRegistrationNumber=parsed.get("damRegistrationNumber"),
            confidenceScore=round(avg_conf, 2),
            hasConflict=False,
            conflictMessage=None
        )

        return OcrResponse(
            success=True,
            message="Document extracted successfully",
            data=extracted
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"OCR extraction failed: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    from app.config import settings
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
