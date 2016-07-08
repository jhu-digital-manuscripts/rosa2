package rosa.search.core.analyzer;

import org.apache.lucene.analysis.Analyzer;
import rosa.search.model.SearchFieldType;

import java.util.HashMap;
import java.util.Map;

public class RosaLanguageAnalyzers {
    private final Map<SearchFieldType, Analyzer> analyzerMap;

    /**
     * Convenience Builder to add analyzers to a new RosaAnalyzers class
     */
    public static class Builder {
        private RosaLanguageAnalyzers rosaAnalyzers;

        public Builder() {
            rosaAnalyzers = new RosaLanguageAnalyzers();
        }

        public Builder englishAnalyzer(Analyzer analyzer) {
            rosaAnalyzers.addAnalyzer(SearchFieldType.ENGLISH, analyzer);
            return this;
        }

        public Builder frenchAnalyzer(Analyzer analyzer) {
            rosaAnalyzers.addAnalyzer(SearchFieldType.FRENCH, analyzer);
            return this;
        }

        public Builder oldFrenchAnalyzer(Analyzer analyzer) {
            rosaAnalyzers.addAnalyzer(SearchFieldType.OLD_FRENCH, analyzer);
            return this;
        }

        public Builder italianAnalyzer(Analyzer analyzer) {
            rosaAnalyzers.addAnalyzer(SearchFieldType.ITALIAN, analyzer);
            return this;
        }

        public Builder spanishAnalyzer(Analyzer analyzer) {
            rosaAnalyzers.addAnalyzer(SearchFieldType.SPANISH, analyzer);
            return this;
        }

        public Builder greekAnalyzer(Analyzer analyzer) {
            rosaAnalyzers.addAnalyzer(SearchFieldType.GREEK, analyzer);
            return this;
        }

        public Builder latinAnalyzer(Analyzer analyzer) {
            rosaAnalyzers.addAnalyzer(SearchFieldType.LATIN, analyzer);
            return this;
        }

        public RosaLanguageAnalyzers build() {
            return this.rosaAnalyzers;
        }
    }

    public RosaLanguageAnalyzers() {
        analyzerMap = new HashMap<>();
    }

    /**
     * @return a copy of the collection of analyzers
     */
    public Map<SearchFieldType, Analyzer> getAllAnalyzers() {
        return new HashMap<>(analyzerMap);
    }

    /**
     * @param type type of analyzer to retrieve
     * @return the one analyzer for a given type
     */
    public Analyzer getAnalyzer(SearchFieldType type) {
        return analyzerMap.get(type);
    }

    /**
     * Add an analyzer for a given field type.
     *
     * @param type field type
     * @param analyzer analyzer
     */
    public void addAnalyzer(SearchFieldType type, Analyzer analyzer) {
        analyzerMap.put(type, analyzer);
    }

    public Analyzer englishAnalyzer() {
        return analyzerMap.get(SearchFieldType.ENGLISH);
    }

    public Analyzer frenchAnalyzer() {
        return analyzerMap.get(SearchFieldType.FRENCH);
    }

    public Analyzer oldFrenchAnalyzer() {
        return analyzerMap.get(SearchFieldType.OLD_FRENCH);
    }

    public Analyzer italianAnalyzer() {
        return analyzerMap.get(SearchFieldType.ITALIAN);
    }

    public Analyzer spanishAnalyzer() {
        return analyzerMap.get(SearchFieldType.SPANISH);
    }

    public Analyzer greekAnalyzer() {
        return analyzerMap.get(SearchFieldType.GREEK);
    }

    public Analyzer latinAnalyzer() {
        return analyzerMap.get(SearchFieldType.LATIN);
    }

}
