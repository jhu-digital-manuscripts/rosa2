# ---------- IAM ----------
#
# Two roles, both assumable only by ECS tasks in this account:
#
#   execution  used by the ECS agent: pull this environment's image and write to
#              its log group. Nothing else.
#   task       used by Cantaloupe: read the source images, read/write/delete
#              under this environment's prefix in its own cache bucket.
#
# s3:ListBucket is granted (limited by prefix) so that a missing object is
# reported by S3 as 404 rather than 403, which Cantaloupe turns into a 404
# for the client instead of a 500.

data "aws_iam_policy_document" "ecs_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [local.account_id]
    }

    condition {
      test     = "ArnLike"
      variable = "aws:SourceArn"
      values   = ["arn:aws:ecs:${local.region}:${local.account_id}:*"]
    }
  }
}

# ----- Execution role -----

resource "aws_iam_role" "execution" {
  name               = "${local.name}-execution"
  description        = "ECS agent role for ${local.name}: image pull and logging"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

data "aws_iam_policy_document" "execution" {
  statement {
    sid       = "EcrAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EcrPull"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
    ]
    resources = [aws_ecr_repository.this.arn]
  }

  statement {
    sid       = "Logs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.this.arn}:*"]
  }
}

resource "aws_iam_role_policy" "execution" {
  name   = "pull-and-log"
  role   = aws_iam_role.execution.id
  policy = data.aws_iam_policy_document.execution.json
}

# ----- Task role -----

resource "aws_iam_role" "task" {
  name               = "${local.name}-task"
  description        = "Cantaloupe role for ${local.name}: read source images, read/write derivative cache"
  assume_role_policy = data.aws_iam_policy_document.ecs_tasks_assume.json
}

data "aws_iam_policy_document" "task" {
  statement {
    sid       = "ReadSourceImages"
    actions   = ["s3:GetObject"]
    resources = ["arn:aws:s3:::${var.source_bucket}/${var.source_prefix}*"]
  }

  statement {
    sid       = "ListSourceImages"
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::${var.source_bucket}"]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["${var.source_prefix}*"]
    }
  }

  statement {
    sid = "ReadWriteCache"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]
    resources = ["${aws_s3_bucket.cache.arn}/${local.cache_prefix}*"]
  }

  statement {
    sid       = "ListCache"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.cache.arn]

    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["${local.cache_prefix}*"]
    }
  }
}

resource "aws_iam_role_policy" "task" {
  name   = "s3-images-and-cache"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task.json
}

# ECS Exec: the SSM agent Fargate injects into the task opens its Session
# Manager channels with the task role. These actions cannot be scoped to a
# resource. Only attached while enable_execute_command is on.
data "aws_iam_policy_document" "task_exec" {
  statement {
    sid = "EcsExecSessionChannels"
    actions = [
      "ssmmessages:CreateControlChannel",
      "ssmmessages:CreateDataChannel",
      "ssmmessages:OpenControlChannel",
      "ssmmessages:OpenDataChannel",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "task_exec" {
  count  = var.enable_execute_command ? 1 : 0
  name   = "ecs-exec"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task_exec.json
}
