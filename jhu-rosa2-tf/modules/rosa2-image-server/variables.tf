# =============================================================================
# ROSA2 IMAGE SERVER MODULE - VARIABLES
# =============================================================================
#
# Variables with a default are declared nullable = false, so a caller passing
# null (as the root module does for anything left unset in the tfvars) gets
# the default declared here. The defaults therefore live in one place.

# ---------- General ----------

variable "environment" {
  description = "Deployment environment name (stage or prod). Also the key prefix in the cache bucket."
  type        = string

  validation {
    condition     = contains(["stage", "prod"], var.environment)
    error_message = "environment must be either \"stage\" or \"prod\"."
  }
}

variable "aws_region" {
  description = "AWS region to deploy into. Must be us-east-1 while CloudFront's WAF web ACL and ACM certificate live in the same provider."
  type        = string
  default     = "us-east-1"
  nullable    = false
}

variable "name_prefix" {
  description = "Prefix for resource names; \"-image-server\" is appended. Keep distinct per environment when sharing an account."
  type        = string

  validation {
    # <prefix>-image-server must fit the 32-character ALB and target group limit.
    condition     = can(regex("^[a-z][a-z0-9-]{2,18}$", var.name_prefix))
    error_message = "name_prefix must be 3-19 characters, start with a lowercase letter, and contain only lowercase letters, numbers, and hyphens."
  }
}

# ---------- Networking ----------

variable "vpc_id" {
  description = "VPC the image server attaches to (the dspace VPC for this environment)."
  type        = string
}

variable "public_subnet_ids" {
  description = "Subnet IDs for the Fargate tasks. Public subnets with an IGW default route; tasks get a public IP and reach ECR and CloudWatch Logs over the IGW (no NAT gateway). S3 goes via the dspace VPC's S3 gateway endpoint, which dspace associates with the public route table."
  type        = list(string)

  validation {
    condition     = length(var.public_subnet_ids) > 0
    error_message = "at least one public subnet ID is required."
  }
}

variable "private_subnet_ids" {
  description = "Subnet IDs for the internal ALB and the CloudFront VPC origin ENIs. Must be in at least two distinct availability zones (ALB requirement) and must avoid AZs CloudFront VPC origins do not support."
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_ids) >= 2
    error_message = "at least two private subnet IDs (in distinct AZs) are required for the internal ALB."
  }
}

# ---------- Source images ----------

variable "source_bucket" {
  description = "S3 bucket holding the JPEG 2000 source images. Read-only for the image server."
  type        = string
}

variable "source_prefix" {
  description = "Key prefix of the source images in source_bucket, with a trailing slash. The image for identifier a/b/c is <prefix>a/b/c.jp2."
  type        = string
  default     = "iiifstore/"
  nullable    = false

  validation {
    condition     = var.source_prefix == "" || (can(regex("/$", var.source_prefix)) && !can(regex("^/", var.source_prefix)))
    error_message = "source_prefix must be empty or end with \"/\", and must not start with \"/\"."
  }
}

# ---------- Derivative cache ----------

variable "cache_bucket_name" {
  description = "Name of the derivative cache bucket this environment creates. Empty for <name_prefix>-image-cache-<account id>."
  type        = string
  default     = ""
  nullable    = false
}

variable "cache_ttl_days" {
  description = "How long derivatives and infos stay in the S3 cache. Drives both Cantaloupe's cache.server.derivative.ttl_seconds and the bucket's lifecycle expiry."
  type        = number
  default     = 30
  nullable    = false

  validation {
    condition     = var.cache_ttl_days >= 1 && floor(var.cache_ttl_days) == var.cache_ttl_days
    error_message = "cache_ttl_days must be a whole number of days, at least 1."
  }
}

variable "cache_bucket_force_destroy" {
  description = "Allow tofu destroy to delete the cache bucket while it still contains objects. Cache content is disposable, so this is safe to enable."
  type        = bool
  default     = true
  nullable    = false
}

# ---------- Container image ----------

variable "image_tag" {
  description = "Tag in this environment's ECR repository to run, as printed by rosa-image-server/build-and-push.sh. Empty creates everything with the service scaled to zero, for the first apply before an image has been pushed."
  type        = string
  default     = ""
  nullable    = false
}

variable "ecr_keep_images" {
  description = "Number of most recent images kept in the ECR repository; older ones are expired."
  type        = number
  default     = 15
  nullable    = false
}

# ---------- ECS ----------

variable "task_cpu" {
  description = "Fargate task CPU units (1024 = 1 vCPU)."
  type        = number
  default     = 1024
  nullable    = false
}

variable "task_memory" {
  description = "Fargate task memory in MiB. The JVM heap is a percentage of this (see java_opts)."
  type        = number
  default     = 4096
  nullable    = false
}

variable "min_tasks" {
  description = "Minimum (and initial) number of running tasks."
  type        = number
  default     = 1
  nullable    = false
}

variable "max_tasks" {
  description = "Maximum number of tasks the CPU target-tracking policy may scale out to. Equal to min_tasks disables scaling in practice."
  type        = number
  default     = 1
  nullable    = false

  validation {
    condition     = var.max_tasks >= 1
    error_message = "max_tasks must be at least 1."
  }
}

variable "cpu_target_percent" {
  description = "Average service CPU utilisation the scaling policy aims for."
  type        = number
  default     = 60
  nullable    = false
}

