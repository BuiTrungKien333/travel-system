const http = require('http');
const state = pm.state;
const key = 'cloudinary-media.assets';
const failureKey = 'cloudinary-media.mode';

function send(res, code, value) {
  res.writeHead(code, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(value));
}
function field(body, name) {
  if (body && typeof body === 'object' && !Buffer.isBuffer(body)) return body[name];
  const text = Buffer.isBuffer(body) ? body.toString('latin1') : String(body || '');
  const form = new URLSearchParams(text);
  if (form.has(name)) return form.get(name);
  const match = text.match(new RegExp('name="' + name + '"[\\s\\S]*?\\r\\n\\r\\n([^\\r\\n]*)'));
  return match && match[1];
}

http.createServer(async (req, res) => {
  const path = new URL(req.url, 'http://localhost').pathname;
  if (path === '/__control' && req.method === 'POST') {
    await state.set(failureKey, new URL(req.url, 'http://localhost').searchParams.get('mode') || 'ok');
    return send(res, 200, { result: 'ok' });
  }
  if (path === '/__state' && req.method === 'GET') return send(res, 200, { assets: await state.get(key) || [], mode: await state.get(failureKey) || 'ok' });
  if (req.method !== 'POST' || !/^\/v1_1\/[^/]+\/image\/(upload|destroy)$/.test(path)) return send(res, 501, { error: { message: 'Endpoint not modeled' } });
  let body = req.body;
  if (!body) { const chunks = []; for await (const chunk of req) chunks.push(chunk); body = Buffer.concat(chunks); }
  const id = field(body, 'public_id');
  if (!id || !field(body, 'signature') || !field(body, 'api_key') || !field(body, 'timestamp'))
    return send(res, 400, { error: { message: 'Required signed upload fields missing' } });
  const mode = await state.get(failureKey) || 'ok';
  if (mode === 'error') return send(res, 500, { error: { message: 'Simulated storage outage' } });
  if (mode === 'missing') return send(res, 404, { error: { message: 'Not found' } });
  if (mode === 'malformed') return send(res, 200, { secure_url: 'https://example.com/invalid.png' });
  const assets = await state.get(key) || [];
  if (path.endsWith('/upload')) {
    if (!assets.includes(id)) assets.push(id);
    await state.set(key, assets);
    return send(res, 200, { public_id: id, secure_url: 'https://res.cloudinary.com/test/image/upload/' + id + '.png', bytes: 68, width: 2, height: 2 });
  }
  if (String(field(body, 'invalidate')) !== 'true') return send(res, 400, { error: { message: 'This caller must invalidate cached copies' } });
  await state.set(key, assets.filter(asset => asset !== id));
  return send(res, 200, { result: assets.includes(id) ? 'ok' : 'not found' });
}).listen(process.env.PORT || 4507);
