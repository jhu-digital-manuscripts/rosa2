mock_provider "aws" {
  mock_resource "aws_sns_topic" {
    defaults = { arn = "arn:aws:sns:us-east-1:123456789012:rosa-test-alarms" }
  }
  mock_data "aws_sns_topic" {
    defaults = { arn = "arn:aws:sns:us-east-1:123456789012:jhu-rosa2-alarms" }
  }
  mock_data "aws_caller_identity" {
    defaults = { account_id = "123456789012" }
  }
}
mock_provider "aws" {
  alias = "chatbot"
}

override_resource {
  target = aws_sns_topic.alarms
  values = { arn = "arn:aws:sns:us-east-1:123456789012:rosa-test-alarms" }
}

variables {
  environment = "stage"
}

run "application_topics_and_shared_connector" {
  command = plan
  assert {
    condition = (
      length(aws_sns_topic.alarms) == 2 &&
      aws_sns_topic.alarms["stage"].name == "jhu-rosa2-stage-alarms" &&
      aws_sns_topic.alarms["prod"].name == "jhu-rosa2-alarms"
    )
    error_message = "Stage and prod must have separate Rosa-owned topics."
  }
  assert {
    condition = (
      alltrue([for topic in aws_sns_topic.alarms : contains(aws_chatbot_slack_channel_configuration.alarms[0].sns_topic_arns, topic.arn)]) &&
      length(setsubtract(local.existing_connector_topics, aws_chatbot_slack_channel_configuration.alarms[0].sns_topic_arns)) == 0
    )
    error_message = "The existing connector must subscribe to both Rosa topics and preserve all four previous topic associations."
  }
  assert {
    condition = (
      aws_chatbot_slack_channel_configuration.alarms[0].slack_team_id == "TBYPTS56V" &&
      aws_chatbot_slack_channel_configuration.alarms[0].slack_channel_id == "C071QCVBGJJ" &&
      aws_chatbot_slack_channel_configuration.alarms[0].iam_role_arn == "arn:aws:iam::123456789012:role/service-role/JHU-AWS-Q-Slack-Role" &&
      length(aws_chatbot_slack_channel_configuration.alarms[0].guardrail_policy_arns) == 4
    )
    error_message = "Adding SNS routes must preserve the connector's existing channel, role and guardrails."
  }
  assert {
    condition = alltrue([
      for environment, policy in aws_sns_topic_policy.alarms :
      jsondecode(policy.policy).Statement[1].Principal.Service == "cloudwatch.amazonaws.com" &&
      jsondecode(policy.policy).Statement[1].Condition.StringEquals["aws:SourceAccount"] == "123456789012" &&
      jsondecode(policy.policy).Statement[1].Resource == aws_sns_topic.alarms[environment].arn &&
      length(jsondecode(policy.policy).Statement[1].Condition.ArnLike["aws:SourceArn"]) == 3
    ])
    error_message = "Alarm publication must be scoped to this account, environment topic, and Rosa alarm names."
  }
}

run "prod_uses_stage_owned_topic" {
  command = plan
  variables {
    environment = "prod"
  }
  assert {
    condition = (
      length(aws_sns_topic.alarms) == 0 &&
      length(aws_sns_topic_policy.alarms) == 0 &&
      length(aws_chatbot_slack_channel_configuration.alarms) == 0 &&
      data.aws_sns_topic.alarms[0].name == "jhu-rosa2-alarms" &&
      output.alarm_topic_arn == "arn:aws:sns:us-east-1:123456789012:jhu-rosa2-alarms"
    )
    error_message = "Prod must read its topic without owning the shared connector or duplicating topics."
  }
}
