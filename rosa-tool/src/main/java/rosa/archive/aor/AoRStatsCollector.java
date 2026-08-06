package rosa.archive.aor;

import rosa.archive.core.serialize.ArchiveReaders;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNote;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TableCell;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Collects statistics on AoR annotations from book directories and produces CSV output.
 *
 * <p>This collector reads AoR annotation XML files from book directories, computes
 * per-book totals, per-page statistics, and vocabulary frequencies, then writes
 * the results as CSV files.
 */
public final class AoRStatsCollector {

    /**
     * Per-book annotation totals including counts and reference statistics.
     *
     * @param bookId the identifier of the book
     * @param counts the annotation type counts for the book
     * @param refs   the external reference counts for the book
     */
    public record BookTotals(String bookId, AnnotationCounts counts, ReferenceCounts refs) {}

    /**
     * Per-page annotation statistics within a book.
     *
     * @param pageId    the identifier of the page
     * @param pageIndex the computed index of the page within the book
     * @param counts    the annotation type counts for the page
     * @param refs      the external reference counts for the page
     */
    public record PageStats(String pageId, int pageIndex, AnnotationCounts counts, ReferenceCounts refs) {}

    /**
     * A vocabulary entry consisting of a word and its frequency.
     *
     * @param word      the word or term
     * @param frequency the number of occurrences
     */
    public record VocabEntry(String word, int frequency) {}

    /**
     * Collects total annotation counts for a single book directory.
     *
     * @param bookDir path to the book directory containing AoR XML files
     * @return the aggregated annotation and reference counts for the book
     * @throws IOException if the directory cannot be read or XML parsing fails
     */
    public BookTotals collectBookTotals(Path bookDir) throws IOException {
        String bookId = bookDir.getFileName().toString();
        List<PageStats> pages = collectPageStats(bookDir);

        int marginalia = 0, marginaliaWords = 0;
        int underlines = 0, underlineWords = 0;
        int marks = 0, markWords = 0;
        int symbols = 0, symbolWords = 0;
        int drawings = 0, drawingWords = 0;
        int numerals = 0, calculations = 0;
        int graphs = 0, graphWords = 0;
        int tables = 0, tableWords = 0;
        int physLinks = 0;
        int books = 0, people = 0, locations = 0;

        for (PageStats ps : pages) {
            AnnotationCounts c = ps.counts();
            marginalia += c.marginalia();
            marginaliaWords += c.marginaliaWords();
            underlines += c.underlines();
            underlineWords += c.underlineWords();
            marks += c.marks();
            markWords += c.markWords();
            symbols += c.symbols();
            symbolWords += c.symbolWords();
            drawings += c.drawings();
            drawingWords += c.drawingWords();
            numerals += c.numerals();
            calculations += c.calculations();
            graphs += c.graphs();
            graphWords += c.graphWords();
            tables += c.tables();
            tableWords += c.tableWords();
            physLinks += c.physLinks();

            ReferenceCounts r = ps.refs();
            books += r.books();
            people += r.people();
            locations += r.locations();
        }

        return new BookTotals(bookId,
                new AnnotationCounts(marginalia, marginaliaWords, underlines, underlineWords,
                        marks, markWords, symbols, symbolWords, drawings, drawingWords,
                        numerals, calculations, graphs, graphWords, tables, tableWords, physLinks),
                new ReferenceCounts(books, people, locations));
    }

