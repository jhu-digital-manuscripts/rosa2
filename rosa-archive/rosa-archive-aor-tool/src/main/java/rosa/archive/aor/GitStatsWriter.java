package rosa.archive.aor;

import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import rosa.archive.core.util.CSV;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitStatsWriter {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    // Options for opening a file to write, create if it does not already exist, append to existing file.
    private static final OpenOption[] WRITE_APPEND_OPTION = {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND};
    private static final String COMMITS_HEADER = "commit_id,parent_id,date,author,email,message,added,modified,deleted,renamed,copied,unreadable";

    private final String OUTPUT_BOOKS;
    private final String OUTPUT_COMMITS;

    private Path output;

    public GitStatsWriter(String output) {
        this(output, "books.csv", "commits.csv");
    }

    public GitStatsWriter(String outputDir, String statsFilename, String commitsFilename) {
        this.OUTPUT_BOOKS = statsFilename;
        this.OUTPUT_COMMITS = commitsFilename;
        this.output = Paths.get(outputDir);
    }

    public void cleanOutputDir() {
        try {
            Files.deleteIfExists(output.resolve(OUTPUT_BOOKS));
            Files.deleteIfExists(output.resolve(OUTPUT_COMMITS));
        } catch (IOException e) {
            System.err.println("Failed to clean output directory.");
        }
    }

    /**
     * Write out data to both the commits data file and the book stats
     * data file.
     *
     * @param commit git commit info
     * @param stats stats of repo aggregated by book
     */
    public void writeGitStats(GitCommit commit, BookStats stats) {
        writeOnlyStats(commit, stats);
        writeOnlyCommit(commit, stats.getNumberOfUnreadablePages());
    }

    /**
     * Write out data to only the book stats data file.
     *
     * @param commit git commit info
     * @param stats stats of repo aggregated by book
     */
    public void writeOnlyStats(GitCommit commit, BookStats stats) {
        Path booksCsvPath = output.resolve(OUTPUT_BOOKS);

        boolean isFirst = !Files.exists(booksCsvPath);
        try (BufferedWriter out = Files.newBufferedWriter(booksCsvPath, CHARSET, WRITE_APPEND_OPTION)) {
            writeSingleGitStat(out, commit, stats, isFirst);
        } catch (IOException e) {
            System.err.println("Failed to write to books.csv on commit [" + commit.id + "]");
        }
    }

    /**
     * Write out data to only the commits data file.
     *
     * @param commit git commit info
     */
    public void writeOnlyCommit(GitCommit commit, int unreadablePages) {
        Path commitsCsvPath = output.resolve(OUTPUT_COMMITS);

        boolean isFirst = !Files.exists(commitsCsvPath);
        try (BufferedWriter out = Files.newBufferedWriter(commitsCsvPath, CHARSET, WRITE_APPEND_OPTION)) {
            writeSingleCommit(out, commit, unreadablePages, isFirst);
        } catch (IOException e) {
            System.err.println("Failed to write to commits.csv on commit [" + commit.id + "]");
        }
    }

    private void writeSingleCommit(BufferedWriter out, GitCommit commit, int unreadablePages,
                                   boolean writeHeader) throws IOException {
        if (writeHeader) {
            out.write(COMMITS_HEADER);
            out.newLine();
        }

        writeCommitRow(out, commit, unreadablePages);
    }

    private void writeCommitRow(BufferedWriter out, GitCommit commit, int unreadable) throws IOException {
        out.write(commit.id);
        out.write(',');

        out.write(commit.parentCommit);
        out.write(',');

        out.write(commit.getISO8601Date());
        out.write(',');

        out.write(commit.author);
        out.write(',');

        out.write(commit.email);
        out.write(',');

        // Strip trailing newLines if applicable
        if (commit.message.endsWith(System.lineSeparator())) {
            out.write(CSV.escape(commit.message.substring(0,
                    commit.message.length() - System.lineSeparator().length())));
        } else {
            out.write(CSV.escape(commit.message));
        }
        out.write(',');

        out.write(String.valueOf(commit.getFilesCount(ChangeType.ADD)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesCount(ChangeType.MODIFY)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesCount(ChangeType.DELETE)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesCount(ChangeType.RENAME)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesCount(ChangeType.COPY)));
        out.write(',');

        out.write(String.valueOf(unreadable));
        out.newLine();
    }

    public void writeSingleGitStat(BufferedWriter out, GitCommit commit, BookStats stats,
                                    boolean writeHeader) throws IOException {
        if (writeHeader) {
            write_header_row(out, "commit_id,book");
        }

        List<String> books = new ArrayList<>(stats.statsMap.keySet());
        Collections.sort(books);

        for (String book : books) {
            write_row(out, stats.statsMap.get(book), commit, stats.getNumberOfUnreadablePages(book));
        }
    }

    private void write_header_row(BufferedWriter out, String first_cell) throws IOException {
        out.write(first_cell);
        out.write(",total,total_words,marginalia,marginalia_words,underlines,underline_words," +
                "marks,mark_words,symbols,symbol_words,drawings,numerals,books,people,locations" +
                ",added,modified,deleted,renamed,copied,unreadable");
        out.newLine();
    }

    private void write_row(BufferedWriter out, Stats s, GitCommit commit, int unreadablePages) throws IOException {
        out.write(commit.id);
        out.write(',');

        out.write(s.id);
        out.write(',');

        out.write(String.valueOf(s.totalAnnotations()));
        out.write(',');

        out.write(String.valueOf(s.totalWords()));
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
        out.write(',');

        out.write(String.valueOf(commit.getFilesChangedForBook(s.id, ChangeType.ADD)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesChangedForBook(s.id, ChangeType.MODIFY)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesChangedForBook(s.id, ChangeType.DELETE)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesChangedForBook(s.id, ChangeType.RENAME)));
        out.write(',');

        out.write(String.valueOf(commit.getFilesChangedForBook(s.id, ChangeType.COPY)));
        out.write(',');

        out.write(String.valueOf(unreadablePages));
        out.newLine();
    }

}
