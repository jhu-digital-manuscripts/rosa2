# ---------- Derivative cache bucket ----------
#
# Cantaloupe's S3Cache stores derivative images and image infos here, under
# "<environment>/". Each environment owns its own bucket (and so its own
# lifecycle configuration, of which S3 allows only one per bucket).
#
# Cantaloupe's cache worker is disabled (cantaloupe.properties), so expiry is
# done here, from the same TTL Cantaloupe is given.

resource "aws_s3_bucket" "cache" {
  bucket        = local.cache_bucket_name
  force_destroy = var.cache_bucket_force_destroy
}

resource "aws_s3_bucket_public_access_block" "cache" {
  bucket = aws_s3_bucket.cache.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "cache" {
  bucket = aws_s3_bucket.cache.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cache" {
  bucket = aws_s3_bucket.cache.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "cache" {
  bucket = aws_s3_bucket.cache.id

  rule {
    id     = "expire-${var.environment}-derivatives"
    status = "Enabled"

    filter {
      prefix = local.cache_prefix
    }

    expiration {
      days = var.cache_ttl_days
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }
}

# Deny anything that is not TLS, as a backstop to the IAM policies.
data "aws_iam_policy_document" "cache_bucket" {
  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.cache.arn,
      "${aws_s3_bucket.cache.arn}/*",
    ]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "cache" {
  bucket = aws_s3_bucket.cache.id
  policy = data.aws_iam_policy_document.cache_bucket.json

  depends_on = [aws_s3_bucket_public_access_block.cache]
}
