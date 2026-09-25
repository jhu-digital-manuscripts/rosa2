environment = "stage"
aws_region  = "us-east-1"

# Stage-qualified so resources can coexist with prod in the same account. This
# value also becomes the OpenSearch domain name, which must be unique per region.
name_prefix = "jhu-rosa2-stage"

# ---------- Networking ----------
vpc_id     = "vpc-07f503e3b0f8d5f8a"
subnet_ids = ["subnet-00cbb11a099d54bbd"]

vpc_cidr_blocks = ["10.64.0.0/10"]

# JHU resolvers from the VPC's DHCP options; see prod.tfvars.
dns_server_cidrs = ["10.200.1.1/32", "10.200.2.2/32"]

# jhu-countertable-tf already owns the S3 gateway endpoint in this VPC.
create_s3_vpc_endpoint = false

# ---------- OpenSearch domain ----------
opensearch_engine_version = "OpenSearch_3.1"

opensearch_instance_type  = "t3.small.search"
opensearch_instance_count = 1
opensearch_volume_size    = 10
opensearch_volume_type    = "gp3"

opensearch_zone_awareness_enabled   = false
opensearch_dedicated_master_enabled = false

opensearch_enable_log_publishing = true
opensearch_slow_log_threshold_ms = 1000

# ---------- Latin analyzer packages ----------
# Stage uploads to its own keys under this prefix, so refreshing the stage
# dictionaries cannot disturb the package prod is serving from.
analyzer_bucket = "jhu-library-images"
analyzer_prefix = "opensearch-packages"

# ---------- Ingest staging ----------
ingest_bucket = ""
ingest_prefix = "opensearch-ingest"

# ---------- Lambda ----------
lambda_runtime = "python3.13"

search_lambda_memory  = 512
search_lambda_timeout = 30

search_lambda_reserved_concurrency = 10

admin_lambda_memory  = 1024
admin_lambda_timeout = 900

log_retention_days = 14

# ---------- Search request limits ----------
max_body_bytes    = 16384
max_page_size     = 100
max_result_window = 10000
search_timeout    = "10s"
terminate_after   = 200000

rate_limit_per_minute = 120
rate_limit_burst      = 30

# ---------- API Gateway ----------
cors_allowed_origins = ["https://jhu-digital-manuscripts.github.io"]

throttle_rate_limit  = 20
throttle_burst_limit = 40

enable_api_access_logs = true
