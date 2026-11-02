package rosa.archive.cli;

import picocli.CommandLine.Option;
import rosa.archive.core.FileSystemArchiveStore;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@code --archive} option shared by every command that reads an archive, with the
 * existence check each of them performs first.
 */
public final class ArchiveOptions {

    @Option(names = "--archive", required = true, description = "Path to archive directory")
    private Path archivePath;

    /** The archive directory. */
    public Path path() {
        return archivePath;
    }

    /**
     * Reports an error on stderr when the archive directory does not exist.
     *
     * @return true if the archive directory exists
     */
    public boolean exists() {
        if (!Files.isDirectory(archivePath)) {
            System.err.println("Error: archive path does not exist or is not a directory: " + archivePath);
            return false;
        }
        return true;
    }

    /** Opens the archive. */
    public FileSystemArchiveStore openStore() {
        return new FileSystemArchiveStore(archivePath);
    }
}
