provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

# Preserve the existing shared connector's tags; do not inherit stage defaults.
provider "aws" {
  alias  = "chatbot"
  region = "us-east-2"
}
