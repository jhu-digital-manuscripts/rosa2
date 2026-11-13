# Stage owns the shared connector and both environment topics in its normal
# application state. Prod reads its topic without managing the shared resource.
module "rosa2_notifications" {
  source = "./modules/rosa2-notifications"
  providers = {
    aws         = aws
    aws.chatbot = aws.chatbot
  }
  environment = var.environment
}

# The connector is adopted only into stage, never into two application states.
# Keeping import in the deployment root also permits offline mocked module tests.
import {
  for_each = var.environment == "stage" ? toset(["shared"]) : toset([])
  to       = module.rosa2_notifications.aws_chatbot_slack_channel_configuration.alarms[0]
  id       = "arn:aws:chatbot::390157243417:chat-configuration/slack-channel/jhu-aws-slack-connector-staging"
}

output "alarm_sns_topic_arn" {
  description = "Rosa-owned SNS topic subscribed to the shared Amazon Q Slack connector."
  value       = module.rosa2_notifications.alarm_topic_arn
}
