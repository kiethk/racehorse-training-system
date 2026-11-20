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
    """Normalize date strings into standard YYYY-MM-DD format."""
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
    Correct common OCR character recognition errors in UELN codes:
    - Replace letter 'O' in numeric/serial parts with '0'
    - Replace letters 'Z', 'I', 'L' in numeric positions with '2', '1'
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
    Extract 15-character UELN or valid equine registration number.
    The source string must contain digits to avoid matching purely alphabetic words.
    """
    if not text:
        return None

    # Valid UELN must contain digits in the raw string
    if not any(c.isdigit() for c in text):
        return None

    lower_text = text.lower().strip()
    if any(k in lower_text for k in UNPUBLISHED_UELN_KEYWORDS):
        return None

    # 1. Search directly for standard UELN pattern with 3-letter country code
    m = UELN_STANDARD_REGEX.search(text)
    if m:
        val = m.group(1).upper()
        if any(c.isdigit() for c in val):
            return clean_ocr_ueln_candidate(val)

    # 2. Search for UELN with numeric ISO country code (e.g. 704 Vietnam, 250 France)
    m = UELN_NUMERIC_REGEX.search(text)
    if m:
        val = m.group(1).upper()
        if any(c.isdigit() for c in val):
            return clean_ocr_ueln_candidate(val)

    # 3. Clean special characters and search in alphanumeric string
    clean = re.sub(r'[^A-Za-z0-9]', '', text)
    if len(clean) == 15 and any(c.isdigit() for c in clean):
        return clean_ocr_ueln_candidate(clean.upper())

    m15 = UELN_GENERAL_15_REGEX.search(clean)
    if m15:
        cand = clean_ocr_ueln_candidate(m15.group(1).upper())
        if any(c.isdigit() for c in cand):
            return cand

    # 4. Flexible length search (10-20 chars) excluding purely alphabetic tokens
    m_flex = UELN_FLEX_REGEX.search(text)
    if m_flex:
        val = m_flex.group(1).upper()
        if any(c.isdigit() for c in val):
            return clean_ocr_ueln_candidate(val)

    return None

def parse_sex_and_gender(text: str) -> Dict[str, Optional[str]]:
    """
    Classify sex (MALE/FEMALE) and detailed gender according to HORSE_SEX.md.
    Applies Sex Safeguard rule: do not infer unless 100% certain.
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

    # Allow M or F if positioned directly after sex/gender label
    m_mf = re.search(r'\b(?:sex|gender|giới tính)\s*[:\-]?>?\s*\b([mf])\b', lower)
    if m_mf:
        val = m_mf.group(1)
        return {"sex": "MALE" if val == "m" else "FEMALE", "gender": None}

    return {"sex": None, "gender": None}


COMMON_BREEDS = [
    "Arabian", "Thoroughbred", "Quarter Horse", "Appaloosa", "Warmblood",
    "Standardbred", "Morgan", "Paint Horse", "Friesian", "Andalusian"
]

def clean_pedigree_name(candidate: str) -> Optional[str]:
    """Clean OCR label noise and return real pedigree name for sire/dam."""
    if not candidate:
        return None
    c_clean = candidate.strip()
    c_lower = c_clean.lower()
    # Label keywords commonly misidentified as names by OCR
    junk_labels = [
        "registered name", "registered", "redistered", "reyileteu", "registeres",
        "nunie", "nanie", "name", "ueln", "birth", "reference", "not published",
        "not verified", "pedigree", "dam", "sire", "table", "horse"
    ]
    if any(c_lower == j or c_lower.startswith(j + " ") or c_lower.endswith(" " + j) for j in junk_labels):
        return None
    # If purely numeric or registration code
    if normalize_ueln(c_clean):
        return None
    # Standard name length sanity check
    if len(c_clean) < 2 or len(c_clean) > 40:
        return None
    return c_clean

def fuzzy_match_breed(text: str) -> Optional[str]:
    """Recognize common horse breeds with 1-2 character OCR fault tolerance (e.g. Arabiar -> Arabian)."""
    if not text:
        return None
    t_clean = text.strip()
    t_lower = t_clean.lower()
    for breed in COMMON_BREEDS:
        b_lower = breed.lower()
        if b_lower in t_lower or t_lower in b_lower:
            return breed
        # 1-2 character tolerance
        if len(t_lower) >= 5 and len(b_lower) >= 5:
            diff = sum(1 for a, b in zip(t_lower[:len(b_lower)], b_lower) if a != b)
            if diff <= 2 and abs(len(t_lower) - len(b_lower)) <= 2:
                return breed
    return t_clean if len(t_clean) >= 3 else None

def extract_structured_fields(lines: list[str]) -> Dict[str, Any]:
    """Rule-based regex parser extracting structured fields from OCR/text lines."""
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
                    # Heuristic for horse name: Uppercase line immediately preceding UELN if name is not yet found
                    if not data["name"] and i > 0:
                        prev_line = lines[i - 1].strip()
                        if prev_line and prev_line.isupper() and len(prev_line) >= 3 and not any(k in prev_line.lower() for k in ["rtms", "demo", "admission", "certificate", "document", "pedigree"]):
                            data["name"] = prev_line

        # 3. Sire and Sire UELN
        if lower == "sire" or lower == "sire:" or any(k in lower for k in ["ngựa cha", "cha:"]) or lower.startswith("sire "):
            # Look ahead up to 4 lines to extract sire name and UELN
            for offset in range(1, min(5, n_lines - i)):
                cand_line = lines[i + offset].strip()
                cand_ueln = normalize_ueln(cand_line)
                if cand_ueln and not data["sireRegistrationNumber"]:
                    data["sireRegistrationNumber"] = cand_ueln
                    continue
                cand_name = clean_pedigree_name(cand_line)
                if cand_name and not data["sireName"]:
                    data["sireName"] = cand_name

        # 4. Dam and Dam UELN
        if lower == "dam" or lower == "dam:" or any(k in lower for k in ["ngựa mẹ", "mẹ:"]) or lower.startswith("dam "):
            # Look ahead up to 4 lines to extract dam name and UELN
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

        # 7. Registry organization
        if lower.startswith("registry") or "registry:" in lower or lower == "registry":
            if not any(k in lower for k in ["status", "number", "no.", "statement"]):
                parts = re.split(r'[:\-]', line_clean, maxsplit=1)
                val = parts[1].strip() if len(parts) > 1 and parts[1].strip() else (lines[i+1].strip() if i+1 < n_lines else "")
                if val and not any(k in val.lower() for k in ["active", "pending", "demo record", "pedigree notes", "reference date", "record purpose", "redistr"]):
                    if not data["registryName"]:
                        data["registryName"] = val

    # Fallback global search if UELN or DateOfBirth has not been identified
    if not data["registrationNumber"]:
        data["registrationNumber"] = normalize_ueln(full_text)
    if not data["dateOfBirth"]:
        data["dateOfBirth"] = normalize_date(full_text)

    # Fallback global search for breed
    if not data["breed"]:
        for l in lines:
            matched_b = fuzzy_match_breed(l)
            if matched_b in COMMON_BREEDS:
                data["breed"] = matched_b
                break

    return data
