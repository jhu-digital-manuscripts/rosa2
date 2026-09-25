# ---------- Security Groups ----------
#
# Two groups, so that reaching the domain requires membership in the Lambda group
# rather than presence in the VPC:
#
#   lambda -> egress anywhere on 443 (OpenSearch and, via the gateway endpoint, S3),
#             and on 53 to the VPC's DNS servers when it uses custom ones
#   domain -> ingress on 443 only from the Lambda group
#
# The domain has no public endpoint, so this is the only network path to it.

resource "aws_security_group" "lambda" {
  name        = "${var.name_prefix}-lambda-sg"
  description = "rosa2 search and admin Lambda functions"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name_prefix}-lambda-sg"
  }

  lifecycle {
    create_before_destroy = true
  }
}

# HTTPS to the OpenSearch domain and to S3 through the gateway endpoint. The
# prefix list of the S3 gateway endpoint is not referenced here because the
# endpoint may be owned by another workspace; 443 egress covers both.
resource "aws_security_group_rule" "lambda_egress_https" {
  type              = "egress"
  description       = "HTTPS to OpenSearch and S3"
  security_group_id = aws_security_group.lambda.id
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

# DNS to the servers in the VPC's DHCP options. The Amazon resolver is exempt from
# security groups, but custom DNS servers are not: without these rules every
# lookup of the domain endpoint times out, which the Lambda runtime reports as
# "[Errno 16] Device or resource busy". DNS uses TCP as well as UDP for large
# responses, so both are opened.
resource "aws_security_group_rule" "lambda_egress_dns" {
  for_each = length(var.dns_server_cidrs) > 0 ? toset(["udp", "tcp"]) : toset([])

  type              = "egress"
  description       = "DNS (${upper(each.key)}) to the VPC DNS servers"
  security_group_id = aws_security_group.lambda.id
  from_port         = 53
  to_port           = 53
  protocol          = each.key
  cidr_blocks       = var.dns_server_cidrs
}

resource "aws_security_group" "opensearch" {
  name        = "${var.name_prefix}-opensearch-sg"
  description = "rosa2 OpenSearch domain"
  vpc_id      = var.vpc_id

  tags = {
    Name = "${var.name_prefix}-opensearch-sg"
  }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group_rule" "opensearch_from_lambda" {
  type                     = "ingress"
  description              = "HTTPS from the rosa2 Lambda functions"
  security_group_id        = aws_security_group.opensearch.id
  from_port                = 443
  to_port                  = 443
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.lambda.id
}

# Optional direct access for troubleshooting from inside the network. Empty by
# default, in which case the Lambdas are the only way in.
resource "aws_security_group_rule" "opensearch_from_network" {
  count = length(var.vpc_cidr_blocks) > 0 ? 1 : 0

  type              = "ingress"
  description       = "HTTPS from internal networks"
  security_group_id = aws_security_group.opensearch.id
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = var.vpc_cidr_blocks
}
