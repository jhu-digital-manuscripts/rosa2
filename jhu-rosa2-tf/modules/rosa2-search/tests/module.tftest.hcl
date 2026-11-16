# Offline tests for the rosa2-search module's monitoring. No AWS credentials are
# used: the providers are mocked. Run from this module's directory:
#
#   tofu init -backend=false && tofu test
#
# They check the module's own logic (alarm dimensions and thresholds, where
# alarms notify, the functional checker's isolation), not AWS behaviour.

mock_provider "aws" {
  mock_data "aws_caller_identity" {
    defaults = {
      account_id = "123456789012"
    }
  }

  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{}"
    }
  }

  mock_resource "aws_iam_role" {
    defaults = {
      arn = "arn:aws:iam::123456789012:role/mock"
    }
  }

  mock_resource "aws_iam_policy" {
    defaults = {
      arn = "arn:aws:iam::123456789012:policy/mock"
    }
  }

  mock_resource "aws_cloudwatch_log_group" {
    defaults = {
      arn = "arn:aws:logs:us-east-1:123456789012:log-group:/aws/lambda/mock"
    }
  }

  mock_resource "aws_lambda_function" {
    defaults = {
      arn        = "arn:aws:lambda:us-east-1:123456789012:function:mock"
      invoke_arn = "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:123456789012:function:mock/invocations"
    }
  }

  mock_resource "aws_cloudwatch_event_rule" {
    defaults = {
      arn = "arn:aws:events:us-east-1:123456789012:rule/mock"
    }
  }

  mock_resource "aws_sns_topic" {
    defaults = {
      arn = "arn:aws:sns:us-east-1:123456789012:mock"
    }
  }

  mock_resource "aws_apigatewayv2_api" {
    defaults = {
      id            = "abc123"
      api_endpoint  = "https://abc123.execute-api.us-east-1.amazonaws.com"
      execution_arn = "arn:aws:execute-api:us-east-1:123456789012:abc123"
    }
  }

  mock_resource "aws_opensearch_domain" {
    defaults = {
      arn      = "arn:aws:es:us-east-1:123456789012:domain/jhu-rosa2-stage"
      endpoint = "vpc-jhu-rosa2-stage-mock.us-east-1.es.amazonaws.com"
    }
  }
}

mock_provider "archive" {}

variables {
  environment            = "stage"
  name_prefix            = "jhu-rosa2-stage"
  vpc_id                 = "vpc-00000000000000000"
  subnet_ids             = ["subnet-priv1"]
  opensearch_volume_size = 10

  analyzer_bucket          = "jhu-library-images"
  latin_stemmer_rules_file = "../../../rosa-search/opensearch/latin-stemmer-rules.txt"
  latin_charmap_file       = "../../../rosa-search/opensearch/latin-charmap.txt"

  search_lambda_source_dir     = "../../../rosa-search/lambda/search_proxy"
  admin_lambda_source_dir      = "../../../rosa-search/lambda/admin"
  functional_check_source_file = "../../functional-check/app.py"
}

run "search_alarms" {
  assert {
    condition     = aws_sns_topic.alarms[0].name == "jhu-rosa2-stage-search-alarms"
    error_message = "the alarm topic is named for this environment's search stack"
  }

  assert {
    condition = (
      length(aws_cloudwatch_metric_alarm.search) == 12 &&
      aws_cloudwatch_metric_alarm.search["api-5xx"].alarm_name == "jhu-rosa2-stage-search-api-5xx" &&
      aws_cloudwatch_metric_alarm.search["api-5xx"].metric_name == "5xx" &&
      aws_cloudwatch_metric_alarm.search["api-5xx"].dimensions == tomap({ ApiId = "abc123", Stage = "$default" }) &&
      aws_cloudwatch_metric_alarm.search["api-latency"].extended_statistic == "p95" &&
      aws_cloudwatch_metric_alarm.search["lambda-errors"].dimensions == tomap({ FunctionName = "jhu-rosa2-stage-search" }) &&
      aws_cloudwatch_metric_alarm.search["cluster-red"].dimensions == tomap({ DomainName = "jhu-rosa2-stage", ClientId = "123456789012" }) &&
      aws_cloudwatch_metric_alarm.search["cluster-red"].treat_missing_data == "breaching"
    )
    error_message = "alarms must use the HTTP API metric names and this module's own API, Lambda and domain"
  }

  assert {
    condition     = aws_cloudwatch_metric_alarm.search["disk-low"].threshold == 2560
    error_message = "the free-storage threshold is 25% of the per-node volume, in MiB"
  }

  assert {
    condition = alltrue([
      for alarm in concat(values(aws_cloudwatch_metric_alarm.search), values(aws_cloudwatch_metric_alarm.functional)) :
      alarm.alarm_actions == toset([aws_sns_topic.alarms[0].arn]) && alarm.alarm_actions == alarm.ok_actions
    ])
    error_message = "every alarm and recovery must notify this module's own topic and nothing else"
  }
}

run "functional_check" {
  assert {
    condition = (
      aws_lambda_function.probe[0].function_name == "jhu-rosa2-stage-search-checks" &&
      aws_lambda_function.probe[0].environment[0].variables == tomap({
        DEPLOYMENT          = "jhu-rosa2-stage"
        SEARCH_API_ENDPOINT = "https://abc123.execute-api.us-east-1.amazonaws.com"
        MANIFEST_ID         = "rose.Douce195"
      }) &&
      aws_cloudwatch_event_rule.probe[0].schedule_expression == "rate(5 minutes)"
    )
    error_message = "the checker must query this module's public API on a schedule, with no image configuration"
  }

  assert {
    condition = (
      length(aws_cloudwatch_metric_alarm.functional) == 2 &&
      aws_cloudwatch_metric_alarm.functional["search-canvas"].alarm_name == "jhu-rosa2-stage-search-functional-search-canvas" &&
      alltrue([
        for check, alarm in aws_cloudwatch_metric_alarm.functional :
        alarm.treat_missing_data == "breaching" && alarm.datapoints_to_alarm == 2 && alarm.evaluation_periods == 3 &&
        alarm.dimensions == tomap({ Deployment = "jhu-rosa2-stage", Check = check })
      ])
    )
    error_message = "both indexes must be checked, and a failed or silent check must alarm"
  }

  assert {
    condition = (
      length(aws_lambda_function.probe[0].vpc_config) == 0 &&
      aws_lambda_function.probe[0].layers == null &&
      aws_lambda_permission.probe[0].source_arn == aws_cloudwatch_event_rule.probe[0].arn &&
      jsondecode(aws_iam_role_policy.probe[0].policy).Statement[1].Condition.StringEquals["cloudwatch:namespace"] == "Rosa/FunctionalChecks"
    )
    error_message = "the checker must stay outside the VPC, publish only to its namespace, and be invocable only by its schedule"
  }
}

run "monitoring_disabled" {
  variables {
    enable_monitoring = false
  }

  assert {
    condition = (
      length(aws_sns_topic.alarms) == 0 &&
      length(aws_cloudwatch_metric_alarm.search) == 0 &&
      length(aws_cloudwatch_metric_alarm.functional) == 0 &&
      length(aws_lambda_function.probe) == 0 &&
      length(aws_iam_role.probe) == 0 &&
      length(aws_cloudwatch_event_rule.probe) == 0 &&
      output.alarm_topic_arn == "" &&
      output.functional_check_lambda_name == ""
    )
    error_message = "monitoring resources must be optional"
  }
}
