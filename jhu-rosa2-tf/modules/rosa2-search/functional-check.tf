# ---------- Public functional checks ----------
#
# A scheduled Lambda outside the VPC queries the public HTTP API like an anonymous
# reader and publishes Success/Duration per check to Rosa/FunctionalChecks. The
# source (../../functional-check/app.py) is shared with the image-server module,
# which deploys its own copy; the two checkers are otherwise unrelated.

locals {
  probe_name = "${var.name_prefix}-search-checks"
  checks     = toset(["search-manifest", "search-canvas"])
}

data "archive_file" "probe" {
  count       = var.enable_monitoring ? 1 : 0
  type        = "zip"
  source_file = var.functional_check_source_file
  output_path = "${path.root}/.build/search-functional-check.zip"
}

resource "aws_cloudwatch_log_group" "probe" {
  count             = var.enable_monitoring ? 1 : 0
  name              = "/aws/lambda/${local.probe_name}"
  retention_in_days = var.log_retention_days
}

# This dedicated role has no S3, OpenSearch, or SNS permissions and no VPC access.
# The checker calls only the same public endpoints as an anonymous reader.
resource "aws_iam_role" "probe" {
  count = var.enable_monitoring ? 1 : 0
  name  = local.probe_name
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "probe" {
  count = var.enable_monitoring ? 1 : 0
  name  = "logs-and-metrics"
  role  = aws_iam_role.probe[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "${aws_cloudwatch_log_group.probe[0].arn}:*"
      },
      {
        Effect    = "Allow"
        Action    = "cloudwatch:PutMetricData"
        Resource  = "*"
        Condition = { StringEquals = { "cloudwatch:namespace" = "Rosa/FunctionalChecks" } }
      }
    ]
  })
}

resource "aws_lambda_function" "probe" {
  count                          = var.enable_monitoring ? 1 : 0
  function_name                  = local.probe_name
  description                    = "Checks real search results in the manifest and canvas indexes every five minutes"
  role                           = aws_iam_role.probe[0].arn
  runtime                        = "python3.13"
  handler                        = "app.handler"
  filename                       = data.archive_file.probe[0].output_path
  source_code_hash               = data.archive_file.probe[0].output_base64sha256
  memory_size                    = 128
  timeout                        = 90
  reserved_concurrent_executions = 1

  environment {
    variables = {
      DEPLOYMENT          = var.name_prefix
      SEARCH_API_ENDPOINT = aws_apigatewayv2_api.search.api_endpoint
      MANIFEST_ID         = var.functional_check_manifest_id
    }
  }
  depends_on = [aws_iam_role_policy.probe]
}

resource "aws_cloudwatch_event_rule" "probe" {
  count               = var.enable_monitoring ? 1 : 0
  name                = local.probe_name
  description         = "Run public Rosa search checks"
  schedule_expression = "rate(5 minutes)"
}

resource "aws_cloudwatch_event_target" "probe" {
  count = var.enable_monitoring ? 1 : 0
  rule  = aws_cloudwatch_event_rule.probe[0].name
  arn   = aws_lambda_function.probe[0].arn
  retry_policy {
    maximum_event_age_in_seconds = 60
    maximum_retry_attempts       = 0
  }
  depends_on = [aws_lambda_permission.probe]
}

resource "aws_lambda_permission" "probe" {
  count         = var.enable_monitoring ? 1 : 0
  statement_id  = "AllowScheduledFunctionalChecks"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.probe[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.probe[0].arn
}

# Do not replay stale checks: each invocation represents current availability.
resource "aws_lambda_function_event_invoke_config" "probe" {
  count                        = var.enable_monitoring ? 1 : 0
  function_name                = aws_lambda_function.probe[0].function_name
  maximum_event_age_in_seconds = 60
  maximum_retry_attempts       = 0
}

resource "aws_cloudwatch_metric_alarm" "functional" {
  for_each = var.enable_monitoring ? local.checks : toset([])

  alarm_name          = "${var.name_prefix}-search-functional-${each.key}"
  alarm_description   = "Public ${each.key} check failed or stopped reporting. Inspect /aws/lambda/${local.probe_name}; verify the known manifest is still indexed, then inspect the API, search Lambda and OpenSearch domain."
  namespace           = "Rosa/FunctionalChecks"
  metric_name         = "Success"
  statistic           = "Minimum"
  period              = 300
  evaluation_periods  = 3
  datapoints_to_alarm = 2
  threshold           = 1
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "breaching"
  dimensions = {
    Deployment = var.name_prefix
    Check      = each.key
  }
  alarm_actions = local.alarm_actions
  ok_actions    = local.alarm_actions
}
