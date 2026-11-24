// Прокси веб-версии дневника для Yandex Cloud Functions (runtime Node.js 18+).
// Тот же контракт, что web/proxy.php: { e, p, cookie, get } -> { ok, code, text }.

const BASE = 'https://journal.school28-kirov.ru';

function json(obj, status, cors) {
  return {
    statusCode: status,
    headers: { ...cors, 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify(obj),
  };
}

function corsHeaders(headers) {
  const origin = headers.origin || headers.Origin || '*';
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Max-Age': '3600',
  };
}

exports.handler = async (event) => {
  const cors = corsHeaders(event.headers || {});
  const method = event.httpMethod || 'POST';

  if (method === 'OPTIONS') return { statusCode: 204, headers: cors, body: '' };
  if (method !== 'POST') return json({ ok: false, error: 'Только POST' }, 405, cors);

  let raw = event.body || '';
  if (event.isBase64Encoded) raw = Buffer.from(raw, 'base64').toString('utf8');

  let body = {};
  try {
    body = JSON.parse(raw);
  } catch (e) {
    body = {};
  }

  const endpoint = String(body.e || '').trim();
  const params = body.p && typeof body.p === 'object' ? body.p : {};
  const cookie = String(body.cookie || '');
  const isGet = Boolean(body.get);

  if (!endpoint) return json({ ok: false, error: 'Пустой endpoint' }, 400, cors);
  if (!endpoint.startsWith('/') || /[?#:\s]/.test(endpoint)) {
    return json({ ok: false, error: 'Недопустимый endpoint' }, 400, cors);
  }

  const url = new URL(BASE + endpoint);
  const headers = {
    'X-Requested-With': 'XMLHttpRequest',
    Referer: BASE + '/',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) SchoolDiaryWeb/1.0',
  };
  if (cookie) headers.Cookie = cookie;

  const init = { method: isGet ? 'GET' : 'POST', headers, redirect: 'follow' };
  if (isGet) {
    for (const [k, v] of Object.entries(params)) url.searchParams.set(k, String(v));
  } else {
    headers['Content-Type'] = 'application/x-www-form-urlencoded';
    init.body = new URLSearchParams(
      Object.entries(params).map(([k, v]) => [k, String(v)])
    ).toString();
  }

  try {
    const resp = await fetch(url.toString(), init);
    const text = await resp.text();
    return json({ ok: true, code: resp.status, text }, 200, cors);
  } catch (e) {
    return json({ ok: false, error: 'Ошибка сети к журналу: ' + e.message }, 502, cors);
  }
};
