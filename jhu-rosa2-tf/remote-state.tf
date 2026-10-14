# =============================================================================
# DSPACE REMOTE STATE
# =============================================================================
# The network foundation (VPC and private subnets) originates in the dspace
# deployment, so rosa2 reads it from dspace's remote state rather than carrying
# hardcoded IDs. The dspace state lives in the same S3 bucket, keyed per
# environment, so var.environment ("stage" or "prod") selects the matching
# dspace state and the rosa2 run lands in that environment's VPC.
#
# Cross-stack coupling: rosa2 depends on the dspace state for this environment
# existing and on dspace continuing to export vpc_id, public_subnet_ids, and
# private_subnet_ids with stable names. Renaming or removing any of these in
# dspace breaks rosa2's plan.
# =============================================================================

data "terraform_remote_state" "dspace" {
  backend = "s3"

  config = {
    bucket = "jhu-drcc-tf-state-bucket"
    key    = "dspace/${var.environment}/opentofu.tfstate"
    region = var.aws_region
  }
}

# dspace exposes subnet IDs only (no AZ metadata), so look each private subnet
# up to read its availability_zone_id for the image server's AZ filtering.
# Keyed by subnet ID (for_each over a toset) so the lookups stay stable if
# dspace reorders or adds subnets.
data "aws_subnet" "dspace_private" {
  for_each = toset(data.terraform_remote_state.dspace.outputs.private_subnet_ids)
  id       = each.value
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

  # Private subnets for the image server's internal ALB and the CloudFront VPC
  # origin ENIs, with any AZ CloudFront VPC origins do not support filtered out.
  # Fail-open: a subnet whose availability_zone_id is empty/unknown does not
  # match the exclusion list and is kept, not dropped. The module's
  # private_subnet_ids >= 2 validation is the backstop. Do NOT invert this into
  # a supported-AZ allowlist.
  image_server_private_subnet_ids = [
    for id, s in data.aws_subnet.dspace_private :
    id if !contains(var.vpc_origin_unsupported_zone_ids, s.availability_zone_id)
  ]

  # Public subnets for the image server's Fargate tasks (public IP, no NAT).
  # Passed through unfiltered: tasks have no AZ restriction, so the asymmetry
  # with the AZ-filtered private list above is intentional — only the private
  # list carries the CloudFront VPC origin constraint.
  image_server_public_subnet_ids = data.terraform_remote_state.dspace.outputs.public_subnet_ids
}
