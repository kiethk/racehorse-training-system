import re
from typing import Dict, Any, Optional

# Match international 15-character UELN or standard alphanumeric registration codes
# E.g. VNM22SC00000001, USA201012345678, GBR18RT00000001, 840003012345678
UELN_STANDARD_REGEX = re.compile(r'\b([A-Z]{3}[A-Z0-9]{12})\b', re.IGNORECASE)
UELN_NUMERIC_REGEX = re.compile(r'\b([0-9]{3}[A-Z0-9]{12})\b', re.IGNORECASE)
UELN_GENERAL_15_REGEX = re.compile(r'\b([A-Z0-9]{15})\b', re.IGNORECASE)
UELN_FLEX_REGEX = re.compile(r'\b([A-Z0-9]{10,20})\b', re.IGNORECASE)

DATE_REGEX_DMY = re.compile(r'\b([0-3]?[0-9])[/\-\.]([0-1]?[0-9])[/\-\.]([1-2][0-9]{3})\b')
DATE_REGEX_YMD = re.compile(r'\b([1-2][0-9]{3})[/\-\.]([0-1]?[0-9])[/\-\.]([0-3]?[0-9])\b')

def normalize_date(text: str) -> Optional[str]:
    """Chuẩn hóa các kiểu ngày về chuẩn YYYY-MM-DD."""
    if not text:
        return None
    m_dmy = DATE_REGEX_DMY.search(text)
    if m_dmy:
        day, month, year = m_dmy.groups()
        return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"

    m_ymd = DATE_REGEX_YMD.search(text)
    if m_ymd:
        year, month, day = m_ymd.groups()
        return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"
    return None

def clean_ocr_ueln_candidate(candidate: str) -> str:
    """
    Sửa các lỗi nhận dạng ký tự phổ biến của OCR trong mã UELN:
    - Chữ 'O' ở cụm số serial chuyển thành '0' (ví dụ VNM22SCOOOOOOO1 -> VNM22SC00000001)
    - Chữ 'Z', 'I', 'L' ở vị trí năm sinh/số chuyển thành '2', '1' (ví dụ VNMI8 -> VNM18, VNM2Z -> VNM22)
    """
    c = candidate.strip().upper()
    if len(c) == 15 and c[:3].isalpha():
        prefix = c[:3]  # VNM, USA, GBR
        mid_year = c[3:5].replace('Z', '2').replace('O', '0').replace('I', '1').replace('L', '1')  # 22, 18, 19
        breed = c[5:7]  # SC, RT, GB
        serial = c[7:].replace('O', '0').replace('I', '1').replace('L', '1').replace('Z', '2').replace('B', '8').replace('S', '5')
        return prefix + mid_year + breed + serial
    return c

def normalize_ueln(text: str) -> Optional[str]:
    """Trích xuất mã định danh UELN 15 ký tự hoặc mã đăng ký ngựa."""
    if not text:
        return None

    # 1. Tìm trực tiếp mẫu UELN chuẩn có 3 chữ cái mã quốc gia
    m = UELN_STANDARD_REGEX.search(text)
    if m:
        return clean_ocr_ueln_candidate(m.group(1).upper())

    # 2. Tìm UELN có 3 số mã quốc gia
    m = UELN_NUMERIC_REGEX.search(text)
    if m:
        return clean_ocr_ueln_candidate(m.group(1).upper())

    # 3. Thử tìm trong chuỗi sau khi bỏ các ký tự đặc biệt
    clean = re.sub(r'[^A-Za-z0-9]', '', text)
    if len(clean) == 15:
        cleaned = clean_ocr_ueln_candidate(clean.upper())
        if UELN_STANDARD_REGEX.match(cleaned) or UELN_GENERAL_15_REGEX.match(cleaned):
            return cleaned

    m15 = UELN_GENERAL_15_REGEX.search(clean)
    if m15:
        return clean_ocr_ueln_candidate(m15.group(1).upper())

    # 4. Tìm kiếm linh hoạt với các mã độ dài từ 10 đến 20 ký tự
    m_flex = UELN_FLEX_REGEX.search(text)
    if m_flex:
        val = m_flex.group(1).upper()
        # Loại trừ các từ tiếng Anh dài không phải mã định danh
        if any(c.isdigit() for c in val):
            return clean_ocr_ueln_candidate(val)

    return None

def parse_sex_and_gender(text: str) -> Dict[str, Optional[str]]:
    """
    Phân loại giới tính (MALE/FEMALE) và enum chi tiết theo chuẩn HORSE_SEX.md.
    Áp dụng quy tắc Sex Safeguard: không tự suy diễn nếu không chắc chắn 100%.
    """
    if not text:
        return {"sex": None, "gender": None}
    lower = text.lower()

    # Dùng regex word boundary để tránh match substring ngẫu nhiên (ví dụ chữ 'm' trong 'demo')
    if re.search(r'\b(colt)\b', lower):
        return {"sex": "MALE", "gender": "COLT"}
    if re.search(r'\b(stallion|ngựa đực)\b', lower):
        return {"sex": "MALE", "gender": "STALLION"}
    if re.search(r'\b(gelding|thiến)\b', lower):
        return {"sex": "MALE", "gender": "GELDING"}
    if re.search(r'\b(filly)\b', lower):
        return {"sex": "FEMALE", "gender": "FILLY"}
    if re.search(r'\b(mare|ngựa cái)\b', lower):
        return {"sex": "FEMALE", "gender": "MARE"}
    if re.search(r'\b(male|đực)\b', lower):
        return {"sex": "MALE", "gender": None}
    if re.search(r'\b(female|cái)\b', lower):
        return {"sex": "FEMALE", "gender": None}

    # Cho phép M hoặc F nếu đứng trực tiếp sau nhãn sex/gender
    m_mf = re.search(r'\b(?:sex|gender|giới tính)\s*[:\-]?\s*\b([mf])\b', lower)
    if m_mf:
        val = m_mf.group(1)
        return {"sex": "MALE" if val == "m" else "FEMALE", "gender": None}

    return {"sex": None, "gender": None}

