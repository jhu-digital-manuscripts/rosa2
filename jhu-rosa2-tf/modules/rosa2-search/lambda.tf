# ---------- Lambda Functions ----------
#
# Both functions are plain Python with no third-party dependencies: signing uses
# botocore, which the Python runtimes already bundle, and everything else is
# standard library. That means no build step, no layer, and no vendored wheels —
# the zip is the source directory.
#
# The zips are written under the root module's .build/ directory, which is
# gitignored there, rather than inside this module.

data "archive_file" "search" {
  type        = "zip"
  source_dir  = var.search_lambda_source_dir
  output_path = "${path.root}/.build/search_proxy.zip"

  # Running the test suite leaves bytecode caches next to the source. They are
  # built by whatever Python the developer has, not the runtime version, so they
  # would be dead weight in the zip.
  excludes = ["__pycache__", "*.pyc"]
}

data "archive_file" "admin" {
  type        = "zip"
  source_dir  = var.admin_lambda_source_dir
  output_path = "${path.root}/.build/admin.zip"

  excludes = ["__pycache__", "*.pyc"]
}

# Created explicitly rather than left to the first invocation, so that retention
# applies from the start and the IAM policies can name a concrete ARN.
resource "aws_cloudwatch_log_group" "search" {
  name              = "/aws/lambda/${local.search_function_name}"
  retention_in_days = var.log_retention_days
}

resource "aws_cloudwatch_log_group" "admin" {
  name              = "/aws/lambda/${local.admin_function_name}"
  retention_in_days = var.log_retention_days
}

# ---------- Search proxy ----------

resource "aws_lambda_function" "search" {
  function_name = local.search_function_name
  description   = "Validates and SigV4-signs read-only queries from rosa-viewer to the rosa2 OpenSearch domain"

  role    = aws_iam_role.search.arn
  runtime = var.lambda_runtime
  handler = "app.handler"

  filename         = data.archive_file.search.output_path
  source_code_hash = data.archive_file.search.output_base64sha256

  memory_size = var.search_lambda_memory
  timeout     = var.search_lambda_timeout

  reserved_concurrent_executions = var.search_lambda_reserved_concurrency

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = {
      OPENSEARCH_ENDPOINT   = aws_opensearch_domain.rosa.endpoint
      OPENSEARCH_REGION     = local.region
      MAX_BODY_BYTES        = tostring(var.max_body_bytes)
      MAX_PAGE_SIZE         = tostring(var.max_page_size)
      MAX_RESULT_WINDOW     = tostring(var.max_result_window)
      SEARCH_TIMEOUT        = var.search_timeout
      TERMINATE_AFTER       = tostring(var.terminate_after)
      RATE_LIMIT_PER_MINUTE = tostring(var.rate_limit_per_minute)
      RATE_LIMIT_BURST      = tostring(var.rate_limit_burst)
      LOG_REJECTIONS        = "true"
    }
  }

  depends_on = [
    aws_iam_role_policy.search,
    aws_cloudwatch_log_group.search,
  ]
}

# ---------- Admin / ingest ----------

resource "aws_lambda_function" "admin" {
  function_name = local.admin_function_name
  description   = "Creates rosa2 OpenSearch indexes and ingests bulk data staged in S3. Invoked by rosa-search/init-opensearch.sh"

  role    = aws_iam_role.admin.arn
  runtime = var.lambda_runtime
  handler = "app.handler"

  filename         = data.archive_file.admin.output_path
  source_code_hash = data.archive_file.admin.output_base64sha256

  memory_size = var.admin_lambda_memory
  timeout     = var.admin_lambda_timeout

  vpc_config {
    subnet_ids         = var.subnet_ids
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = {
      OPENSEARCH_ENDPOINT = aws_opensearch_domain.rosa.endpoint
      OPENSEARCH_REGION   = local.region
      INGEST_BUCKET       = local.ingest_bucket
      BULK_BATCH_LINES    = "2000"
      BULK_BATCH_BYTES    = "5000000"
      BULK_PAUSE_SECONDS  = "0.3"
      RESERVE_SECONDS     = "60"
    }
  }

  depends_on = [
    aws_iam_role_policy.admin,
    aws_cloudwatch_log_group.admin,
  ]
}
