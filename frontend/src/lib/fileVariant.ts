/**
 * Smaller copies of a stored image, served by the same protected endpoints with `?size=`.
 * - THUMB: fits 320 px, for avatars and small tiles (up to 128 px on screen).
 * - PREVIEW: fits 1024 px, for large images on a page or in a modal.
 * Leave the size out for the original: lightbox zoom, download, open in a new tab, PDFs.
 * The backend returns the original when it cannot resize (PDFs, files on local disk), so asking never fails.
 */
export type FileVariant = 'THUMB' | 'PREVIEW';

/** The endpoint URL for `variant` of a stored file. Blob and data URLs (files not uploaded yet) are returned as they are. */
export function fileVariantUrl(source: string, variant: FileVariant): string;
export function fileVariantUrl(source: string | null | undefined, variant: FileVariant): string | null;
export function fileVariantUrl(source: string | null | undefined, variant: FileVariant): string | null {
  if (!source) return null;
  if (/^(blob:|data:)/.test(source)) return source;
  const hashAt = source.indexOf('#');
  const path = hashAt >= 0 ? source.slice(0, hashAt) : source;
  const hash = hashAt >= 0 ? source.slice(hashAt) : '';
  return `${path}${path.includes('?') ? '&' : '?'}size=${variant}${hash}`;
}