variable "java_opts" {
  description = "Extra JVM options (JAVA_OPTS). Applied after the image's fixed -XX:MaxRAMPercentage=75.0, so a MaxRAMPercentage here wins. 65% leaves room for Kakadu and TurboJPEG native buffers outside the heap."
  type        = string
  default     = "-XX:MaxRAMPercentage=65.0"
  nullable    = false
}

variable "http_max_threads" {
  description = "Cantaloupe http.max_threads: concurrent requests per task. Each in-flight request may hold a decoded region in the heap."
  type        = number
  default     = 32
  nullable    = false
}

variable "http_accept_queue_limit" {
  description = "Cantaloupe http.accept_queue_limit: queued requests per task beyond max_threads. null for the Jetty default."
  type        = number
  default     = null
}

variable "log_level" {
  description = "Cantaloupe log.application.level (error, warn, info, debug, trace)."
  type        = string
  default     = "info"
  nullable    = false

  validation {
    condition     = contains(["error", "warn", "info", "debug", "trace"], var.log_level)
    error_message = "log_level must be one of error, warn, info, debug, trace."
  }
}

variable "extra_environment" {
  description = "Additional environment variables for the Cantaloupe container, e.g. { PROCESSOR_JPG_QUALITY = \"85\" }. Any cantaloupe.properties key can be overridden this way (key uppercased, non-alphanumerics replaced by _). Entries here override the module's own values."
  type        = map(string)
  default     = {}
  nullable    = false
}

variable "log_retention_days" {
  description = "CloudWatch Logs retention for the container logs."
  type        = number
  default     = 30
  nullable    = false
}

variable "container_insights" {
  description = "Enable ECS Container Insights on the cluster (extra CloudWatch metrics, billed)."
  type        = bool
  default     = false
  nullable    = false
}

# ---------- Load balancer ----------

variable "alb_idle_timeout" {
  description = "ALB idle timeout in seconds. Must exceed the slowest expected image response."
  type        = number
  default     = 60
  nullable    = false
}

# ---------- CloudFront ----------

variable "domain_aliases" {
  description = "Custom hostnames for the distribution, e.g. [\"image.library.jhu.edu\"]. Empty serves only on the generated *.cloudfront.net name. Requires acm_certificate_arn. The first entry becomes Cantaloupe's base_uri."
  type        = list(string)
  default     = []
  nullable    = false
}

variable "acm_certificate_arn" {
  description = "ARN of an ACM certificate in us-east-1 covering every name in domain_aliases. Empty uses the default *.cloudfront.net certificate."
  type        = string
  default     = ""
  nullable    = false
}

variable "price_class" {
  description = "CloudFront price class. PriceClass_100 serves from North America and Europe edge locations only, which is the cheapest."
  type        = string
  default     = "PriceClass_100"
  nullable    = false

  validation {
    condition     = contains(["PriceClass_100", "PriceClass_200", "PriceClass_All"], var.price_class)
    error_message = "price_class must be PriceClass_100, PriceClass_200 or PriceClass_All."
  }
}

variable "cloudfront_max_ttl" {
  description = "Longest time in seconds CloudFront keeps a response. Cantaloupe sends s-maxage=31536000 for successful responses, capped by this."
  type        = number
  default     = 31536000
  nullable    = false
}

variable "origin_read_timeout" {
  description = "Seconds CloudFront waits for the origin to respond. Full-size renderings of large pages can take several seconds."
  type        = number
  default     = 60
  nullable    = false
}

# ---------- WAF ----------

variable "enable_waf" {
  description = "Attach a CLOUDFRONT-scope WAF web ACL (AWS managed common rules, IP reputation list, per-IP rate limit)."
  type        = bool
  default     = false
  nullable    = false
}

variable "waf_rate_limit" {
  description = "Requests per IP per 5 minutes before WAF blocks. Deep zooming in Mirador issues hundreds of tile requests a minute, so keep this generous."
  type        = number
  default     = 20000
  nullable    = false
}

variable "waf_trusted_ip_set_arn" {
  description = "ARN of an existing CLOUDFRONT-scope WAF IP set whose addresses bypass every other rule. Empty for none."
  type        = string
  default     = ""
  nullable    = false
}

variable "waf_enable_bot_control" {
  description = "Add the AWS Bot Control managed rule group (COMMON level). Billed per request, which adds up with tile traffic. HTTP-library and non-browser user agents are counted, not blocked, so IIIF harvesters keep working."
  type        = bool
  default     = false
  nullable    = false
}

variable "waf_enable_logging" {
  description = "Log every WAF-evaluated request to an aws-waf-logs-* S3 bucket. High volume with tile traffic."
  type        = bool
  default     = false
  nullable    = false
}

# ---------- Monitoring ----------

variable "enable_monitoring" {
  description = "Create an SNS alarm topic, CloudWatch alarms and a dashboard."
  type        = bool
  default     = true
  nullable    = false
}

variable "alarm_notification_email" {
  description = "Email address subscribed to the alarm topic. Empty for no subscription. The subscription must be confirmed from the email AWS sends."
  type        = string
  default     = ""
  nullable    = false
}

variable "alarm_5xx_threshold" {
  description = "Target 5xx responses per 5 minutes at the ALB that trigger an alarm."
  type        = number
  default     = 20
  nullable    = false
}

variable "alarm_cloudfront_5xx_rate" {
  description = "CloudFront 5xx error rate (percent of requests, 5-minute average) that triggers an alarm."
  type        = number
  default     = 5
  nullable    = false
}
