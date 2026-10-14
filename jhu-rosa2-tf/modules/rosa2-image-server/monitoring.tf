# ---------- Monitoring ----------
#
# Adapted from aws-dspace-ecs-tf (baseline-infrastructure/monitoring.tf): an
# SNS topic with an optional email subscription, alarms, and a dashboard. The
# account-wide billing anomaly alarms are not repeated here; dspace owns them.

resource "aws_sns_topic" "alarms" {
  count = var.enable_monitoring ? 1 : 0

  name = "${local.name}-alarms"
}

resource "aws_sns_topic_subscription" "email" {
  count = var.enable_monitoring && var.alarm_notification_email != "" ? 1 : 0

  topic_arn = aws_sns_topic.alarms[0].arn
  protocol  = "email"
  endpoint  = var.alarm_notification_email
}

locals {
  alarm_actions = var.enable_monitoring ? [aws_sns_topic.alarms[0].arn] : []
}

resource "aws_cloudwatch_metric_alarm" "no_healthy_hosts" {
  count = var.enable_monitoring && local.has_image ? 1 : 0

  alarm_name          = "${local.name}-no-healthy-hosts"
  alarm_description   = "The image server has no healthy tasks behind its load balancer."
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HealthyHostCount"
  statistic           = "Minimum"
  period              = 60
  evaluation_periods  = 3
  comparison_operator = "LessThanThreshold"
  threshold           = 1
  treat_missing_data  = "breaching"

  dimensions = {
    LoadBalancer = aws_lb.this.arn_suffix
    TargetGroup  = aws_lb_target_group.this.arn_suffix
  }

  alarm_actions = local.alarm_actions
  ok_actions    = local.alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "target_5xx" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name}-target-5xx"
  alarm_description   = "Cantaloupe is returning 5xx responses."
  namespace           = "AWS/ApplicationELB"
  metric_name         = "HTTPCode_Target_5XX_Count"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  threshold           = var.alarm_5xx_threshold
  treat_missing_data  = "notBreaching"

  dimensions = {
    LoadBalancer = aws_lb.this.arn_suffix
  }

  alarm_actions = local.alarm_actions
  ok_actions    = local.alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name}-cpu-high"
  alarm_description   = "Image server CPU stays high even after scaling out (at max_tasks, or scaling is not keeping up)."
  namespace           = "AWS/ECS"
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  comparison_operator = "GreaterThanThreshold"
  threshold           = 85
  treat_missing_data  = "notBreaching"

  dimensions = {
    ClusterName = aws_ecs_cluster.this.name
    ServiceName = aws_ecs_service.this.name
  }

  alarm_actions = local.alarm_actions
  ok_actions    = local.alarm_actions
}

resource "aws_cloudwatch_metric_alarm" "ecs_memory_high" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name}-memory-high"
  alarm_description   = "Image server memory is close to the task limit; tasks risk being OOM-killed."
  namespace           = "AWS/ECS"
  metric_name         = "MemoryUtilization"
  statistic           = "Maximum"
  period              = 300
  evaluation_periods  = 2
  comparison_operator = "GreaterThanThreshold"
  threshold           = 90
  treat_missing_data  = "notBreaching"

  dimensions = {
    ClusterName = aws_ecs_cluster.this.name
    ServiceName = aws_ecs_service.this.name
  }

  alarm_actions = local.alarm_actions
  ok_actions    = local.alarm_actions
}

# CloudFront metrics are published in us-east-1 with Region=Global.
resource "aws_cloudwatch_metric_alarm" "cloudfront_5xx_rate" {
  count = var.enable_monitoring ? 1 : 0

  alarm_name          = "${local.name}-cloudfront-5xx-rate"
  alarm_description   = "More than ${var.alarm_cloudfront_5xx_rate}% of viewer requests are failing with 5xx."
  namespace           = "AWS/CloudFront"
  metric_name         = "5xxErrorRate"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  comparison_operator = "GreaterThanThreshold"
  threshold           = var.alarm_cloudfront_5xx_rate
  treat_missing_data  = "notBreaching"

  dimensions = {
    DistributionId = aws_cloudfront_distribution.this.id
    Region         = "Global"
  }

  alarm_actions = local.alarm_actions
  ok_actions    = local.alarm_actions
}

