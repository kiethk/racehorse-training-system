import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { isImageDocument, isImageFile } from '../../shared/lib/documentKind.ts';

const doc = (documentType: string, originalFileName: string | null, fileUrl = '/api/admissions/1/documents/9/file') =>
  ({ documentType, originalFileName, fileUrl });

describe('Admission document kind', () => {
  it('treats horse photos as images whatever the file name', () => {
    assert.equal(isImageDocument(doc('HORSE_PHOTO', null)), true);
  });

  it('recognises image files by extension, ignoring case', () => {
    assert.equal(isImageDocument(doc('VACCINATION_RECORD', 'card.JPG')), true);
    assert.equal(isImageDocument(doc('HEALTH_CERTIFICATE', 'scan.webp')), true);
    assert.equal(isImageDocument(doc('HEALTH_CERTIFICATE', 'scan.jpeg')), true);
  });

  it('keeps PDFs and unknown files out of the gallery', () => {
    assert.equal(isImageDocument(doc('REGISTRATION_DOCUMENT', 'papers.pdf')), false);
    assert.equal(isImageDocument(doc('REGISTRATION_DOCUMENT', null)), false);
  });

  it('falls back to the stored path when the original name is missing', () => {
    assert.equal(isImageDocument(doc('REGISTRATION_DOCUMENT', null, '/uploads/x/photo.png?v=2')), true);
  });

  it('previews only browser-displayable picked files', () => {
    assert.equal(isImageFile({ type: 'image/png' }), true);
    assert.equal(isImageFile({ type: 'application/pdf' }), false);
  });
});
