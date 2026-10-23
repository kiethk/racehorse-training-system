from pydantic import BaseModel
from typing import Optional


class ExtractedData(BaseModel):
    name: Optional[str] = None
    sex: Optional[str] = None                     # MALE / FEMALE (raw from cert)
    gender: Optional[str] = None                  # COLT / FILLY / STALLION / MARE / GELDING
    breed: Optional[str] = None
    dateOfBirth: Optional[str] = None            # YYYY-MM-DD
    registrationNumber: Optional[str] = None     # Chuẩn UELN 15 ký tự
    registryName: Optional[str] = None
    sireName: Optional[str] = None
    sireRegistrationNumber: Optional[str] = None
    damName: Optional[str] = None
    damRegistrationNumber: Optional[str] = None
    confidenceScore: Optional[float] = None
    hasConflict: bool = False
    conflictMessage: Optional[str] = None

class OcrResponse(BaseModel):
    success: bool
    message: str
    data: ExtractedData