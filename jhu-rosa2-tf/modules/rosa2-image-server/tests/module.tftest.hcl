# Offline tests for the rosa2-image-server module. No AWS credentials are used:
# the AWS provider is mocked. Run from this module's directory:
#
#   tofu init -backend=false && tofu test
#
# They check the module's own logic (derived names, Cantaloupe environment,
# scale-to-zero before the first image, preconditions), not AWS behaviour.

mock_provider "aws" {
  mock_data "aws_caller_identity" {
    defaults = {
      account_id = "123456789012"
    }
  }

  mock_data "aws_ec2_managed_prefix_list" {
    defaults = {
      id = "pl-3b927c52"
    }
  }

  mock_data "aws_iam_policy_document" {
    defaults = {
      json = "{}"
    }
  }

  mock_resource "aws_cloudfront_distribution" {
    defaults = {
      domain_name = "d111111abcdef8.cloudfront.net"
      arn         = "arn:aws:cloudfront::123456789012:distribution/EDFDVBD6EXAMPLE"
    }
  }

  mock_resource "aws_ecr_repository" {
    defaults = {
      repository_url = "123456789012.dkr.ecr.us-east-1.amazonaws.com/jhu-rosa2-stage-image-server"
      arn            = "arn:aws:ecr:us-east-1:123456789012:repository/jhu-rosa2-stage-image-server"
    }
  }

  mock_resource "aws_s3_bucket" {
    defaults = {
      arn = "arn:aws:s3:::mock-bucket"
    }
  }

  mock_resource "aws_cloudwatch_log_group" {
    defaults = {
      arn = "arn:aws:logs:us-east-1:123456789012:log-group:/ecs/mock"
    }
  }

  mock_resource "aws_iam_role" {
    defaults = {
      arn = "arn:aws:iam::123456789012:role/mock"
    }
  }

  mock_resource "aws_sns_topic" {
    defaults = {
      arn = "arn:aws:sns:us-east-1:123456789012:mock"
    }
  }

  mock_resource "aws_wafv2_web_acl" {
    defaults = {
      arn = "arn:aws:wafv2:us-east-1:123456789012:global/webacl/mock/0000"
    }
  }

  mock_resource "aws_lb" {
    defaults = {
      arn        = "arn:aws:elasticloadbalancing:us-east-1:123456789012:loadbalancer/app/mock/0000"
      arn_suffix = "app/mock/0000"
      dns_name   = "internal-mock-0000.us-east-1.elb.amazonaws.com"
    }
  }

  mock_resource "aws_lb_target_group" {
    defaults = {
      arn        = "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/mock/0000"
      arn_suffix = "targetgroup/mock/0000"
    }
  }

  mock_resource "aws_cloudfront_function" {
    defaults = {
      arn = "arn:aws:cloudfront::123456789012:function/mock"
    }
  }
}

variables {
  environment        = "stage"
  name_prefix        = "jhu-rosa2-stage"
  vpc_id             = "vpc-00000000000000000"
  public_subnet_ids  = ["subnet-pub1", "subnet-pub2"]
  private_subnet_ids = ["subnet-priv1", "subnet-priv2"]
  source_bucket      = "jhu-library-images"
}

run "first_apply_without_image_scales_to_zero" {
  assert {
    condition     = aws_ecs_service.this.desired_count == 0
    error_message = "service must run no tasks before an image tag is set"
  }

  assert {
    condition     = aws_appautoscaling_target.this.max_capacity == 0
    error_message = "auto scaling must be pinned to zero before an image tag is set"
  }

  assert {
    condition     = endswith(output.image, ":unset")
    error_message = "placeholder image tag expected, got ${output.image}"
  }

  assert {
    condition     = length(aws_cloudwatch_metric_alarm.no_healthy_hosts) == 0
    error_message = "the no-healthy-hosts alarm would fire permanently while scaled to zero"
  }
}

