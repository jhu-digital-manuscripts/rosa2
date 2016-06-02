package rosa.archive.tool;

import com.google.inject.Inject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import rosa.archive.core.serialize.AORAnnotatedPageSerializer;
import rosa.archive.core.serialize.FileMapSerializer;
import rosa.archive.model.FileMap;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;

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
import java.util.Map.Entry;
import java.util.Set;

public class AORTranscriptionChecker {

    private class MultiValue {
        final String primary;
        final Set<String> alternates;
        final Set<String> lowerCase;

        public MultiValue(String primary) {
            this.primary = primary;
            this.alternates = new HashSet<>();
            this.lowerCase = new HashSet<>();
        }

        void addAlternate(String alt) {
            alternates.add(alt);
            lowerCase.add(alt.toLowerCase());
        }

        boolean hasValue(String val) {
            return primary.equals(val) || alternates.contains(val);
        }

        boolean hasValueIgnoreCase(String val) {
            return primary.toLowerCase().equals(val.toLowerCase())
                    || lowerCase.contains(val.toLowerCase());
        }

        /**
         * @param val value to check
         * @return is val equal to primary, ignoring case
         */
        boolean isValueIgnoreCase(String val) {
            return primary.toLowerCase().equals(val.toLowerCase());
        }
    }

    private final AORAnnotatedPageSerializer serializer;
    private final FileMapSerializer fileMapSerializer;

    private Path peoplePath;
    private Path booksPath;
    private Path locationsPath;

    private List<MultiValue> peopleList;
    private List<MultiValue> booksList;
    private List<MultiValue> locationsList;

    private FileMap gitArchiveMap;

