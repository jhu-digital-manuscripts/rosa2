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
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import rosa.archive.aor.GitCommit.Builder;
import rosa.archive.core.util.CSV;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitStatCollector {
    private static final Charset CHARSET = Charset.forName("UTF-8");
    // Options for opening a file to write, create if it does not already exist, append to existing file.
    private static final OpenOption[] WRITE_APPEND_OPTION = {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND};
    // List of files and directories to ignore
    private static final String[] IGNORE_NAMES = {"XMLschema", ".git", "README.md"};

    private static final String COMMITS_HEADER = "commit_id,parent_id,date,author,email,message,added,modfied,deleted,renamed,copied";

    private final Set<String> ignore;
    private String output;

    public GitStatCollector() {
        this.ignore = new HashSet<>(Arrays.asList(IGNORE_NAMES));
        output = ".";
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
        }
    }

    public void setOutputDirectory(String outputDirectory) {
        this.output = outputDirectory;
    }

    /**
     * Collect stats for a collection on Github across all commits.
     *
     * @param repositoryUrl Github URL of the repository
     */
    protected void collectGitStats(String repositoryUrl) {
        Path localRepo = null;
        try {
            Path current = Paths.get(output);
            localRepo = Files.createTempDirectory("git_tmp");

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

                walkCommitTree(aorGit, head, localRepo, current);

            } catch (GitAPIException e) {
                System.err.println("Failed to clone repository.");
            }
        } catch (IOException e) {
            System.err.println("Failed to read files. " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (localRepo != null) {
                System.out.println("\nDeleting local repository. [" + localRepo.toString() + "]");
                FileUtils.deleteQuietly(localRepo.toFile());
            }
        }
    }

    private void walkCommitTree(Git aorGit, Ref head, Path localRepo, Path outputPath)
            throws GitAPIException, IOException {
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
                CheckoutCommand checkout = aorGit.checkout().setName(rev.getId().getName());
                checkout.call();

                CheckoutResult result = checkout.getResult(); // Check status, modified list, etc

                // Hack to jam all parent commit IDs into a single String
                // TODO more fully represent ancestor commits - (more than 1 parent for merges)
//                StringBuilder parents = new StringBuilder();
//                for (int i = 0; i < rev.getParentCount(); i++) {
//                    if (i != 0) {
//                        parents.append(',');
//                    }
//                    parents.append(rev.getParent(i));
//                }

                GitCommit gcom = Builder.newBuilder()
                        .id(rev.getId().getName())
                        // NOTE: Could contain more than 1 parent commit, this will only display the first parent.
                        .parentCommit(rev.getParentCount() > 0 ? rev.getParent(0).getId().getName() : "")
                        .date(rev.getAuthorIdent().getWhen())
                        .timeZone(rev.getAuthorIdent().getTimeZone())
                        .author(rev.getAuthorIdent().getName())
                        .email(rev.getAuthorIdent().getEmailAddress())
                        .message(rev.getFullMessage())
                        .diffs(diffs(rev, aorGit))
                        .build();

                System.out.print("Processing commit [" + count + "]: " + gcom.id + " | "
                        + gcom.getISO8601Date() + " | " + gcom.author + " | " + gcom.getFilesCount(ChangeType.ADD) + " | ");

                if (result.getStatus() == Status.OK) {
                    long totalMem = Runtime.getRuntime().totalMemory();
                    long maxMem = Runtime.getRuntime().maxMemory();
                    System.out.println("OK [" + result.getModifiedList().size() + "] modifications. | Memory usage: "
                            + totalMem + " / " + maxMem);
                    BookStats stats = collectBookStats(localRepo);
                    if (stats != null) {
                        writeGitStats(gcom, stats, outputPath);
                    }
                } else {
                    System.err.println("CHECKOUT FAILED [" + result.getStatus() + "]");
                }
            }
            System.out.println("Found commits: " + count);
            walk.dispose();
        }
    }

    /**
     * Get a list of all changes made in this commit. This is done by listing all
     * differences between the parent commits and the current commit. Each difference
     * is recorded in a {@link DiffEntry} object, which marks each file changed with
     * the type of change made.
     *
     * @param currentRev current commit
     * @param aorGit Git object
     * @return list of all diffs
     * @throws GitAPIException .
     * @throws IOException .
     */
    private List<DiffEntry> diffs(RevCommit currentRev, Git aorGit) throws GitAPIException, IOException {
        List<DiffEntry> allDiffs = new ArrayList<>();
        ObjectId currentId = aorGit.getRepository().resolve(currentRev.getId().getName() + "^{tree}");

        ObjectReader reader = aorGit.getRepository().newObjectReader();
        CanonicalTreeParser currentTreeParser = new CanonicalTreeParser();
        for (int i = 0; i < currentRev.getParentCount(); i++) {
            ObjectId parentId = aorGit.getRepository().resolve(currentRev.getParent(i).getId().getName() + "^{tree}");

            try {
                CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();

                currentTreeParser.reset(reader, currentId);
                parentTreeParser.reset(reader, parentId);

                List<DiffEntry> diffs = aorGit.diff()
                        .setNewTree(currentTreeParser)
                        .setOldTree(parentTreeParser)
                        .call();

                if (diffs != null) {
                    allDiffs.addAll(diffs);
                }
            } catch (GitAPIException | IOException e) {
                System.err.println("Failed to calculate diffs. [" + parentId.name() + " -> " + currentId.name() + "]");
            }
        }

        return allDiffs;
    }

    /**
     * Collect all of the aggregated stats for each book in this repository.
     *
     * @param localRepo as Path
     * @return .
     */
    protected BookStats collectBookStats(Path localRepo) {
        BookStats bookStats;
        // Walk all book directories
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(localRepo, new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return !ignore.contains(entry.getFileName().toString());
            }
        })) {
            bookStats = new BookStats();

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

    private void writeGitStats(GitCommit commit, BookStats stats, Path output) {
        Path booksCsvPath = output.resolve("books.csv");
        Path commitsCsvPath = output.resolve("commits.csv");

        boolean isFirst = !Files.exists(booksCsvPath);
        try (BufferedWriter out = Files.newBufferedWriter(booksCsvPath, CHARSET, WRITE_APPEND_OPTION)) {
            writeSingleGitStat(out, commit.id, stats, isFirst);
        } catch (IOException e) {
            System.err.println("Failed to write to books.csv on commit [" + commit.id + "]");
        }

        isFirst = !Files.exists(commitsCsvPath);
        try (BufferedWriter out = Files.newBufferedWriter(commitsCsvPath, CHARSET, WRITE_APPEND_OPTION)) {
            writeSingleCommit(out, commit, isFirst);
        } catch (IOException e) {
            System.err.println("Failed to write to commits.csv on commit [" + commit.id + "]");
        }
    }

    private void writeSingleCommit(BufferedWriter out, GitCommit commit, boolean writeHeader) throws IOException {
        if (writeHeader) {
            out.write(COMMITS_HEADER);
            out.newLine();
        }

        writeCommitRow(out, commit);
    }

    private void writeCommitRow(BufferedWriter out, GitCommit commit) throws IOException {
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

        out.newLine();
    }

    private void writeSingleGitStat(BufferedWriter out, String commitId, BookStats stats,
                                    boolean writeHeader) throws IOException {
        if (writeHeader) {
            write_header_row(out, "commit_id,book");
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
        out.newLine();
    }

}
