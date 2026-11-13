variable "environment" {
  description = "Application environment. Stage owns both topics and the shared connector; prod only looks up its topic."
  type        = string
  validation {
    condition     = contains(["stage", "prod"], var.environment)
    error_message = "environment must be stage or prod."
  }
}
