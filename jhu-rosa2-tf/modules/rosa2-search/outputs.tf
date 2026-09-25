# =============================================================================
# ROSA2 SEARCH MODULE - OUTPUTS
# =============================================================================
# Re-exported by the root module, whose output names init-opensearch.sh depends on.

output "search_url" {
  description = "The _search endpoint to pass to generate-iiif-pres --opensearch-url. This is what rosa-viewer posts queries to."
  value       = "${aws_apigatewayv2_api.search.api_endpoint}/_search"
}

output "api_endpoint" {
  description = "Base URL of the search HTTP API."
  value       = aws_apigatewayv2_api.search.api_endpoint
}

output "api_id" {
  description = "ID of the search HTTP API."
  value       = aws_apigatewayv2_api.search.id
}

# ---------- OpenSearch ----------

output "opensearch_domain_name" {
  description = "Name of the OpenSearch domain."
  value       = aws_opensearch_domain.rosa.domain_name
}

output "opensearch_endpoint" {
  description = "VPC endpoint host of the OpenSearch domain. Reachable only from within the VPC."
  value       = aws_opensearch_domain.rosa.endpoint
}

output "opensearch_domain_arn" {
  description = "ARN of the OpenSearch domain."
  value       = aws_opensearch_domain.rosa.arn
}

output "opensearch_dashboards_endpoint" {
  description = "OpenSearch Dashboards host. Reachable only from within the VPC."
  value       = aws_opensearch_domain.rosa.dashboard_endpoint
}

output "opensearch_engine_version" {
  description = "Engine version running on the domain."
  value       = aws_opensearch_domain.rosa.engine_version
}

output "opensearch_security_group_id" {
  description = "Security group attached to the OpenSearch domain ENIs."
  value       = aws_security_group.opensearch.id
}

output "lambda_security_group_id" {
  description = "Security group attached to both Lambda functions. Membership is what grants access to the domain."
  value       = aws_security_group.lambda.id
}

# ---------- Latin analyzer packages ----------

output "latin_stemmer_package_id" {
  description = "Custom package ID for latin-stemmer-rules.txt."
  value       = aws_opensearch_package.latin_stemmer_rules.id
}

output "latin_charmap_package_id" {
  description = "Custom package ID for latin-charmap.txt."
  value       = aws_opensearch_package.latin_charmap.id
}

output "latin_stemmer_rules_path" {
  description = "Value to substitute for rules_path in the index definitions, replacing the local-dev path analyzers/latin-stemmer-rules.txt."
  value       = "analyzers/${aws_opensearch_package.latin_stemmer_rules.id}"
}

output "latin_charmap_path" {
  description = "Value to substitute for mappings_path in the index definitions, replacing the local-dev path analyzers/latin-charmap.txt."
  value       = "analyzers/${aws_opensearch_package.latin_charmap.id}"
}

# ---------- Ingest ----------

output "admin_lambda_name" {
  description = "Name of the admin Lambda that init-opensearch.sh invokes."
  value       = aws_lambda_function.admin.function_name
}

output "admin_lambda_arn" {
  description = "ARN of the admin Lambda."
  value       = aws_lambda_function.admin.arn
}

output "ingest_bucket" {
  description = "S3 bucket that index definitions and bulk files are staged in."
  value       = local.ingest_bucket
}

output "ingest_prefix" {
  description = "S3 key prefix that index definitions and bulk files are staged under."
  value       = local.ingest_key_prefix
}

output "ingest_operator_policy_arn" {
  description = "IAM policy granting the permissions needed to run init-opensearch.sh. Attach it to the identity that performs ingests."
  value       = aws_iam_policy.ingest_operator.arn
}

# ---------- Logs ----------

output "search_log_group" {
  description = "CloudWatch log group for the search proxy. Rejected queries are logged here."
  value       = aws_cloudwatch_log_group.search.name
}

output "admin_log_group" {
  description = "CloudWatch log group for the admin Lambda."
  value       = aws_cloudwatch_log_group.admin.name
}

output "api_log_group" {
  description = "CloudWatch log group for API Gateway access logs, empty when disabled."
  value       = var.enable_api_access_logs ? aws_cloudwatch_log_group.api[0].name : ""
}

output "opensearch_log_group" {
  description = "CloudWatch log group for OpenSearch application and slow logs, empty when disabled."
  value       = var.opensearch_enable_log_publishing ? aws_cloudwatch_log_group.opensearch[0].name : ""
}
