terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
      # 5.82 added aws_cloudfront_vpc_origin.
      version = "~> 5.82"
    }
  }
}
