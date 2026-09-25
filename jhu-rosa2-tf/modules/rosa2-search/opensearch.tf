# ---------- OpenSearch Domain ----------
#
# VPC-only: the domain gets no public endpoint, so the only way to reach it is
# from inside the VPC through the Lambda security group. That is why both the
# search proxy and the admin function are VPC-attached, and why the ingest script
# drives the admin function instead of talking to the domain directly.
#
# Access is restricted twice over. The domain access policy below names the two
# Lambda roles and, for the search role, the exact request paths it may use; the
# roles' own identity policies in iam.tf grant the matching permissions. Both
# sides must allow a request for it to succeed, so the search proxy cannot reach
# a write API even if its code were replaced.

data "aws_iam_policy_document" "domain_access" {
  # Read-only, and only the three _search paths the viewer uses. es:ESHttpPost is
  # also what _bulk and _delete_by_query require, so restricting the action alone
  # would not prevent writes; the resource paths are what make this read-only.
  statement {
    sid    = "SearchProxyReadOnly"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = [aws_iam_role.search.arn]
    }

    actions = ["es:ESHttpPost"]

    resources = [
      "${local.domain_arn}/manifest/_search",
      "${local.domain_arn}/canvas/_search",
      "${local.domain_arn}/manifest,canvas/_search",
      "${local.domain_arn}/manifest%2Ccanvas/_search",
    ]
  }

  statement {
    sid    = "AdminFullAccess"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = [aws_iam_role.admin.arn]
    }

    actions   = ["es:ESHttp*"]
    resources = ["${local.domain_arn}/*"]
  }
}

resource "aws_opensearch_domain" "rosa" {
  domain_name    = local.domain_name
  engine_version = var.opensearch_engine_version

  cluster_config {
    instance_type  = var.opensearch_instance_type
    instance_count = var.opensearch_instance_count

    zone_awareness_enabled = var.opensearch_zone_awareness_enabled

    dynamic "zone_awareness_config" {
      for_each = var.opensearch_zone_awareness_enabled ? [1] : []
      content {
        availability_zone_count = length(var.subnet_ids)
      }
    }

    dedicated_master_enabled = var.opensearch_dedicated_master_enabled
    dedicated_master_type    = var.opensearch_dedicated_master_enabled ? var.opensearch_dedicated_master_type : null
    dedicated_master_count   = var.opensearch_dedicated_master_enabled ? var.opensearch_dedicated_master_count : null
  }

  ebs_options {
    ebs_enabled = true
    volume_size = var.opensearch_volume_size
    volume_type = var.opensearch_volume_type

    # Stated explicitly for gp3 so the gp3 baseline does not read back as a diff.
    iops       = var.opensearch_volume_type == "gp3" ? 3000 : null
    throughput = var.opensearch_volume_type == "gp3" ? 125 : null
  }

  vpc_options {
    subnet_ids         = var.subnet_ids
    security_group_ids = [aws_security_group.opensearch.id]
  }

  encrypt_at_rest {
    enabled = true
  }

  node_to_node_encryption {
    enabled = true
  }

  domain_endpoint_options {
    enforce_https       = true
    tls_security_policy = "Policy-Min-TLS-1-2-PFS-2023-10"
  }

  # T3 instance types do not support Auto-Tune. Leaving this unset lets AWS
  # enable it and the apply fails.
  auto_tune_options {
    desired_state       = "DISABLED"
    rollback_on_disable = "NO_ROLLBACK"
  }

  off_peak_window_options {
    enabled = true

    off_peak_window {
      window_start_time {
        hours   = 2
        minutes = 0
      }
    }
  }

  software_update_options {
    auto_software_update_enabled = true
  }

  access_policies = data.aws_iam_policy_document.domain_access.json

  dynamic "log_publishing_options" {
    for_each = var.opensearch_enable_log_publishing ? ["ES_APPLICATION_LOGS", "SEARCH_SLOW_LOGS", "INDEX_SLOW_LOGS"] : []

    content {
      log_type                 = log_publishing_options.value
      cloudwatch_log_group_arn = aws_cloudwatch_log_group.opensearch[0].arn
      enabled                  = true
    }
  }

  tags = {
    Name = local.domain_name
  }

  depends_on = [
    aws_cloudwatch_log_resource_policy.opensearch,
  ]
}

# ---------- Domain logs ----------

resource "aws_cloudwatch_log_group" "opensearch" {
  count = var.opensearch_enable_log_publishing ? 1 : 0

  name              = "/aws/opensearch/${local.domain_name}"
  retention_in_days = var.log_retention_days
}

data "aws_iam_policy_document" "opensearch_logs" {
  count = var.opensearch_enable_log_publishing ? 1 : 0

  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["es.amazonaws.com"]
    }

    actions = [
      "logs:PutLogEvents",
      "logs:CreateLogStream",
    ]

    resources = ["${aws_cloudwatch_log_group.opensearch[0].arn}:*"]

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [local.account_id]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = [local.domain_arn]
    }
  }
}

resource "aws_cloudwatch_log_resource_policy" "opensearch" {
  count = var.opensearch_enable_log_publishing ? 1 : 0

  policy_name     = "${var.name_prefix}-opensearch-logs"
  policy_document = data.aws_iam_policy_document.opensearch_logs[0].json
}
