from typing import Optional, List
import numpy as np
import uvicorn
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.middleware.cors import CORSMiddleware

try:
    import pymupdf as fitz
except ImportError:
    import fitz

from app.config import settings
from app.schemas import OcrResponse, ExtractedData
from app.services.layout_detector import layout_detector
from app.services.ocr_engine import ocr_engine
from app.services.parser import extract_structured_fields
from app.services.preprocessor import pdf_to_cv2_images, bytes_to_cv2_image, preprocess_image

app = FastAPI(title="RTMS AI OCR Worker", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

def extract_direct_pdf_lines(file_bytes: bytes) -> List[str]:
    """Extract direct digital text layer from PDF file if present."""
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

        # High-quality digital text layer detected (> 5 lines with identity keywords)
        has_rich_text_layer = (
            len(direct_lines) >= 5 and
            any(k in " ".join(direct_lines).lower() for k in ["horse", "ueln", "registration", "certificate", "pedigree"])
        )

        if has_rich_text_layer:
            # Prioritize digital text layer: near-instant speed, 100% precision, zero OCR noise
            parsed = extract_structured_fields(direct_lines)
            avg_conf = 0.98
        else:
            # 1. Convert to image matrix
            if is_pdf:
                cv_images = pdf_to_cv2_images(file_bytes, dpi=300)
                target_image = cv_images[0] if cv_images else None
            else:
                target_image = bytes_to_cv2_image(file_bytes)

            if target_image is None:
                raise HTTPException(status_code=400, detail="Cannot read image from uploaded file")

            # 2. Preprocessing (deskew, enhance contrast)
            processed_image = preprocess_image(target_image)

            # 3. Layout Detection (YOLOv8 DocLayNet)
            boxes = layout_detector.detect_regions(processed_image)

            # 4. OCR text recognition (Region-based OCR following layout, filtering Pictures)
            all_lines = []
            all_confidences = []

            if boxes:
                h_img, w_img = processed_image.shape[:2]
                valid_boxes = [b for b in boxes if b.get("label") not in ["Picture", "Formula"]]
                for box in valid_boxes:
                    bx = box["bbox"]
                    pad = 4
                    x1 = max(0, bx[0] - pad)
                    y1 = max(0, bx[1] - pad)
                    x2 = min(w_img, bx[2] + pad)
                    y2 = min(h_img, bx[3] + pad)
                    crop = processed_image[y1:y2, x1:x2]
                    if crop.size > 0:
                        crop_ocr = ocr_engine.read_text(crop)
                        for text, conf in crop_ocr:
                            all_lines.append(text)
                            all_confidences.append(conf)

            # Fallback if layout detector caught no bounding boxes or text
            if not all_lines:
                ocr_results = ocr_engine.read_text(processed_image)
                all_lines = [text for text, _ in ocr_results]
                all_confidences = [conf for _, conf in ocr_results]

            lines = all_lines
            avg_conf = float(np.mean(all_confidences)) if all_confidences else 0.0

            # 5. Parsing & regex field normalization
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
    uvicorn.run("app.main:app", host=settings.HOST, port=settings.PORT, reload=True)
