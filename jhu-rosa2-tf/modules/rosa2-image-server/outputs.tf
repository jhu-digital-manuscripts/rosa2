# ---------- Outputs ----------

output "public_base_url" {
  description = "Public origin of the image server, e.g. https://dxxxx.cloudfront.net. This is Cantaloupe's base_uri."
  value       = local.public_base
}

output "iiif2_base_url" {
  description = "IIIF Image API 2 base URL: pass as generate-iiif-pres --image-base-url with --image-api-version 2."
  value       = "${local.public_base}/iiif/2"
}

output "iiif3_base_url" {
  description = "IIIF Image API 3 base URL: pass as generate-iiif-pres --image-base-url with --image-api-version 3."
  value       = "${local.public_base}/iiif/3"
}

output "legacy_iiif_base_url" {
  description = "Legacy Image API 2 base URL (/iiif/<id>), as used by existing sites."
  value       = "${local.public_base}/iiif"
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID, for invalidations."
  value       = aws_cloudfront_distribution.this.id
}

output "cloudfront_domain_name" {
  description = "Generated *.cloudfront.net hostname. A custom domain's DNS record (CNAME) must point here."
  value       = aws_cloudfront_distribution.this.domain_name
}

output "legacy_request_function_name" {
  description = "Name of the viewer-request CloudFront Function, for aws cloudfront test-function."
  value       = aws_cloudfront_function.legacy_request.name
}

output "legacy_response_function_name" {
  description = "Name of the viewer-response CloudFront Function, for aws cloudfront test-function."
  value       = aws_cloudfront_function.legacy_response.name
}

output "ecr_repository_url" {
  description = "ECR repository that rosa-image-server/build-and-push.sh pushes to."
  value       = aws_ecr_repository.this.repository_url
}

output "image" {
  description = "Image the service runs (repository:tag); the tag is \"unset\" until image_tag is set."
  value       = local.image
}

output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.this.name
}

output "ecs_service_name" {
  description = "ECS service name."
  value       = aws_ecs_service.this.name
}

output "log_group" {
  description = "CloudWatch log group of the Cantaloupe containers."
  value       = aws_cloudwatch_log_group.this.name
}

output "cache_bucket" {
  description = "S3 derivative cache bucket."
  value       = aws_s3_bucket.cache.bucket
}

output "cache_prefix" {
  description = "Key prefix of this environment's derivatives in cache_bucket."
  value       = local.cache_prefix
}

output "vpc_id" {
  description = "ID of the image server's VPC."
  value       = aws_vpc.this.id
}

output "alb_dns_name" {
  description = "Internal ALB hostname. Not reachable from outside the VPC."
  value       = aws_lb.this.dns_name
}

output "waf_web_acl_arn" {
  description = "ARN of the WAF web ACL, empty when enable_waf is false."
  value       = var.enable_waf ? aws_wafv2_web_acl.this[0].arn : ""
}

output "alarm_topic_arn" {
  description = "SNS topic the alarms notify, empty when enable_monitoring is false."
  value       = var.enable_monitoring ? aws_sns_topic.alarms[0].arn : ""
}
