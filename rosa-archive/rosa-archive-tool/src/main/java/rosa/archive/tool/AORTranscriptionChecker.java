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
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TableCell;
import rosa.archive.model.aor.TextEl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private Set<String> annotationIds;


    @Inject
    public AORTranscriptionChecker(AORAnnotatedPageSerializer serializer, FileMapSerializer fileMapSerializer)
            throws IOException {
        this.serializer = serializer;
        this.fileMapSerializer = fileMapSerializer;
        this.annotationIds = new HashSet<>();
    }

    /**
     * Running this will check AoR transcription XML files.
     *
     * The files are first parsed, revealing any XML syntax errors. The files are
     * not validated against the schema here, however. After parsing the files,
     * some content is checked for logical consistency.
     *
     * This tool will perform its operations on the given <em>path</em>. If it is
     * specified to be a book, the book check will be run on the path, otherwise,
     * the book check will be run on all subdirectories of the given path. If no
     * <em>spreadsheetDirectory</em> is specified, it is assumed that all
     * spreadsheets are held in the <em>path</em>. Any errors or inconsistencies
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
     * Within each transcription file, there will exist a list of elements,
     * that may contain references to books, people, and/or locations. These
     * references are held in an external spreadsheet along with any alternate spelling
     * that has been used in the corpus. The references in the marginalia tags
     * MUST be a recognized "standard" name, which serves as an index in the
     * spreadsheet. This tool will check all references against the spreadsheets.
     *
     * Elements can also contain references to other transcribed pages in the
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

        loadDirectoryMap();

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
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(path),
                entry -> Files.isDirectory(entry)
        )) {
            // First, build a list of all annotation IDs from the books in the collection
            for (Path p : ds) {
                addBookIds(p, report);
            }
        } catch (IOException e) {
            report.println("Failed to read path. [" + path + "]\n");
            e.printStackTrace(report);
            return;
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(path),
                entry -> Files.isDirectory(entry)
        )) {
            for (Path p : ds) {
                doBook(p, report);
            }
        } catch (IOException e) {
            report.println("Failed to read path. [" + path + "]\n");
            e.printStackTrace(report);
        }

    }

    private void doBook(String bookPath, PrintStream report) {
        addBookIds(Paths.get(bookPath), report);
        doBook(Paths.get(bookPath), report);
    }

    /**
     * Run data consistency check on AoR transcriptions in this path.
     *
     * @param bookPath path of book
     * @param report PrintStream to record output
     */
    private void doBook(Path bookPath, PrintStream report) {
        report.println("Reading transcriptions for book. [" + bookPath + "]");

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(bookPath,
                entry -> entry.getFileName().toString().endsWith(".xml")
        )) {

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
                        checkInternalRefs(aorPage, report);
                    }

                } catch (IOException e) {
                    report.println("  !! Failed to read file. [" + xmlPath + "]");
                    report.println("\t> " + e.getMessage());
                }
            }

        } catch (IOException e) {
            report.println("Failed to find transcriptions in path. [" + bookPath + "]");
            report.println("\t> " + e.getMessage());
        }
    }

    /**
     * Add all annotation IDs to the internal list so that we can check to see if referenced
     * IDs exist.
     *
     * @param bookPath path of book
     * @param report PrintStream to record output
     */
    private void addBookIds(Path bookPath, PrintStream report) {
        List<String> errors = new ArrayList<>();
        String book = bookPath.getFileName().toString();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(
                bookPath,
                entry -> entry.getFileName().toString().endsWith(".xml")
        )) {
            for (Path xmlP : ds) {
                String transcriptionName = xmlP.getFileName().toString();

                try (InputStream xmlIn = Files.newInputStream(xmlP)) {
                    errors.clear();
                    AnnotatedPage aorPage = serializer.read(xmlIn, errors);
                    aorPage.setId(transcriptionName);

                    if (!errors.isEmpty()) {
                        continue;
                    }

                    // Only add IDs present in original transcriptions, ignore auto-generated IDs
                    aorPage.getAnnotations().stream()
                            .filter(a -> !a.isGeneratedId())
                            .map(Annotation::getId)
                            .forEach(id -> {
                                if (annotationIds.contains(id)) {
                                    report.println("  Duplicate ID found on page [" + book + ":" + transcriptionName +
                                            "] (" + id + ")");
                                } else {
                                    annotationIds.add(id);
                                }
                            });
                } catch (IOException e) {
                    continue;
                }
            }
        } catch (IOException e) {
            report.println("Failed to get transcription IDs present in book (" + bookPath.getFileName().toString() + ")");
        }
    }

    private void checkAgainstSpreadsheets(AnnotatedPage annotatedPage, PrintStream report) {
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
        String prefix = "Page [" + annotatedPage.getPage() + "]";

        for (Marginalia marg : annotatedPage.getMarginalia()) {
            for (MarginaliaLanguage ml : marg.getLanguages()) {
                for (Position pos : ml.getPositions()) {
                    pos.getBooks().forEach(book -> checkString(book, booksList, prefix + " book - ", report));
                    pos.getPeople().forEach(person -> checkString(person, peopleList, prefix + " person - ", report));
                    pos.getLocations().forEach(loc -> checkString(loc, locationsList, prefix + " location - ", report));
                }
            }
        }

        for (Drawing drawing : annotatedPage.getDrawings()) {
            drawing.getBooks().forEach(book -> checkString(book, booksList, prefix + " book - ", report));
            drawing.getPeople().forEach(p -> checkString(p, peopleList, prefix + " person - ", report));
            drawing.getLocations().forEach(l -> checkString(l, locationsList, prefix + " location - ", report));
        }

        for (Graph graph : annotatedPage.getGraphs()) {
            for (GraphText gt : graph.getGraphTexts()) {
                gt.getBooks().forEach(book -> checkString(book, booksList, prefix + " book - ", report));
                gt.getPeople().forEach(p -> checkString(p, peopleList, prefix + " person - ", report));
                gt.getLocations().forEach(l -> checkString(l, locationsList, prefix + " location - ", report));
            }
        }

        for (Table table : annotatedPage.getTables()) {
            table.getBooks().forEach(book -> checkString(book, booksList, prefix + " book - ", report));
            table.getPeople().forEach(p -> checkString(p, peopleList, prefix + " person - ", report));
            table.getLocations().forEach(l -> checkString(l, locationsList, prefix + " location - ", report));
        }
    }

    private void checkString(String toCheck, List<MultiValue> reference, String reportPrefix, PrintStream report) {
        if (reference == null || reference.isEmpty()) {
            return;
        } else if (isEmpty(toCheck)) {
            report.println("  " + reportPrefix + " Invalid: empty value");
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
            try (Workbook wb = new XSSFWorkbook(in)) {
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
            }
        } catch (IOException e) {
            report.println("## [ERROR] Failed to parse spreadsheet. (" + filePath.getFileName().toString() + ")");
            e.printStackTrace(report);
        }

        return list;
    }

    private void checkInternalRefs(AnnotatedPage aPage, PrintStream report) {
        // First check all annotations for 'internal_ref' attribute
        aPage.getAnnotations()
                .forEach(annotation -> checkTarget(annotation.getInternalRef(), aPage.getId(), annotation.getId(), report));

        for (Marginalia marg : aPage.getMarginalia()) {
            String transcription = getTranscription(marg);
            String id = aPage.getId() + ":" + marg.getId();

            checkTarget(marg.getContinuesFrom(), aPage.getId(), marg.getId(), report);
            checkTarget(marg.getContinuesTo(), aPage.getId(), marg.getId(), report);
            for (MarginaliaLanguage lang : marg.getLanguages()) {
                for (Position pos : lang.getPositions()) {
                    for (InternalReference ref : pos.getInternalRefs()) {
                        checkRefText(transcription, ref, id, report);
                        for (ReferenceTarget target : ref.getTargets()) {
                            checkTarget(target, aPage.getId(), marg.getId(), report);
                        }
                    }
                }
            }
        }

        for (Drawing d : aPage.getDrawings()) {
            String id = aPage.getId() + ":" + d.getId();
            String transcription = String.join(" ", d.getTexts().stream().map(TextEl::getText).collect(Collectors.toList()));
            for (InternalReference ref : d.getInternalRefs()) {
                checkRefText(transcription, ref, id, report);
                for (ReferenceTarget target : ref.getTargets()) {
                    checkTarget(target, aPage.getId(), d.getId(), report);
                }
            }
        }

        for (Graph g : aPage.getGraphs()) {
            String id = aPage.getId() + ":" + g.getId();
            String transcription = String.join(" ", g.getNodes().stream().map(GraphNode::getContent).collect(Collectors.toList()));

            checkTarget(g.getContinuesFrom(), aPage.getId(), g.getId(), report);
            checkTarget(g.getContinuesTo(), aPage.getId(), g.getId(), report);
            for (InternalReference ref : g.getInternalRefs()) {
                checkRefText(transcription, ref, id, report);
                for (ReferenceTarget target : ref.getTargets()) {
                    checkTarget(target, aPage.getId(), g.getId(), report);
                }
            }
        }

        for (Table table : aPage.getTables()) {
            String id = aPage.getId() + ":" + table.getId();
            String transcription = String.join(" ", table.getCells().stream().map(TableCell::getContent).collect(Collectors.toList()));

            for (InternalReference ref : table.getInternalRefs()) {
                checkRefText(transcription, ref, id, report);
                for (ReferenceTarget target : ref.getTargets()) {
                    checkTarget(target, aPage.getId(), table.getId(), report);
                }
            }
        }

        aPage.getLinks().forEach(link ->
            link.getAllIds().forEach(id -> {
                if (isEmpty(id)) {
                    report.println("  [" + aPage.getId() + ":" + link.getId() + "] Empty physical link node ID");
                } else if (!annotationIds.contains(id)) {
                    report.println("  [" + aPage.getId() + ":" + link.getId() + "] Physical link node ID not found (" + id + ")");
                }
            })
        );
    }

    private String getTranscription(Marginalia marg) {
        String allText = "";

        for (MarginaliaLanguage lang : marg.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                allText = allText.concat(String.join(" ", pos.getTexts()));
            }
        }

        return allText;
    }

    /**
     * Ensure that an internal reference contains text that actually points to text in an annotations
     * transcription text.
     *
     * @param transcription transcription text to check against
     * @param ref internal reference
     * @param report print stream
     */
    private void checkRefText(String transcription, InternalReference ref, String reportId, PrintStream report) {
        if (ref.getText() != null && !ref.getText().isEmpty()) {
            // If this exists, then chances are that targets actually have text from the target :)
            checkRefText2(transcription, ref.getText(), reportId, report);

        } else {
            // If ref.text is not present, then targets will probably contain text from the source :(
            List<String> moo = ref.getTargets().stream().map(tar -> {
                String result = "";
                if (tar.getTextPrefix() != null && !tar.getTextPrefix().isEmpty()) {
                    result += tar.getTextPrefix();
                }
                result += tar.getText();
                if (tar.getTextSuffix() != null && !tar.getTextSuffix().isEmpty()) {
                    result += tar.getTextSuffix();
                }
                return result;
            }).collect(Collectors.toList());

            for (String refText : moo) {
                checkRefText2(transcription, refText, reportId, report);
            }
        }
    }

    private void checkRefText2(String transcription, String refText, String reportId, PrintStream report) {
        if (transcription == null || transcription.isEmpty()) {
            return;
        }

        if (refText == null || refText.isEmpty()) {
            report.println("   [" + reportId + "] internal reference found with no source text to reference");
        } else if (!transcription.contains(refText)) {
            report.println("   [" + reportId + "] internal reference found with text that does not match its annotation's transcription");
        }
    }

    private void checkTarget(String ref, String transcriptionId, String annotationId, PrintStream report) {
        if (!isEmpty(ref)) {
            checkTarget(new ReferenceTarget(ref, null), transcriptionId, annotationId, report);
        }
    }

    /**
     * Check internal ref targets to make sure their referenced IDs exist in the corpus
     * (unless target points to external entity)
     *
     * @param target internal reference target
     * @param transcriptionId page ID for reporting
     * @param annotationId ID of parent annotation of this reference target for reporting
     * @param report PrintStream to record output
     */
    private void checkTarget(ReferenceTarget target, String transcriptionId, String annotationId, PrintStream report) {
        String targetId = target.getTargetId();
        if (targetId == null) {
            report.println("  Internal Reference [" + transcriptionId + ":" + annotationId +
                    "] no 'ref' ID found.");
            return;
        }
        if (!annotationIds.contains(targetId) && !targetId.startsWith("http")) {
            report.println("  Internal Reference [" + transcriptionId + ":" + annotationId +
                    "] target ID not found in corpus. (" + targetId + ")");
        }
        if (target.getBookId() != null && !target.getBookId().isEmpty()) {
            report.println("  Internal Reference [" + transcriptionId + ":" + annotationId +
                    "] found using deprecated attribute (book_id)");
        }
        if (target.getFilename() != null && !target.getFilename().isEmpty()) {
            report.println("  Internal Reference [" + transcriptionId + ":" + annotationId +
                    "] found using deprecated attribute (filename)");
        }
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
