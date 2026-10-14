# ---------- General ----------

variable "environment" {
  description = "Deployment environment name (e.g. stage, prod). Used for tagging and resource naming."
  type        = string

  validation {
    condition     = contains(["stage", "prod"], var.environment)
    error_message = "environment must be either \"stage\" or \"prod\"."
  }
}

variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "name_prefix" {
  description = "Prefix applied to the OpenSearch domain, Lambda functions, IAM roles, security groups, and the HTTP API. Keep distinct per environment when sharing an account."
  type        = string
  default     = "jhu-rosa2"

  validation {
    # The prefix becomes the OpenSearch domain name, which AWS restricts to
    # 3-28 lowercase alphanumeric characters and hyphens, starting with a letter.
    condition     = can(regex("^[a-z][a-z0-9-]{2,27}$", var.name_prefix))
    error_message = "name_prefix must be 3-28 characters, start with a lowercase letter, and contain only lowercase letters, numbers, and hyphens."
  }
}

variable "tags" {
  description = "Additional tags merged into the provider default_tags."
  type        = map(string)
  default     = {}
}

# ---------- Networking ----------

variable "vpc_cidr_blocks" {
  description = "CIDR blocks permitted to reach the OpenSearch domain directly, in addition to the Lambda security group. Used for troubleshooting from inside the network; set to [] to allow only the Lambdas."
  type        = list(string)
  default     = []
}

variable "dns_server_cidrs" {
  description = "CIDR blocks of the DNS servers handed out by the VPC's DHCP options. Security groups do not filter the Amazon-provided resolver, but they do filter custom DNS servers, so when the VPC uses its own the Lambda group needs egress on port 53 to them or every lookup of the domain endpoint times out. Leave [] in a VPC that uses the Amazon resolver."
  type        = list(string)
  default     = []
}

variable "create_s3_vpc_endpoint" {
  description = "Whether to create an S3 gateway VPC endpoint. The admin Lambda reads bulk data from S3 and has no route to the internet, so the VPC needs one. AWS permits only one S3 gateway endpoint per route table, so leave this false if another workspace already created one in this VPC."
  type        = bool
  default     = false
}

variable "vpc_origin_unsupported_zone_ids" {
  description = "Availability zone IDs to exclude when choosing the image server's private subnets, because CloudFront VPC origins do not support them (us-east-1: use1-az3, per the CloudFront developer guide)."
  type        = list(string)
  default     = ["use1-az3"]
  nullable    = false
}

# ---------- OpenSearch domain ----------

variable "opensearch_engine_version" {
  description = "OpenSearch engine version. The index definitions in rosa-search/opensearch target 3.x."
  type        = string
  default     = "OpenSearch_3.1"
}

variable "opensearch_instance_type" {
  description = "Data node instance type. Only t3.small.search and t3.medium.search exist in the T3 family. The Latin stemmer dictionary is 18 MB and consumes a comparable amount of heap per index that loads it. t3.small.search (2 GiB) proved too small: query latency swung from tens of milliseconds warm to many seconds under light concurrency, which the search proxy surfaced as 502/503/504. Both T3 types are burstable, so if CPUCreditBalance trends to zero under real traffic the answer is a non-burstable family rather than a larger t3."
  type        = string
  default     = "t3.medium.search"
}

variable "opensearch_instance_count" {
  description = "Number of data nodes. T3 instance types are limited to 10."
  type        = number
  default     = 1
}

variable "opensearch_volume_size" {
  description = "EBS volume size in GiB per data node. The generated bulk data is roughly 35 MB, so this is dominated by the 10 GiB minimum rather than by the corpus."
  type        = number
  default     = 20
}

variable "opensearch_volume_type" {
  description = "EBS volume type for data nodes."
  type        = string
  default     = "gp3"
}

variable "opensearch_zone_awareness_enabled" {
  description = "Whether to spread data nodes across availability zones. Requires an even instance_count of at least 2 and one subnet per zone."
  type        = bool
  default     = false
}

variable "opensearch_dedicated_master_enabled" {
  description = "Whether to run dedicated cluster manager nodes. Not needed for a single-node domain."
  type        = bool
  default     = false
}

variable "opensearch_dedicated_master_type" {
  description = "Instance type for dedicated cluster manager nodes."
  type        = string
  default     = "t3.small.search"
}

variable "opensearch_dedicated_master_count" {
  description = "Number of dedicated cluster manager nodes."
  type        = number
  default     = 3
}

