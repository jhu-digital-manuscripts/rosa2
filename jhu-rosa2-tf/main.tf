# =============================================================================
# ROSA2 SEARCH DEPLOYMENT - MAIN CONFIGURATION
# =============================================================================
# One environment (prod or stage) per state, selected at init time by the
# *.s3.tfbackend file and at plan/apply time by the matching *.tfvars file.
#
# Everything is in modules/rosa2-search. This root module owns the backend, the
# provider and its default_tags, and the paths to the rosa-search sources that
# the module packages and uploads.
# =============================================================================

module "rosa2_search" {
  source = "./modules/rosa2-search"

  environment = var.environment
  aws_region  = var.aws_region
  name_prefix = var.name_prefix

  # Networking
  vpc_id                 = var.vpc_id
  subnet_ids             = var.subnet_ids
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

  # API Gateway
  cors_allowed_origins   = var.cors_allowed_origins
  throttle_rate_limit    = var.throttle_rate_limit
  throttle_burst_limit   = var.throttle_burst_limit
  enable_api_access_logs = var.enable_api_access_logs
}
