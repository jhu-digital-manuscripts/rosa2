package rosa.archive.tool;

import com.google.inject.Inject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AORTranscriptionChecker {

    private class MultiValue {
        final String primary;
        final Set<String> alternates;

        public MultiValue(String primary) {
            this.primary = primary;
            this.alternates = new HashSet<>();
        }

        void addAlternate(String alt) {
            alternates.add(alt);
        }

        boolean hasValue(String val) {
            return primary.equals(val) || alternates.contains(val);
        }
    }

    private final AORAnnotatedPageSerializer serializer;

    private Path peoplePath;
    private Path booksPath;
    private Path locationsPath;

    private List<MultiValue> peopleList;
    private List<MultiValue> booksList;
    private List<MultiValue> locationsList;

    @Inject
    public AORTranscriptionChecker(AORAnnotatedPageSerializer serializer)
            throws IOException {
        this.serializer = serializer;
    }

    /**
     * Running this will check AoR transcription XML files.
     *
     * The files are first parsed, revealing any XML syntax errors. The files are
     * not validated against the schema here, however. After parsing the files,
     * some content is checked for logical consistency.
     *
     * This tool will perform its operations on the given {@param path}. If it is
     * specified to be a book, the book check will be run on the path, otherwise,
     * the book check will be run on all subdirectories of the given path. If no
     * {@param spreadsheetDirectory} is specified, it is assumed that all
     * spreadsheets are held in the {@param path}. Any errors or inconsistencies
     * found with the data will be output to the given PrintStream.
     *
     * In the &lt;page&gt; element, there is a reference to an image file name.
     * By convention, this image file should match the name of the transcription
     * file. EX: image 00000001.xml should map to 00000001.tif. There is the exception
     * of after the data has been imported into the archive. In the archive, all
     * names are mapped to standard names, but the image file name should still be
     * related to the transcription name. Transcription names will have an extra
     * part within to identify it as an AoR transcription, as opposed to other
     * type of transcription. EX: 00000001.xml will change to something like
     * PrincetonK6233.aor.001r.xml. Likewise, the image 00000001.tif will change to
     * PrincetonK6233.001r.tif. This tool will check to see if the associated
     * image is the same page as the transcription.
     *
     * Within each transcription file, there will exist a list of Marginalia,
     * each of which may contain references to books, people, and/or locations. These
     * references are held in an external spreadsheet along with any alternate spelling
     * that has been used in the corpus. The references in the marginalia tags
     * MUST be a recognized "standard" name, which serves as an index in the
     * spreadsheet. This tool will check all references against the spreadsheets.
     *
     * @param path path to check
     * @param isBook is this path a book?
     * @param spreadsheetDirectory directory holding reference spreadsheets
     *                             (Books.xlsx, People.xlsx, Locations.xlsx)
     * @param report PrintStream to leave messages
     */
    public void run(String path, boolean isBook, String spreadsheetDirectory, PrintStream report) {
        if (!isEmpty(spreadsheetDirectory)) {
            peoplePath = Paths.get(spreadsheetDirectory).resolve("People.xlsx");
            booksPath = Paths.get(spreadsheetDirectory).resolve("Books.xlsx");
            locationsPath = Paths.get(spreadsheetDirectory).resolve("Locations.xlsx");
        } else {
            peoplePath = Paths.get(path).resolve("People.xlsx");
            booksPath = Paths.get(path).resolve("Books.xlsx");
            locationsPath = Paths.get(path).resolve("Locations.xlsx");
        }

        if (isBook) {
            doBook(path, report);
        } else {
            doCollection(path, report);
        }
    }

    /**
     * Run the book check on all subdirectories in the given path.
     *
     * @param path path of collection
     * @param report PrintStream to record output
     */
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

    /**
     * Run data consistency check on AoR transcriptions in this path.
     *
     * @param bookPath path of book
     * @param report PrintStream to record output
     */
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

                        checkAgainstSpreadsheets(aorPage, report);
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

    private void checkAgainstSpreadsheets(AnnotatedPage annotatedPage, PrintStream report) throws IOException {
        if (annotatedPage == null) {
            return;
        }

        // -----------------------------------------------------------------------
        // Parse spreadsheets, will occur only the first time the method is called
        if (peopleList == null) {
            peopleList = readSpreadsheet(peoplePath, report);
        }
        if (booksList == null) {
            booksList = readSpreadsheet(booksPath, report);
        }
        if (locationsList == null) {
            locationsList = readSpreadsheet(locationsPath, report);
        }
        // -----------------------------------------------------------------------
        // Check annotated page against spreadsheets
        for (Marginalia marg : annotatedPage.getMarginalia()) {
            for (MarginaliaLanguage ml : marg.getLanguages()) {
                for (Position pos : ml.getPositions()) {

                    String prefix = "Page [" + annotatedPage.getPage() + "]";
                    for (String book : pos.getBooks()) {
                        checkString(book, booksList, prefix + " books - ", report);
                    }

                    for (String person : pos.getPeople()) {
                        checkString(person, peopleList, prefix + " person - ", report);
                    }

                    for (String loc : pos.getLocations()) {
                        checkString(loc, locationsList, prefix + " location - ", report);
                    }
                }
            }
        }
    }

    private void checkString(String toCheck, List<MultiValue> reference, String reportPrefix, PrintStream report) {
        if (isEmpty(toCheck) || reference == null || reference.isEmpty()) {
            return;
        }

        boolean good = false;
        boolean isAlternate = false;

        int alternateRow = -1;
        for (int i = 0; i < reference.size(); i++) {
            MultiValue val = reference.get(i);

            if (toCheck.equals(val.primary)) {
                good = true;
                break;
            } else if (val.hasValue(toCheck)) {
                isAlternate = true;
                alternateRow = i;
            }
        }

        if (!good) {
            report.print("  " + reportPrefix);
            report.print(" [" + toCheck + "] was not found in spreadsheet. ");
        }
        if (isAlternate) {
            report.print("  " + reportPrefix);
            report.print(" [" + toCheck + "] was found as an alternate spelling on row (" + alternateRow + ")");
        }
        if (!good || isAlternate) {
            report.println();
        }
    }

    private List<MultiValue> readSpreadsheet(Path filePath, PrintStream report){
        List<MultiValue> list = new ArrayList<>();

        try (InputStream in = Files.newInputStream(filePath)) {

            Workbook wb = new XSSFWorkbook(in);
            Sheet sheet = wb.getSheetAt(0);

            for (Row row : sheet) {
                MultiValue rowVal = null;
                boolean isFirst = true;

                for (Cell cell : row) {
                    if (isFirst) {
                        rowVal = new MultiValue(cell.getStringCellValue());
                        isFirst = false;
                    } else {
                        rowVal.addAlternate(cell.getStringCellValue());
                    }
                }

                list.add(rowVal);
            }

        } catch (IOException e) {
            report.println("## [ERROR] Failed to parse spreadsheet. (" + filePath.getFileName().toString() + ")");
            e.printStackTrace(report);
        }

        return list;
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