variable "opensearch_enable_log_publishing" {
  description = "Publish OpenSearch application logs and search/index slow logs to CloudWatch Logs."
  type        = bool
  default     = true
}

variable "opensearch_slow_log_threshold_ms" {
  description = "Search slow log threshold in milliseconds, applied to both indexes after creation. Set to -1 to disable. Informational only: the value is exposed as an output for use by init-opensearch.sh, since index-level settings are not managed here."
  type        = number
  default     = 2000
}

# ---------- Latin analyzer packages ----------

variable "analyzer_bucket" {
  description = "Existing S3 bucket holding the Latin analyzer dictionary files that are imported as OpenSearch custom packages. Must be in the same region as the domain and must not use KMS encryption, because OpenSearch cannot read KMS-encrypted objects."
  type        = string
}

variable "analyzer_prefix" {
  description = "Key prefix within analyzer_bucket for the dictionary files."
  type        = string
  default     = "opensearch-packages"
}

variable "latin_stemmer_rules_file" {
  description = "Local path to the Latin stemmer override rules, relative to this directory."
  type        = string
  default     = "../rosa-search/opensearch/latin-stemmer-rules.txt"
}

variable "latin_charmap_file" {
  description = "Local path to the Latin character normalization mappings, relative to this directory."
  type        = string
  default     = "../rosa-search/opensearch/latin-charmap.txt"
}

# ---------- Ingest staging ----------

variable "ingest_bucket" {
  description = "S3 bucket used to stage index definitions and bulk NDJSON files for the admin Lambda. Defaults to analyzer_bucket when empty."
  type        = string
  default     = ""
}

variable "ingest_prefix" {
  description = "Key prefix within ingest_bucket for staged ingest data."
  type        = string
  default     = "opensearch-ingest"
}

# ---------- Lambda ----------

variable "lambda_runtime" {
  description = "Python runtime for both Lambda functions. Must be a runtime whose bundled boto3 provides botocore.auth.SigV4Auth."
  type        = string
  default     = "python3.13"
}

variable "search_lambda_memory" {
  description = "Memory in MB for the search proxy. Validation is cheap; this mainly buys CPU for JSON handling and TLS."
  type        = number
  default     = 512
}

variable "search_lambda_timeout" {
  description = "Timeout in seconds for the search proxy."
  type        = number
  default     = 30
}

variable "search_lambda_reserved_concurrency" {
  description = "Reserved concurrent executions for the search proxy. -1 leaves it unreserved. Setting a ceiling caps how much load a traffic spike can put on a single-node domain."
  type        = number
  default     = -1
}

variable "admin_lambda_memory" {
  description = "Memory in MB for the admin Lambda. Bulk batches are buffered in memory before being posted."
  type        = number
  default     = 1024
}

variable "admin_lambda_timeout" {
  description = "Timeout in seconds for the admin Lambda. Ingest is resumable, so this is a per-invocation budget rather than a total."
  type        = number
  default     = 900
}

variable "log_retention_days" {
  description = "CloudWatch log group retention in days for the Lambda functions, the API access log, and the OpenSearch log groups."
  type        = number
  default     = 30
}

# ---------- Search proxy request limits ----------

variable "max_body_bytes" {
  description = "Reject search request bodies larger than this many bytes."
  type        = number
  default     = 16384
}

variable "max_page_size" {
  description = "Maximum permitted 'size' in a search request."
  type        = number
  default     = 100
}

variable "max_result_window" {
  description = "Maximum permitted 'from' + 'size' in a search request."
  type        = number
  default     = 10000
}

variable "search_timeout" {
  description = "Per-shard timeout injected into every forwarded query. Keep this below upstream_timeout: when OpenSearch gives up first it answers 200 with partial results and timed_out: true, which the viewer can show as a warning alongside the hits it did get, whereas if the proxy's socket times out first the user gets a 504 and nothing."
  type        = string
  default     = "6s"
}

variable "terminate_after" {
  description = "Per-shard document cap injected into every forwarded query. Set well above the corpus size so it acts only as a safety net."
  type        = number
  default     = 200000
}

variable "upstream_timeout" {
  description = "Socket timeout in seconds for the search proxy's call to OpenSearch. Keep well below search_lambda_timeout: a request waiting on the domain holds a Lambda concurrency slot for the whole wait, so a long timeout lets one slow query starve other callers. The proxy retries once, and does not retry timeouts, so the worst case is roughly upstream_timeout + upstream_backoff + upstream_timeout only when the first attempt fails without timing out."
  type        = number
  default     = 8
}

