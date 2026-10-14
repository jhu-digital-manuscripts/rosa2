# ---------- Security Groups ----------
#
#   alb   ingress 80 only from CloudFront's origin-facing addresses (the AWS
#         managed prefix list), which includes the VPC origin's ENIs. The ALB is
#         internal and in private subnets, so nothing else can route to it.
#         Egress only to the task group on 8182.
#   task  ingress 8182 only from the ALB group. Egress 443 for ECR and
#         CloudWatch Logs (over the IGW) and for S3 (via the dspace VPC's S3
#         gateway endpoint on the public route table). The VPC uses the Amazon
#         resolver, which security groups do not filter.

data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}

resource "aws_security_group" "alb" {
  name        = "${local.name}-alb-sg"
  description = "rosa2 image server internal ALB"
  vpc_id      = var.vpc_id

  tags = { Name = "${local.name}-alb-sg" }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_from_cloudfront" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP from CloudFront"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
  prefix_list_id    = data.aws_ec2_managed_prefix_list.cloudfront.id
}

resource "aws_vpc_security_group_egress_rule" "alb_to_tasks" {
  security_group_id            = aws_security_group.alb.id
  description                  = "Cantaloupe on the ECS tasks"
  ip_protocol                  = "tcp"
  from_port                    = local.container_port
  to_port                      = local.container_port
  referenced_security_group_id = aws_security_group.task.id
}

resource "aws_security_group" "task" {
  name        = "${local.name}-task-sg"
  description = "rosa2 image server ECS tasks"
  vpc_id      = var.vpc_id

  tags = { Name = "${local.name}-task-sg" }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "task_from_alb" {
  security_group_id            = aws_security_group.task.id
  description                  = "Cantaloupe from the ALB"
  ip_protocol                  = "tcp"
  from_port                    = local.container_port
  to_port                      = local.container_port
  referenced_security_group_id = aws_security_group.alb.id
}

resource "aws_vpc_security_group_egress_rule" "task_https" {
  security_group_id = aws_security_group.task.id
  description       = "HTTPS to ECR, CloudWatch Logs and S3"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
  cidr_ipv4         = "0.0.0.0/0"
}