def extract_structured_fields(lines: list[str]) -> Dict[str, Any]:
    """Rule-based regex parser bóc tách các trường từ danh sách chuỗi OCR/Text."""
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

        # 1. UELN / Registration Number
        # Lưu ý: Bỏ qua nếu là nhãn Sire UELN hoặc Dam UELN
        if any(k in lower for k in ["ueln", "reg no", "registration no", "registration number", "microchip", "số đk"]):
            if "sire" not in lower and "dam" not in lower:
                # Có thể nằm cùng dòng sau dấu hai chấm hoặc dấu gạch
                parts = re.split(r'[:\-]', line_clean, maxsplit=1)
                ueln = None
                if len(parts) > 1 and parts[1].strip():
                    ueln = normalize_ueln(parts[1])
                if not ueln:
                    ueln = normalize_ueln(line_clean)
                if not ueln and i + 1 < len(lines):
                    ueln = normalize_ueln(lines[i + 1])
                if ueln and not data["registrationNumber"]:
                    data["registrationNumber"] = ueln

        # 2. Sire UELN
        if "sire ueln" in lower or ("sire" in lower and any(k in lower for k in ["reg", "ueln", "no."])):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < len(lines) else "")
            sire_ueln = normalize_ueln(val)
            if sire_ueln:
                data["sireRegistrationNumber"] = sire_ueln

        # 3. Dam UELN
        if "dam ueln" in lower or ("dam" in lower and any(k in lower for k in ["reg", "ueln", "no."])):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < len(lines) else "")
            dam_ueln = normalize_ueln(val)
            if dam_ueln:
                data["damRegistrationNumber"] = dam_ueln

        # 4. Horse Name
        if any(k in lower for k in ["name of horse", "horse name", "tên ngựa", "horse:"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = None
            if len(parts) > 1 and parts[1].strip():
                # Nếu có phần ueln đi kèm như "Horse: Silver Comet | UELN: VNM22..."
                subparts = parts[1].split("|")
                val = subparts[0].strip()
            elif i + 1 < len(lines):
                val = lines[i + 1].strip()

            if val and not any(k in val.lower() for k in ["field", "value", "table", "passport"]):
                data["name"] = val.upper()

        # 5. Date of birth / Foaled
        if any(k in lower for k in ["date of birth", "foaled", "dob", "ngày sinh"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1] if len(parts) > 1 else line_clean
            dob = normalize_date(val)
            if not dob and i + 1 < len(lines):
                dob = normalize_date(lines[i + 1])
            if dob and not data["dateOfBirth"]:
                data["dateOfBirth"] = dob

        # 6. Sex / Gender
        if any(k in lower for k in ["sex", "gender", "giới tính"]):
            sg = parse_sex_and_gender(line_clean)
            if not sg["sex"] and i + 1 < len(lines):
                sg = parse_sex_and_gender(lines[i + 1])
            if sg["sex"]:
                data["sex"] = sg["sex"]
                data["gender"] = sg["gender"]

        # 7. Breed
        if any(k in lower for k in ["breed", "giống"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < len(lines) else "")
            if val and not any(k in val.lower() for k in ["thoroughbred registry", "standard"]):
                data["breed"] = val

        # 8. Registry / Tổ chức đăng ký
        if lower.startswith("registry") or "registry:" in lower or lower == "registry":
            if not any(k in lower for k in ["status", "number", "no."]):
                parts = re.split(r'[:\-]', line_clean, maxsplit=1)
                val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < len(lines) else "")
                if val and not any(k in val.lower() for k in ["active", "pending", "demo record", "pedigree notes"]):
                    data["registryName"] = val

        # 9. Sire (Ngựa cha)
        if any(k in lower for k in ["sire", "ngựa cha", "cha:"]) and "ueln" not in lower:
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < len(lines) else "")
            if val and not data["sireName"] and not any(k in val.lower() for k in ["ueln", "vnm"]):
                data["sireName"] = val.upper()
                sire_ueln = normalize_ueln(val)
                if sire_ueln:
                    data["sireRegistrationNumber"] = sire_ueln

        # 10. Dam (Ngựa mẹ)
        if any(k in lower for k in ["dam", "ngựa mẹ", "mẹ:"]) and "ueln" not in lower:
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < len(lines) else "")
            if val and not data["damName"] and not any(k in val.lower() for k in ["ueln", "vnm"]):
                data["damName"] = val.upper()
                dam_ueln = normalize_ueln(val)
                if dam_ueln:
                    data["damRegistrationNumber"] = dam_ueln

    # Global search nếu chưa thấy UELN hoặc DateOfBirth
    if not data["registrationNumber"]:
        data["registrationNumber"] = normalize_ueln(full_text)
    if not data["dateOfBirth"]:
        data["dateOfBirth"] = normalize_date(full_text)

    return data
