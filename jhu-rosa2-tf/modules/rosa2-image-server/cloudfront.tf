# ---------- CloudFront ----------
#
# HTTPS front door and edge cache. Every IIIF URL is immutable for a given
# image, so almost all traffic (tiles, thumbnails, info.json) is served from
# the edge. Edge misses go through an Origin Shield in the deployment region
# (enable_origin_shield), so each response is fetched from Cantaloupe once
# rather than once per edge location; Cantaloupe's S3 derivative cache is the
# tier behind that.
#
# Cache behaviours, in precedence order:
#
#   /iiif/2/*  Image API 2  no functions
#   /iiif/3/*  Image API 3  no functions
#   /iiif/*    legacy form  legacy-request (viewer-request) rewrites to /iiif/2
#                           and marks info.json requests; legacy-response
#                           (viewer-response) rewrites Location and Link back
#   *          everything else (Cantaloupe's landing page, /health)
#
# Only the cache-key items below are forwarded to the origin: no cookies, no
# query strings (which also stops ?cache=false bypassing the server caches) and
# no viewer headers, except the legacy marker on the legacy behaviour. That is
# why a client cannot send the marker header to /iiif/2/ and poison its cache.

resource "aws_cloudfront_vpc_origin" "alb" {
  vpc_origin_endpoint_config {
    name                   = local.name
    arn                    = aws_lb.this.arn
    http_port              = 80
    https_port             = 443
    origin_protocol_policy = "http-only"

    origin_ssl_protocols {
      items    = ["TLSv1.2"]
      quantity = 1
    }
  }
}

resource "aws_cloudfront_cache_policy" "iiif" {
  name        = "${local.name}-iiif"
  comment     = "IIIF Image API: path only, long TTLs from Cantaloupe's s-maxage"
  min_ttl     = 0
  default_ttl = 86400
  max_ttl     = var.cloudfront_max_ttl

  parameters_in_cache_key_and_forwarded_to_origin {
    enable_accept_encoding_brotli = true
    enable_accept_encoding_gzip   = true

    cookies_config {
      cookie_behavior = "none"
    }
    headers_config {
      header_behavior = "none"
    }
    query_strings_config {
      query_string_behavior = "none"
    }
  }
}

resource "aws_cloudfront_cache_policy" "legacy" {
  name        = "${local.name}-legacy"
  comment     = "Legacy /iiif/<id>: as iiif, plus the marker header set on info.json by legacy-request"
  min_ttl     = 0
  default_ttl = 86400
  max_ttl     = var.cloudfront_max_ttl

  parameters_in_cache_key_and_forwarded_to_origin {
    enable_accept_encoding_brotli = true
    enable_accept_encoding_gzip   = true

    cookies_config {
      cookie_behavior = "none"
    }
    headers_config {
      header_behavior = "whitelist"
      headers {
        items = [local.legacy_marker_header]
      }
    }
    query_strings_config {
      query_string_behavior = "none"
    }
  }
}

# Cantaloupe already sends Access-Control-Allow-Origin: *; this guarantees it
# on every response, including CloudFront-generated errors.
resource "aws_cloudfront_response_headers_policy" "cors" {
  name    = "${local.name}-cors"
  comment = "IIIF images and info.json are public: allow any origin"

  cors_config {
    access_control_allow_credentials = false
    origin_override                  = true
    access_control_max_age_sec       = 86400

    access_control_allow_origins {
      items = ["*"]
    }
    access_control_allow_methods {
      items = ["GET", "HEAD", "OPTIONS"]
    }
    access_control_allow_headers {
      items = ["*"]
    }
    access_control_expose_headers {
      items = ["Link"]
    }
  }
}

resource "aws_cloudfront_function" "legacy_request" {
  name    = "${local.name}-legacy-request"
  runtime = "cloudfront-js-2.0"
  comment = "Rewrite legacy /iiif/<id>/... to /iiif/2/<id>/..."
  publish = true
  code    = file("${path.module}/functions/legacy-request.js")
}

resource "aws_cloudfront_function" "legacy_response" {
  name    = "${local.name}-legacy-response"
  runtime = "cloudfront-js-2.0"
  comment = "Rewrite /iiif/2/ to /iiif/ in Location and Link for legacy requests"
  publish = true
  code    = file("${path.module}/functions/legacy-response.js")
}

