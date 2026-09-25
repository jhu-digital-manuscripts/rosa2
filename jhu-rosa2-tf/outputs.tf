# ---------- Outputs ----------
#
# rosa-search/init-opensearch.sh reads these with `tofu output -json`, so renaming
# one is a breaking change for that script.

output "environment" {
  description = "Environment this state belongs to. init-opensearch.sh compares it against --env so that a workspace initialized for one environment cannot be used to ingest into the other."
  value       = var.environment
}

output "aws_region" {
  description = "Region the resources live in."
  value       = var.aws_region
}

output "search_url" {
  description = "The _search endpoint to pass to generate-iiif-pres --opensearch-url. This is what rosa-viewer posts queries to."
  value       = module.rosa2_search.search_url
}

output "api_endpoint" {
  description = "Base URL of the search HTTP API."
  value       = module.rosa2_search.api_endpoint
}

output "api_id" {
  description = "ID of the search HTTP API."
  value       = module.rosa2_search.api_id
}

output "cors_allowed_origins" {
  description = "Origins currently permitted to call the search API from a browser."
  value       = var.cors_allowed_origins
}

# ---------- OpenSearch ----------

output "opensearch_domain_name" {
  description = "Name of the OpenSearch domain."
  value       = module.rosa2_search.opensearch_domain_name
}

output "opensearch_endpoint" {
  description = "VPC endpoint host of the OpenSearch domain. Reachable only from within the VPC."
  value       = module.rosa2_search.opensearch_endpoint
}

output "opensearch_domain_arn" {
  description = "ARN of the OpenSearch domain."
  value       = module.rosa2_search.opensearch_domain_arn
}

output "opensearch_dashboards_endpoint" {
  description = "OpenSearch Dashboards host. Reachable only from within the VPC."
  value       = module.rosa2_search.opensearch_dashboards_endpoint
}

output "opensearch_engine_version" {
  description = "Engine version running on the domain."
  value       = module.rosa2_search.opensearch_engine_version
}

output "opensearch_security_group_id" {
  description = "Security group attached to the OpenSearch domain ENIs."
  value       = module.rosa2_search.opensearch_security_group_id
}

output "lambda_security_group_id" {
  description = "Security group attached to both Lambda functions. Membership is what grants access to the domain."
  value       = module.rosa2_search.lambda_security_group_id
}

output "slow_log_threshold_ms" {
  description = "Search slow log threshold to apply as an index setting, in milliseconds."
  value       = var.opensearch_slow_log_threshold_ms
}

# ---------- Latin analyzer packages ----------

output "latin_stemmer_package_id" {
  description = "Custom package ID for latin-stemmer-rules.txt."
  value       = module.rosa2_search.latin_stemmer_package_id
}

output "latin_charmap_package_id" {
  description = "Custom package ID for latin-charmap.txt."
  value       = module.rosa2_search.latin_charmap_package_id
}

output "latin_stemmer_rules_path" {
  description = "Value to substitute for rules_path in the index definitions, replacing the local-dev path analyzers/latin-stemmer-rules.txt."
  value       = module.rosa2_search.latin_stemmer_rules_path
}

output "latin_charmap_path" {
  description = "Value to substitute for mappings_path in the index definitions, replacing the local-dev path analyzers/latin-charmap.txt."
  value       = module.rosa2_search.latin_charmap_path
}

# ---------- Ingest ----------

output "admin_lambda_name" {
  description = "Name of the admin Lambda that init-opensearch.sh invokes."
  value       = module.rosa2_search.admin_lambda_name
}

output "admin_lambda_arn" {
  description = "ARN of the admin Lambda."
  value       = module.rosa2_search.admin_lambda_arn
}

output "ingest_bucket" {
  description = "S3 bucket that index definitions and bulk files are staged in."
  value       = module.rosa2_search.ingest_bucket
}

output "ingest_prefix" {
  description = "S3 key prefix that index definitions and bulk files are staged under."
  value       = module.rosa2_search.ingest_prefix
}

output "ingest_operator_policy_arn" {
  description = "IAM policy granting the permissions needed to run init-opensearch.sh. Attach it to the identity that performs ingests."
  value       = module.rosa2_search.ingest_operator_policy_arn
}

# ---------- Logs ----------

output "search_log_group" {
  description = "CloudWatch log group for the search proxy. Rejected queries are logged here."
  value       = module.rosa2_search.search_log_group
}

output "admin_log_group" {
  description = "CloudWatch log group for the admin Lambda."
  value       = module.rosa2_search.admin_log_group
}

output "api_log_group" {
  description = "CloudWatch log group for API Gateway access logs, empty when disabled."
  value       = module.rosa2_search.api_log_group
}

output "opensearch_log_group" {
  description = "CloudWatch log group for OpenSearch application and slow logs, empty when disabled."
  value       = module.rosa2_search.opensearch_log_group
}