    @Inject
    public AORTranscriptionChecker(AORAnnotatedPageSerializer serializer, FileMapSerializer fileMapSerializer)
            throws IOException {
        this.serializer = serializer;
        this.fileMapSerializer = fileMapSerializer;
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
     * The marginalia can also contain references to other transcribed pages in the
     * corpus. These "internal references" must follow certain rules in order to
     * be useful. The targets of these references must cite a valid transcription
     * file that ends with a '.xml' file extension and exists within the specified
     * book. The book ID must match the name of a book directory.
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

        gitArchiveMap = loadDirectoryMap();

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
                doBook(p.toString(), path, report);
            }

        } catch (IOException e) {
            report.println("Failed to read path. [" + path + "]\n");
            e.printStackTrace(report);
        }

    }

    private void doBook(String bookPath, PrintStream report) {
        Path book = Paths.get(bookPath);
        doBook(bookPath, book.getParent().toString(), report);
    }

    /**
     * Run data consistency check on AoR transcriptions in this path.
     *
     * @param bookPath path of book
     * @param report PrintStream to record output
     */
    private void doBook(String bookPath, String collectionPath, PrintStream report) {
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
                    aorPage.setId(transcriptionName);

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

                        checkInternalRefs(aorPage, collectionPath, report);
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
                        if (isEmpty(book)) {
                            report.println("  [" + annotatedPage.getId() + "] Found invalid book: Empty title");
                        } else {
                            checkString(book, booksList, prefix + " books - ", report);
                        }
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
        boolean changeCapitalization = false;
        boolean isAlternate = false;
        boolean isAlternateChangeCapitalization = false;

        int alternateRow = -1;
        String spreadsheetId = null;
        for (int i = 0; i < reference.size(); i++) {
            MultiValue val = reference.get(i);

            // Find the 'toCheck' string with different conditions
            if (toCheck.equals(val.primary)) {
                // Check ID exact
                good = true;
                break;
            } else if (val.isValueIgnoreCase(toCheck)) {
                // Check ID ignore case
                changeCapitalization = true;
                spreadsheetId = val.primary;
                break;
            } else if (val.hasValue(toCheck)) {
                // Check alternate spellings exact
                isAlternate = true;
                alternateRow = i;
                spreadsheetId = val.primary;
                break;
            } else if (val.hasValueIgnoreCase(toCheck)) {
                // Check alternate spellings ignore case
                isAlternateChangeCapitalization = true;
                alternateRow = i;
                spreadsheetId = val.primary;
                break;
            }
        }

        // Print relevant error messages
        if (!good) {
            report.print("  " + reportPrefix);
            report.print(" [" + toCheck + "] was not found in spreadsheet. ");

            if (changeCapitalization) {
                report.print(" was found with different capitalization. (spreadsheetID=" + spreadsheetId + ")");
            }
            if (isAlternate) {
                report.print(" [" + toCheck + "] was found as an alternate spelling on row ("
                        + alternateRow + ") under ID (spreadsheetID=" + spreadsheetId + ")");
            }
            if (isAlternateChangeCapitalization) {
                report.print(" [" + toCheck + "] was found as an alternate spelling on row ("
                        + alternateRow + ") with different capitalization under ID (spreadsheetID="
                        + spreadsheetId + ")");
            }

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

    private void checkInternalRefs(AnnotatedPage aPage, String collection, PrintStream report) {
        Path colPath = Paths.get(collection);

        for (Marginalia marg : aPage.getMarginalia()) {
            for (MarginaliaLanguage lang : marg.getLanguages()) {
                for (Position pos : lang.getPositions()) {
                    for (InternalReference ref : pos.getInternalRefs()) {
                        for (ReferenceTarget target : ref.getTargets()) {
                            String bookId = target.getBookId();
                            String filename = target.getFilename();

//                            System.out.println("  #### Internal reference target found: " + target.toString());

                            // Check Book ID
                            if (isEmpty(bookId)) {
                                report.println("  [" + aPage.getId() + "]Internal reference book_id is blank. "
                                        + target.toString());
                                continue;
                            }
                            Path desiredPath = colPath.resolve(bookId);
                            if (!Files.exists(desiredPath) || !Files.isDirectory(desiredPath)) {
                                // If names don't exist, first apply the git-archive mapping and recheck
                                String mappedPath = tryFileMap(bookId);
                                if (mappedPath == null || mappedPath.isEmpty()) {
                                    report.println("  [" + aPage.getId() + "] Cannot check internal references. " +
                                            "book_id is invalid or does not exist. (" + bookId + ")");
                                    return;
                                }
                                desiredPath = colPath.resolve(tryFileMap(bookId));
                                if (!Files.exists(desiredPath) || !Files.isDirectory(desiredPath)) {
                                    report.println("  [" + aPage.getId() + "] Internal reference target " +
                                            "book_id is invalid or does not exist (" + bookId + ")");
                                    // Might as well continue, since the file cannot be searched for...
                                    continue;
                                }
                                // TODO add warning here
                            }

                            // Check filename
                            if (isEmpty(filename)) {
                                report.println("  [" + aPage.getId() + "] Internal reference 'filename' is " +
                                        "blank. " + target.toString());
                                return;
                            }
                            if (!filename.endsWith(".xml")) {
                                // Must be an XML target
                                report.println("  [" + aPage.getId() + "] Internal reference filename is " +
                                        "invalid: must target an XML transcription. (filename=\"" + filename + "\")");
                            } else {
                                Path desiredFile = desiredPath.resolve(filename);
                                if (!Files.exists(desiredFile)) {
                                    // File must exist...
                                    report.println("  [" + aPage.getId() + "] Internal reference filename is " +
                                            "invalid: file does not exist. (" + bookId + "/" + filename + ")");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The data might have it that the BOOK_ID is actually the name of the book in
     * our archive, as opposed to the name of the book in the AoR Git repo. Attempt
     * to reconcile these names.
     *
     * @param toTransform String to transform using mapping
     * @return transformed directory name
     */
    private String tryFileMap(String toTransform) {
        if (gitArchiveMap == null) {
            return null;
        }

        for (Entry<String, String> entry: gitArchiveMap.getMap().entrySet()) {
            if (entry.getValue().equals(toTransform)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private FileMap loadDirectoryMap() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("dir-map.csv")) {
            List<String> errors = new ArrayList<>();

            FileMap map = fileMapSerializer.read(in, errors);

            if (!errors.isEmpty()) {
                System.err.println("Error reading file map relating git directories and archive directories:");
                for (String err : errors) {
                    System.out.println("  " + err);
                }
            }

            return map;
        } catch (IOException e) {
            System.err.println("Failed to load file map relating git directories and archive directories.");
            return null;
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
