package rosa.archive.aor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.aor.AnnotatedPage;
/**
 * Collect stats on AOR annotations and write them out as CSV spreadsheets.
 */
public class AorStatsCollector {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final AORAnnotatedPageSerializer aor_serializer = new AORAnnotatedPageSerializer();

    // book id -> stats
    private final Map<String, Stats> book_stats;

    // book id -> page stats
    private final Map<String, List<Stats>> page_stats;

    /**
     * @param xmlPath fully qualified path to AOR page transcription
     * @return AOR annotated page
     * @throws IOException
     */
    public static AnnotatedPage readAorPage(String xmlPath) throws IOException {
        List<String> errors = new ArrayList<>();
        AnnotatedPage page;

        try(InputStream in = Files.newInputStream(Paths.get(xmlPath))) {
            try {
                page = aor_serializer.read(in, errors);
            } catch (IOException e) {
                System.err.println("Skipping " + xmlPath + " due to error: " + e.getMessage());
                return null;
            }
        }

        if (errors.size() > 0) {
            System.err.println("Errors reading " + xmlPath);

            for (String err : errors) {
                System.err.println(err);
            }
        }

        return page;
    }

    public AorStatsCollector() {
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
        AnnotatedPage ap = readAorPage(xml_path.toString());
        if (ap == null) {
            return;
        }

        // Collect page stats and add to list

        Stats ps = AorStatsAdapter.adaptAnnotatedPage(ap, page_id);

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

    public void writeBookStats(Path output_dir) throws IOException {
        Path book_csv_path = output_dir.resolve("books-latest.csv");

        try (BufferedWriter out = Files.newBufferedWriter(book_csv_path,
                CHARSET)) {
            write_book_stats(out, book_stats);
        }

        for (String book_id: book_stats.keySet()) {
            Path page_csv_path = output_dir.resolve(book_id + ".csv");

            try (BufferedWriter out = Files.newBufferedWriter(page_csv_path,
                    CHARSET)) {
                write_page_stats(out, page_stats.get(book_id));
            }
        }

        BookStats allStats = new BookStats();
        allStats.statsMap.putAll(book_stats);

        GitStatsWriter writer = new GitStatsWriter(output_dir);
        writer.cleanOutputDir();
        writer.writeVocab(allStats);
    }

    private void write_page_stats(BufferedWriter out, List<Stats> list)
            throws IOException {
        write_header_row(out, "page,id");

        // TODO Assume sorting puts them in order...

        Collections.sort(list);

        for (Stats stats: list) {
            write_row(out, stats, true);
        }
    }

    private void write_row(BufferedWriter out, Stats s, boolean writeIndex) throws IOException {
        out.write(s.id);
        out.write(',');

        if (writeIndex) {
            out.write(String.valueOf(s.pageIndex()));
            out.write(',');
        }

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
            write_row(out, stats.get(book_id), false);
        }
    }

    private void write_header_row(BufferedWriter out, String first_cell)
            throws IOException {
        out.write(first_cell);
        out.write(",marginalia,marginalia_words,underlines,underline_words,marks,mark_words,symbols,symbol_words,drawings,numerals,books,people,locations");
        out.newLine();
    }
}