    /**
     * Collects per-page annotation statistics for a single book directory.
     *
     * @param bookDir path to the book directory containing AoR XML files
     * @return a list of page statistics sorted by page ID
     * @throws IOException if the directory cannot be read or XML parsing fails
     */
    public List<PageStats> collectPageStats(Path bookDir) throws IOException {
        List<PageStats> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, "*.xml")) {
            for (Path xmlPath : stream) {
                String filename = xmlPath.getFileName().toString();
                if (!filename.contains(".aor.")) {
                    continue;
                }

                AnnotatedPage ap = ArchiveReaders.readAnnotatedPage(xmlPath, errors);
                if (ap == null) {
                    continue;
                }

                String pageId = stripExtension(filename);
                int pageIndex = computePageIndex(pageId);
                AnnotationCounts counts = countAnnotations(ap);
                ReferenceCounts refs = countReferences(ap);

                results.add(new PageStats(pageId, pageIndex, counts, refs));
            }
        }

        results.sort(Comparator.comparing(PageStats::pageId));
        return results;
    }

    /**
     * Collects vocabulary frequencies for each annotation type and language combination.
     *
     * <p>The returned map keys follow the pattern {@code {annotation_type}_{language_code}},
     * and the values are lists of vocabulary entries sorted by frequency in descending order.
     *
     * @param bookDir path to the book directory containing AoR XML files
     * @return a map from type-language keys to sorted vocabulary entries
     * @throws IOException if the directory cannot be read or XML parsing fails
     */
    public Map<String, List<VocabEntry>> collectVocab(Path bookDir) throws IOException {
        Map<String, Map<String, Integer>> freqMaps = new java.util.HashMap<>();
        List<String> errors = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, "*.xml")) {
            for (Path xmlPath : stream) {
                String filename = xmlPath.getFileName().toString();
                if (!filename.contains(".aor.")) {
                    continue;
                }

                AnnotatedPage ap = ArchiveReaders.readAnnotatedPage(xmlPath, errors);
                if (ap == null) {
                    continue;
                }

                collectMarginaliaVocab(ap, freqMaps);
                collectUnderlineVocab(ap, freqMaps);
                collectMarkVocab(ap, freqMaps);
                collectSymbolVocab(ap, freqMaps);
                collectDrawingVocab(ap, freqMaps);
                collectGraphVocab(ap, freqMaps);
                collectTableVocab(ap, freqMaps);
            }
        }

        // Convert frequency maps to sorted VocabEntry lists
        Map<String, List<VocabEntry>> result = new java.util.HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : freqMaps.entrySet()) {
            List<VocabEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> wordEntry : entry.getValue().entrySet()) {
                entries.add(new VocabEntry(wordEntry.getKey(), wordEntry.getValue()));
            }
            entries.sort(Comparator.comparingInt(VocabEntry::frequency).reversed());
            result.put(entry.getKey(), entries);
        }

        return result;
    }

    private static void collectMarginaliaVocab(AnnotatedPage ap, Map<String, Map<String, Integer>> freqMaps) {
        for (Marginalia m : ap.getMarginalia()) {
            for (MarginaliaLanguage ml : m.getLanguages()) {
                String lang = ml.getLang();
                if (lang == null || lang.isBlank()) {
                    continue;
                }
                String key = "marginalia_" + lang;
                for (Position p : ml.getPositions()) {
                    for (String text : p.getTexts()) {
                        addWords(freqMaps, key, text);
                    }
                }
            }
        }
    }

    private static void collectUnderlineVocab(AnnotatedPage ap, Map<String, Map<String, Integer>> freqMaps) {
        for (Underline ul : ap.getUnderlines()) {
            String lang = ul.getLanguage();
            if (lang == null || lang.isBlank()) {
                continue;
            }
            String key = "underlines_" + lang;
            addWords(freqMaps, key, ul.getReferencedText());
        }
    }

    private static void collectMarkVocab(AnnotatedPage ap, Map<String, Map<String, Integer>> freqMaps) {
        for (Mark m : ap.getMarks()) {
            String lang = m.getLanguage();
            if (lang == null || lang.isBlank()) {
                continue;
            }
            String key = "marks_" + lang;
            addWords(freqMaps, key, m.getReferencedText());
        }
    }

    private static void collectSymbolVocab(AnnotatedPage ap, Map<String, Map<String, Integer>> freqMaps) {
        for (Symbol s : ap.getSymbols()) {
            String lang = s.getLanguage();
            if (lang == null || lang.isBlank()) {
                continue;
            }
            String key = "symbols_" + lang;
            addWords(freqMaps, key, s.getReferencedText());
        }
    }

    private static void collectDrawingVocab(AnnotatedPage ap, Map<String, Map<String, Integer>> freqMaps) {
        for (Drawing d : ap.getDrawings()) {
            String lang = d.getLanguage();
            if (lang == null || lang.isBlank()) {
                continue;
            }
            String key = "drawings_" + lang;
            for (TextEl t : d.getTexts()) {
                addWords(freqMaps, key, t.text());
            }
        }
    }

    private static void collectGraphVocab(AnnotatedPage ap, Map<String, Map<String, Integer>> freqMaps) {
        for (Graph g : ap.getGraphs()) {
            String lang = g.getLanguage();
            if (lang == null || lang.isBlank()) {
                continue;
            }
            String key = "graphs_" + lang;
            for (GraphText gt : g.getGraphTexts()) {
                for (GraphNote n : gt.getNotes()) {
                    addWords(freqMaps, key, n.content());
                }
            }
        }
    }

    private static void collectTableVocab(AnnotatedPage ap, Map<String, Map<String, Integer>> freqMaps) {
        for (Table t : ap.getTables()) {
            String lang = t.getLanguage();
            if (lang == null || lang.isBlank()) {
                continue;
            }
            String key = "tables_" + lang;
            for (TextEl txt : t.getTexts()) {
                addWords(freqMaps, key, txt.text());
            }
            for (TableCell cell : t.getCells()) {
                addWords(freqMaps, key, cell.content());
            }
        }
    }

    private static void addWords(Map<String, Map<String, Integer>> freqMaps, String key, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Map<String, Integer> wordMap = freqMaps.computeIfAbsent(key, k -> new java.util.HashMap<>());
        String[] tokens = text.trim().split("\\s+");
        for (String token : tokens) {
            if (!token.isEmpty()) {
                String word = token.toLowerCase();
                wordMap.merge(word, 1, Integer::sum);
            }
        }
    }

    // --- Private helpers ---

    private static String stripExtension(String filename) {
        int end = filename.lastIndexOf('.');
        return end == -1 ? filename : filename.substring(0, end);
    }

    private static AnnotationCounts countAnnotations(AnnotatedPage ap) {
        int marginaliaWords = countMarginaliaWords(ap);
        int underlineWords = countUnderlineWords(ap);
        int markWords = countMarkWords(ap);
        int symbolWords = countSymbolWords(ap);
        int drawingWords = countDrawingWords(ap);
        int graphWords = countGraphWords(ap);
        int tableWords = countTableWords(ap);

        return new AnnotationCounts(
                ap.getMarginalia().size(), marginaliaWords,
                ap.getUnderlines().size(), underlineWords,
                ap.getMarks().size(), markWords,
                ap.getSymbols().size(), symbolWords,
                ap.getDrawings().size(), drawingWords,
                ap.getNumerals().size(),
                ap.getCalculations().size(),
                ap.getGraphs().size(), graphWords,
                ap.getTables().size(), tableWords,
                ap.getLinks().size());
    }

    private static ReferenceCounts countReferences(AnnotatedPage ap) {
        int books = 0;
        int people = 0;
        int locations = 0;

        // From marginalia
        for (Marginalia m : ap.getMarginalia()) {
            for (MarginaliaLanguage ml : m.getLanguages()) {
                for (Position p : ml.getPositions()) {
                    books += p.getBooks().size();
                    people += p.getPeople().size();
                    locations += p.getLocations().size();
                }
            }
        }

        // From graphs
        for (Graph g : ap.getGraphs()) {
            for (GraphText t : g.getGraphTexts()) {
                books += t.getBooks().size();
                people += t.getPeople().size();
                locations += t.getLocations().size();
            }
        }

        // From drawings
        for (Drawing d : ap.getDrawings()) {
            books += d.getBooks().size();
            people += d.getPeople().size();
            locations += d.getLocations().size();
        }

        // From tables
        for (Table t : ap.getTables()) {
            books += t.getBooks().size();
            people += t.getPeople().size();
            locations += t.getLocations().size();
        }

        return new ReferenceCounts(books, people, locations);
    }

    private static int countMarginaliaWords(AnnotatedPage ap) {
        int count = 0;
        for (Marginalia m : ap.getMarginalia()) {
            for (MarginaliaLanguage ml : m.getLanguages()) {
                for (Position p : ml.getPositions()) {
                    for (String text : p.getTexts()) {
                        count += countWords(text);
                    }
                }
            }
        }
        return count;
    }

    private static int countUnderlineWords(AnnotatedPage ap) {
        int count = 0;
        for (Underline ul : ap.getUnderlines()) {
            count += countWords(ul.getReferencedText());
        }
        return count;
    }

    private static int countMarkWords(AnnotatedPage ap) {
        int count = 0;
        for (Mark m : ap.getMarks()) {
            count += countWords(m.getReferencedText());
        }
        return count;
    }

    private static int countSymbolWords(AnnotatedPage ap) {
        int count = 0;
        for (Symbol s : ap.getSymbols()) {
            count += countWords(s.getReferencedText());
        }
        return count;
    }

    private static int countDrawingWords(AnnotatedPage ap) {
        int count = 0;
        for (Drawing d : ap.getDrawings()) {
            for (TextEl t : d.getTexts()) {
                count += countWords(t.anchorText()) + countWords(t.text());
            }
            count += countWords(d.getTranslation());
        }
        return count;
    }

    private static int countGraphWords(AnnotatedPage ap) {
        int count = 0;
        for (Graph g : ap.getGraphs()) {
            for (GraphText t : g.getGraphTexts()) {
                for (GraphNote n : t.getNotes()) {
                    count += countWords(n.content()) + countWords(n.anchorText());
                }
                for (String translation : t.getTranslations()) {
                    count += countWords(translation);
                }
            }
        }
        return count;
    }

    private static int countTableWords(AnnotatedPage ap) {
        int count = 0;
        for (Table t : ap.getTables()) {
            count += countWords(t.getTranslation());
            for (TextEl txt : t.getTexts()) {
                count += countWords(txt.text()) + countWords(txt.anchorText());
            }
        }
        return count;
    }

    /**
     * Counts words in a text string by splitting on whitespace and counting non-empty tokens.
     *
     * @param text the text to count words in
     * @return the word count, or 0 if text is null or empty
     */
    static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String[] tokens = text.trim().split("\\s+");
        int count = 0;
        for (String token : tokens) {
            if (!token.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Computes a page index from the page ID using common naming patterns.
     *
     * <p>Recognizes manuscript patterns (e.g., "Ha2.003r" → index 5) and
     * numeric patterns (e.g., "005" → 5). Returns -1 for unknown formats.
     *
     * @param pageId the page identifier
     * @return the computed page index, or -1 if format is unrecognized
     */
    static int computePageIndex(String pageId) {
        // Skip insert patterns (e.g., "123_456")
        if (pageId.matches("^\\d+_\\d+$")) {
            return -1;
        }

        String test = pageId;

        // Handle underscore-separated patterns (e.g., "Castiglione_005")
        if (test.contains("_") && containsDigits(test.substring(test.lastIndexOf('_')))) {
            test = test.substring(test.lastIndexOf('_') + 1);
        } else if (test.contains("-") && containsDigits(test.substring(test.lastIndexOf('-')))) {
            test = test.substring(test.lastIndexOf('-') + 1);
        }

        // Match manuscript pattern: optional prefix + optional letters + digits + r/v
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(.+\\.)?([a-zA-Z])*(\\d+)([rvRV])$")
                .matcher(test);

        if (matcher.find()) {
            int index = Integer.parseInt(matcher.group(3)) * 2;
            String side = matcher.group(4);
            if (side.equals("r") || side.equals("R")) {
                index--;
            }
            return index;
        }

        // Try pure numeric
        try {
            return Integer.parseInt(test);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean containsDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