locals {
  origin_id = "alb"

  # Shared by every cache behaviour.
  behaviour_defaults = {
    allowed_methods            = ["GET", "HEAD", "OPTIONS"]
    cached_methods             = ["GET", "HEAD", "OPTIONS"]
    viewer_protocol_policy     = "redirect-to-https"
    compress                   = true
    response_headers_policy_id = aws_cloudfront_response_headers_policy.cors.id
  }
}

resource "aws_cloudfront_distribution" "this" {
  enabled         = true
  is_ipv6_enabled = true
  http_version    = "http2and3"
  comment         = "${local.name} (${var.environment}) IIIF Image API"
  price_class     = var.price_class
  aliases         = var.domain_aliases
  web_acl_id      = var.enable_waf ? aws_wafv2_web_acl.this[0].arn : null

  origin {
    origin_id   = local.origin_id
    domain_name = aws_lb.this.dns_name

    origin_shield {
      enabled              = var.enable_origin_shield
      origin_shield_region = local.region
    }

    vpc_origin_config {
      vpc_origin_id            = aws_cloudfront_vpc_origin.alb.id
      origin_read_timeout      = var.origin_read_timeout
      origin_keepalive_timeout = 60
    }
  }

  ordered_cache_behavior {
    path_pattern               = "/iiif/2/*"
    target_origin_id           = local.origin_id
    cache_policy_id            = aws_cloudfront_cache_policy.iiif.id
    allowed_methods            = local.behaviour_defaults.allowed_methods
    cached_methods             = local.behaviour_defaults.cached_methods
    viewer_protocol_policy     = local.behaviour_defaults.viewer_protocol_policy
    compress                   = local.behaviour_defaults.compress
    response_headers_policy_id = local.behaviour_defaults.response_headers_policy_id
  }

  ordered_cache_behavior {
    path_pattern               = "/iiif/3/*"
    target_origin_id           = local.origin_id
    cache_policy_id            = aws_cloudfront_cache_policy.iiif.id
    allowed_methods            = local.behaviour_defaults.allowed_methods
    cached_methods             = local.behaviour_defaults.cached_methods
    viewer_protocol_policy     = local.behaviour_defaults.viewer_protocol_policy
    compress                   = local.behaviour_defaults.compress
    response_headers_policy_id = local.behaviour_defaults.response_headers_policy_id
  }

  ordered_cache_behavior {
    path_pattern               = "/iiif/*"
    target_origin_id           = local.origin_id
    cache_policy_id            = aws_cloudfront_cache_policy.legacy.id
    allowed_methods            = local.behaviour_defaults.allowed_methods
    cached_methods             = local.behaviour_defaults.cached_methods
    viewer_protocol_policy     = local.behaviour_defaults.viewer_protocol_policy
    compress                   = local.behaviour_defaults.compress
    response_headers_policy_id = local.behaviour_defaults.response_headers_policy_id

    function_association {
      event_type   = "viewer-request"
      function_arn = aws_cloudfront_function.legacy_request.arn
    }

    function_association {
      event_type   = "viewer-response"
      function_arn = aws_cloudfront_function.legacy_response.arn
    }
  }

  default_cache_behavior {
    target_origin_id           = local.origin_id
    cache_policy_id            = aws_cloudfront_cache_policy.iiif.id
    allowed_methods            = local.behaviour_defaults.allowed_methods
    cached_methods             = local.behaviour_defaults.cached_methods
    viewer_protocol_policy     = local.behaviour_defaults.viewer_protocol_policy
    compress                   = local.behaviour_defaults.compress
    response_headers_policy_id = local.behaviour_defaults.response_headers_policy_id
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = !local.custom_cert
    acm_certificate_arn            = local.custom_cert ? var.acm_certificate_arn : null
    ssl_support_method             = local.custom_cert ? "sni-only" : null
    minimum_protocol_version       = local.custom_cert ? "TLSv1.2_2021" : "TLSv1"
  }

  lifecycle {
    precondition {
      condition     = length(var.domain_aliases) == 0 || local.custom_cert
      error_message = "domain_aliases requires acm_certificate_arn (an ACM certificate in us-east-1 covering every alias)."
    }
    precondition {
      condition     = local.region == "us-east-1"
      error_message = "This module must be deployed in us-east-1: CloudFront requires its ACM certificate and WAF web ACL there."
    }
  }
}
