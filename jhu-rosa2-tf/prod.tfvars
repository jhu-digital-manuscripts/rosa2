environment = "prod"
aws_region  = "us-east-1"

name_prefix = "jhu-rosa2"

# ---------- Networking ----------
# vpc_id and subnet_ids are sourced from the dspace prod remote state
# (dspace/prod/opentofu.tfstate); see remote-state.tf. The dspace stack is the
# origin of the network foundation, so prod runs in the dspace prod VPC and the
# module slices the private subnets to one for this single-node domain.

# Allow direct access to the domain from the internal Hopkins network for
# troubleshooting. Set to [] to make the Lambda functions the only path in.
vpc_cidr_blocks = ["10.64.0.0/10"]

# The dspace prod VPC's DHCP options use AmazonProvidedDNS, which security
# groups do not filter, so the Lambdas resolve the domain endpoint without
# this. These JHU resolvers are a leftover from the previous VPC and only add
# an unused DNS egress rule; set to [] to drop it.
dns_server_cidrs = ["10.200.1.1/32", "10.200.2.2/32"]

# dspace already owns the S3 gateway endpoint in this VPC, associated with its
# private route tables, and AWS permits only one per route table.
create_s3_vpc_endpoint = false

# ---------- OpenSearch domain ----------
opensearch_engine_version = "OpenSearch_3.1"

# Small single-node domain. The corpus is roughly 35 MB of bulk data across two
# indexes. The constraint is heap rather than disk: the Latin stemmer dictionary
# is 18 MB and is loaded per index that uses it, against the ~1 GiB heap of a
# 2 GiB instance. t3.small.search proved too small in practice — search latency
# swung between 20 ms warm and 13 s under even light concurrency, which surfaced
# to the viewer as 502/503/504 — so this is t3.medium.search (4 GiB, ~2 GiB heap).
# Note that t3 is burstable: if CPUCreditBalance still trends to zero under real
# traffic, the fix is to leave the T family rather than to grow within it.
opensearch_instance_type  = "t3.medium.search"
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
terminate_after   = 200000

# Below upstream_timeout on purpose, so a slow query ends as a 200 carrying
# partial results and timed_out: true, which the viewer shows as a warning beside
# whatever it did find. If this were the larger of the two, the proxy's socket
# would give up first and the user would get a 504 and nothing.
search_timeout = "6s"

rate_limit_per_minute = 120
rate_limit_burst      = 30

# ---------- Search proxy upstream behaviour ----------
# Fail fast rather than hold a concurrency slot. The proxy retries once on an
# unreachable domain or an upstream 502/503/504, and never retries a timeout, so
# a single request occupies its Lambda slot for at most about 16 s.
upstream_timeout  = 8
upstream_attempts = 2
upstream_backoff  = 0.25

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

# =============================================================================
# Image server (modules/rosa2-image-server)
# =============================================================================
# Unset inputs take the module defaults in modules/rosa2-image-server/variables.tf.

# Image built and pushed with rosa-image-server/build-and-push.sh --env prod --push.
# Empty creates the infrastructure with the service scaled to zero.
image_server_image_tag = ""

# ---------- Networking ----------
# Runs in the dspace prod VPC, sourced from the dspace prod remote state
# (dspace/prod/opentofu.tfstate); see remote-state.tf. Tasks use the dspace
# public subnets and the internal ALB uses the AZ-filtered private subnets.

# ---------- Images ----------
image_server_source_bucket = "jhu-library-images"
image_server_source_prefix = "iiifstore/"

# Derivative cache: own bucket (<name_prefix>-image-cache-<account>), prefix "prod/".
image_server_cache_ttl_days = 30

# ---------- ECS ----------
# 2 vCPU / 4 GiB
image_server_task_cpu         = 2048
image_server_task_memory      = 4096
image_server_min_tasks        = 1
image_server_max_tasks        = 1
image_server_http_max_threads = 64

image_server_log_level          = "warn"
image_server_log_retention_days = 30

# ---------- CloudFront ----------
# Served on the generated *.cloudfront.net name until the cutover from the
# current image.library.jhu.edu server. Then set, with a us-east-1 ACM
# certificate requested outside tofu:
#   image_server_domain_aliases      = ["image.library.jhu.edu"]
#   image_server_acm_certificate_arn = "arn:aws:acm:us-east-1:...:certificate/..."
# and point the DNS CNAME at the image_server_cloudfront_domain_name output.
image_server_domain_aliases      = []
image_server_acm_certificate_arn = ""

# ---------- WAF and monitoring ----------
image_server_enable_waf     = true
image_server_waf_rate_limit = 20000

image_server_enable_monitoring = true
# e.g. the alerts address used by aws-dspace-ecs-tf.
image_server_alarm_notification_email = ""
