# Latin Analyzer for OpenSearch

This document describes the Latin text analyzer configurations for OpenSearch, including both the primary stemmer-based approach and a fallback configuration.

## Overview

Latin presents unique challenges for text analysis:
- Rich morphology with extensive noun declensions and verb conjugations
- No built-in stemmer support in OpenSearch/Elasticsearch
- Early modern Latin texts may have spelling variations

We provide two analyzer configurations:

1. **Primary**: Uses `stemmer_override` with a comprehensive lemma mapping table
2. **Fallback**: Uses stop words only (no stemming, but removes common function words)

## Primary Configuration: Stemmer Override with Character Normalization

The primary approach uses the `stemmer_override` token filter with a rules file generated from a Latin morphological lexicon. This provides true lemmatization by mapping inflected forms to their dictionary headwords.

**Important**: The char_filter is essential, not optional. The lemma table uses classical orthography where inflected forms use 'u' (e.g., `diuidunt`, `aduenit`) but lemmas use modern 'v' (e.g., `divido`, `advenio`). Without the char_filter:
- Text containing `dividunt` (with 'v') won't match the rule expecting `diuidunt`
- Text containing `advenit` won't match the rule expecting `aduenit`

The char_filter normalizes v→u before the stemmer rules are applied, ensuring matches regardless of spelling convention.

### Files Required

- `latin-stemmer-rules.txt` - Contains 29,760 rules covering ~1.38 million inflected forms
- `latin-charmap.txt` - Character mappings for v→u, j→i, ligatures, and long s

### Analyzer Configuration

```json
{
  "settings": {
    "analysis": {
      "char_filter": {
        "latin_char_normalizer": {
          "type": "mapping",
          "mappings_path": "analyzers/latin-charmap.txt"
        }
      },
      "filter": {
        "latin_stemmer": {
          "type": "stemmer_override",
          "rules_path": "analyzers/latin-stemmer-rules.txt"
        }
      },
      "analyzer": {
        "latin": {
          "type": "custom",
          "char_filter": ["latin_char_normalizer"],
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "latin_stemmer",
            "asciifolding"
          ]
        }
      }
    }
  }
}
```

### Processing Pipeline

1. **char_filter** (latin_char_normalizer): `dividunt` → `diuidunt`, `cælum` → `caelum`
2. **tokenizer** (standard): splits into tokens
3. **lowercase**: `Diuidunt` → `diuidunt`
4. **latin_stemmer**: `diuidunt` → `divido` (matches rule!)
5. **asciifolding**: handles any remaining diacritics

### Local Development Setup

For local Docker-based development, mount both files into the OpenSearch container:

```yaml
volumes:
  - ../opensearch/latin-stemmer-rules.txt:/usr/share/opensearch/config/analyzers/latin-stemmer-rules.txt:ro
  - ../opensearch/latin-charmap.txt:/usr/share/opensearch/config/analyzers/latin-charmap.txt:ro
```

### AWS OpenSearch Service Setup

For AWS managed OpenSearch, both files must be uploaded as custom packages:

#### Step 1: Upload to S3

```bash
# Upload both files to your S3 bucket
aws s3 cp deploy/opensearch/latin-stemmer-rules.txt s3://your-bucket/opensearch-packages/latin-stemmer-rules.txt
aws s3 cp deploy/opensearch/latin-charmap.txt s3://your-bucket/opensearch-packages/latin-charmap.txt
```

#### Step 2: Create Custom Packages

Create two packages - one for the stemmer rules and one for the character mappings:

Using AWS CLI:
```bash
# Create stemmer rules package
aws opensearch create-package \
  --package-name latin-stemmer-rules \
  --package-type TXT-DICTIONARY \
  --package-description "Latin lemmatization rules for stemmer_override filter" \
  --package-source "S3BucketName=your-bucket,S3Key=opensearch-packages/latin-stemmer-rules.txt" \
  --region us-east-1

# Create charmap package
aws opensearch create-package \
  --package-name latin-charmap \
  --package-type TXT-DICTIONARY \
  --package-description "Latin character normalization mappings (v/u, j/i, ligatures)" \
  --package-source "S3BucketName=your-bucket,S3Key=opensearch-packages/latin-charmap.txt" \
  --region us-east-1
```

#### Step 3: Associate Packages with Domain

Associate both packages with your OpenSearch domain:

Using AWS Console:
1. Go to **Packages** and find each package
2. Wait for status to become **Available**
3. Select each package and click **Associate to a domain**
4. Select your OpenSearch domain and click **Associate**
5. Wait for association status to become **Active**
6. Note the **Package IDs** (e.g., `F123456789` for stemmer rules, `F987654321` for charmap)

Using AWS CLI:
```bash
# Associate both packages (get package IDs from create-package responses)
aws opensearch associate-package \
  --package-id F123456789 \
  --domain-name your-domain-name \
  --region us-east-1

aws opensearch associate-package \
  --package-id F987654321 \
  --domain-name your-domain-name \
  --region us-east-1
```

#### Step 4: Update Index Configuration

In your index definition, use both package IDs:

```json
{
  "settings": {
    "analysis": {
      "char_filter": {
        "latin_char_normalizer": {
          "type": "mapping",
          "mappings_path": "analyzers/F987654321"
        }
      },
      "filter": {
        "latin_stemmer": {
          "type": "stemmer_override",
          "rules_path": "analyzers/F123456789"
        }
      },
      "analyzer": {
        "latin": {
          "type": "custom",
          "char_filter": ["latin_char_normalizer"],
          "tokenizer": "standard",
          "filter": ["lowercase", "latin_stemmer", "asciifolding"]
        }
      }
    }
  }
}
```

