# ---------- Latin Analyzer Custom Packages ----------
#
# The manifest and canvas indexes declare a stemmer_override filter and a mapping
# char_filter that read dictionary files from the OpenSearch config directory. In
# local development docker-compose bind-mounts those files; AWS has no such
# mechanism, so each file has to be uploaded to S3, imported as a TXT-DICTIONARY
# package, and associated with the domain. The index definition then refers to it
# as "analyzers/<package-id>" rather than by filename.
#
# The package IDs are only knowable after apply, which is why
# rosa-search/init-opensearch.sh reads them from this workspace's outputs and
# substitutes them into the index definitions before creating the indexes. The
# files checked into rosa-search/opensearch keep their local-dev paths.
#
# Two notes on the upload:
#
#   - Encryption must be SSE-S3. OpenSearch cannot read objects encrypted with a
#     KMS key, and the failure surfaces much later as an unreadable package.
#   - The stemmer rules are 18 MB and load into heap on every index that uses
#     them. Only the file hash is tracked in state, not its contents.

resource "aws_s3_object" "latin_stemmer_rules" {
  bucket = var.analyzer_bucket
  key    = local.stemmer_rules_key
  source = var.latin_stemmer_rules_file

  # source_hash rather than etag: at 18 MB this object may be uploaded as a
  # multipart upload, and a multipart ETag is not the file's MD5, which would
  # leave the resource permanently out of date in the plan.
  source_hash            = filemd5(var.latin_stemmer_rules_file)
  content_type           = "text/plain"
  server_side_encryption = "AES256"

  tags = {
    Name = "${var.name_prefix}-latin-stemmer-rules"
  }
}

resource "aws_s3_object" "latin_charmap" {
  bucket = var.analyzer_bucket
  key    = local.charmap_key
  source = var.latin_charmap_file

  source_hash            = filemd5(var.latin_charmap_file)
  content_type           = "text/plain"
  server_side_encryption = "AES256"

  tags = {
    Name = "${var.name_prefix}-latin-charmap"
  }
}

# Package names are limited to 32 characters, so the suffixes are kept short
# enough to fit the longer name_prefix values (jhu-rosa2-stage is 15).
resource "aws_opensearch_package" "latin_stemmer_rules" {
  package_name        = "${var.name_prefix}-latin-stemmer"
  package_type        = "TXT-DICTIONARY"
  package_description = "Latin lemmatization rules for the stemmer_override filter (rosa2 ${var.environment})"

  package_source {
    s3_bucket_name = aws_s3_object.latin_stemmer_rules.bucket
    s3_key         = aws_s3_object.latin_stemmer_rules.key
  }
}

resource "aws_opensearch_package" "latin_charmap" {
  package_name        = "${var.name_prefix}-latin-charmap"
  package_type        = "TXT-DICTIONARY"
  package_description = "Latin character normalization mappings, v/u j/i ligatures long-s (rosa2 ${var.environment})"

  package_source {
    s3_bucket_name = aws_s3_object.latin_charmap.bucket
    s3_key         = aws_s3_object.latin_charmap.key
  }
}

# Associating a dictionary package does not require a blue/green deployment, but
# it must complete before an index that references the package can be created.
resource "aws_opensearch_package_association" "latin_stemmer_rules" {
  package_id  = aws_opensearch_package.latin_stemmer_rules.id
  domain_name = aws_opensearch_domain.rosa.domain_name
}

resource "aws_opensearch_package_association" "latin_charmap" {
  package_id  = aws_opensearch_package.latin_charmap.id
  domain_name = aws_opensearch_domain.rosa.domain_name
}
