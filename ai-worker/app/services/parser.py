import re
from typing import Dict, Any, Optional

# Match international 15-character UELN or standard alphanumeric registration codes
# E.g. VNM22SC00000001, 380005298842023, 25000116359042G, 724002200001617
UELN_STANDARD_REGEX = re.compile(r'\b([A-Z]{3}[A-Z0-9]{12})\b', re.IGNORECASE)
UELN_NUMERIC_REGEX = re.compile(r'\b([0-9]{3}[A-Z0-9]{12})\b', re.IGNORECASE)
UELN_GENERAL_15_REGEX = re.compile(r'\b([A-Z0-9]{15})\b', re.IGNORECASE)
UELN_FLEX_REGEX = re.compile(r'\b([A-Z0-9]{10,20})\b', re.IGNORECASE)

DATE_REGEX_DMY = re.compile(r'\b([0-3]?[0-9])[/\-\.]([0-1]?[0-9])[/\-\.]([1-2][0-9]{3})\b')
DATE_REGEX_YMD = re.compile(r'\b([1-2][0-9]{3})[/\-\.]([0-1]?[0-9])[/\-\.]([0-3]?[0-9])\b')

UNPUBLISHED_UELN_KEYWORDS = [
    "not published", "unpublished", "unknown", "none", "n/a", "chua cong bo", "chưa công bố", "chua ro", "chưa rõ"
]

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
    - Chữ 'Z', 'I', 'L' ở vị trí năm sinh/số chuyển thành '2', '1'
    """
    c = candidate.strip().upper()
    if len(c) == 15 and c[:3].isalpha():
        prefix = c[:3]  # VNM, USA, GBR
        mid_year = c[3:5].replace('Z', '2').replace('O', '0').replace('I', '1').replace('L', '1')
        breed = c[5:7]
        serial = c[7:].replace('O', '0').replace('I', '1').replace('L', '1').replace('Z', '2').replace('B', '8').replace('S', '5')
        return prefix + mid_year + breed + serial
    return c

def normalize_ueln(text: str) -> Optional[str]:
    """
    Trích xuất mã định danh UELN 15 ký tự hoặc mã đăng ký ngựa hợp lệ.
    Bắt buộc chuỗi nguồn phải chứa ít nhất một chữ số để tránh nhận nhầm từ tiếng Anh thuần chữ cái.
    """
    if not text:
        return None

    # Mọi UELN hợp lệ bắt buộc phải có sẵn chữ số trong chuỗi gốc
    if not any(c.isdigit() for c in text):
        return None

    lower_text = text.lower().strip()
    if any(k in lower_text for k in UNPUBLISHED_UELN_KEYWORDS):
        return None

    # 1. Tìm trực tiếp mẫu UELN chuẩn có 3 chữ cái mã quốc gia
    m = UELN_STANDARD_REGEX.search(text)
    if m:
        val = m.group(1).upper()
        if any(c.isdigit() for c in val):
            return clean_ocr_ueln_candidate(val)

    # 2. Tìm UELN có 3 số mã quốc gia (ISO 3166 numeric, ví dụ 380 Italy, 250 France, 724 Spain)
    m = UELN_NUMERIC_REGEX.search(text)
    if m:
        val = m.group(1).upper()
        if any(c.isdigit() for c in val):
            return clean_ocr_ueln_candidate(val)

    # 3. Tìm trong chuỗi sau khi bỏ các ký tự đặc biệt
    clean = re.sub(r'[^A-Za-z0-9]', '', text)
    if len(clean) == 15 and any(c.isdigit() for c in clean):
        return clean_ocr_ueln_candidate(clean.upper())

    m15 = UELN_GENERAL_15_REGEX.search(clean)
    if m15:
        cand = clean_ocr_ueln_candidate(m15.group(1).upper())
        if any(c.isdigit() for c in cand):
            return cand

    # 4. Tìm kiếm linh hoạt với các mã độ dài từ 10 đến 20 ký tự (loại trừ từ thuần chữ cái)
    m_flex = UELN_FLEX_REGEX.search(text)
    if m_flex:
        val = m_flex.group(1).upper()
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
    m_mf = re.search(r'\b(?:sex|gender|giới tính)\s*[:\-]?>?\s*\b([mf])\b', lower)
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

COMMON_BREEDS = [
    "Arabian", "Thoroughbred", "Quarter Horse", "Appaloosa", "Warmblood",
    "Standardbred", "Morgan", "Paint Horse", "Friesian", "Andalusian"
]

def clean_pedigree_name(candidate: str) -> Optional[str]:
    """Loại bỏ nhãn rác OCR và chỉ lấy tên thật của ngựa bố/mẹ."""
    if not candidate:
        return None
    c_clean = candidate.strip()
    c_lower = c_clean.lower()
    # Danh sách các từ khóa nhãn (label) thường bị OCR đọc nhầm thành tên
    junk_labels = [
        "registered name", "registered", "redistered", "reyileteu", "registeres",
        "nunie", "nanie", "name", "ueln", "birth", "reference", "not published",
        "not verified", "pedigree", "dam", "sire", "table", "horse"
    ]
    if any(c_lower == j or c_lower.startswith(j + " ") or c_lower.endswith(" " + j) for j in junk_labels):
        return None
    # Nếu chỉ toàn ký tự số hoặc mã định danh
    if normalize_ueln(c_clean):
        return None
    # Tên chuẩn phải có độ dài hợp lý
    if len(c_clean) < 2 or len(c_clean) > 40:
        return None
    return c_clean

def fuzzy_match_breed(text: str) -> Optional[str]:
    """Nhận diện giống ngựa phổ biến kể cả khi OCR bị sai 1-2 ký tự (vd Arabiar -> Arabian)."""
    if not text:
        return None
    t_clean = text.strip()
    t_lower = t_clean.lower()
    for breed in COMMON_BREEDS:
        b_lower = breed.lower()
        if b_lower in t_lower or t_lower in b_lower:
            return breed
        # Sai lệch 1-2 ký tự
        if len(t_lower) >= 5 and len(b_lower) >= 5:
            diff = sum(1 for a, b in zip(t_lower[:len(b_lower)], b_lower) if a != b)
            if diff <= 2 and abs(len(t_lower) - len(b_lower)) <= 2:
                return breed
    return t_clean if len(t_clean) >= 3 else None

def extract_structured_fields(lines: list[str]) -> Dict[str, Any]:
    """Rule-based regex parser bóc tách các trường từ danh sách chuỗi OCR/Text."""
    data: Dict[str, Any] = {
        "name": None, "sex": None, "gender": None, "breed": None,
        "dateOfBirth": None, "registrationNumber": None, "registryName": None,
        "sireName": None, "sireRegistrationNumber": None,
        "damName": None, "damRegistrationNumber": None,
    }

    full_text = " \n ".join(lines)
    n_lines = len(lines)

    for i, line in enumerate(lines):
        line_clean = line.strip()
        lower = line_clean.lower()

        # 1. Horse Name
        if lower == "horse" or lower == "horse:" or any(k in lower for k in ["name of horse", "horse name", "tên ngựa"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = None
            if len(parts) > 1 and parts[1].strip():
                val = parts[1].split("|")[0].strip()
            elif i + 1 < n_lines:
                val = lines[i + 1].strip()

            if val and not any(k in val.lower() for k in ["field", "value", "table", "passport", "reference", "rtms", "demoonly", "admission"]):
                if not data["name"]:
                    data["name"] = val.strip()

        # 2. UELN / Registration Number
        if any(k in lower for k in ["ueln", "reg no", "registration no", "registration number", "microchip", "số đk"]):
            if "sire" not in lower and "dam" not in lower:
                parts = re.split(r'[:\-]', line_clean, maxsplit=1)
                ueln = None
                if len(parts) > 1 and parts[1].strip():
                    ueln = normalize_ueln(parts[1])
                if not ueln:
                    ueln = normalize_ueln(line_clean)
                if not ueln and i + 1 < n_lines:
                    ueln = normalize_ueln(lines[i + 1])
                if ueln and not data["registrationNumber"]:
                    data["registrationNumber"] = ueln
                    # Heuristic tên ngựa: Dòng chữ in hoa đứng ngay phía trước UELN nếu chưa có tên
                    if not data["name"] and i > 0:
                        prev_line = lines[i - 1].strip()
                        if prev_line and prev_line.isupper() and len(prev_line) >= 3 and not any(k in prev_line.lower() for k in ["rtms", "demo", "admission", "certificate", "document", "pedigree"]):
                            data["name"] = prev_line

        # 3. Sire (Ngựa cha) & Sire UELN
        if lower == "sire" or lower == "sire:" or any(k in lower for k in ["ngựa cha", "cha:"]) or lower.startswith("sire "):
            # Tìm trong 4 dòng tiếp theo để bóc tách tên và UELN
            for offset in range(1, min(5, n_lines - i)):
                cand_line = lines[i + offset].strip()
                cand_ueln = normalize_ueln(cand_line)
                if cand_ueln and not data["sireRegistrationNumber"]:
                    data["sireRegistrationNumber"] = cand_ueln
                    continue
                cand_name = clean_pedigree_name(cand_line)
                if cand_name and not data["sireName"]:
                    data["sireName"] = cand_name

        # 4. Dam (Ngựa mẹ) & Dam UELN
        if lower == "dam" or lower == "dam:" or any(k in lower for k in ["ngựa mẹ", "mẹ:"]) or lower.startswith("dam "):
            # Tìm trong 4 dòng tiếp theo để bóc tách tên và UELN
            for offset in range(1, min(5, n_lines - i)):
                cand_line = lines[i + offset].strip()
                cand_ueln = normalize_ueln(cand_line)
                if cand_ueln and not data["damRegistrationNumber"]:
                    data["damRegistrationNumber"] = cand_ueln
                    continue
                cand_name = clean_pedigree_name(cand_line)
                if cand_name and not data["damName"]:
                    data["damName"] = cand_name

        # 5. Breed & Sex
        if "breed" in lower and "sex" in lower:
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < n_lines else "")
            if "/" in val:
                b_part, s_part = [p.strip() for p in val.split("/", 1)]
                if not data["breed"]:
                    data["breed"] = fuzzy_match_breed(b_part)
                sg = parse_sex_and_gender(s_part)
                if sg["sex"] and not data["sex"]:
                    data["sex"] = sg["sex"]
                    data["gender"] = sg["gender"]
        else:
            if any(k in lower for k in ["breed", "giống"]):
                parts = re.split(r'[:\-]', line_clean, maxsplit=1)
                val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < n_lines else "")
                if val and not any(k in val.lower() for k in ["registry", "standard", "status"]):
                    if "/" in val:
                        b_part, s_part = [p.strip() for p in val.split("/", 1)]
                        if not data["breed"]:
                            data["breed"] = fuzzy_match_breed(b_part)
                        sg = parse_sex_and_gender(s_part)
                        if sg["sex"] and not data["sex"]:
                            data["sex"] = sg["sex"]
                            data["gender"] = sg["gender"]
                    elif not data["breed"]:
                        data["breed"] = fuzzy_match_breed(val)

            if any(k in lower for k in ["sex", "gender", "giới tính"]):
                sg = parse_sex_and_gender(line_clean)
                if not sg["sex"] and i + 1 < n_lines:
                    sg = parse_sex_and_gender(lines[i + 1])
                if sg["sex"] and not data["sex"]:
                    data["sex"] = sg["sex"]
                    data["gender"] = sg["gender"]

        # 6. Date of birth / Foaled / Born
        if any(k in lower for k in ["date of birth", "foaled", "dob", "ngày sinh", "born"]):
            parts = re.split(r'[:\-]', line_clean, maxsplit=1)
            val = parts[1] if len(parts) > 1 else line_clean
            dob = normalize_date(val)
            if not dob and i + 1 < n_lines:
                dob = normalize_date(lines[i + 1])
            if dob and not data["dateOfBirth"]:
                data["dateOfBirth"] = dob

        # 7. Registry / Tổ chức đăng ký
        if lower.startswith("registry") or "registry:" in lower or lower == "registry":
            if not any(k in lower for k in ["status", "number", "no.", "statement"]):
                parts = re.split(r'[:\-]', line_clean, maxsplit=1)
                val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < n_lines else "")
                if val and not any(k in val.lower() for k in ["active", "pending", "demo record", "pedigree notes", "reference date", "record purpose", "redistr"]):
                    if not data["registryName"]:
                        data["registryName"] = val

    # Global search nếu chưa thấy UELN hoặc DateOfBirth
    if not data["registrationNumber"]:
        data["registrationNumber"] = normalize_ueln(full_text)
    if not data["dateOfBirth"]:
        data["dateOfBirth"] = normalize_date(full_text)

    # Global search breed nếu chưa có
    if not data["breed"]:
        for l in lines:
            matched_b = fuzzy_match_breed(l)
            if matched_b in COMMON_BREEDS:
                data["breed"] = matched_b
                break

    return data
