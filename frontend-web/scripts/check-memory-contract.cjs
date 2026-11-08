const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const filename = path.resolve(__dirname, '../src/features/memory/api/contract.ts');
const js = ts.transpileModule(fs.readFileSync(filename, 'utf8'), { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
const exportsObject = {};
vm.runInNewContext(js, { exports: exportsObject, module: { exports: exportsObject } }, { filename });
const { selectionError, toMemory, DEFAULT_UPLOAD_LIMITS: limits } = exportsObject;
const file = (size, type = 'image/png') => ({ size, type });

test('selection accepts exactly the byte boundary and rejects one byte more', () => {
  assert.equal(selectionError([file(limits.maxFileBytes)], limits), null);
  assert.match(selectionError([file(limits.maxFileBytes + 1)], limits), /MB/);
});
test('selection bounds file count, remaining memory quota, and aggregate bytes', () => {
  assert.match(selectionError(Array(11).fill(file(1)), limits), /10/);
  assert.match(selectionError(Array(3).fill(file(1)), limits, 2), /2/);
  assert.equal(selectionError(Array(5).fill(file(limits.maxFileBytes)), limits), null);
  assert.match(selectionError(Array(6).fill(file(limits.maxFileBytes)), limits), /Tổng/);
});
test('selection rejects empty files and unsupported types', () => {
  assert.match(selectionError([file(0)], limits), /trống/);
  assert.match(selectionError([file(1, 'image/svg+xml')], limits), /JPEG/);
  assert.match(selectionError([file(1, 'application/pdf')], limits), /JPEG/);
});
test('normalization preserves every photo, its real ID, display order, and edit version', () => {
  const memory = toMemory({ id: 'memory', userId: 'user', tripId: null, placeId: null, note: null, version: 4,
    experiencedAt: '2026-11-04T00:00:00Z', createdAt: '2026-11-04T00:00:00Z', updatedAt: '2026-11-04T00:00:00Z',
    coverImageUrl: 'https://example.com/a.png', mediaCount: 2, media: [
      { id: 'a', url: 'https://example.com/a.png', displayOrder: 0, createdAt: '2026-11-04T00:00:00Z' },
      { id: 'b', url: 'https://example.com/b.png', displayOrder: 1, createdAt: '2026-11-04T00:00:00Z' },
    ] });
  assert.equal(memory.version, 4); assert.equal(memory.media.length, 2);
  assert.equal(memory.media[1].id, 'b'); assert.equal(memory.media[1].sortOrder, 1);
  assert.equal(memory.coverMediaUrl, 'https://example.com/a.png'); assert.equal(memory.tripId, '');
});
