// Unit tests for the CloudFront Functions. Run with:
//   node --test jhu-rosa2-tf/modules/rosa2-image-server/functions/test/
//
// Each function is loaded into a fresh vm context, the same way CloudFront
// evaluates a function file: top-level code plus a global `handler`.

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

function load(name) {
  const path = fileURLToPath(new URL(`../${name}`, import.meta.url));
  const source = readFileSync(path, 'utf8');
  assert.doesNotMatch(source, /\brequire\s*\(/, 'CloudFront Functions cannot require modules');
  const context = vm.createContext({});
  vm.runInContext(source, context, { filename: name });
  assert.equal(typeof context.handler, 'function');
  // Results come from another realm; normalise for deepEqual.
  return (event) => JSON.parse(JSON.stringify(context.handler(event)));
}

const ID = 'rose%2FDouce195%2FDouce195.001r';
const HOST = 'd111111abcdef8.cloudfront.net';

// --------------------------------------------------------------------------
// legacy-request.js
// --------------------------------------------------------------------------

const legacyRequest = load('legacy-request.js');

function request(uri, extraHeaders = {}) {
  return {
    version: '1.0',
    context: { eventType: 'viewer-request' },
    viewer: { ip: '192.0.2.1' },
    request: {
      method: 'GET',
      uri,
      querystring: {},
      headers: { host: { value: HOST }, ...extraHeaders },
      cookies: {},
    },
  };
}

test('legacy info.json is rewritten to /iiif/2 with the marker header', () => {
  const out = legacyRequest(request(`/iiif/${ID}/info.json`));
  assert.equal(out.uri, `/iiif/2/${ID}/info.json`);
  assert.deepEqual(out.headers['x-rosa-legacy-path'], { value: '1' });
});

test('legacy tile is rewritten without the marker header', () => {
  const out = legacyRequest(request(`/iiif/${ID}/0,0,512,512/512,/0/default.jpg`));
  assert.equal(out.uri, `/iiif/2/${ID}/0,0,512,512/512,/0/default.jpg`);
  assert.equal(out.headers['x-rosa-legacy-path'], undefined);
});

test('legacy thumbnail and full image are rewritten', () => {
  for (const tail of ['/full/80,/0/default.jpg', '/full/full/0/default.jpg', '/full/!200,200/90/gray.png']) {
    const out = legacyRequest(request(`/iiif/${ID}${tail}`));
    assert.equal(out.uri, `/iiif/2/${ID}${tail}`);
  }
});

test('client-supplied marker header is removed from tile requests', () => {
  const out = legacyRequest(
    request(`/iiif/${ID}/full/80,/0/default.jpg`, { 'x-rosa-legacy-path': { value: 'evil' } }),
  );
  assert.equal(out.headers['x-rosa-legacy-path'], undefined);
});

test('client-supplied marker header is replaced on info.json', () => {
  const out = legacyRequest(request(`/iiif/${ID}/info.json`, { 'x-rosa-legacy-path': { value: 'evil' } }));
  assert.deepEqual(out.headers['x-rosa-legacy-path'], { value: '1' });
});

test('percent-encoded slashes are preserved, not decoded', () => {
  const out = legacyRequest(request(`/iiif/${ID}/info.json`));
  assert.match(out.uri, /%2F/);
  assert.equal(out.uri.split('/').length, 5); // '', iiif, 2, id, info.json
});

test('/iiif/<id> redirects to the legacy info.json at the edge', () => {
  for (const uri of [`/iiif/${ID}`, `/iiif/${ID}/`]) {
    const out = legacyRequest(request(uri));
    assert.equal(out.statusCode, 303);
    assert.equal(out.headers.location.value, `https://${HOST}/iiif/${ID}/info.json`);
    assert.equal(out.headers['access-control-allow-origin'].value, '*');
  }
});

test('/iiif/2 and /iiif/3 requests pass through untouched', () => {
  for (const uri of [
    `/iiif/2/${ID}/info.json`,
    `/iiif/3/${ID}/info.json`,
    `/iiif/2/${ID}/full/80,/0/default.jpg`,
    `/iiif/3/${ID}/full/max/0/default.jpg`,
    '/iiif/2',
    '/iiif/3/',
  ]) {
    const out = legacyRequest(request(uri));
    assert.equal(out.uri, uri, uri);
    assert.equal(out.headers['x-rosa-legacy-path'], undefined, uri);
  }
});

test('marker header is stripped even on /iiif/2 passthrough', () => {
  const out = legacyRequest(request(`/iiif/2/${ID}/info.json`, { 'x-rosa-legacy-path': { value: '1' } }));
  assert.equal(out.headers['x-rosa-legacy-path'], undefined);
});

test('paths outside /iiif/<id> pass through', () => {
  for (const uri of ['/', '/iiif', '/iiif/', '/health', '/iiifx/foo']) {
    const out = legacyRequest(request(uri));
    assert.equal(out.uri, uri, uri);
  }
});

// --------------------------------------------------------------------------
// legacy-response.js
// --------------------------------------------------------------------------

const legacyResponse = load('legacy-response.js');

function response(statusCode, headers) {
  return {
    version: '1.0',
    context: { eventType: 'viewer-response' },
    request: { method: 'GET', uri: `/iiif/2/${ID}/info.json`, headers: {} },
    response: { statusCode, headers },
  };
}

const BASE = `https://${HOST}`;

test('Location is rewritten from /iiif/2/ to /iiif/', () => {
  const out = legacyResponse(response(303, { location: { value: `${BASE}/iiif/2/${ID}/info.json` } }));
  assert.equal(out.headers.location.value, `${BASE}/iiif/${ID}/info.json`);
});

test('canonical Link header is rewritten', () => {
  const out = legacyResponse(
    response(200, { link: { value: `<${BASE}/iiif/2/${ID}/full/80,/0/default.jpg>;rel="canonical"` } }),
  );
  assert.equal(out.headers.link.value, `<${BASE}/iiif/${ID}/full/80,/0/default.jpg>;rel="canonical"`);
});

test('multi-value Link headers are all rewritten', () => {
  const out = legacyResponse(
    response(200, {
      link: {
        value: `<${BASE}/iiif/2/${ID}>;rel="canonical"`,
        multiValue: [
          { value: `<${BASE}/iiif/2/${ID}>;rel="canonical"` },
          { value: '<http://iiif.io/api/image/2/level2.json>;rel="profile"' },
        ],
      },
    }),
  );
  assert.equal(out.headers.link.multiValue[0].value, `<${BASE}/iiif/${ID}>;rel="canonical"`);
  assert.equal(out.headers.link.multiValue[1].value, '<http://iiif.io/api/image/2/level2.json>;rel="profile"');
});

test('only the first /iiif/2/ occurrence is rewritten', () => {
  const out = legacyResponse(response(303, { location: { value: `${BASE}/iiif/2/a/iiif/2/b` } }));
  assert.equal(out.headers.location.value, `${BASE}/iiif/a/iiif/2/b`);
});

test('responses without URL headers are unchanged', () => {
  const headers = { 'content-type': { value: 'image/jpeg' }, 'cache-control': { value: 'max-age=1' } };
  const out = legacyResponse(response(200, headers));
  assert.deepEqual(out.headers, headers);
  assert.equal(out.statusCode, 200);
});
