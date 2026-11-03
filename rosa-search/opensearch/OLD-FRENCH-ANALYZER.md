# Old French Analyzer for OpenSearch

How the `old_french` analyzer in `canvas.json` and `manifest.json` normalizes the
spelling of the Roman de la Rose transcriptions, the evidence behind each step,
and what it does not do. Verified against OpenSearch 3.1.

## The text it is built for

The analyzer is used for the `.ofr` sub-fields: in the canvas index the verse,
rubrics and catchwords of the TEI transcriptions (`transcription.ofr`), in the
manifest index Old French titles. The transcriptions are seven manuscripts of the
Roman de la Rose (CoxMacro, Douce195, Douce332, LudwigXV7, Morgan948,
SeldenSupra57, Walters143), about 120,000 verse lines, 680,000 word tokens and
40,700 distinct spellings. A search has to find the same word across manuscripts
written two centuries apart by scribes with no fixed orthography, and transcribed
with different editorial conventions.

What the corpus looks like (counts over the verse tokens):

| Feature | Share | Examples |
|---|---|---|
| Elided pronoun or article attached with an apostrophe | 6.0% (40,750) | `qu'il` 8,241, `n'a` 7,895, `l'en` 7,139, `s'il` 5,836, `d'amours` 4,984, `m'en` 3,067, `c'est` 1,470, `t'en` 605, `i'ay` 493, `j'ai` 330, `entr'eus` 109, `jusqu'a` 85, `quanqu'il` 74 |
| `v` (in other manuscripts `u`) | 7.5% | `avision`/`auision`, `voudra`/`uoudra`, `avez`/`auez` |
| `y` (elsewhere `i`) | 3.0% | `moy`/`moi`, `roy`/`roi`, `luy`/`lui`, `nuys`/`nuis`, `cuyde`/`cuide` |
| `j` (elsewhere `i`) | 1.0% | `je` 2,532 against `ie` 2,513; `ja` 1,052 against `ia` 1,023 |
| final `z` (elsewhere `s`) | 3.3% | `telz`/`tels`, `genz`/`gens`, `sanz`/`sans`, `appellez`/`apelés` |
| `ou` (elsewhere `o`) | | `amours`/`amors`, `pour`/`por`, `vous`/`vos`, `tout`/`tot`, `moult`/`molt`, `jour`/`jor` |
| `-ier` (elsewhere `-er`) | | `songier`/`songer`, `mensongier`/`mensonger`, `dangier`/`danger`, `laissier`/`laisser` |
| Editorial diacritics | | `é` 5,477, `ü` 2,185 (`eüst`), `ï` 1,735 (`oïr`), `ç` 674, `ÿ` 255, `ë` 128 (`myë`) |
| Doubled consonants | 6.9% | `apparant`/`aparant`, `signiffiance`/`signifiance`, `belle`/`bele` |

Abbreviations are already expanded in the XML (`ge<expan>n</expan>s`), and the
generator indexes the expanded text, so `gens` needs no handling here.

## Analyzer configuration

```json
{
  "char_filter": {
    "old_french_charmap": {
      "type": "mapping",
      "mappings": ["’=>'", "\u0092=>'", "\u009C=>oe"]
    }
  },
  "filter": {
    "old_french_elision": {
      "type": "elision", "articles_case": true,
      "articles": ["l", "m", "t", "n", "s", "j", "i", "g", "d", "c", "q", "k", "y",
                   "qu", "jusqu", "iusqu", "puisqu", "quanqu", "desqu", "lorsqu", "quoiqu",
                   "entr", "encontr", "contr"]
    },
    "old_french_u_to_v":   { "type": "pattern_replace", "pattern": "(?<=^|[aeiouy])u(?=[aeiouy])", "replacement": "v" },
    "old_french_j_to_i":   { "type": "pattern_replace", "pattern": "j", "replacement": "i" },
    "old_french_y_to_i":   { "type": "pattern_replace", "pattern": "y", "replacement": "i" },
    "old_french_ou_to_o":  { "type": "pattern_replace", "pattern": "ou", "replacement": "o" },
    "old_french_ier_to_er": { "type": "pattern_replace", "pattern": "(?<=.{2}[^rt])i(e[rz]s?|ees?)$", "replacement": "$1" },
    "old_french_z_to_s":   { "type": "pattern_replace", "pattern": "z$", "replacement": "s" },
    "french_stemmer":      { "type": "stemmer", "language": "light_french" }
  },
  "analyzer": {
    "old_french": {
      "type": "custom",
      "char_filter": ["strip_brackets", "old_french_charmap"],
      "tokenizer": "standard",
      "filter": ["lowercase", "asciifolding", "old_french_elision",
                 "old_french_u_to_v", "old_french_j_to_i", "old_french_y_to_i", "old_french_ou_to_o",
                 "old_french_ier_to_er", "old_french_z_to_s",
                 "french_stemmer"]
    }
  }
}
```

