package rosa.archive.aor;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Per page or book stats.
 */
public class Stats implements Comparable<Stats> {
//    private static final Pattern MANUSCRIPT_PATTERN = Pattern.compile("^(.+\\.)(\\d+)(r|v|R|V)$");
    private static final Pattern MANUSCRIPT_PATTERN2 = Pattern.compile("^(.+\\.)?([a-zA-Z])*(\\d+)([rvRV])$");
    private static final Pattern INSERT_PATTERN1 = Pattern.compile("^(\\d+)_(\\d+)$");

    final String id;
    int marginalia;
    int marginalia_words;
    int underlines;
    int underline_words;
    int marks;
    int mark_words;
    int symbols;
    int symbol_words;
    int drawings;
    int drawing_words;
    int numerals;
    int calculations;
    int graphs;
    int graph_words;
    int tables;
    int table_words;

    int books;
    int people;
    int locations;

    Vocab marginalia_vocab;
    Vocab underlines_vocab;
    Vocab marks_vocab;
    Vocab symbols_vocab;
    Vocab drawing_vocab;
    Vocab graph_vocab;
    Vocab table_vocab;

    private Matcher manuscriptMatcher;
    private Matcher insertMatcher;

    public Stats(String id) {
        this.id = id;
        this.marginalia_vocab = new Vocab();
        this.underlines_vocab = new Vocab();
        this.marks_vocab = new Vocab();
        this.symbols_vocab = new Vocab();
        this.drawing_vocab = new Vocab();
        this.graph_vocab = new Vocab();
        this.table_vocab = new Vocab();

        this.manuscriptMatcher = MANUSCRIPT_PATTERN2.matcher(id);
        this.insertMatcher = INSERT_PATTERN1.matcher(id);
    }

    public void add(Stats s) {
        marginalia += s.marginalia;
        marginalia_words += s.marginalia_words;
        underlines += s.underlines;
        underline_words += s.underline_words;
        marks += s.marks;
        mark_words += s.mark_words;
        symbols += s.symbols;
        symbol_words += s.symbol_words;
        drawings += s.drawings;
        numerals += s.numerals;
        books += s.books;
        people += s.people;
        locations += s.locations;

        marginalia_vocab.update(s.marginalia_vocab);
        underlines_vocab.update(s.underlines_vocab);
        marks_vocab.update(s.marks_vocab);
        symbols_vocab.update(s.symbols_vocab);
    }

    /**
     * If ID refers to a single page in a book, guess the index of the page based off
     * its ID. Return -1 if the page ID format is unknown or this Stats refers to a
     * book instead of a page.
     *
     * Inserts will not be assigned an index. There are other edge cases such as
     * odd names for front/end matter pages which are ignored here. Those cases
     * will simply not be assigned an index.
     *
     * @return index of page in a book, or -1 if index is of unknown form
     *         or this refers to a book.
     */
    public int pageIndex() {
        String test = id;

        if (insertMatcher.find()) {
            return -1;
        }

        if (test.contains("_") && containsDigits(test.substring(test.lastIndexOf('_')))) { // EX: Castiglione, Castiglione_Newberry, Frontinus, etc
            test = test.substring(test.lastIndexOf('_') + 1, test.length());
            manuscriptMatcher = MANUSCRIPT_PATTERN2.matcher(test);
        } else if (test.contains("-") && containsDigits(test.substring(test.lastIndexOf('-')))) { // EX: De navigatione
            test = test.substring(test.lastIndexOf('-') + 1, test.length());
            manuscriptMatcher = MANUSCRIPT_PATTERN2.matcher(test);
        }

        if (manuscriptMatcher.find()) {
            int index = Integer.parseInt(manuscriptMatcher.group(3)) * 2;

            if (manuscriptMatcher.group(4).equals("r") || manuscriptMatcher.group(4).equals("R")) {
                index --;
            }
            return index;
        } else {
            try {
                return Integer.parseInt(test);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
    }

    private boolean containsDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public int totalAnnotations() {
        return marginalia + underlines + marks + symbols + drawings + numerals + calculations + graphs + tables;
    }

    public int totalWords() {
        return marginalia_words + underline_words + mark_words + symbol_words + drawing_words + graph_words + table_words;
    }

    @Override
    public int compareTo(Stats s) {
        return id.compareTo(s.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stats stats = (Stats) o;
        return marginalia == stats.marginalia &&
                marginalia_words == stats.marginalia_words &&
                underlines == stats.underlines &&
                underline_words == stats.underline_words &&
                marks == stats.marks &&
                mark_words == stats.mark_words &&
                symbols == stats.symbols &&
                symbol_words == stats.symbol_words &&
                drawings == stats.drawings &&
                drawing_words == stats.drawing_words &&
                numerals == stats.numerals &&
                calculations == stats.calculations &&
                graphs == stats.graphs &&
                graph_words == stats.graph_words &&
                tables == stats.tables &&
                table_words == stats.table_words &&
                books == stats.books &&
                people == stats.people &&
                locations == stats.locations &&
                Objects.equals(id, stats.id) &&
                Objects.equals(marginalia_vocab, stats.marginalia_vocab) &&
                Objects.equals(underlines_vocab, stats.underlines_vocab) &&
                Objects.equals(marks_vocab, stats.marks_vocab) &&
                Objects.equals(symbols_vocab, stats.symbols_vocab) &&
                Objects.equals(drawing_vocab, stats.drawing_vocab) &&
                Objects.equals(graph_vocab, stats.graph_vocab) &&
                Objects.equals(table_vocab, stats.table_vocab) &&
                Objects.equals(manuscriptMatcher, stats.manuscriptMatcher);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, marginalia, marginalia_words, underlines, underline_words, marks, mark_words, symbols,
                symbol_words, drawings, drawing_words, numerals, calculations, graphs, graph_words, tables, table_words,
                books, people, locations, marginalia_vocab, underlines_vocab, marks_vocab, symbols_vocab, drawing_vocab,
                graph_vocab, table_vocab, manuscriptMatcher);
    }

    @Override
    public String toString() {
        return "Stats{" +
                "id='" + id + '\'' +
                ", marginalia=" + marginalia +
                ", marginalia_words=" + marginalia_words +
                ", underlines=" + underlines +
                ", underline_words=" + underline_words +
                ", marks=" + marks +
                ", mark_words=" + mark_words +
                ", symbols=" + symbols +
                ", symbol_words=" + symbol_words +
                ", drawings=" + drawings +
                ", drawing_words=" + drawing_words +
                ", numerals=" + numerals +
                ", calculations=" + calculations +
                ", graphs=" + graphs +
                ", graph_words=" + graph_words +
                ", tables=" + tables +
                ", table_words=" + table_words +
                ", books=" + books +
                ", people=" + people +
                ", locations=" + locations +
                ", marginalia_vocab=" + marginalia_vocab +
                ", underlines_vocab=" + underlines_vocab +
                ", marks_vocab=" + marks_vocab +
                ", symbols_vocab=" + symbols_vocab +
                ", drawing_vocab=" + drawing_vocab +
                ", graph_vocab=" + graph_vocab +
                ", table_vocab=" + table_vocab +
                ", manuscriptMatcher=" + manuscriptMatcher +
                '}';
    }
}
