package rosa.archive.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A standalone file-renaming utility that renames files in a directory
 * according to old,new pairs specified in a CSV file.
 *
 * <p>This command does not depend on {@code ArchiveStore} — it operates
 * directly on a directory and CSV mapping provided as positional arguments.</p>
 *
 * <p>Each line in the CSV file should contain exactly two comma-separated values:
 * the current filename and the desired new filename. Lines that are blank,
 * malformed, or reference missing source files are reported as errors.</p>
 */
@Command(name = "rename-files",
         mixinStandardHelpOptions = true,
         description = "Rename files in a directory according to a CSV mapping")
public final class RenameFilesCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Directory containing files to rename")
    private Path directory;

    @Parameters(index = "1", description = "CSV file with old,new filename pairs")
    private Path csvFile;

    /**
     * Executes the file rename operation.
     *
     * @return 0 if all renames succeeded, 1 if any errors occurred
     */
    @Override
    public Integer call() {
        if (!Files.isDirectory(directory)) {
            System.err.println("Directory does not exist: " + directory);
            return 1;
        }
        if (!Files.isRegularFile(csvFile) || !Files.isReadable(csvFile)) {
            System.err.println("CSV file does not exist or is not readable: " + csvFile);
            return 1;
        }

        List<String> errors = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(csvFile);

            for (String line : lines) {
                String[] parts = line.split(",", 2);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    errors.add("Invalid CSV line: " + line);
                    continue;
                }

                Path source = directory.resolve(parts[0].trim());
                Path target = directory.resolve(parts[1].trim());

                if (Files.exists(source)) {
                    Files.move(source, target);
                } else {
                    errors.add("Source file not found: " + parts[0].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV or renaming files: " + e.getMessage());
            return 1;
        }

        errors.forEach(System.err::println);
        return errors.isEmpty() ? 0 : 1;
    }
}
