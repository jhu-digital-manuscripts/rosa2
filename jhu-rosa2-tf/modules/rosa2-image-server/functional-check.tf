# ---------- Public functional checks ----------
#
# A scheduled Lambda outside the VPC requests info.json and a small JPEG through
# CloudFront like an anonymous reader and publishes Success/Duration to
# Rosa/FunctionalChecks. The source (../../functional-check/app.py) is shared with
# the search module, which deploys its own copy; the two checkers are otherwise
# unrelated. Nothing is checked until an image tag is deployed.

locals {
  checks_enabled = var.enable_monitoring && local.has_image
  probe_name     = "${local.name}-checks"
  checks         = toset(["image-delivery"])
}

data "archive_file" "probe" {
  count       = local.checks_enabled ? 1 : 0
  type        = "zip"
  source_file = var.functional_check_source_file
  output_path = "${path.root}/.build/image-functional-check.zip"
}

resource "aws_cloudwatch_log_group" "probe" {
  count             = local.checks_enabled ? 1 : 0
  name              = "/aws/lambda/${local.probe_name}"
  retention_in_days = var.log_retention_days
}

# This dedicated role has no source/cache S3, ECS, or SNS permissions.
# The checker calls only the same public endpoints as an anonymous reader.
resource "aws_iam_role" "probe" {
  count = local.checks_enabled ? 1 : 0
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
  count = local.checks_enabled ? 1 : 0
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
  count                          = local.checks_enabled ? 1 : 0
  function_name                  = local.probe_name
  description                    = "Checks public IIIF metadata and JPEG delivery every five minutes"
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
      DEPLOYMENT       = var.name_prefix
      IMAGE_BASE_URL   = "${local.public_base}/iiif/3"
      IMAGE_IDENTIFIER = var.functional_check_image_identifier
    }
  }
  depends_on = [aws_iam_role_policy.probe]
}

resource "aws_cloudwatch_event_rule" "probe" {
  count               = local.checks_enabled ? 1 : 0
  name                = local.probe_name
  description         = "Run public Rosa image delivery checks"
  schedule_expression = "rate(5 minutes)"
}

resource "aws_cloudwatch_event_target" "probe" {
  count = local.checks_enabled ? 1 : 0
  rule  = aws_cloudwatch_event_rule.probe[0].name
  arn   = aws_lambda_function.probe[0].arn
  retry_policy {
    maximum_event_age_in_seconds = 60
    maximum_retry_attempts       = 0
  }
  depends_on = [aws_lambda_permission.probe]
}

resource "aws_lambda_permission" "probe" {
  count         = local.checks_enabled ? 1 : 0
  statement_id  = "AllowScheduledFunctionalChecks"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.probe[0].function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.probe[0].arn
}

# Do not replay stale checks: each invocation represents current availability.
resource "aws_lambda_function_event_invoke_config" "probe" {
  count                        = local.checks_enabled ? 1 : 0
  function_name                = aws_lambda_function.probe[0].function_name
  maximum_event_age_in_seconds = 60
  maximum_retry_attempts       = 0
}

resource "aws_cloudwatch_metric_alarm" "functional" {
  for_each = local.checks_enabled ? local.checks : toset([])

  alarm_name          = "${local.name}-functional-${each.key}"
  alarm_description   = "Public ${each.key} check failed or stopped reporting. Inspect /aws/lambda/${local.probe_name}; verify the known image still exists, then inspect the service. Responses may be served by caches and do not prove origin freshness."
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
