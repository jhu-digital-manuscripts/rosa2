# ---------- IAM ----------
#
# Two roles with deliberately different reach:
#
#   search - may POST to the three _search paths and nothing else
#   admin  - may call any OpenSearch API and read staged data from S3
#
# These identity policies are the second half of the pair; the domain's own
# access policy in opensearch.tf grants the matching permissions. A request needs
# both, so widening one without the other has no effect.

data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

# ---------- Search proxy role ----------

resource "aws_iam_role" "search" {
  name               = "${var.name_prefix}-search-role"
  description        = "rosa2 search proxy Lambda"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

data "aws_iam_policy_document" "search" {
  statement {
    sid    = "Logs"
    effect = "Allow"

    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = ["${aws_cloudwatch_log_group.search.arn}:*"]
  }

  # Managing the ENI that attaches the function to the VPC. These calls are not
  # resource-scopable.
  statement {
    sid    = "VpcNetworking"
    effect = "Allow"

    actions = [
      "ec2:CreateNetworkInterface",
      "ec2:DescribeNetworkInterfaces",
      "ec2:DeleteNetworkInterface",
      "ec2:AssignPrivateIpAddresses",
      "ec2:UnassignPrivateIpAddresses",
    ]

    resources = ["*"]
  }

  statement {
    sid    = "SearchOnly"
    effect = "Allow"

    actions = ["es:ESHttpPost"]

    resources = [
      "${local.domain_arn}/manifest/_search",
      "${local.domain_arn}/canvas/_search",
      "${local.domain_arn}/manifest,canvas/_search",
      "${local.domain_arn}/manifest%2Ccanvas/_search",
    ]
  }
}

resource "aws_iam_role_policy" "search" {
  name   = "${var.name_prefix}-search-policy"
  role   = aws_iam_role.search.id
  policy = data.aws_iam_policy_document.search.json
}

# ---------- Admin role ----------

resource "aws_iam_role" "admin" {
  name               = "${var.name_prefix}-admin-role"
  description        = "rosa2 OpenSearch index administration and bulk ingest Lambda"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

data "aws_iam_policy_document" "admin" {
  statement {
    sid    = "Logs"
    effect = "Allow"

    actions = [
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]

    resources = ["${aws_cloudwatch_log_group.admin.arn}:*"]
  }

  statement {
    sid    = "VpcNetworking"
    effect = "Allow"

    actions = [
      "ec2:CreateNetworkInterface",
      "ec2:DescribeNetworkInterfaces",
      "ec2:DeleteNetworkInterface",
      "ec2:AssignPrivateIpAddresses",
      "ec2:UnassignPrivateIpAddresses",
    ]

    resources = ["*"]
  }

  statement {
    sid       = "OpenSearchAdmin"
    effect    = "Allow"
    actions   = ["es:ESHttp*"]
    resources = ["${local.domain_arn}/*"]
  }

  # Read-only, and scoped to the staging prefix. The function only ever reads
  # index definitions and bulk files that init-opensearch.sh has uploaded.
  statement {
    sid       = "ReadStagedIngestData"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["arn:aws:s3:::${local.ingest_bucket}/${local.ingest_key_prefix}/*"]
  }

  statement {
    sid       = "ListStagedIngestData"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::${local.ingest_bucket}"]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["${local.ingest_key_prefix}/*"]
    }
  }
}

resource "aws_iam_role_policy" "admin" {
  name   = "${var.name_prefix}-admin-policy"
  role   = aws_iam_role.admin.id
  policy = data.aws_iam_policy_document.admin.json
}

# ---------- Operator policy ----------
#
# Attach this to whoever runs init-opensearch.sh. It is created but not attached
# to any principal, since the identities that deploy rosa2 are managed elsewhere.

data "aws_iam_policy_document" "ingest_operator" {
  statement {
    sid       = "InvokeAdminLambda"
    effect    = "Allow"
    actions   = ["lambda:InvokeFunction"]
    resources = [aws_lambda_function.admin.arn]
  }

  statement {
    sid    = "StageIngestData"
    effect = "Allow"

    actions = [
      "s3:PutObject",
      "s3:DeleteObject",
    ]

    resources = ["arn:aws:s3:::${local.ingest_bucket}/${local.ingest_key_prefix}/*"]
  }

  statement {
    sid       = "ListIngestBucket"
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::${local.ingest_bucket}"]
  }
}

resource "aws_iam_policy" "ingest_operator" {
  name        = "${var.name_prefix}-ingest-operator"
  description = "Permissions required to run rosa-search/init-opensearch.sh against ${var.environment}"
  policy      = data.aws_iam_policy_document.ingest_operator.json
}
