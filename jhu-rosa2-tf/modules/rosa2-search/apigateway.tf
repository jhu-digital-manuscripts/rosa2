# ---------- API Gateway (HTTP API) ----------
#
# This endpoint is intentionally unauthenticated. rosa-viewer runs in the browsers
# of anonymous users, so there is no credential it could present. The protections
# are therefore: the stage throttle below, which caps total request rate before a
# request reaches the Lambda; the request validation in the search proxy; and the
# IAM path restriction that makes the proxy's credentials read-only. Anyone on the
# internet can run a search, which is the intent, but nobody can write, run a
# script, read another index, or ask for an unbounded result set.
#
# Only the three routes rosa-viewer posts to exist. There is no $default route, so
# any other path returns 404 from API Gateway without invoking anything.
#
# An HTTP API cannot have AWS WAF attached. If per-IP rate limiting at the edge
# becomes necessary, the options are a REST API with a WAF web ACL, or CloudFront
# in front of this API with the ACL there. The in-Lambda limiter is a stopgap, not
# an equivalent.

resource "aws_apigatewayv2_api" "search" {
  name          = "${var.name_prefix}-search-api"
  description   = "rosa2 JHSearch endpoint: signs and validates browser queries to OpenSearch"
  protocol_type = "HTTP"

  cors_configuration {
    allow_origins = var.cors_allowed_origins
    allow_methods = ["POST", "OPTIONS"]
    allow_headers = ["content-type"]

    # The viewer sends no cookies or auth headers, and credentialed requests are
    # incompatible with an "*" origin.
    allow_credentials = false
    max_age           = 3600
  }
}

resource "aws_apigatewayv2_integration" "search" {
  api_id = aws_apigatewayv2_api.search.id

  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.search.invoke_arn
  payload_format_version = "2.0"

  # Below the Lambda timeout so a slow query surfaces as a gateway timeout rather
  # than a dropped connection.
  timeout_milliseconds = min(var.search_lambda_timeout * 1000, 29000)
}

resource "aws_apigatewayv2_route" "search" {
  for_each = toset(local.search_routes)

  api_id    = aws_apigatewayv2_api.search.id
  route_key = each.value
  target    = "integrations/${aws_apigatewayv2_integration.search.id}"
}

# The $default stage serves paths without a stage prefix, so the endpoint the
# viewer is given is https://<api-id>.execute-api.<region>.amazonaws.com/_search.
resource "aws_apigatewayv2_stage" "default" {
  api_id      = aws_apigatewayv2_api.search.id
  name        = "$default"
  auto_deploy = true

  default_route_settings {
    throttling_rate_limit    = var.throttle_rate_limit
    throttling_burst_limit   = var.throttle_burst_limit
    detailed_metrics_enabled = true
  }

  dynamic "access_log_settings" {
    for_each = var.enable_api_access_logs ? [1] : []

    content {
      destination_arn = aws_cloudwatch_log_group.api[0].arn

      format = jsonencode({
        requestId         = "$context.requestId"
        requestTime       = "$context.requestTime"
        httpMethod        = "$context.httpMethod"
        path              = "$context.path"
        status            = "$context.status"
        responseLength    = "$context.responseLength"
        responseLatency   = "$context.responseLatency"
        integrationStatus = "$context.integration.status"
        integrationError  = "$context.integration.error"
        sourceIp          = "$context.identity.sourceIp"
        userAgent         = "$context.identity.userAgent"
      })
    }
  }
}

resource "aws_cloudwatch_log_group" "api" {
  count = var.enable_api_access_logs ? 1 : 0

  name              = "/aws/apigateway/${var.name_prefix}-search-api"
  retention_in_days = var.log_retention_days
}

resource "aws_lambda_permission" "api_invoke_search" {
  statement_id  = "AllowInvokeFromSearchApi"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.search.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_apigatewayv2_api.search.execution_arn}/*/*"
}
