variable "enabled" {
  description = "Create alarms and the scheduled public functional checker."
  type        = bool
  default     = true
  nullable    = false
}

variable "name_prefix" {
  description = "Environment-qualified resource name prefix."
  type        = string

  nullable = false
}

variable "alarm_sns_topic_arns" {
  description = "Existing SNS destinations for alarms and recoveries."
  type        = list(string)
  default     = []
  nullable    = false
}

variable "search_api_id" {
  description = "HTTP API ID."
  type        = string

  nullable = false
}

variable "search_api_endpoint" {
  description = "Public HTTP API base URL."
  type        = string

  nullable = false
}

variable "search_lambda_name" {
  description = "Search proxy Lambda name."
  type        = string

  nullable = false
}

variable "opensearch_domain" {
  description = "OpenSearch domain name."
  type        = string

  nullable = false
}

variable "opensearch_disk_gib" {
  description = "Per-node EBS size; free-storage warning is 25% of this size."
  type        = number

  nullable = false
}

variable "image_base_url" {
  description = "Public IIIF Image API 3 base URL."
  type        = string

  nullable = false
}

variable "image_enabled" {
  description = "Check images only after an image-server image has been configured."
  type        = bool
  default     = true
  nullable    = false
}

variable "manifest_id" {
  description = "Known manifest to find in both search indexes."
  type        = string
  default     = "rose.Douce195"
  nullable    = false
}

variable "image_identifier" {
  description = "Known unencoded IIIF image identifier."
  type        = string
  default     = "rose/Douce195/Douce195.001r"
  nullable    = false
}

variable "log_retention_days" {
  description = "Functional-check log retention in days."
  type        = number
  default     = 14
  nullable    = false
}
