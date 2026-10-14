// CloudFront Function (cloudfront-js-2.0), viewer-response event, attached
// only to the legacy /iiif/* cache behaviour.
//
// Cantaloupe builds every URL it emits as <base_uri>/iiif/2/<id>/..., and the
// delegate script can only change the info.json body. For requests that came
// in as /iiif/<id>/..., this rewrites the /iiif/2/ path prefix back to /iiif/
// in the headers that carry URLs:
//
//   Location  e.g. the 303 from a non-normalised request to its canonical form
//   Link      rel="canonical" (and rel="profile", which points elsewhere and
//             is left alone because it does not contain /iiif/2/)
//
// Viewer-response functions also run on cache hits, so responses cached for
// the shared /iiif/2/ object are rewritten correctly for legacy clients.

var URL_HEADERS = ['location', 'link'];

function rewrite(value) {
  // Only the first occurrence: the path prefix, never an identifier (which
  // is percent-encoded and so contains no literal "/").
  return value.replace('/iiif/2/', '/iiif/');
}

function handler(event) {
  var headers = event.response.headers;

  for (var i = 0; i < URL_HEADERS.length; i++) {
    var header = headers[URL_HEADERS[i]];
    if (!header) {
      continue;
    }
    header.value = rewrite(header.value);
    if (header.multiValue) {
      for (var j = 0; j < header.multiValue.length; j++) {
        header.multiValue[j].value = rewrite(header.multiValue[j].value);
      }
    }
  }

  return event.response;
}
