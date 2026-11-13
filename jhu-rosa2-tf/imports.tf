# Import blocks must stay in the deployment root. Only stage adopts the shared
# connector; notification resources themselves belong to the monitoring module.
import {
  for_each = var.environment == "stage" ? toset(["shared"]) : toset([])
  to       = module.rosa2_monitoring.module.rosa2_notifications.aws_chatbot_slack_channel_configuration.alarms[0]
  id       = "arn:aws:chatbot::390157243417:chat-configuration/slack-channel/jhu-aws-slack-connector-staging"
}

# Preserve existing state if the previous top-level module was already applied.
moved {
  from = module.rosa2_notifications
  to   = module.rosa2_monitoring.module.rosa2_notifications
}
