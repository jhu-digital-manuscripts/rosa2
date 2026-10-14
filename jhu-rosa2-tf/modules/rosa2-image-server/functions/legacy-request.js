// CloudFront Function (cloudfront-js-2.0), viewer-request event, attached
// only to the legacy /iiif/* cache behaviour.
//
// Older sites address the Image API 2 endpoint as /iiif/<id>/... while
// Cantaloupe serves it at /iiif/2/<id>/... This function:
//
//   /iiif/<id> and /iiif/<id>/  -> 303 to /iiif/<id>/info.json, answered at
//                                  the edge (the IIIF base URI redirect)
//   /iiif/<id>/info.json        -> /iiif/2/<id>/info.json, plus the marker
//                                  header that makes delegates.rb rewrite
//                                  @id back to /iiif/<id>. The header is part
//                                  of this behaviour's cache key.
//   /iiif/<id>/<anything else>  -> /iiif/2/<id>/<anything else>; any
//                                  client-supplied marker header is removed,
//                                  so image responses cache identically.
//
// /iiif/2/... and /iiif/3/... have their own cache behaviours and never reach
// this function; they are passed through untouched here as a safeguard.
// Identifiers are percent-encoded single path segments (rose%2FDouce195%2F...),
// so a legacy identifier can never be exactly "2" or "3".

var LEGACY_HEADER = 'x-rosa-legacy-path';

function handler(event) {
  var request = event.request;
  var uri = request.uri;

  // Never trust a marker header sent by the client.
  delete request.headers[LEGACY_HEADER];

  var match = /^\/iiif\/([^\/]+)(\/.*)?$/.exec(uri);
  if (!match) {
    return request;
  }

  var id = match[1];
  var rest = match[2] || '';

  if (id === '2' || id === '3') {
    return request;
  }

  if (rest === '' || rest === '/') {
    var host = request.headers.host ? request.headers.host.value : '';
    var location = (host ? 'https://' + host : '') + '/iiif/' + id + '/info.json';
    return {
      statusCode: 303,
      statusDescription: 'See Other',
      headers: {
        location: { value: location },
        'access-control-allow-origin': { value: '*' },
        'cache-control': { value: 'public, max-age=86400' },
      },
    };
  }

  request.uri = '/iiif/2/' + id + rest;
  if (rest === '/info.json') {
    request.headers[LEGACY_HEADER] = { value: '1' };
  }
  return request;
}
