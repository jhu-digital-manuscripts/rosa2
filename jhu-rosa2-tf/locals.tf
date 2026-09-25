locals {
  common_tags = merge(
    {
      Owner       = "drcc"
      Environment = var.environment
      System      = "rosa2"
    },
    var.tags,
  )
}
