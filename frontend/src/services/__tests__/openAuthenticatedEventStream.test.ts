import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

// api.ts reads NEXT_PUBLIC_API_URL when the module is first evaluated, so the
// variable has to be set before the dynamic import below runs.
process.env.NEXT_PUBLIC_API_URL ??= 'http://localhost:8080';

const { openAuthenticatedEventStream } = await import('../api.ts');

type StreamEvent = { event: string; data: string };

function streamOf(chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  return new ReadableStream<Uint8Array>({
    start(controller) {
      for (const chunk of chunks) {
        controller.enqueue(encoder.encode(chunk));
      }
      controller.close();
    },
  });
}

async function withFetchStub(fetchImpl: () => Promise<Response>, run: () => Promise<void>) {
  const original = globalThis.fetch;
  globalThis.fetch = fetchImpl as unknown as typeof fetch;
  try {
    await run();
  } finally {
    globalThis.fetch = original;
  }
}

function okResponse(chunks: string[]): Response {
  return { ok: true, status: 200, body: streamOf(chunks) } as unknown as Response;
}

describe('SSE parser for the authenticated event stream', () => {
  it('1. Parses a frame split across chunk boundaries and skips frames without data', async () => {
    const events: StreamEvent[] = [];

    await withFetchStub(
      async () => okResponse(['event: urgent-assigned\ndata: {"scheduleId":4', '2}\n\n', 'event: no-data\n\n']),
      async () => {
        await openAuthenticatedEventStream('/api/vet/urgent/stream', {
          onEvent: (event, data) => events.push({ event, data }),
        });
      },
    );

    assert.deepEqual(events, [{ event: 'urgent-assigned', data: '{"scheduleId":42}' }]);
  });

  it('2. Reads CRLF frames, joins multi-line data and resolves when the stream closes', async () => {
    const events: StreamEvent[] = [];

    await withFetchStub(
      async () => okResponse(['event: x\r\ndata: y\r\n\r\n', 'data: line1\ndata: line2\n\n']),
      async () => {
        await openAuthenticatedEventStream('/api/vet/urgent/stream', {
          onEvent: (event, data) => events.push({ event, data }),
        });
      },
    );

    assert.deepEqual(events, [
      { event: 'x', data: 'y' },
      { event: 'message', data: 'line1\nline2' },
    ]);
  });

  it('3. Rejects when the request fails', async () => {
    await withFetchStub(
      async () => {
        throw new Error('network down');
      },
      async () => {
        await assert.rejects(
          () => openAuthenticatedEventStream('/api/vet/urgent/stream', { onEvent: () => {} }),
          /network down/,
        );
      },
    );
  });
});
