# ---------- S3 Gateway VPC Endpoint ----------
#
# The admin Lambda reads staged bulk data from S3 while attached to the VPC with
# no route to the internet, so the VPC needs a path to S3. A gateway endpoint is
# free and adds a route rather than an ENI.
#
# AWS allows only one S3 gateway endpoint association per route table, so this is
# off by default: the jhu-countertable-tf workspace already creates one in
# vpc-07f503e3b0f8d5f8a. Creating a second one against the same route tables fails
# with a RouteAlreadyExists error. Enable this only in a VPC where nothing else
# owns the endpoint.

data "aws_route_tables" "vpc" {
  count  = var.create_s3_vpc_endpoint ? 1 : 0
  vpc_id = var.vpc_id
}

resource "aws_vpc_endpoint" "s3" {
  count = var.create_s3_vpc_endpoint ? 1 : 0

  vpc_id            = var.vpc_id
  service_name      = "com.amazonaws.${var.aws_region}.s3"
  vpc_endpoint_type = "Gateway"

  route_table_ids = data.aws_route_tables.vpc[0].ids

  tags = {
    Name = "${var.name_prefix}-s3-gateway"
  }
}
