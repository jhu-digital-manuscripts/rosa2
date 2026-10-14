# =============================================================================
# DSPACE REMOTE STATE
# =============================================================================
# The network foundation (VPC and private subnets) originates in the dspace
# deployment, so rosa2 reads it from dspace's remote state rather than carrying
# hardcoded IDs. The dspace state lives in the same S3 bucket, keyed per
# environment, so var.environment ("stage" or "prod") selects the matching
# dspace state and the rosa2 run lands in that environment's VPC.
# =============================================================================

data "terraform_remote_state" "dspace" {
  backend = "s3"

  config = {
    bucket = "jhu-drcc-tf-state-bucket"
    key    = "dspace/${var.environment}/opentofu.tfstate"
    region = var.aws_region
  }
}

locals {
  # dspace exposes several private subnets, but this single-node OpenSearch
  # domain needs exactly one subnet per availability zone. Slice the remote
  # list down to the subnet count the domain vars imply: one zone unless zone
  # awareness is enabled, in which case one per data node.
  domain_subnet_ids = slice(
    data.terraform_remote_state.dspace.outputs.private_subnet_ids,
    0,
    var.opensearch_zone_awareness_enabled ? var.opensearch_instance_count : 1,
  )
}
