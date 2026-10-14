# ---------- Internal Application Load Balancer ----------
#
# The CloudFront VPC origin. HTTP only: TLS terminates at CloudFront, and the
# hop from the VPC origin ENIs to the ALB stays inside the VPC.

resource "aws_lb" "this" {
  name               = local.name
  internal           = true
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.private[*].id

  idle_timeout               = var.alb_idle_timeout
  drop_invalid_header_fields = true
}

resource "aws_lb_target_group" "this" {
  name                 = local.name
  port                 = local.container_port
  protocol             = "HTTP"
  vpc_id               = aws_vpc.this.id
  target_type          = "ip"
  deregistration_delay = 30

  health_check {
    path                = "/health"
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.this.arn
  }
}
