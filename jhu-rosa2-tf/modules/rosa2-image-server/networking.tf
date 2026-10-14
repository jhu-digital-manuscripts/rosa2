# ---------- Networking ----------
#
# Modelled on aws-dspace-ecs-tf/modules/baseline-infrastructure/networking.tf,
# without the NAT gateway:
#
#   public subnets  ECS tasks with public IPs. They need outbound HTTPS to ECR
#                   and CloudWatch Logs; inbound is limited to the ALB's
#                   security group. A public IPv4 address costs far less than a
#                   NAT gateway or a set of interface endpoints.
#   private subnets The internal ALB. CloudFront reaches it through a VPC
#                   origin, which places service-managed ENIs here; there is no
#                   route to or from the internet.
#
# The internet gateway also has to exist for CloudFront VPC origins to work,
# even though they do not route through it.

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "${local.name}-vpc" }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = { Name = "${local.name}-igw" }
}

resource "aws_subnet" "public" {
  count = 2

  vpc_id                  = aws_vpc.this.id
  cidr_block              = local.public_subnet_cidrs[count.index]
  availability_zone       = local.azs[count.index]
  map_public_ip_on_launch = false # tasks request one explicitly

  tags = { Name = "${local.name}-public-${local.azs[count.index]}" }
}

resource "aws_subnet" "private" {
  count = 2

  vpc_id            = aws_vpc.this.id
  cidr_block        = local.private_subnet_cidrs[count.index]
  availability_zone = local.azs[count.index]

  tags = { Name = "${local.name}-private-${local.azs[count.index]}" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = { Name = "${local.name}-public-rt" }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  tags = { Name = "${local.name}-private-rt" }
}

resource "aws_route_table_association" "public" {
  count = 2

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count = 2

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# S3 gateway endpoint: free, and keeps the (heavy) source and cache traffic on
# the AWS network instead of the internet gateway.
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${local.region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_route_table.public.id, aws_route_table.private.id]

  tags = { Name = "${local.name}-s3-endpoint" }
}
