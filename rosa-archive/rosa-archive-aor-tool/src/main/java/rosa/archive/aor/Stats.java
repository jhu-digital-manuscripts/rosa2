package rosa.archive.aor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stats implements Comparable<Stats> {
    private static final Pattern MANUSCRIPT_PATTERN = Pattern.compile("^(.+\\.)(\\d+)(r|v|R|V)$");

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
    int numerals;
    int books;
    int people;
    int locations;

    Vocab marginalia_vocab;
    Vocab underlines_vocab;
    Vocab marks_vocab;
    Vocab symbols_vocab;

    private Matcher manuscriptMatcher;

    public Stats(String id) {
        this.id = id;
        this.marginalia_vocab = new Vocab();
        this.underlines_vocab = new Vocab();
        this.marks_vocab = new Vocab();
        this.symbols_vocab = new Vocab();

        this.manuscriptMatcher = MANUSCRIPT_PATTERN.matcher(id);
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
     * @return index of page in a book, or -1 if index is of unknown form
     *         or this refers to a book.
     */
    public int pageIndex() {
        if (id.contains("_") && containsDigits(id.substring(id.lastIndexOf('_')))) {
            // Castiglione, Castiglione_Newberry, and Frontinus
            return Integer.parseInt(id.substring(id.lastIndexOf('_') + 1, id.length()));
        } else if (manuscriptMatcher.find()) {
            int index = Integer.parseInt(manuscriptMatcher.group(2)) * 2;
            if (manuscriptMatcher.group(3).equals("r") || manuscriptMatcher.group(3).equals("R")) {
                index --;
            }
            return index;
        } else {
            try {
                return Integer.parseInt(id);
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
        return marginalia + underlines + marks + symbols + drawings + numerals;
    }

    public int totalWords() {
        return marginalia_words + underline_words + mark_words + symbol_words;
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

        if (marginalia != stats.marginalia) return false;
        if (marginalia_words != stats.marginalia_words) return false;
        if (underlines != stats.underlines) return false;
        if (underline_words != stats.underline_words) return false;
        if (marks != stats.marks) return false;
        if (mark_words != stats.mark_words) return false;
        if (symbols != stats.symbols) return false;
        if (symbol_words != stats.symbol_words) return false;
        if (drawings != stats.drawings) return false;
        if (numerals != stats.numerals) return false;
        if (books != stats.books) return false;
        if (people != stats.people) return false;
        if (locations != stats.locations) return false;
        if (id != null ? !id.equals(stats.id) : stats.id != null) return false;
        return !(marginalia_vocab != null ? !marginalia_vocab.equals(stats.marginalia_vocab) : stats.marginalia_vocab != null);

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + marginalia;
        result = 31 * result + marginalia_words;
        result = 31 * result + underlines;
        result = 31 * result + underline_words;
        result = 31 * result + marks;
        result = 31 * result + mark_words;
        result = 31 * result + symbols;
        result = 31 * result + symbol_words;
        result = 31 * result + drawings;
        result = 31 * result + numerals;
        result = 31 * result + books;
        result = 31 * result + people;
        result = 31 * result + locations;
        result = 31 * result + (marginalia_vocab != null ? marginalia_vocab.hashCode() : 0);
        return result;
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
                ", numerals=" + numerals +
                ", books=" + books +
                ", people=" + people +
                ", locations=" + locations +
                ", marginalia_vocab=" + marginalia_vocab +
                '}';
    }
}