variable "upstream_attempts" {
  description = "Total attempts the search proxy makes per request, including the first. 2 means one retry, which covers a domain that is briefly unreachable or answers 502/503/504. Set to 1 to disable retrying."
  type        = number
  default     = 2
}

variable "upstream_backoff" {
  description = "Seconds the search proxy waits before retrying a failed call to OpenSearch."
  type        = number
  default     = 0.25
}

variable "rate_limit_per_minute" {
  description = "Best-effort per-IP request budget enforced inside the search proxy. State is per execution environment, so this is a brake on a single abusive client rather than a global limit; the API stage throttle is the authoritative control. Set to 0 to disable."
  type        = number
  default     = 120
}

variable "rate_limit_burst" {
  description = "Best-effort per-IP burst allowance for the search proxy."
  type        = number
  default     = 30
}

# ---------- API Gateway ----------

variable "cors_allowed_origins" {
  description = "Origins permitted to call the search API from a browser. The production viewer is publicly accessible, so this must list the site origins, e.g. [\"https://rosa2.library.jhu.edu\"]. [\"*\"] allows any origin, which is acceptable for a read-only public API but permits other sites to use this endpoint."
  type        = list(string)
  default     = ["*"]

  validation {
    condition     = length(var.cors_allowed_origins) > 0
    error_message = "at least one allowed origin is required."
  }
}

variable "throttle_rate_limit" {
  description = "Steady-state request-per-second limit for the API stage, across all clients. This is the authoritative protection for the domain."
  type        = number
  default     = 50
}

variable "throttle_burst_limit" {
  description = "Burst request limit for the API stage, across all clients."
  type        = number
  default     = 100
}

variable "enable_api_access_logs" {
  description = "Write API Gateway access logs to CloudWatch Logs."
  type        = bool
  default     = true
}

# =============================================================================
# IMAGE SERVER (modules/rosa2-image-server)
# =============================================================================
# Each image_server_<name> is passed to the module's <name> input. Optional
# ones default to null here, which makes the module use its own default, so
# defaults and full descriptions live in modules/rosa2-image-server/variables.tf.

variable "image_server_source_bucket" {
  description = "S3 bucket holding the JPEG 2000 source images. Read-only for the image server."
  type        = string
}

variable "image_server_source_prefix" {
  description = "Key prefix of the source images in source_bucket, with a trailing slash. The image for identifier a/b/c is <prefix>a/b/c.jp2."
  type        = string
  default     = null
}

variable "image_server_cache_bucket_name" {
  description = "Name of the derivative cache bucket this environment creates. Empty for <name_prefix>-image-cache-<account id>."
  type        = string
  default     = null
}

variable "image_server_cache_ttl_days" {
  description = "How long derivatives and infos stay in the S3 cache. Drives both Cantaloupe's cache.server.derivative.ttl_seconds and the bucket's lifecycle expiry."
  type        = number
  default     = null
}

variable "image_server_cache_bucket_force_destroy" {
  description = "Allow tofu destroy to delete the cache bucket while it still contains objects. Cache content is disposable, so this is safe to enable."
  type        = bool
  default     = null
}

variable "image_server_image_tag" {
  description = "Tag in this environment's ECR repository to run, as printed by rosa-image-server/build-and-push.sh. Empty creates everything with the service scaled to zero, for the first apply before an image has been pushed."
  type        = string
  default     = null
}

variable "image_server_ecr_keep_images" {
  description = "Number of most recent images kept in the ECR repository; older ones are expired."
  type        = number
  default     = null
}

variable "image_server_task_cpu" {
  description = "Fargate task CPU units (1024 = 1 vCPU)."
  type        = number
  default     = null
}

variable "image_server_task_memory" {
  description = "Fargate task memory in MiB. The JVM heap is a percentage of this (see java_opts)."
  type        = number
  default     = null
}

variable "image_server_min_tasks" {
  description = "Minimum (and initial) number of running tasks."
  type        = number
  default     = null
}

variable "image_server_max_tasks" {
  description = "Maximum number of tasks the CPU target-tracking policy may scale out to. Equal to min_tasks disables scaling in practice."
  type        = number
  default     = null
}

variable "image_server_cpu_target_percent" {
  description = "Average service CPU utilisation the scaling policy aims for."
  type        = number
  default     = null
}

variable "image_server_java_opts" {
  description = "Extra JVM options (JAVA_OPTS). Applied after the image's fixed -XX:MaxRAMPercentage=75.0, so a MaxRAMPercentage here wins. 65% leaves room for Kakadu and TurboJPEG native buffers outside the heap."
  type        = string
  default     = null
}

