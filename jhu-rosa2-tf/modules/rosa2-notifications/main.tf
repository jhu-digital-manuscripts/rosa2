data "aws_caller_identity" "current" {}

locals {
  account_id                   = data.aws_caller_identity.current.account_id
  manages_shared_notifications = var.environment == "stage"
  applications = {
    stage = "jhu-rosa2-stage"
    prod  = "jhu-rosa2"
  }

  # Preserve all existing non-Rosa routes when adopting the shared connector.
  # This stack is the single owner of the entire topic list after adoption.
  existing_connector_topics = toset([
    "arn:aws:sns:us-east-1:${local.account_id}:dspace-stage-alerts",
    "arn:aws:sns:us-east-1:${local.account_id}:dspace-prod-alerts",
    "arn:aws:sns:us-east-1:${local.account_id}:aws-health-webhook-alerts",
    "arn:aws:sns:us-east-1:${local.account_id}:dspace-prod-prod-alarms",
  ])
}

resource "aws_sns_topic" "alarms" {
  for_each = local.manages_shared_notifications ? local.applications : {}
  name     = "${each.value}-alarms"
  tags = {
    Owner       = "drcc"
    System      = "rosa2"
    Environment = each.key
  }
}

resource "aws_sns_topic_policy" "alarms" {
  for_each = local.manages_shared_notifications ? local.applications : {}
  arn      = aws_sns_topic.alarms[each.key].arn
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AccountAdministration"
        Effect    = "Allow"
        Principal = { AWS = "arn:aws:iam::${local.account_id}:root" }
        Action    = "SNS:*"
        Resource  = aws_sns_topic.alarms[each.key].arn
      },
      {
        Sid       = "RosaCloudWatchAlarms"
        Effect    = "Allow"
        Principal = { Service = "cloudwatch.amazonaws.com" }
        Action    = "SNS:Publish"
        Resource  = aws_sns_topic.alarms[each.key].arn
        Condition = {
          StringEquals = { "aws:SourceAccount" = local.account_id }
          ArnLike = {
            "aws:SourceArn" = [
              for component in ["image-server", "search", "functional"] :
              "arn:aws:cloudwatch:us-east-1:${local.account_id}:alarm:${each.value}-${component}-*"
            ]
          }
        }
      }
    ]
  })
}

resource "aws_chatbot_slack_channel_configuration" "alarms" {
  count                       = local.manages_shared_notifications ? 1 : 0
  provider                    = aws.chatbot
  configuration_name          = "jhu-aws-slack-connector-staging"
  iam_role_arn                = "arn:aws:iam::${local.account_id}:role/service-role/JHU-AWS-Q-Slack-Role"
  slack_team_id               = "TBYPTS56V"
  slack_channel_id            = "C071QCVBGJJ"
  logging_level               = "ERROR"
  user_authorization_required = false
  guardrail_policy_arns = [
    "arn:aws:iam::aws:policy/AWS-SSM-RemediationAutomation-AdministrationRolePolicy",
    "arn:aws:iam::aws:policy/service-role/AWSLambdaRole",
    "arn:aws:iam::aws:policy/AWSLambdaExecute",
    "arn:aws:iam::${local.account_id}:policy/SlackCloudWatchDashboardAccess",
  ]
  sns_topic_arns = setunion(local.existing_connector_topics, toset([
    for topic in aws_sns_topic.alarms : topic.arn
  ]))
  tags = {
    Environment = "prod"
    Owner       = "DRCC"
  }

  # This configuration serves other applications too; prevent accidental deletion.
  lifecycle {
    prevent_destroy = true
  }
  depends_on = [aws_sns_topic_policy.alarms]
}

# Amazon Q manages/validates the HTTPS subscriptions when SNS topics are added
# to sns_topic_arns. A bare SNS subscription does not register a topic with Q.
# Prod reads the topic owned by stage. It never adopts the shared connector.
data "aws_sns_topic" "alarms" {
  count = local.manages_shared_notifications ? 0 : 1
  name  = "${local.applications[var.environment]}-alarms"
}

output "alarm_topic_arn" {
  description = "Environment-specific Rosa SNS topic for all application alarms."
  value       = local.manages_shared_notifications ? aws_sns_topic.alarms[var.environment].arn : data.aws_sns_topic.alarms[0].arn
}

output "alarm_topic_arns" {
  description = "Both topic ARNs in the owning stage state; empty in prod."
  value       = { for environment, topic in aws_sns_topic.alarms : environment => topic.arn }
}

output "connector_arn" {
  value = local.manages_shared_notifications ? aws_chatbot_slack_channel_configuration.alarms[0].chat_configuration_arn : ""
}
