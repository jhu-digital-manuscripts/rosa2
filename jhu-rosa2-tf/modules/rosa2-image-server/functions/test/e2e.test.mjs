// End-to-end check of the legacy /iiif chain against a running image server:
// viewer-request function -> Cantaloupe (with delegates.rb) -> viewer-response
// function. Skipped unless ROSA_IMAGE_SERVER_URL is set, e.g.
//
//   ROSA_IMAGE_SERVER_URL=http://127.0.0.1:18182 \
//   ROSA_PUBLIC_BASE_URL=https://images.example.org \
//   ROSA_TEST_ID=rose%2FTest%2FTest.001r \
//     node --test jhu-rosa2-tf/modules/rosa2-image-server/functions/test/
//
// ROSA_PUBLIC_BASE_URL must equal the server's BASE_URI; rosa-image-server/
// test/smoke-test.sh --keep starts a suitable server and prints these values.

import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import vm from 'node:vm';

const SERVER = process.env.ROSA_IMAGE_SERVER_URL;
const PUBLIC = process.env.ROSA_PUBLIC_BASE_URL;
const ID = process.env.ROSA_TEST_ID;
const skip = !(SERVER && PUBLIC && ID) && 'ROSA_IMAGE_SERVER_URL, ROSA_PUBLIC_BASE_URL and ROSA_TEST_ID not set';

function load(name) {
  const source = readFileSync(fileURLToPath(new URL(`../${name}`, import.meta.url)), 'utf8');
  const context = vm.createContext({});
  vm.runInContext(source, context, { filename: name });
  return (event) => JSON.parse(JSON.stringify(context.handler(event)));
}

// Simulates the legacy cache behaviour: request function, origin fetch with the
// headers CloudFront forwards (cache-key headers only), response function.
async function viaLegacyBehaviour(path) {
  const host = new URL(PUBLIC).host;
  const onRequest = load('legacy-request.js');
  const onResponse = load('legacy-response.js');

  const req = onRequest({ request: { method: 'GET', uri: path, querystring: {}, headers: { host: { value: host } } } });
  if (req.statusCode) {
    return { status: req.statusCode, headers: req.headers, body: null };
  }

  const forwarded = {};
  if (req.headers['x-rosa-legacy-path']) forwarded['X-Rosa-Legacy-Path'] = req.headers['x-rosa-legacy-path'].value;
  const res = await fetch(SERVER + req.uri, { headers: forwarded, redirect: 'manual' });

  const headers = {};
  res.headers.forEach((value, name) => {
    headers[name] = { value };
  });
  const out = onResponse({ request: req, response: { statusCode: res.status, headers } });
  const type = res.headers.get('content-type') || '';
  const body = type.includes('json') ? await res.json() : await res.arrayBuffer();
  return { status: out.statusCode, headers: out.headers, body };
}

test('legacy info.json has @id under /iiif/', { skip }, async () => {
  const res = await viaLegacyBehaviour(`/iiif/${ID}/info.json`);
  assert.equal(res.status, 200);
  assert.equal(res.body['@id'], `${PUBLIC}/iiif/${ID}`);
});

test('legacy /iiif/<id> redirects to legacy info.json', { skip }, async () => {
  const res = await viaLegacyBehaviour(`/iiif/${ID}`);
  assert.equal(res.status, 303);
  assert.equal(res.headers.location.value, `${PUBLIC}/iiif/${ID}/info.json`);
});

test('legacy tile has a canonical Link under /iiif/', { skip }, async () => {
  const res = await viaLegacyBehaviour(`/iiif/${ID}/full/80,/0/default.jpg`);
  assert.equal(res.status, 200);
  assert.equal(res.headers['content-type'].value, 'image/jpeg');
  assert.match(res.headers.link.value, new RegExp(`^<${PUBLIC}/iiif/${ID}/full/80,/0/default\\.jpg>`));
});

test('/iiif/2 info.json is untouched by the legacy chain', { skip }, async () => {
  const res = await fetch(`${SERVER}/iiif/2/${ID}/info.json`);
  assert.equal((await res.json())['@id'], `${PUBLIC}/iiif/2/${ID}`);
});
