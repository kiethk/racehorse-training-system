package com.rtms.backend.medical.enums;

/** Đường dùng thuốc theo danh mục lâm sàng (WF-04). */
public enum MedicationRoute {
    ORAL,
    TOPICAL,
    INJECTION_IV,
    INJECTION_IM,
    FEED_ADDITIVE;

    /** IV/IM là thủ thuật chuyên môn — chỉ Vet/Tech được tiêm (B5). */
    public boolean requiresVeterinarian() {
        return this == INJECTION_IV || this == INJECTION_IM;
    }
}
