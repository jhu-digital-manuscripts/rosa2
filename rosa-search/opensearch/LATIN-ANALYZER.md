# Latin Analyzer for OpenSearch

How the `latin` analyzer in `manifest.json` and `canvas.json` lemmatizes Latin
text, what its files are, how to run it locally and in AWS, and what it cannot do.
Verified against OpenSearch 3.1, the version the AWS domains run and the version
`local-dev/docker-compose.yml` pins.

## Overview

Latin has rich morphology (noun declensions, verb conjugations), no built-in
OpenSearch stemmer, and in early modern and medieval texts a variable
orthography: `u`/`v` and `i`/`j` are interchangeable, ligatures (`æ`, `œ`) and
the long s (`ſ`) are common, and macrons may mark vowel length.

The analyzer handles this in two layers:

1. **Orthographic normalization** with a `mapping` character filter
   (`latin-charmap.txt`): `v → u`, `j → i`, `æ → ae`, `œ → oe`, `ſ → s`, applied
   to both indexed text and queries.
2. **Lemmatization** with a `stemmer_override` token filter
   (`latin-stemmer-rules.txt`): 29,760 rules mapping about 1.38 million inflected
   forms to their dictionary headwords, generated from a Latin morphological
   lexicon.

## Files

| File | Purpose |
|---|---|
| `latin-charmap.txt` | Character mappings for the `latin_char_normalizer` char filter. |
| `latin-stemmer-rules.txt` | `form1, form2 => lemma` rules for the `latin_stemmer` filter. |
| `generate_latin_stemmer_rules.py` | Regenerates the rules file from the lemma table (see below). |

Both text files are referenced from the index definitions by path. Locally that
path is a bind mount; in AWS it is a custom package ID that
`../init-opensearch.sh` substitutes into a copy of each definition (see
[Deploying to AWS](#deploying-to-aws)).

## Analyzer configuration

```json
{
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
    },
    "latin_v_to_u": { "type": "pattern_replace", "pattern": "v", "replacement": "u" },
    "latin_j_to_i": { "type": "pattern_replace", "pattern": "j", "replacement": "i" }
  },
  "analyzer": {
    "latin": {
      "type": "custom",
      "char_filter": ["latin_char_normalizer"],
      "tokenizer": "standard",
      "filter": ["lowercase", "asciifolding", "latin_stemmer", "latin_v_to_u", "latin_j_to_i"]
    }
  }
}
```

`canvas.json` adds the `strip_brackets` char filter in front of
`latin_char_normalizer`, because editorial brackets (`[sic]`, `ab[b]as`) in the
transcriptions would otherwise split tokens.

### Processing pipeline

Every spelling of a word has to end as the same token. Taking *dividunt*:

| Step | Input → output | Why |
|---|---|---|
| 1. `latin_char_normalizer` | `dividunt` → `diuidunt` | The rules file uses classical orthography (`u`, `i`) for inflected forms, so modern spellings have to be normalized before the lookup. |
| 2. `standard` tokenizer | one token | |
| 3. `lowercase` | `Diuidunt` → `diuidunt` | Rules are lowercase. |
| 4. `asciifolding` | `dīuidunt` → `diuidunt` | Macrons and other diacritics are removed **before** the lookup so a marked vowel still matches a rule. |
| 5. `latin_stemmer` | `diuidunt` → `divido` | The rule `diuidunt => divido`. Headwords in the table keep modern `v`/`j`. |
| 6. `latin_v_to_u`, `latin_j_to_i` | `divido` → `diuido` | The headword is folded to the same orthography the char filter produces, so that querying the headword itself (`divido` → `diuido`, no rule) finds it. |

So `dividunt`, `diuidunt`, `dīvidunt`, `Dividunt` and the headword `divido` all
index and search as `diuido`. Without step 6, searching a dictionary form whose
headword contains `v` or `j` (3,776 rules with `v`, 586 with `j`) found nothing:
`divido` → `diuido` had no rule, while `dividunt` was lemmatized to `divido`.
Without step 4 before step 5, `dīvidunt` was not lemmatized at all.

Forms that are not in the table pass through steps 1–4 and 6 unchanged, so
spelling variants of unknown words still match each other.

## Local development

`local-dev/docker-compose.yml` mounts both files into the OpenSearch container:

```yaml
volumes:
  - ../rosa-search/opensearch/latin-stemmer-rules.txt:/usr/share/opensearch/config/analyzers/latin-stemmer-rules.txt:ro,Z
  - ../rosa-search/opensearch/latin-charmap.txt:/usr/share/opensearch/config/analyzers/latin-charmap.txt:ro,Z
```

The `Z` suffix applies a private SELinux label and is required on
SELinux-enforcing hosts such as Fedora and RHEL; it is harmless elsewhere.
Because the label is private to one container, do not mount these same files into
a second container with `:Z`: doing so relabels them and the first container
loses read access, which makes index creation fail with `IOException while
reading mappings_path: file not readable`. Give additional containers their own
copies.

## Deploying to AWS

AWS managed OpenSearch cannot read files from the node file system. Both files
are uploaded as **custom packages** and associated with the domain, and the index
definitions reference them as `analyzers/<package-id>`. All of that is automated:

- `../../jhu-rosa2-tf/modules/rosa2-search/packages.tf` uploads the files to S3,
  creates the two packages and associates them with the domain.
- `../init-opensearch.sh` reads the package IDs from the OpenTofu outputs,
  rewrites copies of `manifest.json` and `canvas.json` into `../build/index-defs/`,
  and creates the indexes from those copies. The checked-in definitions keep the
  local paths.

Package lifecycle, IAM and the per-environment IDs are documented in
[`../../jhu-rosa2-tf/README.md`](../../jhu-rosa2-tf/README.md). Two things to
know when changing the dictionaries:

- The analyzers are **not** declared `updateable`, so an updated package only
  takes effect in indexes created after the update. `init-opensearch.sh` drops and
  recreates both indexes.
- The OpenTofu package resource reads a fixed S3 key and has no content trigger,
  so after changing a file check `aws opensearch describe-packages` and update the
  package version explicitly if `tofu apply` did not.

## Regenerating the stemmer rules

```sh
cd rosa-search/opensearch
python3 generate_latin_stemmer_rules.py
```

The script reads `latin-lemma-table.jsonl` from the current directory. The table
is not in the repository; download it from <https://zenodo.org/records/21884913>.
Forms with more than one possible lemma are skipped, trading recall for
precision. The committed `latin-stemmer-rules.txt` is the generated output.

## Testing the analyzer

```sh
curl -s -X POST 'http://localhost:9200/canvas/_analyze' \
  -H 'Content-Type: application/json' \
  -d '{"analyzer":"latin","text":"Gallia est omnis divisa in partes tres"}'
```

Expected tokens: `gallia`, `est`, `omnis`, `diuisus`, `in`, `partes`, `tres`
(`divisa` → `diuisa` → rule `diuisa => divisus` → folded to `diuisus`). The
check in `init-opensearch.sh` asserts that `dividunt`, `divido` and `dīvidunt`
all analyze to `diuido`.

## Known limitations

- **Ambiguous forms are not lemmatized.** A form that maps to several headwords
  (for example *amor* as noun and verb) is left as its normalized spelling.
- **No stop words.** Function words (`et`, `in`, `est`) are indexed; relevance
  ranking handles their low value through term frequency.
- **Headwords are folded, not restored.** Tokens in the index are `u`/`i`
  spellings (`diuido`), which is only visible if you inspect the index or use
  `_analyze`.
- **Greek and other scripts** in Latin text are passed through the standard
  tokenizer untouched.
