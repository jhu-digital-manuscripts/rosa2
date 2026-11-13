mock_provider "aws" {
  mock_data "aws_sns_topic" {
    defaults = { arn = "arn:aws:sns:us-east-1:123456789012:jhu-rosa2-alarms" }
  }
  mock_data "aws_caller_identity" {
    defaults = { account_id = "123456789012" }
  }
  mock_resource "aws_iam_role" {
    defaults = { arn = "arn:aws:iam::123456789012:role/probe" }
  }
  mock_resource "aws_cloudwatch_log_group" {
    defaults = { arn = "arn:aws:logs:us-east-1:123456789012:log-group:/aws/lambda/probe" }
  }
  mock_resource "aws_lambda_function" {
    defaults = { arn = "arn:aws:lambda:us-east-1:123456789012:function:probe" }
  }
  mock_resource "aws_cloudwatch_event_rule" {
    defaults = { arn = "arn:aws:events:us-east-1:123456789012:rule/probe" }
  }
}
mock_provider "archive" {}
mock_provider "aws" {
  alias = "chatbot"
}

variables {
  environment          = "prod"
  name_prefix          = "jhu-rosa2-stage"
  alarm_sns_topic_arns = ["arn:aws:sns:us-east-1:123456789012:jhu-rosa2-stage-alarms"]
  search_api_id        = "abc123"
  search_api_endpoint  = "https://abc123.execute-api.us-east-1.amazonaws.com"
  search_lambda_name   = "jhu-rosa2-stage-search"
  opensearch_domain    = "jhu-rosa2-stage"
  opensearch_disk_gib  = 10
  image_base_url       = "https://images.example/iiif/3"
}

run "monitoring_enabled" {
  assert {
    condition = (
      aws_cloudwatch_metric_alarm.search["api-5xx"].metric_name == "5xx" &&
      aws_cloudwatch_metric_alarm.search["api-5xx"].dimensions == tomap({ ApiId = "abc123", Stage = "$default" }) &&
      aws_cloudwatch_metric_alarm.search["disk-low"].threshold == 2560
    )
    error_message = "HTTP API metrics require HTTP API names/dimensions; storage threshold must scale per node."
  }
  assert {
    condition = alltrue([
      for alarm in concat(values(aws_cloudwatch_metric_alarm.search), values(aws_cloudwatch_metric_alarm.functional)) :
      alarm.alarm_actions == toset(concat(var.alarm_sns_topic_arns, ["arn:aws:sns:us-east-1:123456789012:jhu-rosa2-alarms"])) && alarm.ok_actions == toset(concat(var.alarm_sns_topic_arns, ["arn:aws:sns:us-east-1:123456789012:jhu-rosa2-alarms"]))
    ])
    error_message = "Every search and functional alarm must notify the Rosa Slack SNS route, including recovery."
  }
  assert {
    condition = (
      length(aws_cloudwatch_metric_alarm.functional) == 3 &&
      alltrue([for alarm in aws_cloudwatch_metric_alarm.functional : alarm.treat_missing_data == "breaching" && alarm.datapoints_to_alarm == 2 && alarm.evaluation_periods == 3]) &&
      aws_cloudwatch_event_rule.probe[0].schedule_expression == "rate(5 minutes)"
    )
    error_message = "Functional checks must cover both indexes and image delivery and detect silent checker failures."
  }
  assert {
    condition = (
      length(aws_lambda_function.probe[0].vpc_config) == 0 &&
      aws_lambda_function.probe[0].layers == null &&
      aws_lambda_permission.probe[0].source_arn == aws_cloudwatch_event_rule.probe[0].arn &&
      jsondecode(aws_iam_role_policy.probe[0].policy).Statement[1].Condition.StringEquals["cloudwatch:namespace"] == "Rosa/FunctionalChecks"
    )
    error_message = "The probe must use public HTTP endpoints, publish only its namespace, and allow only its scheduled rule to invoke it."
  }
}

run "no_image_yet" {
  variables { image_enabled = false }
  assert {
    condition     = length(aws_cloudwatch_metric_alarm.functional) == 2 && aws_lambda_function.probe[0].environment[0].variables.IMAGE_ENABLED == "false"
    error_message = "Before the first image deployment, only search should be checked."
  }
}

run "disabled" {
  variables { enabled = false }
  assert {
    condition = (
      length(aws_cloudwatch_metric_alarm.search) == 0 &&
      length(aws_cloudwatch_metric_alarm.functional) == 0 &&
      length(aws_lambda_function.probe) == 0 &&
      length(aws_iam_role.probe) == 0 &&
      length(aws_cloudwatch_event_rule.probe) == 0 &&
      output.alarm_sns_topic_arn == "arn:aws:sns:us-east-1:123456789012:jhu-rosa2-alarms"
    )
    error_message = "Disabling monitoring must remove alarms, checker, scheduler and its role."
  }
}
