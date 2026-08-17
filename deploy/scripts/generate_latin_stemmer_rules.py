#!/usr/bin/env python3
"""
Convert latin-lemma-table.jsonl to OpenSearch stemmer_override rules format.

The stemmer_override format is:
    form1, form2, form3 => lemma

This script groups all inflected forms by their lemma and generates rules
that map each form to its canonical dictionary headword.

For forms with multiple possible lemmas (ambiguous), we skip them to avoid
incorrect stemming. This is a conservative approach that prioritizes precision.
"""

import json
import sys
from collections import defaultdict
from pathlib import Path


def load_lemma_table(input_path: Path) -> dict[str, list[str]]:
    """
    Load the JSONL lemma table and return a mapping of form -> list of lemmas.
    """
    form_to_lemmas: dict[str, list[str]] = {}
    
    with open(input_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            entry = json.loads(line)
            form = entry['form'].lower()
            lemmas = [lem['headword'].lower() for lem in entry['lemmas']]
            
            # Deduplicate lemmas (same headword can appear with different ls values)
            unique_lemmas = list(dict.fromkeys(lemmas))
            form_to_lemmas[form] = unique_lemmas
    
    return form_to_lemmas


def build_lemma_to_forms(form_to_lemmas: dict[str, list[str]]) -> dict[str, set[str]]:
    """
    Invert the mapping: for each lemma, collect all forms that map ONLY to that lemma.
    
    We exclude ambiguous forms (those mapping to multiple different lemmas) to ensure
    accurate stemming.
    """
    lemma_to_forms: dict[str, set[str]] = defaultdict(set)
    
    for form, lemmas in form_to_lemmas.items():
        # Only include forms with a single unambiguous lemma
        if len(lemmas) == 1:
            lemma = lemmas[0]
            # Don't add the lemma itself as a form (it maps to itself)
            if form != lemma:
                lemma_to_forms[lemma].add(form)
    
    return lemma_to_forms


def generate_rules(lemma_to_forms: dict[str, set[str]], min_forms: int = 1) -> list[str]:
    """
    Generate stemmer_override rules from the lemma->forms mapping.
    
    Format: form1, form2, ... => lemma
    
    Args:
        lemma_to_forms: Mapping of lemma to set of inflected forms
        min_forms: Minimum number of forms required to generate a rule (default 1)
    
    Returns:
        List of rule strings
    """
    rules = []
    
    for lemma in sorted(lemma_to_forms.keys()):
        forms = lemma_to_forms[lemma]
        
        if len(forms) < min_forms:
            continue
        
        # Sort forms for consistent output
        sorted_forms = sorted(forms)
        
        # Generate rule: form1, form2, ... => lemma
        rule = f"{', '.join(sorted_forms)} => {lemma}"
        rules.append(rule)
    
    return rules


def main():
    # Paths
    script_dir = Path(__file__).parent
    deploy_dir = script_dir.parent
    
    input_path = deploy_dir / 'latin-lemma-table.jsonl'
    output_path = deploy_dir / 'opensearch' / 'latin-stemmer-rules.txt'
    
    if not input_path.exists():
        print(f"Error: Input file not found: {input_path}", file=sys.stderr)
        sys.exit(1)
    
    print(f"Loading lemma table from {input_path}...")
    form_to_lemmas = load_lemma_table(input_path)
    print(f"  Loaded {len(form_to_lemmas):,} forms")
    
    # Count ambiguous forms
    ambiguous_count = sum(1 for lemmas in form_to_lemmas.values() if len(lemmas) > 1)
    print(f"  Ambiguous forms (excluded): {ambiguous_count:,}")
    
    print("Building lemma -> forms mapping...")
    lemma_to_forms = build_lemma_to_forms(form_to_lemmas)
    print(f"  Found {len(lemma_to_forms):,} lemmas with inflected forms")
    
    print("Generating stemmer_override rules...")
    rules = generate_rules(lemma_to_forms)
    print(f"  Generated {len(rules):,} rules")
    
    # Write output
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        for rule in rules:
            f.write(rule + '\n')
    
    print(f"Wrote rules to {output_path}")
    
    # Print some statistics
    total_forms = sum(len(forms) for forms in lemma_to_forms.values())
    print(f"\nStatistics:")
    print(f"  Total inflected forms covered: {total_forms:,}")
    print(f"  Average forms per lemma: {total_forms / len(lemma_to_forms):.1f}")
    
    # Show a few example rules
    print(f"\nExample rules (first 5):")
    for rule in rules[:5]:
        # Truncate long rules for display
        if len(rule) > 100:
            print(f"  {rule[:100]}...")
        else:
            print(f"  {rule}")


if __name__ == '__main__':
    main()
