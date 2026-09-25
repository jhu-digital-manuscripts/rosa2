terraform {
  # 1.10 introduced S3-native state locking (use_lockfile), which the
  # *.s3.tfbackend files rely on instead of a DynamoDB lock table.
  required_version = ">= 1.10.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.4"
    }
  }

  # Partial backend configuration. The concrete bucket/key/region values live in
  # the per-environment *.s3.tfbackend files and are supplied at init time:
  #
  #   tofu init -backend-config=prod.s3.tfbackend
  #   tofu init -reconfigure -backend-config=stage.s3.tfbackend
  #
  # The backend block cannot reference variables, which is why each environment
  # uses its own state key in a dedicated tfbackend file.
  backend "s3" {}
}
