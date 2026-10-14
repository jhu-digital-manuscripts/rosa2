# =============================================================================
# ROSA2 IMAGE SERVER MODULE
# =============================================================================
# IIIF Image API server (Cantaloupe + Kakadu, rosa-image-server/) on AWS:
# - Attaches to the dspace VPC: tasks in the public subnets, the internal ALB
#   and the CloudFront VPC origin in the private subnets. No NAT gateway; the
#   VPC, IGW, subnets and routing are injected from the dspace remote state.
# - ECS Fargate service with CPU target-tracking auto scaling
# - Internal ALB, reachable only through a CloudFront VPC origin
# - CloudFront distribution: HTTPS, edge caching, legacy /iiif/<id> support via
#   two CloudFront Functions (functions/)
# - Per-environment S3 derivative cache bucket with lifecycle expiry
# - Per-environment ECR repository
# - Optional WAF web ACL, CloudWatch alarms and dashboard
#
# Traffic flow:
#   viewer → CloudFront (cache) → VPC origin → internal ALB → ECS task :8182
#   ECS task → S3 source bucket (read-only) and cache bucket, via the dspace
#              VPC's S3 gateway endpoint (associated with the public route table)
#
# The AWS provider, including default_tags, is configured by the caller.
# =============================================================================

data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
  region     = var.aws_region

  name = "${var.name_prefix}-image-server"

  cache_bucket_name = var.cache_bucket_name != "" ? var.cache_bucket_name : "${var.name_prefix}-image-cache-${local.account_id}"
  cache_prefix      = "${var.environment}/"
  cache_ttl_seconds = var.cache_ttl_days * 86400

  container_name = "cantaloupe"
  container_port = 8182

  # With no image tag yet, everything is created but the service runs no tasks.
  has_image            = var.image_tag != ""
  min_tasks            = local.has_image ? var.min_tasks : 0
  max_tasks            = local.has_image ? max(var.max_tasks, var.min_tasks) : 0
  image                = "${aws_ecr_repository.this.repository_url}:${local.has_image ? var.image_tag : "unset"}"
  custom_cert          = var.acm_certificate_arn != ""
  public_host          = length(var.domain_aliases) > 0 ? var.domain_aliases[0] : aws_cloudfront_distribution.this.domain_name
  public_base          = "https://${local.public_host}"
  legacy_marker_header = "x-rosa-legacy-path"
}
