/**
 * Horse types — REFERENCE IMPLEMENTATION
 *
 * Đây là ví dụ mẫu (reference) về cách định nghĩa TypeScript types cho một entity.
 * Mỗi entity backend cần có một file types tương ứng trong thư mục này.
 *
 * Convention:
 *   - Interface tên PascalCase, map 1-1 với entity Java (camelCase field names)
 *   - Các field nullable ở DB → dùng `string | null` ở đây, không dùng `string | undefined`
 *   - ApiResponse<T> là wrapper chung cho mọi response từ backend
 */

export interface Horse {
  id: number;
  name: string;
  breed: string | null;
  dateOfBirth: string | null;
  pedigreeInfo: string | null;
  currentStatus: string;
  stableLocation: string | null;
  ownerId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
}