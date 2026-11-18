import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { fileVariantUrl } from '../fileVariant.ts';

describe('fileVariantUrl', () => {
  it('asks a stored file endpoint for the size', () => {
    assert.equal(fileVariantUrl('/api/horses/5/photo', 'THUMB'), '/api/horses/5/photo?size=THUMB');
    assert.equal(
      fileVariantUrl('http://localhost:8080/api/admissions/1/documents/2/file', 'PREVIEW'),
      'http://localhost:8080/api/admissions/1/documents/2/file?size=PREVIEW',
    );
  });

  it('keeps an existing query string', () => {
    assert.equal(fileVariantUrl('/api/auth/me/avatar?v=abc', 'THUMB'), '/api/auth/me/avatar?v=abc&size=THUMB');
  });

  it('leaves files that are not uploaded yet and missing sources alone', () => {
    assert.equal(fileVariantUrl('blob:http://localhost:3000/123', 'THUMB'), 'blob:http://localhost:3000/123');
    assert.equal(fileVariantUrl('data:image/png;base64,AAA', 'PREVIEW'), 'data:image/png;base64,AAA');
    assert.equal(fileVariantUrl(null, 'THUMB'), null);
    assert.equal(fileVariantUrl(undefined, 'THUMB'), null);
    assert.equal(fileVariantUrl('', 'THUMB'), null);
  });
});
