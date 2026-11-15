package com.rtms.backend.common.storage;

/**
 * Cỡ ảnh thu nhỏ một màn hình có thể xin qua tham số {@code size} của các endpoint trả file. Không truyền thì nhận bản gốc.
 * Ảnh được thu vào khung vuông cạnh {@link #maxSide()} pixel, giữ tỉ lệ và không phóng to ảnh nhỏ hơn.
 * (A smaller copy of an image a screen may ask for with the {@code size} parameter of the file endpoints. Without it the
 * original is returned. The image is fitted into a square of {@link #maxSide()} pixels, keeping its ratio and never
 * enlarging a smaller one.)
 */
public enum FileVariant {
    /** Avatar và ô ảnh nhỏ, hiển thị tới 128 px trên màn hình mật độ 2x. (Avatars and small tiles, up to 128 px at 2x.) */
    THUMB(320),
    /** Ảnh hiển thị lớn trong trang hoặc modal, tới khoảng 500 px. (Large images on a page or in a modal, up to ~500 px.) */
    PREVIEW(1024);

    private final int maxSide;

    FileVariant(int maxSide) {
        this.maxSide = maxSide;
    }

    public int maxSide() {
        return maxSide;
    }
}
