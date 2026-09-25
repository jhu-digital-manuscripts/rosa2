# =============================================================================
# ROSA2 SEARCH MODULE
# =============================================================================
# Self-contained search backend for rosa-viewer:
# - VPC-only OpenSearch domain with Latin analyzer dictionary packages
# - Search proxy Lambda (read-only, validates and signs viewer queries)
# - Admin Lambda (index creation and bulk ingest, driven by init-opensearch.sh)
# - Public HTTP API in front of the search proxy
# - IAM roles, security groups, and CloudWatch log groups for the above
# - Optional S3 gateway VPC endpoint
#
# Traffic flow:
#   Browser → HTTP API (throttled) → search Lambda → OpenSearch (443, VPC)
#   init-opensearch.sh → S3 staging + admin Lambda → OpenSearch (443, VPC)
#
# The AWS provider, including default_tags, is configured by the caller.
# =============================================================================

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = var.aws_region

  domain_name = var.name_prefix

  # Built from parts rather than read off aws_opensearch_domain. The domain's
  # access policy names the Lambda roles, and the Lambda role policies name the
  # domain; referencing the domain resource from those policies would create a
  # dependency cycle.
  domain_arn = "arn:aws:es:${local.region}:${local.account_id}:domain/${local.domain_name}"

  ingest_bucket = var.ingest_bucket != "" ? var.ingest_bucket : var.analyzer_bucket

  # Per-environment keys so that updating the stage dictionaries cannot disturb
  # the package prod is serving from.
  stemmer_rules_key = "${var.analyzer_prefix}/${var.environment}/latin-stemmer-rules.txt"
  charmap_key       = "${var.analyzer_prefix}/${var.environment}/latin-charmap.txt"

  ingest_key_prefix = "${var.ingest_prefix}/${var.environment}"

  search_function_name = "${var.name_prefix}-search"
  admin_function_name  = "${var.name_prefix}-admin"

  # The three paths rosa-viewer posts to. See
  # rosa-viewer/src/plugins/jhsearch/services/opensearch.js.
  search_routes = [
    "POST /_search",
    "POST /manifest/_search",
    "POST /canvas/_search",
  ]
}