**Important**: Replace `F123456789` and `F987654321` with your actual package IDs from Step 3.

#### Updating the Rules File

When you need to update the stemmer rules:

1. Upload the new file to S3 (same path or new path)
2. In OpenSearch Service console, go to **Packages**
3. Select your package and click **Update**
4. Provide the new S3 path
5. After the package status returns to **Available**, select it
6. Choose your domain and click **Apply update**

If your analyzer uses `"updateable": true`, the update will apply automatically via hot-reload. Otherwise, you'll need to reindex.

## Fallback Configuration: Stop Words with Orthographic Normalization

If stemmer_override is not feasible (e.g., file size constraints, deployment complexity), use this configuration that:
1. Normalizes early modern Latin spelling variations (u/v, i/j, ligatures)
2. Filters common Latin function words

This approach significantly improves search recall by ensuring that spelling variations match, even without true stemming.

### Files Required

- `latin-stopwords.txt` - Common Latin conjunctions, prepositions, pronouns, and particles
- `latin-charmap.txt` - Character mappings for orthographic normalization

### Orthographic Normalizations

The `latin_char_normalizer` character filter handles these common variations:

| Variation | Example | Normalized |
|-----------|---------|------------|
| V → U | vnde, vbi | unde, ubi |
| J → I | iam, iustitia | iam, iustitia |
| Æ → AE | cælum | caelum |
| Œ → OE | fœdus | foedus |
| ſ → s | ſumma | summa |

### Analyzer Configuration

```json
{
  "settings": {
    "analysis": {
      "char_filter": {
        "latin_char_normalizer": {
          "type": "mapping",
          "mappings_path": "analyzers/latin-charmap.txt"
        }
      },
      "filter": {
        "latin_stop": {
          "type": "stop",
          "stopwords_path": "analyzers/latin-stopwords.txt"
        }
      },
      "analyzer": {
        "latin": {
          "type": "custom",
          "char_filter": ["latin_char_normalizer"],
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "latin_stop",
            "asciifolding"
          ]
        }
      }
    }
  }
}
```

### Why This Helps

Consider searching for "universum" (universe):
- A text might contain "vniuerſum" (early modern spelling)
- Without normalization: no match
- With char_filter: "vniuerſum" → "uniuersum" → matches "universum" after lowercase

### Local Development Setup (Fallback)

For local Docker, mount both files:

```yaml
volumes:
  - ../opensearch/latin-stopwords.txt:/usr/share/opensearch/config/analyzers/latin-stopwords.txt:ro
  - ../opensearch/latin-charmap.txt:/usr/share/opensearch/config/analyzers/latin-charmap.txt:ro
```

### AWS Setup (Fallback)

Upload both files as separate custom packages and reference them by package ID:

```json
{
  "char_filter": {
    "latin_char_normalizer": {
      "type": "mapping",
      "mappings_path": "analyzers/F111111111"
    }
  },
  "filter": {
    "latin_stop": {
      "type": "stop",
      "stopwords_path": "analyzers/F222222222"
    }
  }
}
```

### Limitations of Fallback

- No stemming: "amat", "amabat", "amavit" remain separate tokens
- Search for "amo" will not find "amat" or "amavit"
- However, spelling variations ARE normalized, improving recall for variant spellings

## Comparison

| Feature | Primary (stemmer_override) | Fallback (char_filter + stop words) |
|---------|---------------------------|-------------------------------------|
| True stemming | ✓ | ✗ |
| Spelling normalization (u/v, i/j) | via rules | ✓ (char_filter) |
| Ligature expansion (æ, œ) | via rules | ✓ (char_filter) |
| Stop word removal | ✗ | ✓ |
| Total file size | 18 MB | ~4 KB |
| Setup complexity | Higher | Medium |
| Search recall (inflections) | High | Low |
| Search recall (spelling variants) | High | High |
| Search precision | High | High |
| Hot-reload support | ✓ | ✓ |

### When to Use Each

**Use Primary (stemmer_override)** when:
- You need full morphological matching (amo ↔ amat ↔ amavit)
- File size is not a constraint
- You can set up custom packages in AWS

**Use Fallback (char_filter + stop words)** when:
- You want simpler deployment
- Spelling variant matching is more important than inflection matching
- Your corpus has significant u/v and i/j variations

## Regenerating the Stemmer Rules

If you need to regenerate the stemmer rules (e.g., after updating the lemma table):

```bash
python3 deploy/scripts/generate_latin_stemmer_rules.py
```

This reads `deploy/latin-lemma-table.jsonl` and outputs to `deploy/opensearch/latin-stemmer-rules.txt`.

The table is from https://zenodo.org/records/21884913.


## Testing the Analyzer

After creating an index with the Latin analyzer, test it:

```bash
# Test tokenization
POST /your-index/_analyze
{
  "analyzer": "latin",
  "text": "Gallia est omnis divisa in partes tres"
}
```

Expected output with primary analyzer: tokens will be lemmatized (e.g., "divisa" → "divido").
Expected output with fallback: tokens preserved but stop words removed (e.g., "est", "in" removed).
