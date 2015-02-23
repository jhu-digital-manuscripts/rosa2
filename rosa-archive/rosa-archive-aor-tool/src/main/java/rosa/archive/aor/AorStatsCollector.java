package rosa.archive.aor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;

/**
 * Collect stats on AOR annotations and write them out as CSV spreadsheets.
 */
public class AorStatsCollector {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private final AORAnnotatedPageSerializer aor_serializer;

    // book id -> stats
    private final Map<String, Stats> book_stats;

    // book id -> page stats
    private final Map<String, List<Stats>> page_stats;

    private static class Stats implements Comparable<Stats> {
        String id;
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

        public Stats(String id) {
            this.id = id;
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
        }

        @Override
        public int compareTo(Stats s) {
            return id.compareTo(s.id);
        }
    }

    public AorStatsCollector() {
        this.aor_serializer = new AORAnnotatedPageSerializer();
        this.book_stats = new HashMap<>();
        this.page_stats = new HashMap<>();
    }

    public void collectBookStats(String book_id, Path path) throws IOException {
        for (Path xml_path: Files.newDirectoryStream(path, "*.xml")) {
            String page_id = get_page_id(xml_path.getFileName().toString());
            collect_stats(book_id, page_id, xml_path);
        }
    }

    private String get_page_id(String filename) {
        int end = filename.lastIndexOf('.');

        if (end == -1) {
            return filename;
        }

        return filename.substring(0, end);
    }

    private void collect_stats(String book_id, String page_id, Path xml_path)
            throws IOException {
        List<String> errors = new ArrayList<>();

        AnnotatedPage ap;

        try (InputStream is = Files.newInputStream(xml_path)) {
            try {
                ap = aor_serializer.read(is, errors);
            } catch (IOException e) {
                System.err.println("Skipping " + xml_path + " error:"
                        + e.getMessage());
                return;
            }
        }

        if (errors.size() > 0) {
            System.err.println("Errors reading " + xml_path);

            for (String error: errors) {
                System.err.println(error);
            }
        }

        // Collect page stats and add to list

        Stats ps = new Stats(page_id);
        collect_stats(ap, ps);

        List<Stats> ps_list = page_stats.get(book_id);

        if (ps_list == null) {
            ps_list = new ArrayList<>();
            page_stats.put(book_id, ps_list);
        }

        ps_list.add(ps);

        // Update stats about the book with page stats

        Stats bs = book_stats.get(book_id);

        if (bs == null) {
            bs = new Stats(book_id);
            book_stats.put(book_id, bs);
        }

        bs.add(ps);
    }

    private void collect_stats(AnnotatedPage ap, Stats bs) {
        bs.marginalia += ap.getMarginalia().size();
        bs.marginalia_words += count_marginalia_words(ap);
        bs.underlines += ap.getUnderlines().size();
        bs.underline_words += count_underline_words(ap);
        bs.marks += ap.getMarks().size();
        bs.mark_words += count_mark_words(ap);
        bs.symbols += ap.getSymbols().size();
        bs.symbol_words += count_symbol_words(ap);
        bs.drawings += ap.getDrawings().size();
        bs.numerals += ap.getNumerals().size();
        bs.books += count_marginalia_books(ap);
        bs.people += count_marginalia_people(ap);
        bs.locations += count_marginalia_locations(ap);
    }

    private int count_marginalia_books(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {

            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getBooks().size();
                }
            }
        }

        return count;
    }

    private int count_marginalia_people(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {

            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getPeople().size();
                }
            }
        }

        return count;
    }

    private int count_marginalia_locations(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {

            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    count += p.getLocations().size();
                }
            }
        }

        return count;
    }

    private int count_underline_words(AnnotatedPage ap) {
        int count = 0;

        for (Underline ul: ap.getUnderlines()) {
            count += count_words(ul.getReferringText());
        }

        return count;
    }

    private int count_mark_words(AnnotatedPage ap) {
        int count = 0;

        for (Mark m: ap.getMarks()) {
            count += count_words(m.getReferringText());
        }

        return count;
    }

    private int count_symbol_words(AnnotatedPage ap) {
        int count = 0;

        for (Symbol s: ap.getSymbols()) {
            count += count_words(s.getReferringText());
        }

        return count;
    }

    private int count_marginalia_words(AnnotatedPage ap) {
        int count = 0;

        for (Marginalia m: ap.getMarginalia()) {
            for (MarginaliaLanguage ml: m.getLanguages()) {
                for (Position p: ml.getPositions()) {
                    for (String text: p.getTexts()) {
                        count += count_words(text);
                    }
                }
            }
        }

        return count;
    }

    private int count_words(String text) {
        return text.trim().split("\\s+").length;
    }

    public void writeBookStats(Path output_dir) throws IOException {
        Path book_csv_path = output_dir.resolve("books.csv");

        try (BufferedWriter out = Files.newBufferedWriter(book_csv_path, CHARSET)) {
            write_book_stats(out, book_stats);
        }

        for (String book_id: book_stats.keySet()) {
            Path page_csv_path = output_dir.resolve(book_id + ".csv");

            try (BufferedWriter out = Files.newBufferedWriter(page_csv_path, CHARSET)) {
                write_page_stats(out, page_stats.get(book_id));
            }
        }
    }

    private void write_page_stats(BufferedWriter out, List<Stats> list)
            throws IOException {
        write_header_row(out, "page");

        // TODO Assume sorting puts them in order...

        Collections.sort(list);

        for (Stats stats: list) {
            write_row(out, stats);
        }
    }

    private void write_row(BufferedWriter out, Stats s) throws IOException {
        out.write(s.id);
        out.write(',');

        out.write(String.valueOf(s.marginalia));
        out.write(',');

        out.write(String.valueOf(s.marginalia_words));
        out.write(',');

        out.write(String.valueOf(s.underlines));
        out.write(',');

        out.write(String.valueOf(s.underline_words));
        out.write(',');

        out.write(String.valueOf(s.marks));
        out.write(',');
        
        out.write(String.valueOf(s.mark_words));
        out.write(',');

        out.write(String.valueOf(s.symbols));
        out.write(',');
        
        out.write(String.valueOf(s.symbol_words));
        out.write(',');

        out.write(String.valueOf(s.drawings));
        out.write(',');

        out.write(String.valueOf(s.numerals));
        out.write(',');

        out.write(String.valueOf(s.books));
        out.write(',');

        out.write(String.valueOf(s.people));
        out.write(',');

        out.write(String.valueOf(s.locations));
        out.newLine();
    }

    private void write_book_stats(BufferedWriter out, Map<String, Stats> stats)
            throws IOException {
        write_header_row(out, "book");

        for (String book_id: stats.keySet()) {
            write_row(out, stats.get(book_id));
        }
    }
    
    private void write_header_row(BufferedWriter out, String first_cell) throws IOException {
        out.write(first_cell);
        out.write(",marginalia,marginalia_words,underlines,underline_words,marks,mark_words,symbols,symbol_words,drawings,numerals,books,people,locations");
        out.newLine();
    }
}