resource "aws_cloudwatch_dashboard" "this" {
  count = var.enable_monitoring ? 1 : 0

  dashboard_name = local.name

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        x      = 0
        y      = 0
        width  = 12
        height = 6
        properties = {
          title   = "CloudFront requests and errors"
          region  = "us-east-1"
          view    = "timeSeries"
          stacked = false
          period  = 300
          metrics = [
            ["AWS/CloudFront", "Requests", "DistributionId", aws_cloudfront_distribution.this.id, "Region", "Global", { stat = "Sum", label = "Requests" }],
            ["AWS/CloudFront", "4xxErrorRate", "DistributionId", aws_cloudfront_distribution.this.id, "Region", "Global", { stat = "Average", yAxis = "right", label = "4xx %" }],
            ["AWS/CloudFront", "5xxErrorRate", "DistributionId", aws_cloudfront_distribution.this.id, "Region", "Global", { stat = "Average", yAxis = "right", label = "5xx %" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 0
        width  = 12
        height = 6
        properties = {
          title   = "Edge cache: requests reaching the ALB vs. all requests"
          region  = local.region
          view    = "timeSeries"
          stacked = false
          period  = 300
          metrics = [
            ["AWS/CloudFront", "Requests", "DistributionId", aws_cloudfront_distribution.this.id, "Region", "Global", { stat = "Sum", label = "CloudFront requests", region = "us-east-1" }],
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.this.arn_suffix, { stat = "Sum", label = "Origin (ALB) requests" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 6
        width  = 12
        height = 6
        properties = {
          title   = "ALB target response time"
          region  = local.region
          view    = "timeSeries"
          stacked = false
          period  = 60
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.this.arn_suffix, { stat = "p50", label = "p50" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.this.arn_suffix, { stat = "p90", label = "p90" }],
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_lb.this.arn_suffix, { stat = "p99", label = "p99" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 12
        y      = 6
        width  = 12
        height = 6
        properties = {
          title   = "ALB errors and healthy hosts"
          region  = local.region
          view    = "timeSeries"
          stacked = false
          period  = 60
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", aws_lb.this.arn_suffix, { stat = "Sum", label = "Target 5xx" }],
            ["AWS/ApplicationELB", "HTTPCode_Target_4XX_Count", "LoadBalancer", aws_lb.this.arn_suffix, { stat = "Sum", label = "Target 4xx" }],
            ["AWS/ApplicationELB", "TargetConnectionErrorCount", "LoadBalancer", aws_lb.this.arn_suffix, { stat = "Sum", label = "Connection errors" }],
            ["AWS/ApplicationELB", "HealthyHostCount", "TargetGroup", aws_lb_target_group.this.arn_suffix, "LoadBalancer", aws_lb.this.arn_suffix, { stat = "Minimum", yAxis = "right", label = "Healthy hosts" }],
          ]
        }
      },
      {
        type   = "metric"
        x      = 0
        y      = 12
        width  = 24
        height = 6
        properties = {
          title   = "ECS service CPU and memory"
          region  = local.region
          view    = "timeSeries"
          stacked = false
          period  = 60
          yAxis   = { left = { min = 0, max = 100 } }
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.this.name, "ServiceName", aws_ecs_service.this.name, { stat = "Average", label = "CPU % (avg)" }],
            ["AWS/ECS", "CPUUtilization", "ClusterName", aws_ecs_cluster.this.name, "ServiceName", aws_ecs_service.this.name, { stat = "Maximum", label = "CPU % (max)" }],
            ["AWS/ECS", "MemoryUtilization", "ClusterName", aws_ecs_cluster.this.name, "ServiceName", aws_ecs_service.this.name, { stat = "Maximum", label = "Memory % (max)" }],
          ]
        }
      },
    ]
  })
}
