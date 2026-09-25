package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BiblioData;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.HTMLAnnotations;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.ObjectRef;
import rosa.archive.model.Permission;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.SHA1Checksum;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.AnnotationLink;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.PhysicalLink;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TableCell;
import rosa.archive.model.aor.TableHeader;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

/**
 * Collection of reader methods for parsing archive data files (CSV, XML, TXT, HTML)
 * into model objects. Uses direct static methods for simplicity and clarity.
 */
public final class ArchiveReaders {

    private static final ArchiveNameParser PARSER = new ArchiveNameParser();

    private ArchiveReaders() {}

    /**
     * Reads an image list from a CSV file (comma-separated: id,width,height,label).
     *
     * @param path   the path to the images CSV file
     * @param errors list to collect error messages
     * @return the parsed image list, or null if the file doesn't exist
     * @throws IOException if reading fails
     */
    public static ImageList readImageList(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        ImageList images = new ImageList();
        List<BookImage> imgList = images.getImages();
        List<String> rows = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (String row : rows) {
            if (row.isBlank()) {
                continue;
            }
            BookImage image = buildBookImage(CSV.parse(row), errors);
            if (image != null) {
                imgList.add(image);
            }
        }

        return images;
    }

    /**
     * Reads character names from a CSV file.
     *
     * @param path   the path to the CSV file
     * @param errors list to collect error messages
     * @return the parsed character names, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static CharacterNames readCharacterNames(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        CharacterNames names = new CharacterNames();

        try (var reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            String[][] table = CSV.parseTable(reader);
            CSV.normalizeWhiteSpaceAndCharacters(table);

            if (table.length == 0) {
                return names;
            }

            String[] headers = table[0];
            for (int i = 1; i < table.length; i++) {
                String[] row = table[i];
                if (row.length == 0 || row[0].isBlank()) {
                    continue;
                }

                CharacterName name = new CharacterName();
                name.setId(row[0]);

                for (int j = 1; j < row.length && j < headers.length; j++) {
                    name.addName(row[j], headers[j]);
                }

                if (names.getAllCharacterIds().contains(name.getId())) {
                    errors.add("ID [" + name.getId() + "] already exists.");
                }
                names.addCharacterName(name);
            }
        }

        return names;
    }

    /**
     * Reads illustration titles from a CSV file (id,title columns).
     *
     * @param path   the path to the CSV file
     * @param errors list to collect error messages
     * @return the parsed illustration titles, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static IllustrationTitles readIllustrationTitles(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        IllustrationTitles titles = new IllustrationTitles();

        try (var reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
            String[][] table = CSV.parseTable(reader);
            CSV.normalizeWhiteSpaceAndCharacters(table);

            Map<String, String> dataMap = new HashMap<>();
            for (int row = 1; row < table.length; row++) {
                if (table[row].length < 2) {
                    continue;
                }
                String id = table[row][0];
                String title = table[row][1];

                if (dataMap.containsKey(id)) {
                    errors.add("ID [" + id + "] already exists.");
                }
                dataMap.put(id, title);
            }
            titles.setData(dataMap);
        }

        return titles;
    }

    /**
     * Reads narrative sections from a CSV file.
     *
     * @param path   the path to the CSV file
     * @param errors list to collect error messages
     * @return the parsed narrative sections, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static NarrativeSections readNarrativeSections(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        NarrativeSections sections = new NarrativeSections();
        List<NarrativeScene> scenes = sections.asScenes();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (int i = 1; i < lines.size(); i++) {
            String[] row = CSV.parse(lines.get(i));

            if (row.length < 4) {
                errors.add("Malformed row in narrative sections [" + i + "]: " + Arrays.toString(row));
                continue;
            }

            NarrativeScene scene = createScene(row, errors);
            if (scene != null) {
                scenes.add(scene);
            }
        }

        return sections;
    }

    /**
     * Reads a SHA-1 checksum file.
     *
     * @param path   the path to the checksum file
     * @param errors list to collect error messages
     * @return the parsed checksums, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static SHA1Checksum readSHA1Checksum(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        SHA1Checksum info = new SHA1Checksum();
        Map<String, String> checksums = info.checksums();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (String line : lines) {
            String[] parts = line.split("\\s+");
            if (parts.length != 2) {
                errors.add("Malformed line in checksum data: [" + line + "]");
                continue;
            }
            checksums.put(parts[1], parts[0]);
        }

        return info;
    }

    /**
     * Reads a reference sheet (people.csv, locations.csv) from a CSV file.
     *
     * @param path   the path to the CSV file
     * @param errors list to collect error messages
     * @return the parsed reference sheet, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static ReferenceSheet readReferenceSheet(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        ReferenceSheet reference = new ReferenceSheet();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (String line : lines) {
            String[] row = CSV.parse(line);
            if (row.length == 0) {
                continue;
            }
            if (row.length == 1) {
                reference.addValues(row[0]);
            } else {
                reference.addValues(row[0], row);
            }
        }

        return reference;
    }

    /**
     * Reads a book reference sheet (books.csv) from a CSV file.
     *
     * @param path   the path to the CSV file
     * @param errors list to collect error messages
     * @return the parsed book reference sheet, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static BookReferenceSheet readBookReferenceSheet(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        BookReferenceSheet reference = new BookReferenceSheet();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        for (String line : lines) {
            String[] row = CSV.parse(line);
            if (row.length == 0 || row[0].equalsIgnoreCase("Standard title")) {
                continue;
            }
            reference.addValues(row[0], row);
        }

        return reference;
    }

    /**
     * Reads a permission HTML file.
     *
     * @param path the path to the HTML file
     * @return the parsed permission, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static Permission readPermission(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        var content = new StringBuilder();
        for (String line : lines) {
            content.append(line);
        }

        Permission permission = new Permission();
        permission.setPermission(content.toString());
        return permission;
    }

    /**
     * Reads a transcription XML file as raw content.
     *
     * @param path the path to the XML file
     * @return the transcription, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static Transcription readTranscription(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        var content = new StringBuilder();
        for (String line : lines) {
            content.append(line);
        }

        Transcription transcription = new Transcription();
        transcription.setXML(content.toString());
        return transcription;
    }

    /**
     * Reads book metadata from an XML file.
     *
     * @param path   the path to the metadata XML file
     * @param errors list to collect error messages
     * @return the parsed metadata, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static BookMetadata readBookMetadata(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        try (InputStream is = Files.newInputStream(path)) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);
            return buildMetadata(doc);
        } catch (ParserConfigurationException | SAXException e) {
            errors.add("Failed to parse metadata XML: " + path + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads an AoR annotated page from an XML file.
     *
     * @param path   the path to the AoR XML file
     * @param errors list to collect error messages
     * @return the parsed annotated page, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static AnnotatedPage readAnnotatedPage(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        try (InputStream is = Files.newInputStream(path)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            return buildAnnotatedPage(doc, errors);
        } catch (ParserConfigurationException | SAXException e) {
            errors.add("Failed to parse AoR XML: " + path + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Reads an annotation location map from a CSV file.
     *
     * @param path the path to the id_locations.csv file
     * @return a map of annotation IDs to locations, or empty map if file doesn't exist
     * @throws IOException if reading fails
     */
    public static Map<String, rosa.archive.model.aor.AorLocation> readAnnotationLocationMap(Path path) throws IOException {
        Map<String, rosa.archive.model.aor.AorLocation> result = new HashMap<>();
        if (!Files.exists(path)) {
            return result;
        }

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length == 0) {
                continue;
            }
            String id = parts[0];
            rosa.archive.model.aor.AorLocation loc = new rosa.archive.model.aor.AorLocation(
                    parts.length > 1 ? parts[1] : null,
                    parts.length > 2 ? parts[2] : null,
                    parts.length > 3 ? parts[3] : null,
                    parts.length > 4 ? parts[4] : null
            );
            result.put(id, loc);
        }

        return result;
    }

    /**
     * Reads HTML annotations from a JSON-LD array file.
     * Since we don't have the org.json dependency, we parse the JSON manually
     * using Jackson (already a dependency).
     *
     * @param path   the path to the JSON-LD file
     * @param errors list to collect error messages
     * @return the parsed HTML annotations, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static HTMLAnnotations readHTMLAnnotations(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        HTMLAnnotations result = new HTMLAnnotations();

        // Parse using Jackson since it's already a dependency
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode array = mapper.readTree(Files.newInputStream(path));

        if (!array.isArray()) {
            errors.add("HTML annotations file is not a JSON array: " + path);
            return result;
        }

        for (com.fasterxml.jackson.databind.JsonNode obj : array) {
            String target = obj.has("target") ? obj.get("target").asText() : null;
            com.fasterxml.jackson.databind.JsonNode bodyObj = obj.get("body");

            if (target == null || bodyObj == null) {
                continue;
            }

            String bodyValue = bodyObj.has("value") ? bodyObj.get("value").asText() : null;
            if (bodyValue == null) {
                continue;
            }

            // Target format: "rosa:ARCHIVE_ID"
            if (target.startsWith("rosa:")) {
                String archiveId = target.substring(5);
                result.setAnnotation(archiveId, bodyValue);
            }
        }

        return result;
    }

    /**
     * Reads illustration tagging from a CSV file.
     * CSV columns: id, Folio #, Illustration title, Textual elements, Initials,
     * Characters, Costume, Objects, Landscape, Architecture, Other
     *
     * @param path   the path to the .imagetag.csv file
     * @param errors list to collect error messages
     * @return the parsed illustration tagging, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static IllustrationTagging readIllustrationTagging(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        IllustrationTagging tagging = new IllustrationTagging();
        tagging.setId(path.getFileName().toString());

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        if (lines.isEmpty()) {
            return tagging;
        }

        // Parse header to determine column positions (some files have an "Initials" column, some don't)
        String[] header = CSV.parse(lines.get(0));
        int colId = -1, colPage = -1, colTitles = -1, colTextual = -1, colInitials = -1;
        int colCharacters = -1, colCostume = -1, colObjects = -1;
        int colLandscape = -1, colArchitecture = -1, colOther = -1;

        for (int h = 0; h < header.length; h++) {
            String name = header[h].trim().toLowerCase();
            switch (name) {
                case "id" -> colId = h;
                case "folio", "folio #" -> colPage = h;
                case "illustration title" -> colTitles = h;
                case "textual elements" -> colTextual = h;
                case "initials" -> colInitials = h;
                case "characters" -> colCharacters = h;
                case "costume" -> colCostume = h;
                case "objects" -> colObjects = h;
                case "landscape" -> colLandscape = h;
                case "architecture" -> colArchitecture = h;
                case "other" -> colOther = h;
                default -> { /* ignore unknown columns */ }
            }
        }

        // Skip header row (index 0)
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String[] cols = CSV.parse(line);

            Illustration illustration = new Illustration();
            illustration.setId(colId >= 0 && cols.length > colId ? cols[colId] : "");
            illustration.setPage(colPage >= 0 && cols.length > colPage ? cols[colPage] : "");

            // Titles - split by semicolons or commas into array of IDs
            if (colTitles >= 0 && cols.length > colTitles && !cols[colTitles].isBlank()) {
                String[] titles = cols[colTitles].split("[;,]");
                for (int t = 0; t < titles.length; t++) {
                    titles[t] = titles[t].trim();
                }
                illustration.setTitles(titles);
            } else {
                illustration.setTitles(new String[0]);
            }

            illustration.setTextualElement(colTextual >= 0 && cols.length > colTextual ? cols[colTextual] : "");
            illustration.setInitials(colInitials >= 0 && cols.length > colInitials ? cols[colInitials] : "");

            // Characters - split by semicolons or commas into array of IDs
            if (colCharacters >= 0 && cols.length > colCharacters && !cols[colCharacters].isBlank()) {
                String[] characters = cols[colCharacters].split("[;,]");
                for (int c = 0; c < characters.length; c++) {
                    characters[c] = characters[c].trim();
                }
                illustration.setCharacters(characters);
            } else {
                illustration.setCharacters(new String[0]);
            }

            illustration.setCostume(colCostume >= 0 && cols.length > colCostume ? cols[colCostume] : "");
            illustration.setObject(colObjects >= 0 && cols.length > colObjects ? cols[colObjects] : "");
            illustration.setLandscape(colLandscape >= 0 && cols.length > colLandscape ? cols[colLandscape] : "");
            illustration.setArchitecture(colArchitecture >= 0 && cols.length > colArchitecture ? cols[colArchitecture] : "");
            illustration.setOther(colOther >= 0 && cols.length > colOther ? cols[colOther] : "");

            tagging.addIllustrationData(illustration);
        }

        return tagging;
    }

    /**
     * Reads a book description from a TEI P5 XML file.
     * 
     * <p>The description file contains a TEI document with notesStmt containing
     * multiple note elements with different @rend attributes (IDENTIFICATION,
     * BASIC INFORMATION, MATERIAL, QUIRES, LAYOUT, SCRIPT, DECORATION, BINDING,
     * HISTORY, TEXT, etc.).
     *
     * @param path   the path to the description XML file
     * @param errors list to collect error messages
     * @return the parsed book description, or null if file doesn't exist
     * @throws IOException if reading fails
     */
    public static BookDescription readBookDescription(Path path, List<String> errors) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }

        try (InputStream is = Files.newInputStream(path)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            return buildBookDescription(doc);
        } catch (ParserConfigurationException | SAXException e) {
            errors.add("Failed to parse description XML: " + path + " - " + e.getMessage());
            return null;
        }
    }

    // ---- Book Description XML parsing ----

    private static BookDescription buildBookDescription(Document doc) {
        BookDescription description = new BookDescription();

        // Find notesStmt element - it contains the note elements with descriptions
        NodeList notesStmtList = doc.getElementsByTagNameNS("http://www.tei-c.org/ns/1.0", "notesStmt");
        if (notesStmtList.getLength() == 0) {
            // Try without namespace
            notesStmtList = doc.getElementsByTagName("notesStmt");
        }

        if (notesStmtList.getLength() == 0) {
            return description;
        }

        Element notesStmt = (Element) notesStmtList.item(0);

        // Get all note elements
        NodeList noteList = notesStmt.getElementsByTagNameNS("http://www.tei-c.org/ns/1.0", "note");
        if (noteList.getLength() == 0) {
            noteList = notesStmt.getElementsByTagName("note");
        }

        for (int i = 0; i < noteList.getLength(); i++) {
            Node node = noteList.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element noteEl = (Element) node;
            
            // Only process direct children of notesStmt
            if (noteEl.getParentNode() != notesStmt) {
                continue;
            }

            String rend = noteEl.getAttribute("rend");
            String textContent = extractTextContent(noteEl);

            if (rend != null && !rend.isEmpty() && !textContent.isBlank()) {
                description.addNote(rend, textContent.trim());
            } else if ((rend == null || rend.isEmpty()) && !textContent.isBlank()) {
                // Notes without rend attribute (like attribution notes)
                description.addNote("OTHER", textContent.trim());
            }
        }

        return description;
    }

    /**
     * Extracts text content from an element, recursively getting text from
     * all child elements and normalizing whitespace.
     */
    private static String extractTextContent(Element element) {
        StringBuilder sb = new StringBuilder();
        extractTextContentRecursive(element, sb);
        // Normalize whitespace: collapse multiple spaces/newlines into single space
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static void extractTextContentRecursive(Node node, StringBuilder sb) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getTextContent());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                extractTextContentRecursive(child, sb);
            }
        }
    }

    // ---- Private helpers ----

    private static BookImage buildBookImage(String[] csvRow, List<String> errors) {
        if (csvRow.length == 0 || csvRow[0].isBlank()) {
            return null;
        }

        BookImage image = new BookImage();
        String id = csvRow[0];
        boolean missing = PARSER.isMissing(id);

        if (missing) {
            id = id.substring(ArchiveNameParser.MISSING_PREFIX.length());
        }

        image.setId(id);
        image.setMissing(missing);
        image.setLocation(PARSER.location(id));
        image.setRole(PARSER.role(id));
        image.setName(PARSER.shortName(id));

        int width = 0;
        int height = 0;

        if (csvRow.length > 1) {
            try {
                width = Integer.parseInt(csvRow[1]);
                height = Integer.parseInt(csvRow[2]);
            } catch (NumberFormatException e) {
                errors.add("Error parsing image dimensions: [" + Arrays.toString(csvRow) + "]");
                return null;
            }
        }
        image.setWidth(width);
        image.setHeight(height);

        if (csvRow.length > 3) {
            String label = csvRow[3];
            if (label.startsWith(ArchiveNameParser.GENERATED_PREFIX)) {
                label = label.substring(ArchiveNameParser.GENERATED_PREFIX.length());
                image.setAutoGeneratedName(true);
            } else {
                image.setAutoGeneratedName(false);
            }
            if (!label.isEmpty()) {
                image.setName(label);
            }
        }

        return image;
    }

    private static NarrativeScene createScene(String[] row, List<String> errors) {
        if (row == null || row.length < 4) {
            return null;
        }

        int[] lines = getRangeValue(row[1], errors);
        int[] lecoy = getRangeValue(row[2], errors);

        if (lecoy == null || lines == null) {
            return null;
        }

        NarrativeScene scene = new NarrativeScene();
        scene.setId(row[0]);
        scene.setDescription(row[3]);
        scene.setCriticalEditionStart(lecoy[0]);
        scene.setCriticalEditionEnd(lecoy[1]);
        scene.setRelLineStart(lines[0]);
        scene.setRelLineEnd(lines[1]);

        return scene;
    }

    private static int[] getRangeValue(String data, List<String> errors) {
        if (data == null || data.isBlank()) {
            return null;
        }

        String[] parts = data.split("-");
        try {
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            return new int[]{start, end};
        } catch (IndexOutOfBoundsException e) {
            errors.add("Malformed range in narrative sections: [" + data + "]");
            return null;
        } catch (NumberFormatException e) {
            if (!data.equals("a-j")) {
                errors.add("Error parsing range in narrative sections: [" + data + "]");
            }
            return null;
        }
    }

    // ---- Book metadata XML parsing ----

    private static BookMetadata buildMetadata(Document doc) {
        Element top = doc.getDocumentElement();
        BookMetadata metadata = new BookMetadata();

        metadata.setWidth(xmlNumber("width", top));
        metadata.setHeight(xmlNumber("height", top));
        metadata.setDimensionUnits(xmlAttribute("units", "dimensions", top));
        metadata.setNumberOfPages(xmlNumber("totalPages", top));
        metadata.setNumberOfIllustrations(xmlNumber("illustrations", top));
        metadata.setYearStart(xmlNumber("startDate", top));
        metadata.setYearEnd(xmlNumber("endDate", top));
        metadata.setBookTexts(getBookTexts(top));
        metadata.setBiblioDataMap(getBibliographies(top));

        NodeList licenseList = top.getElementsByTagName("license");
        if (licenseList.getLength() == 1 && licenseList.item(0).getNodeType() == Node.ELEMENT_NODE) {
            Element licenseElement = (Element) licenseList.item(0);
            String url = xmlText("url", licenseElement);
            metadata.setLicenseUrl(url.isEmpty() ? null : url);
            String logo = xmlText("logo", licenseElement);
            metadata.setLicenseLogo(logo.isEmpty() ? null : logo);
        }

        return metadata;
    }

    private static List<BookText> getBookTexts(Element parent) {
        List<BookText> textList = new ArrayList<>();
        NodeList list = parent.getElementsByTagName("texts");
        if (list.getLength() != 1 || list.item(0).getNodeType() != Node.ELEMENT_NODE) {
            return textList;
        }

        Element texts = (Element) list.item(0);
        for (Element textEl : getChildElements("text", texts)) {
            BookText text = new BookText();
            text.setColumnsPerPage(xmlNumber("columnsPerPage", textEl));
            text.setLeavesPerGathering(xmlNumber("leavesPerGathering", textEl));
            text.setLinesPerColumn(xmlNumber("linesPerColumn", textEl));
            text.setNumberOfIllustrations(xmlNumber("illustrations", textEl));
            text.setNumberOfPages(xmlNumber("pages", textEl));
            text.setFirstPage(xmlAttribute("start", "pages", textEl));
            text.setLastPage(xmlAttribute("end", "pages", textEl));
            text.setTitle(xmlText("title", textEl));
            text.setLanguage(xmlText("language", textEl));
            textList.add(text);
        }

        return textList;
    }

    private static Map<String, BiblioData> getBibliographies(Element parent) {
        Map<String, BiblioData> map = new HashMap<>();
        NodeList list = parent.getElementsByTagName("bibliographies");
        if (list.getLength() != 1 || list.item(0).getNodeType() != Node.ELEMENT_NODE) {
            return map;
        }

        Element bibs = (Element) list.item(0);
        for (Element el : getChildElements("bibliography", bibs)) {
            String lang = el.getAttribute("lang");
            BiblioData data = new BiblioData();

            data.setLanguage(lang);
            data.setTitle(xmlText("title", el));
            data.setCommonName(xmlText("commonName", el));
            data.setCurrentLocation(xmlText("currentLocation", el));
            data.setDateLabel(xmlText("dateLabel", el));
            data.setMaterial(xmlText("material", el));
            data.setOrigin(xmlText("origin", el));
            data.setRepository(xmlText("repository", el));
            data.setShelfmark(xmlText("shelfmark", el));
            data.setType(xmlText("type", el));
            data.setDetails(getTextValues("detail", el).toArray(new String[0]));
            data.setAuthors(getObjectRefs("author", el));
            data.setReaders(getObjectRefs("reader", el));
            data.setWebsites(getTextValues("website", el).toArray(new String[0]));
            data.setNotes(getTextValues("note", el).toArray(new String[0]));

            map.put(lang, data);
        }

        return map;
    }

    private static ObjectRef[] getObjectRefs(String elementName, Element parent) {
        List<ObjectRef> objs = new ArrayList<>();
        for (Element el : getChildElements(elementName, parent)) {
            objs.add(new ObjectRef(xmlText("name", el), xmlText("id", el)));
        }
        return objs.toArray(new ObjectRef[0]);
    }

    private static List<Element> getChildElements(String elementName, Element parent) {
        List<Element> elements = new ArrayList<>();
        NodeList list = parent.getElementsByTagName(elementName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    private static List<String> getTextValues(String elementName, Element parent) {
        List<String> values = new ArrayList<>();
        for (Element el : getChildElements(elementName, parent)) {
            values.add(el.getTextContent());
        }
        return values;
    }

    private static String xmlAttribute(String attribute, String tag, Element parent) {
        List<Element> els = getChildElements(tag, parent);
        if (!els.isEmpty()) {
            return els.getFirst().getAttribute(attribute);
        }
        return "";
    }

    private static int xmlNumber(String tagName, Element parent) {
        String text = xmlText(tagName, parent);
        if (text == null || text.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String xmlText(String tagName, Element parent) {
        List<Element> els = getChildElements(tagName, parent);
        if (!els.isEmpty()) {
            return els.getFirst().getTextContent();
        }
        return "";
    }

    // ---- AoR Annotated Page XML parsing ----

    private static AnnotatedPage buildAnnotatedPage(Document doc, List<String> errors) {
        AnnotatedPage page = new AnnotatedPage();

        NodeList pageEls = doc.getElementsByTagName("page");
        if (pageEls.getLength() >= 1) {
            Element pageEl = (Element) pageEls.item(0);
            page.setPage(pageEl.getAttribute("filename"));
            page.setPagination(pageEl.getAttribute("pagination"));
            page.setReader(pageEl.getAttribute("reader"));
            page.setSignature(pageEl.getAttribute("signature"));
        }

        NodeList annotationEls = doc.getElementsByTagName("annotation");
        if (annotationEls.getLength() >= 1) {
            readAnnotations((Element) annotationEls.item(0), page);
        }

        return page;
    }

    private static void readAnnotations(Element annotationEl, AnnotatedPage page) {
        NodeList children = annotationEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element annotation = (Element) child;
            String id = annotation.getAttribute("id");
            boolean hasId = id != null && !id.isEmpty();
            Location loc = getLocation(annotation.getAttribute("place"));

            switch (annotation.getTagName()) {
                case "marginalia" -> {
                    Marginalia marg = buildMarginalia(annotation, page.getPage());
                    if (hasId) marg.setId(id);
                    page.getMarginalia().add(marg);
                }
                case "underline" -> {
                    Underline u = new Underline(
                            id,
                            annotation.getAttribute("text"),
                            annotation.getAttribute("method"),
                            annotation.getAttribute("type"),
                            annotation.getAttribute("language"),
                            Location.INTEXT
                    );
                    page.getUnderlines().add(u);
                }
                case "symbol" -> {
                    Symbol s = new Symbol(
                            id,
                            annotation.getAttribute("text"),
                            annotation.getAttribute("name"),
                            annotation.getAttribute("language"),
                            loc
                    );
                    page.getSymbols().add(s);
                }
                case "mark" -> {
                    Mark m = new Mark(
                            id,
                            annotation.getAttribute("text"),
                            annotation.getAttribute("name"),
                            annotation.getAttribute("method"),
                            annotation.getAttribute("language"),
                            loc
                    );
                    page.getMarks().add(m);
                }
                case "numeral" -> {
                    Numeral n = new Numeral(
                            id,
                            annotation.getAttribute("text"),
                            annotation.getTextContent(),
                            null,
                            loc
                    );
                    page.getNumerals().add(n);
                }
                case "errata" -> {
                    Errata e = new Errata(
                            id,
                            annotation.getAttribute("language"),
                            annotation.getAttribute("copytext"),
                            annotation.getAttribute("amendedtext")
                    );
                    page.getErrata().add(e);
                }
                case "drawing" -> {
                    Drawing d = new Drawing(
                            id,
                            annotation.getAttribute("anchor_text"),
                            loc,
                            annotation.getAttribute("name"),
                            annotation.getAttribute("method"),
                            annotation.getAttribute("language")
                    );
                    d.setType(annotation.getAttribute("type"));
                    NodeList textNodes = annotation.getElementsByTagName("text");
                    for (int j = 0; j < textNodes.getLength(); j++) {
                        if (textNodes.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                        Element txtEl = (Element) textNodes.item(j);
                        d.getTexts().add(new TextEl(
                                txtEl.getAttribute("hand"),
                                txtEl.getAttribute("language"),
                                txtEl.getAttribute("anchor_text"),
                                txtEl.getTextContent()
                        ));
                    }
                    page.getDrawings().add(d);
                }
                case "calculation" -> {
                    Calculation c = buildCalculation(annotation);
                    if (hasId) c.setId(id);
                    page.getCalculations().add(c);
                }
                case "graph" -> {
                    Graph g = buildGraph(annotation);
                    if (hasId) g.setId(id);
                    page.getGraphs().add(g);
                }
                case "table" -> {
                    Table t = buildTable(annotation);
                    if (hasId) t.setId(id);
                    page.getTables().add(t);
                }
                case "physical_link" -> {
                    PhysicalLink l = buildPhysicalLink(annotation);
                    if (hasId) l.setId(id);
                    page.getLinks().add(l);
                }
                default -> {}
            }
        }
    }

    private static Location getLocation(String loc) {
        if (loc == null || loc.isEmpty()) {
            return null;
        }
        for (Location l : Location.values()) {
            if (l.name().equalsIgnoreCase(loc)) {
                return l;
            }
        }
        return null;
    }

    private static Marginalia buildMarginalia(Element annotation, String pageName) {
        Marginalia marg = new Marginalia();

        marg.setHand(annotation.getAttribute("hand"));
        marg.setDate(annotation.getAttribute("date"));
        marg.setOtherReader(annotation.getAttribute("other_reader"));
        marg.setTopic(annotation.getAttribute("topic"));
        marg.setReferencedText(annotation.getAttribute("anchor_text"));

        Location loc = getLocation(annotation.getAttribute("place"));
        if (loc != null) {
            marg.setLocation(loc);
        }

        // Translation
        NodeList transNodes = annotation.getElementsByTagName("translation");
        if (transNodes.getLength() > 0) {
            marg.setTranslation(transNodes.item(0).getTextContent());
        }

        // Languages
        NodeList langNodes = annotation.getElementsByTagName("language");
        for (int i = 0; i < langNodes.getLength(); i++) {
            if (langNodes.item(i).getNodeType() != Node.ELEMENT_NODE) continue;
            Element langEl = (Element) langNodes.item(i);
            // Only direct children of marginalia (not nested in position)
            Node parentNode = langEl.getParentNode();
            if (parentNode.getNodeType() != Node.ELEMENT_NODE) continue;
            if (!((Element) parentNode).getTagName().equals("marginalia")) continue;

            MarginaliaLanguage ml = new MarginaliaLanguage();
            ml.setLang(langEl.getAttribute("ident"));

            // Positions within this language
            NodeList posNodes = langEl.getElementsByTagName("position");
            for (int j = 0; j < posNodes.getLength(); j++) {
                if (posNodes.item(j).getNodeType() != Node.ELEMENT_NODE) continue;
                Position pos = buildPosition((Element) posNodes.item(j), pageName);
                ml.getPositions().add(pos);
            }

            marg.getLanguages().add(ml);
        }

        // Internal refs are handled within position elements and languages

        return marg;
    }

    private static Position buildPosition(Element posEl, String pageName) {
        Position pos = new Position();

        Location loc = getLocation(posEl.getAttribute("place"));
        if (loc != null) {
            pos.setPlace(loc);
        }
        pos.setOrientation(safeParseInt(posEl.getAttribute("book_orientation"), 0));

        // People
        for (Element el : getDirectChildElements("person", posEl)) {
            pos.getPeople().add(el.getAttribute("name"));
        }
        // Books
        for (Element el : getDirectChildElements("book", posEl)) {
            pos.getBooks().add(el.getAttribute("title"));
        }
        // Locations
        for (Element el : getDirectChildElements("location", posEl)) {
            pos.getLocations().add(el.getAttribute("name"));
        }
        // XRefs
        for (Element el : getDirectChildElements("X-ref", posEl)) {
            XRef xref = new XRef(el.getAttribute("person"), el.getAttribute("title"), el.getTextContent(), null);
            pos.getXRefs().add(xref);
        }
        // Emphasis (underline elements within position)
        for (Element el : getDirectChildElements("emphasis", posEl)) {
            Underline emphasis = new Underline(
                    null,
                    el.getTextContent(),
                    el.getAttribute("method"),
                    el.getAttribute("type"),
                    el.getAttribute("language"),
                    null
            );
            pos.getEmphasis().add(emphasis);
        }
        // Marginalia text
        for (Element el : getDirectChildElements("marginalia_text", posEl)) {
            String text = el.getTextContent();
            if (text != null && !text.isBlank()) {
                pos.getTexts().add(text.trim());
            }
        }

        return pos;
    }

    private static Calculation buildCalculation(Element calcEl) {
        Location loc = getLocation(calcEl.getAttribute("place"));
        Calculation calc = new Calculation(
                null,
                calcEl.getAttribute("type"),
                safeParseInt(calcEl.getAttribute("book_orientation"), 0),
                loc,
                calcEl.getAttribute("method"),
                null
        );
        calc.setContent(calcEl.getTextContent().trim());
        return calc;
    }

    private static Graph buildGraph(Element graphEl) {
        Location loc = getLocation(graphEl.getAttribute("place"));
        Graph graph = new Graph(
                null,
                graphEl.getAttribute("type"),
                safeParseInt(graphEl.getAttribute("book_orientation"), 0),
                loc,
                graphEl.getAttribute("method")
        );

        // Graph nodes
        for (Element nodeEl : getDirectChildElements("node", graphEl)) {
            GraphNode node = new GraphNode(
                    nodeEl.getAttribute("id"),
                    nodeEl.getAttribute("person"),
                    null,
                    nodeEl.getTextContent().trim()
            );
            graph.getNodes().add(node);
        }

        // Graph text elements
        for (Element textEl : getDirectChildElements("graph_text", graphEl)) {
            graph.getGraphTexts().add(buildGraphText(textEl));
        }

        return graph;
    }

    private static GraphText buildGraphText(Element el) {
        GraphText gt = new GraphText();

        for (Element personEl : getDirectChildElements("person", el)) {
            gt.addPerson(personEl.getAttribute("name"));
        }
        for (Element bookEl : getDirectChildElements("book", el)) {
            gt.addBook(bookEl.getAttribute("title"));
        }
        for (Element locEl : getDirectChildElements("location", el)) {
            gt.addLocation(locEl.getAttribute("name"));
        }

        return gt;
    }

    private static Table buildTable(Element tableEl) {
        Location loc = getLocation(tableEl.getAttribute("place"));
        Table table = new Table(null, loc);
        table.setType(tableEl.getAttribute("type"));

        // Headers
        for (Element hEl : getDirectChildElements("tr", tableEl)) {
            List<TableHeader> headers = new ArrayList<>();
            for (Element thEl : getDirectChildElements("th", hEl)) {
                TableHeader th = new TableHeader("", "", "", thEl.getTextContent().trim());
                headers.add(th);
            }
            if (!headers.isEmpty()) {
                table.getColHeaders().addAll(headers);
            }
            // Cells
            List<TableCell> cells = new ArrayList<>();
            for (Element tdEl : getDirectChildElements("td", hEl)) {
                TableCell cell = new TableCell(0, 0, "", "", tdEl.getTextContent().trim());
                cells.add(cell);
            }
            if (!cells.isEmpty()) {
                table.getCells().addAll(cells);
            }
        }

        // Translation
        NodeList transNodes = tableEl.getElementsByTagName("translation");
        if (transNodes.getLength() > 0) {
            table.setTranslation(transNodes.item(0).getTextContent());
        }

        return table;
    }

    private static PhysicalLink buildPhysicalLink(Element el) {
        PhysicalLink link = new PhysicalLink();

        for (Element relEl : getDirectChildElements("relation", el)) {
            String from = relEl.getAttribute("from");
            String to = relEl.getAttribute("to");
            String type = relEl.getAttribute("type");
            link.getLinks().add(new AnnotationLink(null, from, to, type));
        }

        return link;
    }

    private static List<Element> getDirectChildElements(String tagName, Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(tagName)) {
                result.add((Element) child);
            }
        }
        return result;
    }

    private static int safeParseInt(String val, int defaultVal) {
        if (val == null || val.isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
