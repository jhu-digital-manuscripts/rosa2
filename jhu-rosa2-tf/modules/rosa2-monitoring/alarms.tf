data "aws_caller_identity" "current" {}

locals {
  api_dimensions = {
    ApiId = var.search_api_id
    Stage = "$default"
  }
  lambda_dimensions = { FunctionName = var.search_lambda_name }
  domain_dimensions = {
    DomainName = var.opensearch_domain
    ClientId   = data.aws_caller_identity.current.account_id
  }
  search_alarms = {
    "api-5xx" = {
      namespace   = "AWS/ApiGateway"
      metric      = "5xx"
      statistic   = "Sum"
      threshold   = 1
      period      = 300
      evaluations = 1
      datapoints  = 1
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.api_dimensions
      description = "Search API returned a server error. Inspect API access logs and search proxy logs; Lambda throttles can surface as HTTP 503."
    }
    "api-latency" = {
      namespace   = "AWS/ApiGateway"
      metric      = "Latency"
      statistic   = "p95"
      threshold   = 3000
      period      = 300
      evaluations = 3
      datapoints  = 2
      comparison  = "GreaterThanThreshold"
      missing     = "notBreaching"
      dimensions  = local.api_dimensions
      description = "Search API p95 latency exceeds 3 seconds. Inspect OpenSearch JVM/CPU and proxy duration."
    }
    "lambda-errors" = {
      namespace   = "AWS/Lambda"
      metric      = "Errors"
      statistic   = "Sum"
      threshold   = 1
      period      = 300
      evaluations = 1
      datapoints  = 1
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.lambda_dimensions
      description = "Search Lambda failed. Inspect its logs. Handled upstream errors are covered by the API 5xx alarm."
    }
    "lambda-throttles" = {
      namespace   = "AWS/Lambda"
      metric      = "Throttles"
      statistic   = "Sum"
      threshold   = 1
      period      = 300
      evaluations = 1
      datapoints  = 1
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.lambda_dimensions
      description = "Search Lambda was throttled. Inspect concurrent executions and upstream latency before raising concurrency."
    }
    "cluster-red" = {
      namespace   = "AWS/ES"
      metric      = "ClusterStatus.red"
      statistic   = "Maximum"
      threshold   = 1
      period      = 60
      evaluations = 1
      datapoints  = 1
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "breaching"
      dimensions  = local.domain_dimensions
      description = "OpenSearch primary shards are unavailable or health telemetry stopped. Check domain health and AWS service events."
    }
    "cluster-yellow" = {
      namespace   = "AWS/ES"
      metric      = "ClusterStatus.yellow"
      statistic   = "Maximum"
      threshold   = 1
      period      = 60
      evaluations = 5
      datapoints  = 5
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.domain_dimensions
      description = "OpenSearch has unassigned replicas for five periods. Inspect shard allocation and node health."
    }
    "disk-low" = {
      namespace   = "AWS/ES"
      metric      = "FreeStorageSpace"
      statistic   = "Minimum"
      threshold   = var.opensearch_disk_gib * 1024 * 0.25
      period      = 60
      evaluations = 1
      datapoints  = 1
      comparison  = "LessThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.domain_dimensions
      description = "An OpenSearch node has at most 25% free EBS storage. Inspect disk allocation and increase capacity before writes are blocked."
    }
    "jvm-high" = {
      namespace   = "AWS/ES"
      metric      = "JVMMemoryPressure"
      statistic   = "Maximum"
      threshold   = 85
      period      = 300
      evaluations = 3
      datapoints  = 3
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.domain_dimensions
      description = "OpenSearch JVM pressure is sustained above 85%. Inspect GC logs, query load and instance capacity."
    }
    "old-gen-high" = {
      namespace   = "AWS/ES"
      metric      = "OldGenJVMMemoryPressure"
      statistic   = "Maximum"
      threshold   = 80
      period      = 60
      evaluations = 3
      datapoints  = 3
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.domain_dimensions
      description = "OpenSearch old-generation heap pressure is above 80%. Inspect GC logs and retained heap/instance capacity."
    }
    "cpu-high" = {
      namespace   = "AWS/ES"
      metric      = "CPUUtilization"
      statistic   = "Maximum"
      threshold   = 80
      period      = 300
      evaluations = 3
      datapoints  = 3
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.domain_dimensions
      description = "OpenSearch CPU is sustained above 80%. Inspect queries and capacity; the T3 domain is burstable."
    }
    "writes-blocked" = {
      namespace   = "AWS/ES"
      metric      = "ClusterIndexWritesBlocked"
      statistic   = "Maximum"
      threshold   = 1
      period      = 300
      evaluations = 1
      datapoints  = 1
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.domain_dimensions
      description = "OpenSearch is blocking index writes. Inspect free storage, JVM pressure and cluster health before ingesting."
    }
    "snapshot-failure" = {
      namespace   = "AWS/ES"
      metric      = "AutomatedSnapshotFailure"
      statistic   = "Maximum"
      threshold   = 1
      period      = 60
      evaluations = 1
      datapoints  = 1
      comparison  = "GreaterThanOrEqualToThreshold"
      missing     = "notBreaching"
      dimensions  = local.domain_dimensions
      description = "OpenSearch automated snapshot failed. Inspect snapshot status and cluster health."
    }
  }
}

resource "aws_cloudwatch_metric_alarm" "search" {
  for_each = var.enabled ? local.search_alarms : {}

  alarm_name                            = "${var.name_prefix}-search-${each.key}"
  alarm_description                     = each.value.description
  namespace                             = each.value.namespace
  metric_name                           = each.value.metric
  statistic                             = each.value.statistic == "p95" ? null : each.value.statistic
  extended_statistic                    = each.value.statistic == "p95" ? "p95" : null
  evaluate_low_sample_count_percentiles = each.value.statistic == "p95" ? "ignore" : null
  threshold                             = each.value.threshold
  period                                = each.value.period
  evaluation_periods                    = each.value.evaluations
  datapoints_to_alarm                   = each.value.datapoints
  comparison_operator                   = each.value.comparison
  treat_missing_data                    = each.value.missing
  dimensions                            = each.value.dimensions
  alarm_actions                         = var.alarm_sns_topic_arns
  ok_actions                            = var.alarm_sns_topic_arns
}
