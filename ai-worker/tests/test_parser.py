import os
import json
import unittest
import pymupdf as fitz
from app.services.parser import extract_structured_fields, normalize_ueln, normalize_date, parse_sex_and_gender

BASE_PROJECT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))

class TestParser(unittest.TestCase):
    def test_normalize_ueln_cases(self):
        # Standard UELN format
        self.assertEqual(normalize_ueln("VNM22SC00000001"), "VNM22SC00000001")
        # Numeric country code UELN
        self.assertEqual(normalize_ueln("380005298842023"), "380005298842023")
        self.assertEqual(normalize_ueln("25000116359042G"), "25000116359042G")
        self.assertEqual(normalize_ueln("724002200001617"), "724002200001617")
        # OCR errors auto-correction
        self.assertEqual(normalize_ueln("VNM22SCOOOOOOO1"), "VNM22SC00000001")
        self.assertEqual(normalize_ueln("VNM18RTOOOOOOO1"), "VNM18RT00000001")
        # Unpublished / unknown should return None
        self.assertIsNone(normalize_ueln("Not published"))
        self.assertIsNone(normalize_ueln("Unpublished"))
        self.assertIsNone(normalize_ueln("Unknown"))
        self.assertIsNone(normalize_ueln("None"))
        # 15-letter english words without digits must NOT match
        self.assertIsNone(normalize_ueln("QUEMIR DE BLAZIET"))

    def test_parse_sex_and_gender(self):
        self.assertEqual(parse_sex_and_gender("Arabian / FEMALE")["sex"], "FEMALE")
        self.assertEqual(parse_sex_and_gender("MALE")["sex"], "MALE")
        self.assertEqual(parse_sex_and_gender("Mare")["sex"], "FEMALE")
        self.assertEqual(parse_sex_and_gender("Mare")["gender"], "MARE")
        self.assertEqual(parse_sex_and_gender("Stallion")["gender"], "STALLION")
        self.assertEqual(parse_sex_and_gender("Colt")["gender"], "COLT")
        self.assertEqual(parse_sex_and_gender("Filly")["gender"], "FILLY")
        self.assertEqual(parse_sex_and_gender("Gelding")["gender"], "GELDING")

    def test_all_admission_test_packs_evaluation(self):
        pack_dirs = [
            os.path.join(BASE_PROJECT_DIR, "docs", "RTMS_Admission_Test_Packs_2026-10-18"),
            os.path.join(BASE_PROJECT_DIR, "docs", "RTMS_Admission_Test_Packs"),
        ]

        for base_dir in pack_dirs:
            if not os.path.exists(base_dir):
                continue
            for folder in sorted(os.listdir(base_dir)):
                fpath = os.path.join(base_dir, folder)
                if not os.path.isdir(fpath):
                    continue
                profile_path = os.path.join(fpath, "horse-profile.json")
                if not os.path.exists(profile_path):
                    continue
                with open(profile_path, "r", encoding="utf-8") as f:
                    expected = json.load(f)

                pdfs = [os.path.join(fpath, f) for f in os.listdir(fpath) if f.endswith(".pdf")]
                merged_res = {}
                for pdf in pdfs:
                    doc = fitz.open(pdf)
                    lines = [line.strip() for page in doc for line in page.get_text().split("\n") if line.strip()]
                    res = extract_structured_fields(lines)
                    for k, v in res.items():
                        if v is not None and merged_res.get(k) is None:
                            merged_res[k] = v

                # Check primary fields
                self.assertEqual(
                    merged_res.get("name", "").strip().upper(),
                    expected.get("name", "").strip().upper(),
                    f"Failed name in {folder}"
                )
                self.assertEqual(
                    merged_res.get("registrationNumber"),
                    expected.get("registrationNumber"),
                    f"Failed UELN in {folder}"
                )
                self.assertEqual(
                    merged_res.get("breed", "").strip().upper(),
                    expected.get("breed", "").strip().upper(),
                    f"Failed breed in {folder}"
                )
                self.assertEqual(
                    merged_res.get("sex", "").strip().upper(),
                    expected.get("sex", "").strip().upper(),
                    f"Failed sex in {folder}"
                )
                self.assertEqual(
                    merged_res.get("dateOfBirth"),
                    expected.get("dateOfBirth"),
                    f"Failed DOB in {folder}"
                )

                # Check lineage
                if expected.get("sireRegistrationNumber"):
                    self.assertEqual(
                        merged_res.get("sireRegistrationNumber"),
                        expected.get("sireRegistrationNumber"),
                        f"Failed Sire UELN in {folder}"
                    )
                else:
                    self.assertIsNone(
                        merged_res.get("sireRegistrationNumber"),
                        f"Sire UELN should be None in {folder}"
                    )

                if expected.get("damRegistrationNumber"):
                    self.assertEqual(
                        merged_res.get("damRegistrationNumber"),
                        expected.get("damRegistrationNumber"),
                        f"Failed Dam UELN in {folder}"
                    )

if __name__ == "__main__":
    unittest.main()
