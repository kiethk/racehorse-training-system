import re
from typing import Dict, Any, Optional

UELN_REGEX = re.compile(r'\b([0-9]{6}[A-Z0-9]{9})\b', re.IGNORECASE)
DATE_REGEX_DMY = re.compile(r'\b([0-3]?[0-9])[/\-\.]([0-1]?[0-9])[/\-\.]([1-2][0-9]{3})\b')
DATE_REGEX_YMD = re.compile(r'\b([1-2][0-9]{3})[/\-\.]([0-1]?[0-9])[/\-\.]([0-3]?[0-9])\b')

def normalize_date(text: str) -> Optional[str]:
    """Chuẩn hóa các kiểu ngày về chuẩn YYYY-MM-DD."""
    m_dmy = DATE_REGEX_DMY.search(text)
    if m_dmy:
        day, month, year = m_dmy.groups()
        return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"

    m_ymd = DATE_REGEX_YMD.search(text)
    if m_ymd:
        year, month, day = m_ymd.groups()
        return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"
    return None

def normalize_ueln(text: str) -> Optional[str]:
    """Trích xuất mã định danh UELN 15 ký tự."""
    clean = re.sub(r'[^A-Za-z0-9]', '', text)
    m = UELN_REGEX.search(clean)
    if m:
        return m.group(1).upper()
    return None

def parse_sex_and_gender(text: str) -> Dict[str, Optional[str]]:
    """Phân loại giới tính (MALE/FEMALE) và enum chi tiết theo chuẩn HORSE_SEX.md."""
    lower = text.lower()
    if any(k in lower for k in ["colt"]):
        return {"sex": "MALE", "gender": "COLT"}
    if any(k in lower for k in ["stallion", "ngựa đực"]):
        return {"sex": "MALE", "gender": "STALLION"}
    if any(k in lower for k in ["gelding", "thiến"]):
        return {"sex": "MALE", "gender": "GELDING"}
    if any(k in lower for k in ["filly"]):
        return {"sex": "FEMALE", "gender": "FILLY"}
    if any(k in lower for k in ["mare", "ngựa cái"]):
        return {"sex": "FEMALE", "gender": "MARE"}
    if any(k in lower for k in ["male", "đực", "m"]):
        return {"sex": "MALE", "gender": None}
    if any(k in lower for k in ["female", "cái", "f"]):
        return {"sex": "FEMALE", "gender": None}
    return {"sex": None, "gender": None}

def extract_structured_fields(lines: list[str]) -> Dict[str, Any]:
    """Rule-based regex parser bóc tách các trường từ danh sách chuỗi OCR."""
    data: Dict[str, Any] = {
        "name": None, "sex": None, "gender": None, "breed": None,
        "dateOfBirth": None, "registrationNumber": None, "registryName": None,
        "sireName": None, "sireRegistrationNumber": None,
        "damName": None, "damRegistrationNumber": None,
    }

    full_text = " \n ".join(lines)

    for i, line in enumerate(lines):
        line_clean = line.strip()
        lower = line_clean.lower()

        # UELN / Registration Number
        if any(k in lower for k in ["ueln", "reg no", "registration", "microchip", "số đk"]):
            ueln = normalize_ueln(line_clean)
            if not ueln and i + 1 < len(lines):
                ueln = normalize_ueln(lines[i + 1])
            if ueln and not data["registrationNumber"]:
                data["registrationNumber"] = ueln

        # Horse Name
        if any(k in lower for k in ["name of horse", "horse name", "tên ngựa"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            if len(parts) > 1 and parts[1].strip():
                data["name"] = parts[1].strip().upper()
            elif i + 1 < len(lines):
                data["name"] = lines[i + 1].strip().upper()

        # Date of birth / Foaled
        if any(k in lower for k in ["date of birth", "foaled", "dob", "ngày sinh"]):
            dob = normalize_date(line_clean)
            if not dob and i + 1 < len(lines):
                dob = normalize_date(lines[i + 1])
            if dob and not data["dateOfBirth"]:
                data["dateOfBirth"] = dob

        # Sex / Gender
        if any(k in lower for k in ["sex", "gender", "giới tính"]):
            sg = parse_sex_and_gender(line_clean)
            if not sg["sex"] and i + 1 < len(lines):
                sg = parse_sex_and_gender(lines[i + 1])
            if sg["sex"]:
                data["sex"] = sg["sex"]
                data["gender"] = sg["gender"]

        # Breed
        if any(k in lower for k in ["breed", "giống"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            if len(parts) > 1 and parts[1].strip():
                data["breed"] = parts[1].strip()
            elif i + 1 < len(lines):
                data["breed"] = lines[i + 1].strip()

        # Sire (Ngựa cha)
        if any(k in lower for k in ["sire", "ngựa cha", "cha:"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 else (lines[i+1].strip() if i+1 < len(lines) else "")
            if val and not data["sireName"]:
                data["sireName"] = val.upper()
                sire_ueln = normalize_ueln(val)
                if sire_ueln: data["sireRegistrationNumber"] = sire_ueln

        # Dam (Ngựa mẹ)
        if any(k in lower for k in ["dam", "ngựa mẹ", "mẹ:"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 else (lines[i+1].strip() if i+1 < len(lines) else "")
            if val and not data["damName"]:
                data["damName"] = val.upper()
                dam_ueln = normalize_ueln(val)
                if dam_ueln: data["damRegistrationNumber"] = dam_ueln

    # Global search nếu chưa thấy UELN hoặc DateOfBirth
    if not data["registrationNumber"]:
        data["registrationNumber"] = normalize_ueln(full_text)
    if not data["dateOfBirth"]:
        data["dateOfBirth"] = normalize_date(full_text)
    if not data["sex"]:
        sg = parse_sex_and_gender(full_text)
        data["sex"] = sg["sex"]
        data["gender"] = sg["gender"]

    return data