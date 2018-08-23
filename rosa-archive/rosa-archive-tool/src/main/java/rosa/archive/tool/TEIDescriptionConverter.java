package rosa.archive.tool;

import org.apache.commons.cli.CommandLine;
import rosa.archive.core.serialize.DeprecatedBookMetadataSerializer;
import rosa.archive.core.serialize.BookMetadataSerializer;
import rosa.archive.model.DeprecatedBookMetadata;
import rosa.archive.model.BiblioData;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.tool.config.ToolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Convert TEI descriptions to our custom xml book metadata description.
 * The TEI descriptions are left untouched.
 */
public class TEIDescriptionConverter {
    private static final DeprecatedBookMetadataSerializer deprecatedBookMetadataSerializer = new DeprecatedBookMetadataSerializer();
    private static final BookMetadataSerializer bookMetadataSerializer = new BookMetadataSerializer();

    public static void run(CommandLine cmd, ToolConfig config, PrintStream report) {
        System.out.println("Archive path: " + config.getArchivePath());

        String[] args = cmd.getArgs();
        Path archivePath = Paths.get(config.getArchivePath());

        switch (args.length) {
            case 2:
                handle_collection(cmd, archivePath, report);
                break;
            case 3:
                handle_book(cmd, archivePath.resolve(args[1]), report);
                break;
            default:
                report.println("Command: " + Arrays.toString(args));
                report.println("Too many arguments. Usage: <command> [-options] <collectionId> <bookId>");
                break;
        }

    }

    private static void handle_collection(CommandLine cmd, Path archivePath, PrintStream report) {
        Path collectionPath = archivePath.resolve(cmd.getArgs()[1]);

        report.println("Handling collection [" + collectionPath.toString() + "]");

        try (Stream<Path> stream = Files.list(collectionPath)){

            stream.filter(entry -> Files.isDirectory(entry))                        // Use only directories
                    .map(entry -> entry.getFileName().toString())                   // Map to simple name, from fully qualified name
                    .forEach(name -> handle_book(name, collectionPath, report));    // Treat each as a book, handle accordingly

        } catch (IOException e) {
            report.printf("[ERROR] Command %s\nFailed to execute for collection.",
                    Arrays.toString(cmd.getArgs()) + "\n");
        }
    }

    private static void handle_book(CommandLine cmd, Path colPath, PrintStream report) {
        handle_book(cmd.getArgs()[2], colPath, report);
    }

    private static void handle_book(String bookName, Path colPath, PrintStream report) {
        report.println("  Handling book [" + bookName + "]");
        Path bookPath = colPath.resolve(bookName);


        BookMetadata mm = new BookMetadata();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(bookPath,
             entry -> entry.getFileName().toString().contains("description_"))) {

            for (Path path : ds) {
                addLanguageData(path, mm, report);
            }

        } catch (IOException e) {
            report.printf("[ERROR] Failed to read book. (%s:%s)\n", colPath.getFileName().toAbsolutePath(), bookName);
        }

        Path newDescriptionPath = bookPath.resolve(bookName + ".description.xml");
        writeOutput(mm, newDescriptionPath, report);
    }

    private static void addLanguageData(Path langSpecificPath, BookMetadata mm, PrintStream report) {
        List<String> errors = new ArrayList<>();

        String file = langSpecificPath.getFileName().toString();
        String filename = file.substring(0, file.lastIndexOf('.'));
        if (filename.indexOf('_') < 0) {
            report.printf("   Bad file [%s]\n", file);
            return;
        }
        String lang = filename.substring(filename.indexOf('_') + 1);

        try (InputStream in = Files.newInputStream(langSpecificPath)) {
            DeprecatedBookMetadata metadata = deprecatedBookMetadataSerializer.read(in, errors);

            if (!errors.isEmpty()) {
                return;
            }

            mm.setDimensionUnits(metadata.getDimensionUnits());
            mm.setNumberOfIllustrations(metadata.getNumberOfIllustrations());
            mm.setNumberOfPages(metadata.getNumberOfPages());
            mm.setWidth(metadata.getWidth());
            mm.setHeight(metadata.getHeight());
            mm.setYearStart(metadata.getYearStart());
            mm.setYearEnd(metadata.getYearEnd());

            List<BookText> texts = new ArrayList<>(Arrays.asList(metadata.getTexts()));
            for (BookText text : texts) {
                text.setLanguage("fr");
            }
            mm.setBookTexts(texts);

            BiblioData bib = new BiblioData();
            bib.setCommonName(metadata.getCommonName());
            bib.setCurrentLocation(metadata.getCurrentLocation());
            bib.setRepository(metadata.getRepository());
            bib.setShelfmark(metadata.getShelfmark());
            bib.setOrigin(metadata.getOrigin());
            bib.setType(metadata.getType());
            bib.setTitle(metadata.getTitle());
            bib.setDateLabel(metadata.getDate());
            bib.setMaterial(metadata.getMaterial());

            mm.getBiblioDataMap().put(lang, bib);

        } catch (IOException e) {
            report.printf("[ERROR] Failed to read book description. (%s)\n",
                    langSpecificPath.getFileName().toAbsolutePath());
            e.printStackTrace(report);
        }
    }

    private static void writeOutput(BookMetadata mm, Path outputPath, PrintStream report) {
        if (Files.exists(outputPath)) {
            report.printf("[WARNING] Skipped existing [%s]", outputPath.toAbsolutePath());
        } else {
            try (OutputStream out = Files.newOutputStream(outputPath)) {
                bookMetadataSerializer.write(mm, out);
            } catch (IOException e) {
                report.printf("[ERROR] Failed to write output file. [%s]", outputPath.toAbsolutePath());
            }
        }
    }

}
