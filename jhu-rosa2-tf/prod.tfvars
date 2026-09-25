environment = "prod"
aws_region  = "us-east-1"

name_prefix = "jhu-rosa2"

# ---------- Networking ----------
# Same VPC and subnet as the other JHU internal workloads in this account.
vpc_id     = "vpc-07f503e3b0f8d5f8a"
subnet_ids = ["subnet-00cbb11a099d54bbd"]

# Allow direct access to the domain from the internal Hopkins network for
# troubleshooting. Set to [] to make the Lambda functions the only path in.
vpc_cidr_blocks = ["10.64.0.0/10"]

# The VPC's DHCP options (dopt-0ae1000edc05b7ef9) point at the JHU resolvers
# rather than the Amazon one, so the Lambdas need DNS egress to them.
dns_server_cidrs = ["10.200.1.1/32", "10.200.2.2/32"]

# jhu-countertable-tf already creates the S3 gateway endpoint in this VPC, and
# AWS permits only one per route table.
create_s3_vpc_endpoint = false

# ---------- OpenSearch domain ----------
opensearch_engine_version = "OpenSearch_3.1"

# Small single-node domain. The corpus is roughly 35 MB of bulk data across two
# indexes. The constraint is heap rather than disk: the Latin stemmer dictionary
# is 18 MB and is loaded per index that uses it, against the ~1 GiB heap of a
# 2 GiB instance. Move to t3.medium.search if JVMMemoryPressure runs high.
opensearch_instance_type  = "t3.small.search"
opensearch_instance_count = 1
opensearch_volume_size    = 20
opensearch_volume_type    = "gp3"

opensearch_zone_awareness_enabled   = false
opensearch_dedicated_master_enabled = false

opensearch_enable_log_publishing = true
opensearch_slow_log_threshold_ms = 2000

# ---------- Latin analyzer packages ----------
analyzer_bucket = "jhu-library-images"
analyzer_prefix = "opensearch-packages"

# ---------- Ingest staging ----------
# Defaults to analyzer_bucket when left empty.
ingest_bucket = ""
ingest_prefix = "opensearch-ingest"

# ---------- Lambda ----------
lambda_runtime = "python3.13"

search_lambda_memory  = 512
search_lambda_timeout = 30

# Caps how much concurrent load a traffic spike can put on a single-node domain.
search_lambda_reserved_concurrency = 20

admin_lambda_memory  = 1024
admin_lambda_timeout = 900

log_retention_days = 30

# ---------- Search request limits ----------
max_body_bytes    = 16384
max_page_size     = 100
max_result_window = 10000
search_timeout    = "10s"
terminate_after   = 200000

rate_limit_per_minute = 120
rate_limit_burst      = 30

# ---------- API Gateway ----------
# TODO: replace with the public viewer origin, e.g.
#   ["https://rosa2.library.jhu.edu"]
# "*" lets any site issue searches against this endpoint. That is not a data
# exposure risk — the API is read-only over public content — but it does mean
# other origins can consume the domain's capacity.
cors_allowed_origins = ["*"]

throttle_rate_limit  = 50
throttle_burst_limit = 100

enable_api_access_logs = true
