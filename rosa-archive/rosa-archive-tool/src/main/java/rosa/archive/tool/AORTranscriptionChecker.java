package rosa.archive.tool;

import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.aor.AnnotatedPage;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AORTranscriptionChecker {

    private final AORAnnotatedPageSerializer serializer;

    public AORTranscriptionChecker() {
        this(new AORAnnotatedPageSerializer());
    }

    // For testing
    public AORTranscriptionChecker(AORAnnotatedPageSerializer serializer) {
        this.serializer = serializer;
    }

    public void run(String path, boolean isBook, PrintStream report) {
        if (isBook) {
            doBook(path, report);
        } else {
            doCollection(path, report);
        }
    }

    private void doCollection(final String path, PrintStream report) {

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(path), new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry);
            }
        })) {

            for (Path p : ds) {
                doBook(p.toString(), report);
            }

        } catch (IOException e) {
            report.println("Failed to read path. [" + path + "]\n");
            e.printStackTrace(report);
        }

    }

    private void doBook(String bookPath, PrintStream report) {
        report.println("Reading transcriptions for book. [" + bookPath + "]");

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(bookPath), new Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
                return entry.getFileName().toString().endsWith(".xml");
            }
        })) {

            for (Path xmlPath : ds) {
                List<String> errors = new ArrayList<>();
                String transcriptionName = xmlPath.getFileName().toString();

                if (!Files.isRegularFile(xmlPath)) {
                    continue;
                }
                try (InputStream xmlIn = Files.newInputStream(xmlPath)) {

                    AnnotatedPage aorPage = serializer.read(xmlIn, errors);

                    if (!errors.isEmpty()) {
                        report.println("  Errors for transcription [" + transcriptionName + ":");
                        for (String err : errors) {
                            report.println("    - " + err);
                        }

                        continue;
                    }

                    if (aorPage.getPage() != null) {
                        String[] aorName = transcriptionName.split("\\.");
                        String[] imageName = aorPage.getPage().split("\\.");

                        if (aorName.length < 2 || imageName.length < 2) {
                            report.println("  Invalid transcription or image name. ["
                                    + transcriptionName + " / " + aorPage.getPage() + "]");
                        } else if (!aorName[aorName.length - 2].equals(imageName[imageName.length - 2])) {
                            report.println("  Transcription file name does not match associated image file name. ["
                                    + transcriptionName + " / " + aorPage.getPage() + "]");
                        }
                    }

                } catch (IOException e) {
                    report.println("Failed to read file. [" + xmlPath + "]\n");
                    e.printStackTrace(report);
                }
            }

        } catch (IOException e) {
            report.println("Failed to find transcriptions in path. [" + bookPath + "]\n");
            e.printStackTrace(report);
        }
    }

}