variable "image_server_http_max_threads" {
  description = "Cantaloupe http.max_threads: concurrent requests per task. Each in-flight request may hold a decoded region in the heap."
  type        = number
  default     = null
}

variable "image_server_http_accept_queue_limit" {
  description = "Cantaloupe http.accept_queue_limit: queued requests per task beyond max_threads. null for the Jetty default."
  type        = number
  default     = null
}

variable "image_server_log_level" {
  description = "Cantaloupe log.application.level (error, warn, info, debug, trace)."
  type        = string
  default     = null
}

variable "image_server_extra_environment" {
  description = "Additional environment variables for the Cantaloupe container, e.g. { PROCESSOR_JPG_QUALITY = \"85\" }. Any cantaloupe.properties key can be overridden this way (key uppercased, non-alphanumerics replaced by _). Entries here override the module's own values."
  type        = map(string)
  default     = null
}

variable "image_server_log_retention_days" {
  description = "CloudWatch Logs retention for the container logs."
  type        = number
  default     = null
}

variable "image_server_container_insights" {
  description = "Enable ECS Container Insights on the cluster (extra CloudWatch metrics, billed)."
  type        = bool
  default     = null
}

variable "image_server_alb_idle_timeout" {
  description = "ALB idle timeout in seconds. Must exceed the slowest expected image response."
  type        = number
  default     = null
}

variable "image_server_domain_aliases" {
  description = "Custom hostnames for the distribution, e.g. [\"image.library.jhu.edu\"]. Empty serves only on the generated *.cloudfront.net name. Requires acm_certificate_arn. The first entry becomes Cantaloupe's base_uri."
  type        = list(string)
  default     = null
}

variable "image_server_acm_certificate_arn" {
  description = "ARN of an ACM certificate in us-east-1 covering every name in domain_aliases. Empty uses the default *.cloudfront.net certificate."
  type        = string
  default     = null
}

variable "image_server_price_class" {
  description = "CloudFront price class. PriceClass_100 serves from North America and Europe edge locations only, which is the cheapest."
  type        = string
  default     = null
}

variable "image_server_cloudfront_max_ttl" {
  description = "Longest time in seconds CloudFront keeps a response. Cantaloupe sends s-maxage=31536000 for successful responses, capped by this."
  type        = number
  default     = null
}

variable "image_server_origin_read_timeout" {
  description = "Seconds CloudFront waits for the origin to respond. Full-size renderings of large pages can take several seconds."
  type        = number
  default     = null
}

variable "image_server_enable_waf" {
  description = "Attach a CLOUDFRONT-scope WAF web ACL (AWS managed common rules, IP reputation list, per-IP rate limit)."
  type        = bool
  default     = null
}

variable "image_server_waf_rate_limit" {
  description = "Requests per IP per 5 minutes before WAF blocks. Deep zooming in Mirador issues hundreds of tile requests a minute, so keep this generous."
  type        = number
  default     = null
}

variable "image_server_waf_trusted_ip_set_arn" {
  description = "ARN of an existing CLOUDFRONT-scope WAF IP set whose addresses bypass every other rule. Empty for none."
  type        = string
  default     = null
}

variable "image_server_waf_enable_bot_control" {
  description = "Add the AWS Bot Control managed rule group (COMMON level). Billed per request, which adds up with tile traffic. HTTP-library and non-browser user agents are counted, not blocked, so IIIF harvesters keep working."
  type        = bool
  default     = null
}

variable "image_server_waf_enable_logging" {
  description = "Log every WAF-evaluated request to an aws-waf-logs-* S3 bucket. High volume with tile traffic."
  type        = bool
  default     = null
}

variable "image_server_enable_monitoring" {
  description = "Create an SNS alarm topic, CloudWatch alarms and a dashboard."
  type        = bool
  default     = null
}

variable "image_server_alarm_notification_email" {
  description = "Email address subscribed to the alarm topic. Empty for no subscription. The subscription must be confirmed from the email AWS sends."
  type        = string
  default     = null
}

variable "image_server_alarm_5xx_threshold" {
  description = "Target 5xx responses per 5 minutes at the ALB that trigger an alarm."
  type        = number
  default     = null
}

variable "image_server_alarm_cloudfront_5xx_rate" {
  description = "CloudFront 5xx error rate (percent of requests, 5-minute average) that triggers an alarm."
  type        = number
  default     = null
}
