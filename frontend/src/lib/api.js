async function request(method, path, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error('Impossible de contacter le serveur.');
  }

  if (!res.ok) {
    const ct = res.headers.get('content-type') ?? '';
    let message = 'Erreur serveur';
    if (ct.includes('application/json')) {
      const data = await res.json().catch(() => ({}));
      message = data.message ?? data.error ?? message;
    } else {
      const text = await res.text().catch(() => '');
      if (text) message = text;
    }
    throw new Error(message);
  }

  const ct = res.headers.get('content-type') ?? '';
  if (ct.includes('application/json')) return res.json();
  return null;
}

export const api = {
  post:  (path, body, token) => request('POST',   path, body,      token),
  get:   (path, token)       => request('GET',    path, undefined, token),
  put:   (path, body, token) => request('PUT',    path, body,      token),
  patch: (path, body, token) => request('PATCH',  path, body,      token),
  del:   (path, token)       => request('DELETE', path, undefined, token),
};
