terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      # 5.82 added aws_cloudfront_vpc_origin.
      version = "~> 5.82"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }
}
