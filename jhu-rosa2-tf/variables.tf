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

variable "vpc_id" {
  description = "VPC the OpenSearch domain and both Lambda functions attach to."
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for the OpenSearch domain ENIs and the Lambda ENIs. The domain requires exactly one subnet per availability zone, so supply one subnet for a single-node domain."
  type        = list(string)

  validation {
    condition     = length(var.subnet_ids) > 0
    error_message = "at least one subnet ID is required."
  }
}

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

# ---------- OpenSearch domain ----------

variable "opensearch_engine_version" {
  description = "OpenSearch engine version. The index definitions in rosa-search/opensearch target 3.x."
  type        = string
  default     = "OpenSearch_3.1"
}

variable "opensearch_instance_type" {
  description = "Data node instance type. Only t3.small.search and t3.medium.search exist in the T3 family. The Latin stemmer dictionary is 18 MB and consumes a comparable amount of heap per index that loads it, so t3.small.search (2 GiB) is workable but has limited headroom."
  type        = string
  default     = "t3.small.search"
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
  description = "Per-shard timeout injected into every forwarded query."
  type        = string
  default     = "10s"
}

variable "terminate_after" {
  description = "Per-shard document cap injected into every forwarded query. Set well above the corpus size so it acts only as a safety net."
  type        = number
  default     = 200000
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