`manifest.json` has the same definition without `strip_brackets`, which only
matters for the editorial brackets of the canvas text. There are no external
files, so unlike the Latin analyzer nothing has to be uploaded as a package;
`init-opensearch.sh` checks the analyzer after creating the indexes.

### Processing pipeline

Every spelling of a word has to end as the same token. Taking `l'auision`
(CoxMacro) and `l'advision` (Douce195, which keeps its learned `d`):

| Step | Input → output | Why |
|---|---|---|
| 1. `old_french_charmap` | `s’el` → `s'el` | Typographic and Windows-1252 apostrophes (`’`, U+0092) are made plain so the elision filter sees them; U+009C is a mis-encoded `œ`. |
| 2. `standard` tokenizer | `l'auision` → one token | The tokenizer keeps an apostrophe between letters inside the token, which is exactly why the current analyzer could not find `avision` in `l'avision`. |
| 3. `lowercase`, `asciifolding` | `Aprés` → `apres`, `eüst` → `eust`, `oïr` → `oir` | The diacritics are editorial (syllable counts, modern accents) and vary between transcribers. Folding before the stemmer lets the stemmer see plain letters. |
| 4. `old_french_elision` | `l'auision` → `auision`, `qu'il` → `il`, `jusqu'a` → `a` | Removes the elided word. The article list is the set of prefixes actually found before an apostrophe in the corpus, including the `i'`, `g'` and `k'` spellings of `j'`, `qu'`. |
| 5. `old_french_u_to_v` | `auision` → `avision`, `uoudra` → `voudra` | `u` and `v` are one letter in medieval writing; it is consonantal between vowels and at the start of a word before a vowel, so only those positions are normalized. A blanket `v → u` was tried and rejected: with the stemmer it merged `povre` (poor) into `pour`. |
| 6. `old_french_j_to_i`, `old_french_y_to_i` | `je` → `ie`, `moy` → `moi`, `nuys` → `nuis` | `i`/`j` are one letter; `y` is a scribal variant of `i`. |
| 7. `old_french_ou_to_o` | `amours` → `amors`, `vous` → `vos`, `moult` → `molt` | The older spellings have `o` where later ones have `ou`. Done before the stemmer because its suffix rules depend on word length: after the stemmer `amours` and `amors` would end as `amo` and `amor`. |
| 8. `old_french_ier_to_er` | `songier` → `songer`, `couchiez` → `couchez`, `couchiee` → `couchee` | The Old French `-ier` ending after a palatal consonant is `-er` in later manuscripts and in modern French. The rule needs three letters before the `i` and no `r` or `t` before it, which keeps `liez`, `fiers`, `pries` and the `-tier`, `-rier` nouns (`portier`, `mortier`, `guerrier`) apart from `les`, `fer`, `pres`, `porte`, `mort`, `guerre`. |
| 9. `old_french_z_to_s` | `telz` → `tels`, `genz` → `gens`, `sanz` → `sans` | Final `z` is the Old French spelling of `-ts`/`-s`, which the stemmer only strips as `s`. |
| 10. `french_stemmer` (`light_french`) | `tels` → `tel`, `songer` → `song`, `apparant` → `aparant`, `signiffiance` → `signifianc` | Strips plural and feminine endings and common suffixes, and collapses doubled letters in words longer than four characters. |