run "derived_names_networking_and_cache" {
  variables {
    image_tag      = "5.0.7-abcdef12"
    cache_ttl_days = 14
  }

  # desired_count stays 0 here because the service ignores desired_count
  # changes after creation (auto scaling owns it). Raising the scalable
  # target's minimum to 1 is what makes Application Auto Scaling start a task.
  assert {
    condition     = aws_appautoscaling_target.this.min_capacity == 1 && aws_appautoscaling_target.this.max_capacity == 1
    error_message = "stage defaults should run exactly one task"
  }

  assert {
    condition     = output.image == "123456789012.dkr.ecr.us-east-1.amazonaws.com/jhu-rosa2-stage-image-server:5.0.7-abcdef12"
    error_message = "unexpected image: ${output.image}"
  }

  assert {
    condition     = aws_s3_bucket.cache.bucket == "jhu-rosa2-stage-image-cache-123456789012"
    error_message = "unexpected cache bucket name: ${aws_s3_bucket.cache.bucket}"
  }

  assert {
    condition     = aws_s3_bucket_lifecycle_configuration.cache.rule[0].filter[0].prefix == "stage/"
    error_message = "lifecycle rule must cover the environment prefix"
  }

  assert {
    condition     = aws_s3_bucket_lifecycle_configuration.cache.rule[0].expiration[0].days == 14
    error_message = "lifecycle expiry must equal cache_ttl_days"
  }

  assert {
    condition     = aws_lb.this.subnets == toset(var.private_subnet_ids)
    error_message = "ALB must use the injected private subnets"
  }

  assert {
    condition     = aws_ecs_service.this.network_configuration[0].subnets == toset(var.public_subnet_ids)
    error_message = "tasks must use the injected public subnets"
  }

  assert {
    condition     = aws_lb_target_group.this.vpc_id == var.vpc_id
    error_message = "target group must attach to the injected VPC"
  }

  assert {
    condition     = aws_security_group.alb.vpc_id == var.vpc_id && aws_security_group.task.vpc_id == var.vpc_id
    error_message = "both security groups must attach to the injected VPC"
  }

  assert {
    condition     = aws_lb.this.internal && aws_lb.this.name == "jhu-rosa2-stage-image-server"
    error_message = "ALB must be internal"
  }

  assert {
    condition     = aws_ecs_service.this.network_configuration[0].assign_public_ip
    error_message = "tasks need a public IP for ECR and CloudWatch without a NAT gateway"
  }
}

run "cantaloupe_environment" {
  variables {
    image_tag         = "t1"
    cache_ttl_days    = 14
    http_max_threads  = 48
    extra_environment = { PROCESSOR_JPG_QUALITY = "85", LOG_APPLICATION_LEVEL = "debug" }
  }

  assert {
    condition     = local.cantaloupe_environment.BASE_URI == "https://d111111abcdef8.cloudfront.net"
    error_message = "BASE_URI must be the CloudFront URL, got ${local.cantaloupe_environment.BASE_URI}"
  }

  assert {
    condition = (
      local.cantaloupe_environment.S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME == "jhu-library-images" &&
      local.cantaloupe_environment.S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX == "iiifstore/" &&
      local.cantaloupe_environment.S3CACHE_BUCKET_NAME == "jhu-rosa2-stage-image-cache-123456789012" &&
      local.cantaloupe_environment.S3CACHE_OBJECT_KEY_PREFIX == "stage/" &&
      local.cantaloupe_environment.CACHE_SERVER_DERIVATIVE_TTL_SECONDS == "1209600"
    )
    error_message = "source/cache settings not passed through correctly"
  }

  assert {
    condition     = local.cantaloupe_environment.HTTP_MAX_THREADS == "48" && !contains(keys(local.cantaloupe_environment), "HTTP_ACCEPT_QUEUE_LIMIT")
    error_message = "thread settings not passed through correctly"
  }

  assert {
    condition     = local.cantaloupe_environment.PROCESSOR_JPG_QUALITY == "85" && local.cantaloupe_environment.LOG_APPLICATION_LEVEL == "debug"
    error_message = "extra_environment must be added and must override module values"
  }

  assert {
    condition     = strcontains(aws_ecs_task_definition.this.container_definitions, "\"name\":\"BASE_URI\"")
    error_message = "task definition does not carry the environment"
  }
}

run "custom_domain_sets_base_uri" {
  variables {
    image_tag           = "t1"
    domain_aliases      = ["image.library.jhu.edu"]
    acm_certificate_arn = "arn:aws:acm:us-east-1:123456789012:certificate/00000000-0000-0000-0000-000000000000"
  }

  assert {
    condition     = local.cantaloupe_environment.BASE_URI == "https://image.library.jhu.edu"
    error_message = "BASE_URI must use the first alias"
  }

  assert {
    condition     = output.legacy_iiif_base_url == "https://image.library.jhu.edu/iiif" && output.iiif2_base_url == "https://image.library.jhu.edu/iiif/2"
    error_message = "unexpected base URL outputs"
  }

  assert {
    condition = (
      aws_cloudfront_distribution.this.viewer_certificate[0].ssl_support_method == "sni-only" &&
      aws_cloudfront_distribution.this.viewer_certificate[0].minimum_protocol_version == "TLSv1.2_2021"
    )
    error_message = "custom certificate must use SNI and TLS 1.2+"
  }
}

