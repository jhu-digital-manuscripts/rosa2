# =============================================================================
# ROSA2 DEPLOYMENT - MAIN CONFIGURATION
# =============================================================================
# One environment (prod or stage) per state, selected at init time by the
# *.s3.tfbackend file and at plan/apply time by the matching *.tfvars file.
#
# Two child modules:
#   modules/rosa2-search        OpenSearch, search/admin Lambdas, HTTP API
#   modules/rosa2-image-server  Cantaloupe IIIF Image API on ECS behind CloudFront
#
# This root module owns the backend, the provider and its default_tags, and the
# paths to the rosa-search sources that the search module packages and uploads.
# =============================================================================

module "rosa2_search" {
  source = "./modules/rosa2-search"

  environment = var.environment
  aws_region  = var.aws_region
  name_prefix = var.name_prefix

  # Networking
  vpc_id                 = data.terraform_remote_state.dspace.outputs.vpc_id
  subnet_ids             = local.domain_subnet_ids
  vpc_cidr_blocks        = var.vpc_cidr_blocks
  dns_server_cidrs       = var.dns_server_cidrs
  create_s3_vpc_endpoint = var.create_s3_vpc_endpoint

  # OpenSearch domain
  opensearch_engine_version           = var.opensearch_engine_version
  opensearch_instance_type            = var.opensearch_instance_type
  opensearch_instance_count           = var.opensearch_instance_count
  opensearch_volume_size              = var.opensearch_volume_size
  opensearch_volume_type              = var.opensearch_volume_type
  opensearch_zone_awareness_enabled   = var.opensearch_zone_awareness_enabled
  opensearch_dedicated_master_enabled = var.opensearch_dedicated_master_enabled
  opensearch_dedicated_master_type    = var.opensearch_dedicated_master_type
  opensearch_dedicated_master_count   = var.opensearch_dedicated_master_count
  opensearch_enable_log_publishing    = var.opensearch_enable_log_publishing
  opensearch_slow_log_threshold_ms    = var.opensearch_slow_log_threshold_ms

  # Latin analyzer packages
  analyzer_bucket          = var.analyzer_bucket
  analyzer_prefix          = var.analyzer_prefix
  latin_stemmer_rules_file = var.latin_stemmer_rules_file
  latin_charmap_file       = var.latin_charmap_file

  # Ingest staging
  ingest_bucket = var.ingest_bucket
  ingest_prefix = var.ingest_prefix

  # Lambda
  search_lambda_source_dir           = "${path.module}/../rosa-search/lambda/search_proxy"
  admin_lambda_source_dir            = "${path.module}/../rosa-search/lambda/admin"
  lambda_runtime                     = var.lambda_runtime
  search_lambda_memory               = var.search_lambda_memory
  search_lambda_timeout              = var.search_lambda_timeout
  search_lambda_reserved_concurrency = var.search_lambda_reserved_concurrency
  admin_lambda_memory                = var.admin_lambda_memory
  admin_lambda_timeout               = var.admin_lambda_timeout
  log_retention_days                 = var.log_retention_days

  # Search proxy request limits
  max_body_bytes        = var.max_body_bytes
  max_page_size         = var.max_page_size
  max_result_window     = var.max_result_window
  search_timeout        = var.search_timeout
  terminate_after       = var.terminate_after
  rate_limit_per_minute = var.rate_limit_per_minute
  rate_limit_burst      = var.rate_limit_burst

  # Search proxy upstream behaviour
  upstream_timeout  = var.upstream_timeout
  upstream_attempts = var.upstream_attempts
  upstream_backoff  = var.upstream_backoff

  # API Gateway
  cors_allowed_origins   = var.cors_allowed_origins
  throttle_rate_limit    = var.throttle_rate_limit
  throttle_burst_limit   = var.throttle_burst_limit
  enable_api_access_logs = var.enable_api_access_logs
}


# Cantaloupe IIIF Image API server: ECS Fargate behind CloudFront. The image is
# built and pushed by rosa-image-server/build-and-push.sh; image_tag selects it.
module "rosa2_image_server" {
  source = "./modules/rosa2-image-server"

  environment = var.environment
  aws_region  = var.aws_region
  name_prefix = var.name_prefix

  vpc_cidr                   = var.image_server_vpc_cidr
  source_bucket              = var.image_server_source_bucket
  source_prefix              = var.image_server_source_prefix
  cache_bucket_name          = var.image_server_cache_bucket_name
  cache_ttl_days             = var.image_server_cache_ttl_days
  cache_bucket_force_destroy = var.image_server_cache_bucket_force_destroy
  image_tag                  = var.image_server_image_tag
  ecr_keep_images            = var.image_server_ecr_keep_images
  task_cpu                   = var.image_server_task_cpu
  task_memory                = var.image_server_task_memory
  min_tasks                  = var.image_server_min_tasks
  max_tasks                  = var.image_server_max_tasks
  cpu_target_percent         = var.image_server_cpu_target_percent
  java_opts                  = var.image_server_java_opts
  http_max_threads           = var.image_server_http_max_threads
  http_accept_queue_limit    = var.image_server_http_accept_queue_limit
  log_level                  = var.image_server_log_level
  extra_environment          = var.image_server_extra_environment
  log_retention_days         = var.image_server_log_retention_days
  container_insights         = var.image_server_container_insights
  alb_idle_timeout           = var.image_server_alb_idle_timeout
  domain_aliases             = var.image_server_domain_aliases
  acm_certificate_arn        = var.image_server_acm_certificate_arn
  price_class                = var.image_server_price_class
  cloudfront_max_ttl         = var.image_server_cloudfront_max_ttl
  origin_read_timeout        = var.image_server_origin_read_timeout
  enable_waf                 = var.image_server_enable_waf
  waf_rate_limit             = var.image_server_waf_rate_limit
  waf_trusted_ip_set_arn     = var.image_server_waf_trusted_ip_set_arn
  waf_enable_bot_control     = var.image_server_waf_enable_bot_control
  waf_enable_logging         = var.image_server_waf_enable_logging
  enable_monitoring          = var.image_server_enable_monitoring
  alarm_notification_email   = var.image_server_alarm_notification_email
  alarm_5xx_threshold        = var.image_server_alarm_5xx_threshold
  alarm_cloudfront_5xx_rate  = var.image_server_alarm_cloudfront_5xx_rate
}
