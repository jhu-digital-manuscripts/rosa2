package rosa.archive.aor;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.CheckoutResult.Status;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import rosa.archive.model.aor.AnnotatedPage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GitStatCollector {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    /** Options for opening a file to write, create if it does not already exist, append to existing file. */
    private static final OpenOption[] WRITE_APPEND_OPTION = {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND};
    // List of files and directories to ignore
    private static final String[] IGNORE_NAMES = {"XMLschema", ".git", "README.md"};

    private final Set<String> ignore;
    private final String collection;
    // Commit ID -> book stats
    private Map<GitCommit, BookStats> commitMap;

    public GitStatCollector(String collection) {
        this.ignore = new HashSet<>(Arrays.asList(IGNORE_NAMES));
        this.commitMap = new HashMap<>();
        this.collection = collection;
    }

    public void run(String[] args) {
        Options options = new Options();
        // Empty for now

        CommandLineParser cliParser = new BasicParser();

        try {
            System.out.println("Gathering git stats.");
            CommandLine cmd = cliParser.parse(options, args);

            System.out.println(cmd.getArgList().toString());
            if (cmd.getArgs().length == 2) {
                collectGitStats(cmd.getArgs()[1]);
            }
        } catch (ParseException e) {
            System.err.println("Failed to parser command. " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Command failed.");
            e.printStackTrace();
        }
    }

    /**
     * Collect stats for a collection on Github across all commits.
     *
     * @param repositoryUrl Github URL of the repository
     */
    protected void collectGitStats(String repositoryUrl) throws IOException {
        Path current = Paths.get(".");
        Path localRepo = Files.createTempDirectory("git_tmp");

        // Force delete of repo directory and all subdirectories on JVM exit
        FileUtils.forceDeleteOnExit(localRepo.toFile());

        // Get list of all commits
        //   Parse list to generate a 'commits.csv'
        System.out.print("Cloning git repo [" + repositoryUrl + "]");

        CloneCommand cloner = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(localRepo.toFile());
        try (Git aorGit = cloner.call()) {
            System.out.println("\t\tDONE\n");
            Ref head = aorGit.getRepository().getRef("refs/heads/master");

            // a RevWalk allows to walk over commits based on some filtering that is defined
            try (RevWalk walk = new RevWalk(aorGit.getRepository())) {
                RevCommit commit = walk.parseCommit(head.getObjectId());
                System.out.println("Start-Commit: " + commit + "\n");

                // for each commit
                //   Generate the books.csv, appended to previous
                walk.markStart(commit); // Start at most recent commit, working backward in time
                int count = 0;
                for (RevCommit rev : walk) {
                    count++;
                    GitCommit gcom = new GitCommit(
                            rev.getId().getName(),
                            rev.getAuthorIdent().getWhen(),
                            rev.getAuthorIdent().getTimeZone(),
                            rev.getAuthorIdent().getName(),
                            rev.getFullMessage()
                    );
                    System.out.print("Processing commit: " + gcom.id + " | " + gcom.getISO8601Date() + " | " + gcom.author + "\t\t");

                    CheckoutCommand checkout = aorGit.checkout().setName(gcom.id);

                    checkout.call();        // Perform the checkout
                    CheckoutResult result = checkout.getResult(); // Check status, modified list, etc

                    if (result.getStatus() == Status.OK) {
//                        BookStats stats = collectBookStats(localRepo);
//                        if (stats != null) {
//                            commitMap.put(gcom, stats);
//                        }
                        System.out.println("OK [" + result.getModifiedList().size() + "] modifications.");
                        // TODO write results to file
                    } else {
                        System.err.println("CHECKOUT FAILED [" + result.getStatus() + "]");
                    }
                }
                System.out.println("Found commits: " + count);

                walk.dispose();
            }

        } catch (GitAPIException e) {
            System.err.println("Failed to clone repository.");
        }


//        for (Entry<GitCommit, BookStats> entry : commitMap.entrySet()) {
//            System.out.println(entry.getKey() + " ::: " + entry.getValue());
//        }


        System.out.println("\nDeleting local repository. [" + localRepo.toString() + "]");
        FileUtils.forceDelete(localRepo.toFile());
    }

    /**
     * Write out entire data set once it has been collected.
     *
     * @param output destination path
     */
    public void writeGitStats(Path output) {
        Path booksCsvPath = output.resolve("books.csv");
        Path commitsCsvPath = output.resolve("commits.csv");

        try (BufferedWriter out = Files.newBufferedWriter(commitsCsvPath, CHARSET)) {
            writeCommitStats(out);
        } catch (IOException e) {
            System.err.println("Failed to write commits.csv");
        }

        try (BufferedWriter out = Files.newBufferedWriter(booksCsvPath, CHARSET)) {
            writeGitStats(out);
        } catch (IOException e) {
            System.err.println("Failed to write books.csv");
        }
    }

    private void writeGitStats(GitCommit commit, BookStats stats, Path output) {
        Path booksCsvPath = output.resolve("books.csv");
        Path commitsCsvPath = output.resolve("commits.csv");

        boolean isFirst = Files.exists(booksCsvPath);
        try (BufferedWriter out = Files.newBufferedWriter(booksCsvPath, CHARSET, WRITE_APPEND_OPTION)) {
            writeSingleGitStat(out, commit.id, stats, isFirst);
        } catch (IOException e) {
            System.err.println("Failed to write to books.csv on commit [" + commit.id + "]");
        }

        isFirst = Files.exists(commitsCsvPath);
        try (BufferedWriter out = Files.newBufferedWriter(commitsCsvPath, CHARSET, WRITE_APPEND_OPTION)) {
            writeSingleCommit(out, commit, isFirst);
        } catch (IOException e) {
            System.err.println("Failed to write to commits.csv on commit [" + commit.id + "]");
        }
    }

    /**
     * Collect all of the aggregated stats for each book in this repository.
     *
     * @param localRepo as Path
     * @return .
     */
    private BookStats collectBookStats(Path localRepo) {
        BookStats bookStats;
        // Walk all book directories
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(localRepo, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return !ignore.contains(entry.getFileName().toString());
            }
        })) {
            bookStats = new BookStats(collection);

            for (Path bookPath : ds) {
                statsForBook(bookPath, bookStats);
            }

            return bookStats;
        } catch (Exception e) {
            System.err.println("Could not read books in the collection.");
            return null;
        }
    }

    /**
     * Collect all stats for one book by iterating over the books pages.
     *
     * @param bookPath path of book in file system
     * @param targetBookStats object to store results
     */
    private void statsForBook(Path bookPath, BookStats targetBookStats) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(bookPath, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                String filename = entry.getFileName().toString();
                return filename.endsWith(".xml") && !ignore.contains(filename);
            }
        })) {
            String bookId = bookPath.getFileName().toString();

            for (Path pagePath : ds) {
                AnnotatedPage annotatedPage = AorStatsCollector.readAorPage(pagePath.toString());
                if (annotatedPage == null) {
                    continue;
                }

                String pageId = getPageId(pagePath.getFileName().toString());
                Stats pageStats = AorTranscriptionAdapter.adaptAnnotatedPage(annotatedPage, pageId);

                // TODO Plug in here to generate individual page stats
                // TODO vocab

                targetBookStats.addPageStats(bookId, pageStats);
            }

        } catch (Exception e) {
            System.err.println("Failed to read stats for book. [" + bookPath + "]");
        }
    }

    /**
     * Get the page ID from a filename by stripping the file extension.
     *
     * @param filename .
     * @return .
     */
    private String getPageId(String filename) {
        int end = filename.lastIndexOf('.');

        if (end == -1) {
            return filename;
        }

        return filename.substring(0, end);
    }

    private void writeSingleCommit(BufferedWriter out, GitCommit commit, boolean writeHeader) throws IOException {
        if (writeHeader) {
            out.write("commit_id,date,author,message");
            out.newLine();
        }
    }

    private void writeCommitStats(BufferedWriter out) throws IOException {
        // Sort commits by date before writing
        List<GitCommit> commits = new ArrayList<>(commitMap.keySet());
        Collections.sort(commits);

        out.write("commit_id,date,author,message");
        out.newLine();

        for (GitCommit commit : commits) {
            writeCommitRow(out, commit);
        }
    }

    private void writeCommitRow(BufferedWriter out, GitCommit commit) throws IOException {
        out.write(commit.id);
        out.write(',');

        out.write(commit.getISO8601Date());
        out.write(',');

        out.write(commit.author);
        out.write(',');

        out.write(commit.message);
        out.newLine();
    }

    private void writeGitStats(BufferedWriter writer) throws IOException {
        write_header_row(writer, "commit_id,book");

        // Sort commits by date before writing
        List<GitCommit> commits = new ArrayList<>(commitMap.keySet());
        Collections.sort(commits);

        for (GitCommit commit : commits) {
            BookStats stats = commitMap.get(commit);

            // Sort book titles before writing
            List<String> books = new ArrayList<>(stats.statsMap.keySet());
            Collections.sort(books);

            for (String book : books) {
                write_row(writer, stats.statsMap.get(book), commit.id);
            }
        }
    }

    private void writeSingleGitStat(BufferedWriter out, String commitId, BookStats stats,
                                    boolean writeHeader) throws IOException {
        if (writeHeader) {
            write_header_row(out, "commid_id,book");
        }

        List<String> books = new ArrayList<>(stats.statsMap.keySet());
        Collections.sort(books);

        for (String book : books) {
            write_row(out, stats.statsMap.get(book), commitId);
        }
    }

    private void write_header_row(BufferedWriter out, String first_cell) throws IOException {
        out.write(first_cell);
        out.write(",total,total_words,marginalia,marginalia_words,underlines,underline_words," +
                "marks,mark_words,symbols,symbol_words,drawings,numerals,books,people,locations");
        out.newLine();
    }

    private void write_row(BufferedWriter out, Stats s, String commitId) throws IOException {
        out.write(commitId);
        out.write(',');

        out.write(s.id);
        out.write(',');

        out.write(s.totalAnnotations());
        out.write(',');

        out.write(s.totalWords());
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

}
