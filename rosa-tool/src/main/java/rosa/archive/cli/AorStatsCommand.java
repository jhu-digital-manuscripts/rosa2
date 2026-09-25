package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import rosa.archive.aor.AoRStatsCollector;
import rosa.archive.aor.AoRStatsCollector.BookTotals;
import rosa.archive.aor.AoRStatsCollector.PageStats;
import rosa.archive.aor.AoRStatsCollector.VocabEntry;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Generates statistics about AoR annotations for one or more books.
 *
 * <p>Accepts one or more book directory paths as arguments, collects annotation
 * statistics from each valid directory, and writes CSV output files to the
 * current working directory.
 */
@Command(name = "aor-stats",
         mixinStandardHelpOptions = true,
         description = "Generate statistics about AoR annotations")
public final class AorStatsCommand implements Callable<Integer> {

    @Parameters(description = "One or more book directory paths", arity = "1..*")
    private List<Path> bookDirs;

    @Override
    public Integer call() {
        Path outputDir = Path.of("").toAbsolutePath();
        AoRStatsCollector collector = new AoRStatsCollector();
        List<BookTotals> allBookTotals = new ArrayList<>();

        for (Path bookDir : bookDirs) {
            if (!Files.isDirectory(bookDir)) {
                System.err.println("Warning: skipping invalid directory: " + bookDir);
                continue;
            }

            try {
                BookTotals totals = collector.collectBookTotals(bookDir);
                allBookTotals.add(totals);

                List<PageStats> pageStats = collector.collectPageStats(bookDir);
                writePageStatsCsv(outputDir, totals.bookId(), pageStats);

                Map<String, List<VocabEntry>> vocab = collector.collectVocab(bookDir);
                writeVocabCsvs(outputDir, vocab);
            } catch (IOException e) {
                System.err.println("Warning: error processing directory " + bookDir + ": " + e.getMessage());
            }
        }

        if (!allBookTotals.isEmpty()) {
            try {
                writeBookTotalsCsv(outputDir, allBookTotals);
            } catch (IOException e) {
                System.err.println("Error writing book_totals.csv: " + e.getMessage());
                return 1;
            }
        }

        return 0;
    }

    private void writeBookTotalsCsv(Path outputDir, List<BookTotals> totals) throws IOException {
        Path csvPath = outputDir.resolve("book_totals.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            writer.write("book,marginalia,marginalia_words,underlines,underline_words,marks,mark_words,"
                    + "symbols,symbol_words,drawings,drawing_words,numerals,calculations,graphs,graph_words,"
                    + "tables,table_words,physical_links,books,people,locations");
            writer.newLine();
            for (BookTotals bt : totals) {
                writer.write(String.join(",",
                        bt.bookId(),
                        String.valueOf(bt.counts().marginalia()),
                        String.valueOf(bt.counts().marginaliaWords()),
                        String.valueOf(bt.counts().underlines()),
                        String.valueOf(bt.counts().underlineWords()),
                        String.valueOf(bt.counts().marks()),
                        String.valueOf(bt.counts().markWords()),
                        String.valueOf(bt.counts().symbols()),
                        String.valueOf(bt.counts().symbolWords()),
                        String.valueOf(bt.counts().drawings()),
                        String.valueOf(bt.counts().drawingWords()),
                        String.valueOf(bt.counts().numerals()),
                        String.valueOf(bt.counts().calculations()),
                        String.valueOf(bt.counts().graphs()),
                        String.valueOf(bt.counts().graphWords()),
                        String.valueOf(bt.counts().tables()),
                        String.valueOf(bt.counts().tableWords()),
                        String.valueOf(bt.counts().physLinks()),
                        String.valueOf(bt.refs().books()),
                        String.valueOf(bt.refs().people()),
                        String.valueOf(bt.refs().locations())));
                writer.newLine();
            }
        }
    }

    private void writePageStatsCsv(Path outputDir, String bookId, List<PageStats> pageStats) throws IOException {
        Path csvPath = outputDir.resolve(bookId + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            writer.write("page,id,marginalia,marginalia_words,underlines,underline_words,marks,mark_words,"
                    + "symbols,symbol_words,drawings,drawing_words,numerals,calculations,graphs,graph_words,"
                    + "tables,table_words,physical_links,books,people,locations");
            writer.newLine();
            for (PageStats ps : pageStats) {
                writer.write(String.join(",",
                        ps.pageId(),
                        String.valueOf(ps.pageIndex()),
                        String.valueOf(ps.counts().marginalia()),
                        String.valueOf(ps.counts().marginaliaWords()),
                        String.valueOf(ps.counts().underlines()),
                        String.valueOf(ps.counts().underlineWords()),
                        String.valueOf(ps.counts().marks()),
                        String.valueOf(ps.counts().markWords()),
                        String.valueOf(ps.counts().symbols()),
                        String.valueOf(ps.counts().symbolWords()),
                        String.valueOf(ps.counts().drawings()),
                        String.valueOf(ps.counts().drawingWords()),
                        String.valueOf(ps.counts().numerals()),
                        String.valueOf(ps.counts().calculations()),
                        String.valueOf(ps.counts().graphs()),
                        String.valueOf(ps.counts().graphWords()),
                        String.valueOf(ps.counts().tables()),
                        String.valueOf(ps.counts().tableWords()),
                        String.valueOf(ps.counts().physLinks()),
                        String.valueOf(ps.refs().books()),
                        String.valueOf(ps.refs().people()),
                        String.valueOf(ps.refs().locations())));
                writer.newLine();
            }
        }
    }

    private void writeVocabCsvs(Path outputDir, Map<String, List<VocabEntry>> vocab) throws IOException {
        for (Map.Entry<String, List<VocabEntry>> entry : vocab.entrySet()) {
            Path csvPath = outputDir.resolve("vocab_" + entry.getKey() + ".csv");
            try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
                writer.write("word,frequency");
                writer.newLine();
                for (VocabEntry ve : entry.getValue()) {
                    writer.write(ve.word() + "," + ve.frequency());
                    writer.newLine();
                }
            }
        }
    }
}
