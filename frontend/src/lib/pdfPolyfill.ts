'use client';

/**
 * Polyfill for TC39 Uint8Array.prototype.toHex and Uint8Array.fromHex
 * Required by pdfjs-dist 6.x when resolving PDF document fingerprints
 * on documents that do not contain an /ID trailer (such as generated PDFs).
 */
declare global {
  interface Uint8Array {
    toHex?(): string;
  }
  interface Uint8ArrayConstructor {
    fromHex?(hex: string): Uint8Array;
  }
}

if (typeof Uint8Array !== 'undefined') {
  const proto = Uint8Array.prototype as { toHex?: () => string };
  if (!proto.toHex) {
    proto.toHex = function (this: Uint8Array): string {
      let hex = '';
      for (let i = 0; i < this.length; i++) {
        hex += this[i].toString(16).padStart(2, '0');
      }
      return hex;
    };
  }

  const ctor = Uint8Array as unknown as { fromHex?: (hex: string) => Uint8Array };
  if (!ctor.fromHex) {
    ctor.fromHex = function (hex: string): Uint8Array {
      if (hex.length % 2 !== 0) throw new TypeError('Invalid hex string length');
      const bytes = new Uint8Array(hex.length / 2);
      for (let i = 0; i < bytes.length; i++) {
        bytes[i] = parseInt(hex.substring(i * 2, i * 2 + 2), 16);
      }
      return bytes;
    };
  }
}

export {};
