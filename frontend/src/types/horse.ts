/**
 * Horse types — REFERENCE IMPLEMENTATION
 *
 * This is a reference example of how to define TypeScript types for an entity.
 * Each backend entity needs a matching types file in this folder.
 *
 * Convention:
 *   - PascalCase interface name, mapped 1-1 to the Java entity (camelCase field names)
 *   - DB-nullable fields → use `string | null` here, not `string | undefined`
 *   - ApiResponse<T> is the common wrapper for every backend response
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