# Notifications remain available to image alarms even when search monitoring is
# disabled. Stage owns shared resources; prod only reads its topic.
module "rosa2_notifications" {
  source = "../rosa2-notifications"
  providers = {
    aws         = aws
    aws.chatbot = aws.chatbot
  }
  environment = var.environment
}

locals {
  alarm_sns_topic_arns = distinct(concat([module.rosa2_notifications.alarm_topic_arn], var.alarm_sns_topic_arns))
}

output "alarm_sns_topic_arn" {
  description = "Environment's Rosa topic, also used by image-server alarms."
  value       = module.rosa2_notifications.alarm_topic_arn
}
