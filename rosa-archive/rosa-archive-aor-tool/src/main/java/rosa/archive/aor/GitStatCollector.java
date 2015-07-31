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
import rosa.archive.model.aor.AnnotatedPage;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitStatCollector {
    // List of files and directories to ignore
    private static final String[] IGNORE_NAMES = {"XMLschema", ".git", "README.md"};

    private final Set<String> ignore;
    private String output;

    private GitStatsWriter writer;
    private PrintStream report;
    private PrintStream error;

    private final Filter<Path> BOOK_FILES_FILTER;
    private final Filter<Path> COLLECTION_BOOKS_FILTER;

    public GitStatCollector() {
        this.report = System.out;
        this.error = System.err;

        this.ignore = new HashSet<>(Arrays.asList(IGNORE_NAMES));
        output = ".";

        BOOK_FILES_FILTER = new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                String filename = entry.getFileName().toString();
                return filename.endsWith(".xml") && !ignore.contains(filename);
            }
        };

        COLLECTION_BOOKS_FILTER = new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return !ignore.contains(entry.getFileName().toString());
            }
        };
    }

    /**
     * Set the stream to report messages. If no PrintStream
     * is set through this method, it will default to System.out
     * 
     * @param report PrintStream
     */
    public void setReport(PrintStream report) {
        this.report = report;
    }

    /**
     * Set the stream to report errors. If no PrintStream is set 
     * through this method, it will default to System.err
     * 
     * @param error PrintStream
     */
    public void setError(PrintStream error) {
        this.error = error;
    }

    /**
     * Set the directory for output files. Used mostly for testing.
     *
     * @param outputDirectory .
     */
    public void setOutputDirectory(String outputDirectory) {
        this.output = outputDirectory;
    }

    public void run(String[] args) {
        Options options = new Options();
        // Empty for now

        CommandLineParser cliParser = new BasicParser();

        try {
            report.println("Gathering git stats.");
            CommandLine cmd = cliParser.parse(options, args);

            report.println(cmd.getArgList().toString());
            if (cmd.getArgs().length == 2) {
                collectGitStats(cmd.getArgs()[1], false);
            }
        } catch (ParseException e) {
            error.println("Failed to parser command. " + e.getMessage());
        }
    }

    /**
     * Collect stats for a collection on Github across all commits.
     *
     * @param repositoryUrl Github URL of the repository
     * @param onlyMostRecent collect stats for only most recent commit? Setting this to
     *                       FALSE will collect stats for all commits. Setting this to
     *                       TRUE will collect stats for only the most recent commit.
     */
    protected void collectGitStats(String repositoryUrl, boolean onlyMostRecent) {
        Path localRepo = null;
        writer = new GitStatsWriter(output);

        try {
            writer.cleanOutputDir();
            localRepo = Files.createTempDirectory("git_tmp");

            // Force delete of repo directory and all subdirectories on JVM exit
            FileUtils.forceDeleteOnExit(localRepo.toFile());

            // Get list of all commits
            //   Parse list to generate a 'commits.csv'
            report.print("Cloning git repo [" + repositoryUrl + "]");

            CloneCommand cloner = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(localRepo.toFile());
            try (Git aorGit = cloner.call()) {
                report.println("\t\tDONE\n");
                Ref head = aorGit.getRepository().getRef("refs/heads/master");

                walkCommitTree(aorGit, head, localRepo, onlyMostRecent);

            } catch (GitAPIException e) {
                error.println("Failed to clone repository.");
            }
        } catch (IOException e) {
            error.println("Failed to read files. " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (localRepo != null) {
                report.println("\nDeleting local repository. [" + localRepo.toString() + "]");
                FileUtils.deleteQuietly(localRepo.toFile());
            }
        }
    }

    private void walkCommitTree(Git aorGit, Ref head, Path localRepo, final boolean onlyMostRecent)
            throws GitAPIException, IOException {
        // a RevWalk allows to walk over commits based on some filtering that is defined
        try (RevWalk walk = new RevWalk(aorGit.getRepository())) {
            RevCommit commit = walk.parseCommit(head.getObjectId());
            report.println("Start-Commit: " + commit + "\n");

            // for each commit
            //   Generate the books.csv, appended to previous
            walk.markStart(commit); // Start at most recent commit, working backward in time
            int count = 0;
            for (RevCommit rev : walk) {
                count++;
                CheckoutCommand checkout = aorGit.checkout().setName(rev.getId().getName());
                checkout.call();

                CheckoutResult result = checkout.getResult(); // Check status, modified list, etc

                // TODO more fully represent ancestor commits - (more than 1 parent for merges)

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

                report.print("Processing commit [" + count + "]: " + gcom.id + " | "
                        + gcom.getISO8601Date() + " | " + gcom.author + " | "
                        + gcom.getFilesCount(ChangeType.ADD) + " additions | ");

                if (result.getStatus() == Status.OK) {
                    long totalMem = Runtime.getRuntime().totalMemory();
                    long maxMem = Runtime.getRuntime().maxMemory();

                    report.println("CHECKOUT OK | Memory usage: " + totalMem + " / " + maxMem);

                    BookStats stats = collectBookStats(localRepo);
                    if (stats != null) {
                        writer.writeGitStats(gcom, stats);
                    }
                } else {
                    error.println("CHECKOUT FAILED [" + result.getStatus() + "]");
                }

                // If this flag is TRUE, bail out after first iteration
                if (onlyMostRecent) {
                    break;
                }
            }
            report.println("Found commits: " + count);
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
            ObjectId parentId = aorGit.getRepository().resolve(
                    currentRev.getParent(i).getId().getName() + "^{tree}");

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
                error.println("  - Failed to calculate diffs. [" + parentId.name() + " -> "
                        + currentId.name() + "]");
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
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(localRepo, COLLECTION_BOOKS_FILTER)) {
            bookStats = new BookStats();

            for (Path bookPath : ds) {
                statsForBook(bookPath, bookStats);
            }

            return bookStats;
        } catch (Exception e) {
            error.println("Could not read books in the collection.");
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
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(bookPath, BOOK_FILES_FILTER)) {
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
            error.println("Failed to read stats for book. [" + bookPath + "]");
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

}