run "alias_without_certificate_is_rejected" {
  command = plan

  variables {
    domain_aliases = ["image.library.jhu.edu"]
  }

  expect_failures = [aws_cloudfront_distribution.this]
}

run "cloudfront_behaviours" {
  variables {
    image_tag = "t1"
  }

  assert {
    condition     = [for b in aws_cloudfront_distribution.this.ordered_cache_behavior : b.path_pattern] == ["/iiif/2/*", "/iiif/3/*", "/iiif/*"]
    error_message = "versioned paths must take precedence over the legacy /iiif/* behaviour"
  }

  assert {
    condition = (
      length(aws_cloudfront_distribution.this.ordered_cache_behavior[0].function_association) == 0 &&
      length(aws_cloudfront_distribution.this.ordered_cache_behavior[1].function_association) == 0 &&
      length(aws_cloudfront_distribution.this.ordered_cache_behavior[2].function_association) == 2
    )
    error_message = "only the legacy behaviour runs functions"
  }

  assert {
    condition     = aws_cloudfront_cache_policy.legacy.parameters_in_cache_key_and_forwarded_to_origin[0].headers_config[0].headers[0].items == toset(["x-rosa-legacy-path"])
    error_message = "the legacy marker header must be in the legacy cache key"
  }

  assert {
    condition     = aws_cloudfront_cache_policy.iiif.parameters_in_cache_key_and_forwarded_to_origin[0].headers_config[0].header_behavior == "none"
    error_message = "versioned behaviours must not forward viewer headers (cache poisoning via the marker header)"
  }

  assert {
    condition     = aws_cloudfront_cache_policy.iiif.parameters_in_cache_key_and_forwarded_to_origin[0].query_strings_config[0].query_string_behavior == "none"
    error_message = "query strings must not reach Cantaloupe (cache bypass)"
  }

  assert {
    condition     = aws_cloudfront_distribution.this.viewer_certificate[0].cloudfront_default_certificate
    error_message = "without aliases the default *.cloudfront.net certificate is used"
  }

  assert {
    condition     = aws_cloudfront_distribution.this.web_acl_id == null
    error_message = "no WAF unless enabled"
  }

  assert {
    condition = (
      one([for o in aws_cloudfront_distribution.this.origin : o.origin_shield[0].enabled]) &&
      one([for o in aws_cloudfront_distribution.this.origin : o.origin_shield[0].origin_shield_region]) == "us-east-1"
    )
    error_message = "edge misses must go through an Origin Shield in the deployment region by default"
  }
}

run "waf_enabled" {
  variables {
    image_tag              = "t1"
    enable_waf             = true
    waf_enable_bot_control = true
  }

  assert {
    condition     = aws_wafv2_web_acl.this[0].scope == "CLOUDFRONT"
    error_message = "WAF for CloudFront must use CLOUDFRONT scope"
  }

  assert {
    condition     = aws_cloudfront_distribution.this.web_acl_id == aws_wafv2_web_acl.this[0].arn
    error_message = "WAF must be attached to the distribution"
  }

  assert {
    condition     = length(aws_wafv2_web_acl.this[0].rule) == 4
    error_message = "expected common, IP reputation, bot control and rate limit rules"
  }

  assert {
    condition     = length(aws_s3_bucket.waf_logs) == 0
    error_message = "WAF logging is off by default"
  }
}

run "monitoring_disabled" {
  variables {
    image_tag         = "t1"
    enable_monitoring = false
  }

  assert {
    condition     = length(aws_sns_topic.alarms) == 0 && length(aws_cloudwatch_metric_alarm.target_5xx) == 0 && length(aws_cloudwatch_dashboard.this) == 0
    error_message = "monitoring resources must be optional"
  }
}

run "invalid_source_prefix_is_rejected" {
  command = plan

  variables {
    source_prefix = "/iiifstore"
  }

  expect_failures = [var.source_prefix]
}