So `l'auision`, `l'advision`'s neighbour `avision`, `Avision` and `auision`
all index and search as `avision`; `songes`, `songier` and `songer` as `song`;
`amours`, `amors` and `d'Amours` as `amor`.

No stop-word filter is used. Old French function words (`li`, `ne`, `si`, `que`,
`moult`) differ from the modern French list, and leaving them in keeps phrase
searches on verse possible.

## Evidence

The design was checked by running every distinct spelling of the verse vocabulary
(40,625 words) through candidate analyzers with the `_analyze` API and comparing
how they group spellings:

| Analyzer | Distinct tokens for 40,625 spellings |
|---|---|
| Previous `old_french` (lowercase, light stemmer, ASCII folding) | 28,985 |
| plus elision only | 26,630 |
| plus `u`/`v`, `j`/`i`, `y`/`i`, final `z`, `-ier`, folding before stemming | 21,421 |
| plus `ou`/`o` (the analyzer above) | 20,647 |

Spot checks of what the new steps merge that the previous analyzer kept apart:
`iadis`/`jadis`, `fis`/`fiz`, `oisiaus`/`oysiaus`, `conduire`/`conduyre`,
`veïsse`/`veisse`/`ueisse`, `estoilles`/`estoiles`/`estoile`,
`vouloient`/`voloient`/`vouloyent`, `devourer`/`devorer`, `sommeillier`/`sommeiller`,
`courroucier`/`courroucer`/`corroucier`, `debonnere`/`debonere`.

Steps tried and rejected:

- Blanket `v → u` (as the Latin analyzer does): `povre`/`poure` → `pour`.
- Normalizing after the stemmer: `amours`/`amors` and `dormoie`/`dormoye` stayed
  apart because the stemmer's rules depend on word length.
- `-ier → -er` without the guard: `liez` → `les`, `miez` → `mes`, `diez` → `des`,
  `fier` → `fer`, `priez` → `pres`.
- Dropping an initial `h` (`homme`/`ome`, `heritage`/`eritage`): `honte` → `ont`,
  `haie` → `aie`, `huer` → `vers`.
- `minimal_french` instead of `light_french`: loses the doubled-consonant
  collapse (`apparant`/`aparant`) and most suffix merges.

## Limits

- `ai`/`e` (`mais`/`mes`, `fait`/`fet`, `traire`/`trere`) and `c`/`s`/`ç` before
  a front vowel (`mençonges`/`mensonges`, `aincois`/`ansois`) are not normalized;
  both would conflate frequent unrelated words (`mai`/`me`, `cent`/`sent`).
- Learned spellings with silent letters (`advision`/`avision`, `descript`/`escrit`,
  `adviengne`/`aviengne`) are not reduced.
- Enclitic contractions (`nel`, `jel`, `sel`, `del`, `al`, `es`, `as`) stay as
  they are written.
- The light stemmer conflates some distinct words that happen to share a stem,
  before and after this change: `vergier`/`verge`, `porte`/`porter`/`port`,
  `mie`/`miex`, `fol`/`folie`.
- Words of four letters or fewer are not stemmed, so `fol`/`fou` and `chier`/`cher`
  stay apart.

## Changing it

Analyzers are not `updateable`: after a change to either index definition the
indexes have to be recreated (`local-dev/start.sh` locally, `init-opensearch.sh`
on AWS). To evaluate a change, create a scratch index with the candidate analyzer
and run the vocabulary through `_analyze`:

```sh
curl -s -X POST localhost:9200/canvas/_analyze -H 'Content-Type: application/json' \
  -d '{"analyzer":"old_french","text":"Maintes gens dient qu'"'"'en songes l'"'"'auision amours amors"}'
```

Compare the grouping of spellings before and after, and look especially at groups
that join two frequent words with different meanings: those are the merges that
hurt, because a search for the rarer word returns every page with the common one.
