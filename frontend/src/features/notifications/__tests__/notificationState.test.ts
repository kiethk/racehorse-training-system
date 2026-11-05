import { describe, it, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import type { NotificationItem } from '../types.ts';
import {
  cursorFromItem,
  mergeNotifications,
  readPersistedNotifications,
  writePersistedNotifications,
  NOTIFICATIONS_STATE_VERSION,
  MAX_NOTIFICATIONS,
} from '../lib/notificationState.ts';

function makeItem(id: number, createdAt: string, read = false, notificationType = 'ROUTINE_ASSIGNED'): NotificationItem {
  return {
    id,
    notificationType,
    title: `Title ${id}`,
    message: `Message ${id}`,
    referenceType: null,
    referenceId: null,
    read,
    createdAt,
  };
}

describe('mergeNotifications', () => {
  it('1. deduplicates by id, keeping the incoming version', () => {
    const prev = [makeItem(1, '2026-10-25T10:00:00Z', false)];
    const incoming = [makeItem(1, '2026-10-25T10:00:00Z', true)];
    const merged = mergeNotifications(prev, incoming, MAX_NOTIFICATIONS);
    assert.equal(merged.length, 1);
    assert.equal(merged[0].read, true);
  });

  it('2. sorts newest-first by createdAt descending, then id descending', () => {
    const prev = [makeItem(1, '2026-10-25T08:00:00Z')];
    const incoming = [
      makeItem(2, '2026-10-25T09:00:00Z'),
      makeItem(3, '2026-10-25T09:00:00Z'),
      makeItem(4, '2026-10-25T10:00:00Z'),
    ];
    const merged = mergeNotifications(prev, incoming, MAX_NOTIFICATIONS);
    assert.deepEqual(merged.map((n) => n.id), [4, 3, 2, 1]);
  });

  it('3. caps the result at the requested limit', () => {
    const prev = [makeItem(1, '2026-10-25T08:00:00Z')];
    const incoming = [
      makeItem(2, '2026-10-25T09:00:00Z'),
      makeItem(3, '2026-10-25T10:00:00Z'),
    ];
    const merged = mergeNotifications(prev, incoming, 2);
    assert.equal(merged.length, 2);
    assert.deepEqual(merged.map((n) => n.id), [3, 2]);
  });

  it('4. returns a sorted copy even when incoming is empty', () => {
    const prev = [makeItem(2, '2026-10-25T09:00:00Z'), makeItem(1, '2026-10-25T10:00:00Z')];
    const merged = mergeNotifications(prev, [], MAX_NOTIFICATIONS);
    assert.deepEqual(merged.map((n) => n.id), [1, 2]);
    // Verify a new array is returned.
    assert.notStrictEqual(merged, prev);
  });
});

describe('cursorFromItem', () => {
  it('5. builds a cursor from createdAt and id', () => {
    const item = makeItem(42, '2026-10-25T10:00:00Z');
    assert.equal(cursorFromItem(item), '2026-10-25T10:00:00Z_42');
  });
});

describe('notification persistence', () => {
  let store: Record<string, string> = {};

  beforeEach(() => {
    store = {};
    globalThis.window = {
      localStorage: {
        getItem: (key: string) => store[key] ?? null,
        setItem: (key: string, value: string) => { store[key] = value; },
        removeItem: (key: string) => { delete store[key]; },
      },
    } as unknown as Window & typeof globalThis;
  });

  it('6. round-trips state for the same user', () => {
    const state = {
      v: NOTIFICATIONS_STATE_VERSION,
      items: [makeItem(1, '2026-10-25T10:00:00Z')],
      nextPageCursor: '2026-10-25T09:00:00Z_2',
      hasMore: true,
      lastSeenCursor: '2026-10-25T10:00:00Z_1',
    };
    writePersistedNotifications('user-1', state);
    const restored = readPersistedNotifications('user-1');
    assert.notEqual(restored, null);
    assert.deepEqual(restored, state);
  });

  it('7. wipes data on version mismatch', () => {
    writePersistedNotifications('user-1', {
      v: 999,
      items: [makeItem(1, '2026-10-25T10:00:00Z')],
      nextPageCursor: null,
      hasMore: false,
      lastSeenCursor: '',
    } as unknown as Parameters<typeof writePersistedNotifications>[1]);
    const restored = readPersistedNotifications('user-1');
    assert.equal(restored, null);
    assert.equal(store['rtms_notifications_state_user-1'], undefined);
  });

  it('8. returns null and wipes on parse failure', () => {
    store['rtms_notifications_state_user-1'] = 'not-json';
    const restored = readPersistedNotifications('user-1');
    assert.equal(restored, null);
    assert.equal(store['rtms_notifications_state_user-1'], undefined);
  });

  it('9. returns null when no user id is provided', () => {
    writePersistedNotifications('user-1', {
      v: NOTIFICATIONS_STATE_VERSION,
      items: [makeItem(1, '2026-10-25T10:00:00Z')],
      nextPageCursor: null,
      hasMore: false,
      lastSeenCursor: '',
    });
    assert.equal(readPersistedNotifications(null), null);
  });
});
