# ---------- ECS ----------
#
# One Fargate service running the rosa-image-server image. Deployment-specific
# Cantaloupe settings are passed as environment variables, which take
# precedence over the cantaloupe.properties baked into the image.

resource "aws_cloudwatch_log_group" "this" {
  name              = "/ecs/${local.name}"
  retention_in_days = var.log_retention_days
}

resource "aws_ecs_cluster" "this" {
  name = local.name

  setting {
    name  = "containerInsights"
    value = var.container_insights ? "enabled" : "disabled"
  }
}

locals {
  cantaloupe_environment = merge(
    {
      # Source images, read-only.
      SOURCE_STATIC                            = "S3Source"
      S3SOURCE_REGION                          = local.region
      S3SOURCE_BASICLOOKUPSTRATEGY_BUCKET_NAME = var.source_bucket
      S3SOURCE_BASICLOOKUPSTRATEGY_PATH_PREFIX = var.source_prefix

      # Derivative cache, this environment's bucket and prefix.
      CACHE_SERVER_DERIVATIVE_ENABLED     = "true"
      CACHE_SERVER_DERIVATIVE             = "S3Cache"
      CACHE_SERVER_DERIVATIVE_TTL_SECONDS = tostring(local.cache_ttl_seconds)
      S3CACHE_REGION                      = local.region
      S3CACHE_BUCKET_NAME                 = aws_s3_bucket.cache.bucket
      S3CACHE_OBJECT_KEY_PREFIX           = local.cache_prefix

      # Public URL used in info.json ids and Link/Location headers.
      BASE_URI = local.public_base

      HTTP_MAX_THREADS      = tostring(var.http_max_threads)
      LOG_APPLICATION_LEVEL = var.log_level
      JAVA_OPTS             = var.java_opts
    },
    var.http_accept_queue_limit == null ? {} : {
      HTTP_ACCEPT_QUEUE_LIMIT = tostring(var.http_accept_queue_limit)
    },
    var.extra_environment,
  )
}

resource "aws_ecs_task_definition" "this" {
  family                   = local.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn

  runtime_platform {
    operating_system_family = "LINUX"
    cpu_architecture        = "X86_64"
  }

  container_definitions = jsonencode([
    {
      name      = local.container_name
      image     = local.image
      essential = true

      portMappings = [
        {
          containerPort = local.container_port
          protocol      = "tcp"
        },
      ]

      environment = [
        for k in sort(keys(local.cantaloupe_environment)) : {
          name  = k
          value = local.cantaloupe_environment[k]
        }
      ]

      healthCheck = {
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${local.container_port}/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }

      # Cantaloupe's temp files and the JVM need a writable root filesystem.
      readonlyRootFilesystem = false

      ulimits = [
        {
          name      = "nofile"
          softLimit = 65536
          hardLimit = 65536
        },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.this.name
          awslogs-region        = local.region
          awslogs-stream-prefix = "ecs"
        }
      }
    },
  ])
}

resource "aws_ecs_service" "this" {
  name            = local.name
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = local.min_tasks
  launch_type     = "FARGATE"

  # Cantaloupe takes ~15 s to start; give the ALB check time before ECS acts on it.
  health_check_grace_period_seconds = 60

  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  network_configuration {
    subnets          = var.public_subnet_ids
    security_groups  = [aws_security_group.task.id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.this.arn
    container_name   = local.container_name
    container_port   = local.container_port
  }

  # Auto scaling owns the running count after creation.
  lifecycle {
    ignore_changes = [desired_count]
  }

  depends_on = [aws_lb_listener.http, aws_iam_role_policy.execution]
}

# ---------- Auto scaling ----------

resource "aws_appautoscaling_target" "this" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.this.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = local.min_tasks
  max_capacity       = local.max_tasks
}

resource "aws_appautoscaling_policy" "cpu" {
  name               = "${local.name}-cpu"
  policy_type        = "TargetTrackingScaling"
  service_namespace  = aws_appautoscaling_target.this.service_namespace
  resource_id        = aws_appautoscaling_target.this.resource_id
  scalable_dimension = aws_appautoscaling_target.this.scalable_dimension

  target_tracking_scaling_policy_configuration {
    target_value       = var.cpu_target_percent
    scale_out_cooldown = 60
    scale_in_cooldown  = 300

    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
  }
}
