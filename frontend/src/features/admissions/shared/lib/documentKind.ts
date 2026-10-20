/** Minimal shape needed to tell an image document from other files. */
interface DocumentLike {
  documentType: string;
  fileUrl: string;
  originalFileName: string | null;
}

const IMAGE_EXTENSION = /\.(jpe?g|png|webp)$/i;

/**
 * Whether a stored admission document is an image. The API does not return the content type, so this relies on
 * the document type (horse photos are always images) and the file name; anything else is treated as a file
 * (PDF or unknown) and keeps the regular document preview.
 */
export function isImageDocument(document: DocumentLike): boolean {
  if (document.documentType === 'HORSE_PHOTO') return true;
  const name = document.originalFileName ?? document.fileUrl.split('?')[0];
  return IMAGE_EXTENSION.test(name);
}

/** Whether a stored admission document is a PDF, judged by its file name (the API has no content type). */
export function isPdfDocument(document: DocumentLike): boolean {
  if (isImageDocument(document)) return false;
  const name = document.originalFileName ?? document.fileUrl.split('?')[0];
  return /\.pdf$/i.test(name);
}

/** Whether a file picked in the browser can be previewed as an image. */
export function isImageFile(file: { type: string }): boolean {
  return ['image/jpeg', 'image/png', 'image/webp'].includes(file.type);
}
