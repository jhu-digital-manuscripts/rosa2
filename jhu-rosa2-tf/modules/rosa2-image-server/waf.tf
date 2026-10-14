# ---------- WAF ----------
#
# Adapted from aws-dspace-ecs-tf/modules/baseline-infrastructure/waf.tf, for a
# public, read-only image API rather than a web application:
#
#   - CLOUDFRONT scope (attached to the distribution), so it lives in us-east-1.
#   - The trusted IP set rule is optional.
#   - Bot Control is optional and off by default: it is billed per request,
#     and IIIF clients and harvesters are legitimately "non-browser", so its
#     HTTP-library and non-browser-user-agent rules only count.
#   - The per-IP rate limit is much higher than dspace's 2000/5 min, because a
#     single Mirador user zooming and panning issues hundreds of tile requests
#     a minute (most of them served from the edge cache, but WAF sees them all).

resource "aws_wafv2_web_acl" "this" {
  count = var.enable_waf ? 1 : 0

  name        = "${local.name}-waf"
  description = "rosa2 image server (${var.environment})"
  scope       = "CLOUDFRONT"

  default_action {
    allow {}
  }

  dynamic "rule" {
    for_each = var.waf_trusted_ip_set_arn != "" ? [1] : []
    content {
      name     = "trusted-ips"
      priority = 0

      action {
        allow {}
      }

      statement {
        ip_set_reference_statement {
          arn = var.waf_trusted_ip_set_arn
        }
      }

      visibility_config {
        cloudwatch_metrics_enabled = true
        metric_name                = "${local.name}-trusted-ips"
        sampled_requests_enabled   = true
      }
    }
  }

  rule {
    name     = "AWS-AWSManagedRulesCommonRuleSet"
    priority = 1

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"

        # As in dspace. Image API requests have no body, so these never match
        # legitimate traffic, but keep them non-blocking for consistency.
        rule_action_override {
          name = "SizeRestrictions_BODY"
          action_to_use {
            count {}
          }
        }

        rule_action_override {
          name = "CrossSiteScripting_BODY"
          action_to_use {
            count {}
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name}-common"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "AWS-AWSManagedRulesAmazonIpReputationList"
    priority = 2

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesAmazonIpReputationList"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name}-ip-reputation"
      sampled_requests_enabled   = true
    }
  }

  dynamic "rule" {
    for_each = var.waf_enable_bot_control ? [1] : []
    content {
      name     = "AWS-AWSManagedRulesBotControlRuleSet"
      priority = 3

      override_action {
        none {}
      }

      statement {
        managed_rule_group_statement {
          name        = "AWSManagedRulesBotControlRuleSet"
          vendor_name = "AWS"
          version     = "Version_5.0"

          rule_action_override {
            name = "SignalNonBrowserUserAgent"
            action_to_use {
              count {}
            }
          }

          rule_action_override {
            name = "CategoryHttpLibrary"
            action_to_use {
              count {}
            }
          }

          managed_rule_group_configs {
            aws_managed_rules_bot_control_rule_set {
              enable_machine_learning = false
              inspection_level        = "COMMON"
            }
          }
        }
      }

      visibility_config {
        cloudwatch_metrics_enabled = true
        metric_name                = "${local.name}-bot-control"
        sampled_requests_enabled   = true
      }
    }
  }

  rule {
    name     = "rate-limit-per-ip"
    priority = 4

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit                 = var.waf_rate_limit
        aggregate_key_type    = "IP"
        evaluation_window_sec = 300
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name}-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${local.name}-waf"
    sampled_requests_enabled   = true
  }
}

# ----- Optional logging, as in dspace -----

resource "aws_s3_bucket" "waf_logs" {
  count = var.enable_waf && var.waf_enable_logging ? 1 : 0

  # WAF requires the aws-waf-logs- prefix.
  bucket = "aws-waf-logs-${local.name}-${local.account_id}"
}

resource "aws_s3_bucket_public_access_block" "waf_logs" {
  count = length(aws_s3_bucket.waf_logs)

  bucket                  = aws_s3_bucket.waf_logs[0].id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "waf_logs" {
  count = length(aws_s3_bucket.waf_logs)

  bucket = aws_s3_bucket.waf_logs[0].id

  rule {
    id     = "waf-log-tiering"
    status = "Enabled"

    filter {}

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER_IR"
    }

    expiration {
      days = 365
    }
  }
}

resource "aws_wafv2_web_acl_logging_configuration" "this" {
  count = length(aws_s3_bucket.waf_logs)

  resource_arn            = aws_wafv2_web_acl.this[0].arn
  log_destination_configs = [aws_s3_bucket.waf_logs[0].arn]
}
