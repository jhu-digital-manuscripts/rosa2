package rosa.archive.core;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts per-page text transcription files into a single TEI P5 XML document.
 *
 * <p>Each input file uses a custom text format with {@code [folio col]} headers,
 * poetry lines, and inline markup ({@code <rubric>}, {@code <illustration>},
 * {@code <catchphrase>}, etc.). The converter combines all pages into one TEI
 * document with {@code <pb>} and {@code <cb>} elements marking page/column boundaries.
 *
 * <p>Typical usage:
 * <pre>{@code
 * var converter = new TranscriptionConverter();
 * converter.convert(listOfTxtFiles, outputPath);
 * if (!converter.getErrors().isEmpty()) { ... }
 * }</pre>
 */
public final class TranscriptionConverter {

    private static final String TEI_NS = "http://www.tei-c.org/ns/1.0";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String SCHEMA_LOC = TEI_NS + "    http://www.tei-c.org/release/xml/tei/custom/schema/xsd/tei_ms.xsd";
    private static final Pattern LECOY_NUMBER = Pattern.compile("(.+?)\\s+(\\d+)\\s*$");
    private static final Pattern LECOY_LETTER = Pattern.compile("(.+?)\\s+([a-z])\\s*$");
    private static final Pattern PAGE_FROM_FILENAME = Pattern.compile("\\.transcription\\.(\\d+\\w+)\\.txt$");

    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    // Conversion state
    private String lastFolio;
    private String lastCol;
    private int nextLineNumber;
    private String folioOverride;

    /** Line types in the custom text format. */
    enum LineType { POETRY, RUBRIC, CATCHPHRASE, ABSENTLINES, ILLUSTRATION, ANNOTATION }

    /** A parsed tag from the custom markup. */
    record Tag(int start, int end, String name, String text, String anchorId, Map<String, String> attrs) {
        Tag withNoteText(String noteText) {
            return new Tag(start, end, name, noteText, null, attrs);
        }
    }

    /** A parsed line with metadata. */
    static final class Line {
        String text;
        List<Tag> tags;
        LineType type;
        boolean interpolationStart;
        boolean interpolationEnd;
        boolean interpolated;
        boolean lecoyIsLetter;
        boolean brokenRhyme;
        String folio;
        String col;
        int lineNumber;
        int lecoyLineNumber = -1;
        int fileLineNumber;

        Line(String text, int fileLineNumber, String folio, String col, List<String> errors) {
            this.text = text;
            this.folio = folio;
            this.col = col;
            this.fileLineNumber = fileLineNumber;
            this.tags = TagParser.parseTags(text, fileLineNumber, errors);

            // Process interpolation markers
            for (int i = 0; i < tags.size();) {
                Tag t = tags.get(i);
                if (t.name().equals("startInter")) {
                    this.interpolationStart = true;
                    stripTag(t);
                } else if (t.name().equals("endInter")) {
                    this.interpolationEnd = true;
                    stripTag(t);
                } else {
                    i++;
                }
            }
            determineType();
        }

        void stripTag(Tag tag) {
            text = text.substring(0, tag.start()) + text.substring(tag.end());
            text = text.trim();
            tags = TagParser.parseTags(text, fileLineNumber, new ArrayList<>());
            determineType();
        }

        void determineType() {
            type = LineType.POETRY;
            if (tags.size() == 1) {
                Tag t = tags.getFirst();
                if (t.start() == 0 && t.end() == text.length()) {
                    type = switch (t.name()) {
                        case "rubric" -> LineType.RUBRIC;
                        case "catchphrase" -> LineType.CATCHPHRASE;
                        case "illustration" -> LineType.ILLUSTRATION;
                        case "absLine" -> LineType.ABSENTLINES;
                        case "ann" -> LineType.ANNOTATION;
                        default -> LineType.POETRY;
                    };
                }
            }
            // Multiple illustration tags on same line
            if (tags.size() > 1 && tags.stream().allMatch(t -> t.name().equals("illustration"))) {
                type = LineType.ILLUSTRATION;
            }
            // Rubric with critical note
            if (tags.size() == 2 && tags.get(0).name().equals("rubric") && tags.get(1).name().equals("cn")) {
                type = LineType.RUBRIC;
            }
        }

        Tag findAnchor(String name, String anchorId) {
            for (Tag tag : tags) {
                if (tag.name().equals(name) && tag.anchorId() != null && tag.anchorId().equals(anchorId)) {
                    return tag;
                }
            }
            return null;
        }
    }

    /** Parses the custom inline markup tags from a text line. */
    static final class TagParser {
        private TagParser() {}

        private static boolean isScribalNotation(String s, int start, int end) {
            int len = end - start;
            if (len == 3 && !Character.isLetter(s.charAt(start + 1))) return true;
            return len == 4 && !Character.isLetter(s.charAt(start + 1)) && !Character.isLetter(s.charAt(start + 2));
        }

        static List<Tag> parseTags(String s, int fileLine, List<String> errors) {
            List<Tag> tags = new ArrayList<>();
            int offset = 0;
            while (offset < s.length()) {
                int tagStart = s.indexOf('<', offset);
                if (tagStart == -1) break;
                int tagEnd = s.indexOf('>', tagStart);
                if (tagEnd == -1) {
                    errors.add("Line " + fileLine + "; Malformed tag: " + s);
                    return tags;
                }
                tagEnd++;
                if (isScribalNotation(s, tagStart, tagEnd)) {
                    offset = tagEnd;
                    continue;
                }

                // Parse tag name
                int nameStart = tagStart + 1;
                if (s.charAt(nameStart) == '/') nameStart++;
                int i = nameStart;
                while (i < tagEnd && Character.isLetterOrDigit(s.charAt(i))) i++;
                if (i == nameStart) {
                    errors.add("Line " + fileLine + "; Malformed tag: " + s);
                    return tags;
                }
                String name = s.substring(nameStart, i);

                // Parse attributes
                Map<String, String> attrs = new HashMap<>();
                int attrEnd = tagEnd - 1;
                boolean single = s.charAt(attrEnd - 1) == '/';
                if (single) attrEnd--;
                if (i < attrEnd) {
                    parseAttrs(s.substring(i, attrEnd), attrs, fileLine, errors);
                }

                offset = tagEnd;
                String text = null;

                if (!single) {
                    // Find closing tag
                    int textStart = tagEnd;
                    boolean found = false;
                    while (!found) {
                        int closeStart = s.indexOf('<', offset);
                        if (closeStart == -1) {
                            errors.add("Line " + fileLine + "; Tag not closed: " + s);
                            return tags;
                        }
                        int closeEnd = s.indexOf('>', closeStart);
                        if (closeEnd == -1) {
                            errors.add("Line " + fileLine + "; Malformed tag: " + s);
                            return tags;
                        }
                        closeEnd++;
                        if (isScribalNotation(s, closeStart, closeEnd)) {
                            offset = closeEnd;
                            continue;
                        }
                        if (s.charAt(closeStart + 1) != '/') {
                            errors.add("Line " + fileLine + "; Expecting close tag: " + s);
                            return tags;
                        }
                        text = s.substring(textStart, closeStart).trim();
                        tagEnd = closeEnd;
                        offset = closeEnd;
                        found = true;
                    }
                }

                String anchorId = null;
                if (name.equals("cn") || name.equals("corr")) anchorId = attrs.get("n");
                else if (name.equals("order")) anchorId = attrs.get("id");

                tags.add(new Tag(tagStart, tagEnd, name, text, anchorId, attrs));
            }
            return tags;
        }

        private static void parseAttrs(String raw, Map<String, String> attrs, int fileLine, List<String> errors) {
            // Simple attribute parsing: name="value" pairs separated by spaces
            Pattern p = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"");
            Matcher m = p.matcher(raw);
            while (m.find()) {
                attrs.put(m.group(1), m.group(2));
            }
        }
    }

    // ==================== Public API ====================

    /**
     * Converts per-page text transcription files into a single TEI P5 XML file.
     * Uses Latin1 (ISO-8859-1) as the input encoding.
     *
     * @param inputFiles sorted list of per-page text transcription file paths
     * @param outputFile path for the generated TEI XML output
     * @throws IOException if reading input or writing output fails
     */
    public void convert(List<Path> inputFiles, Path outputFile) throws IOException {
        convert(inputFiles, outputFile, Charset.forName("Latin1"));
    }

    /**
     * Converts per-page text transcription files into a single TEI P5 XML file.
     *
     * @param inputFiles   sorted list of per-page text transcription file paths
     * @param outputFile   path for the generated TEI XML output
     * @param inputCharset character encoding of the input text files
     * @throws IOException if reading input or writing output fails
     */
    public void convert(List<Path> inputFiles, Path outputFile, Charset inputCharset) throws IOException {
        warnings.clear();
        errors.clear();
        lastFolio = null;
        lastCol = null;
        nextLineNumber = 1;

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter xml = factory.createXMLStreamWriter(os, "UTF-8");

            xml.writeStartDocument("UTF-8", "1.0");
            writeTEIHeader(xml);

            // <div type="ms">
            xml.writeStartElement("div");
            xml.writeAttribute("type", "ms");

            for (Path file : inputFiles) {
                folioOverride = extractFolioFromFilename(file);
                String text = readFile(file, inputCharset);
                List<List<Line>> blocks = parse(text);
                process(blocks);
                for (List<Line> block : blocks) {
                    writeBlock(block, xml);
                }
            }

            xml.writeEndElement(); // div
            xml.writeEndElement(); // body
            xml.writeEndElement(); // text
            xml.writeEndElement(); // TEI
            xml.writeEndDocument();
            xml.flush();
        } catch (XMLStreamException e) {
            throw new IOException("XML writing failed: " + e.getMessage(), e);
        }
    }

    /** Returns warnings generated during the last conversion. */
    public List<String> getWarnings() { return warnings; }

    /** Returns errors generated during the last conversion. */
    public List<String> getErrors() { return errors; }

    // ==================== Helpers ====================

    private String extractFolioFromFilename(Path file) {
        Matcher m = PAGE_FROM_FILENAME.matcher(file.getFileName().toString());
        return m.find() ? m.group(1) : null;
    }

    private String readFile(Path file, Charset charset) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        return charset.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private void writeTEIHeader(XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("TEI");
        xml.writeAttribute("xmlns", TEI_NS);
        xml.writeAttribute("xmlns:xsi", XSI_NS);
        xml.writeAttribute("xsi:schemaLocation", SCHEMA_LOC);

        xml.writeStartElement("teiHeader");
        xml.writeStartElement("fileDesc");

        xml.writeStartElement("titleStmt");
        xml.writeEmptyElement("title");
        xml.writeEndElement(); // titleStmt

        xml.writeStartElement("publicationStmt");
        xml.writeEmptyElement("distributor");
        xml.writeEndElement(); // publicationStmt

        xml.writeStartElement("sourceDesc");
        xml.writeEmptyElement("p");
        xml.writeEndElement(); // sourceDesc

        xml.writeEndElement(); // fileDesc
        xml.writeEndElement(); // teiHeader

        xml.writeStartElement("text");
        xml.writeStartElement("body");
    }

    // ==================== Parsing ====================

    private List<List<Line>> parse(String text) {
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] textLines = text.split("\n");

        // Find manuscript declaration
        String ms = null;
        int i = 0;
        for (; i < textLines.length; i++) {
            String s = textLines[i].trim();
            if (s.startsWith("[") && s.endsWith("]")) {
                ms = s.substring(1, s.length() - 1).replaceAll("\\s+", "").toLowerCase();
                i++;
                break;
            }
        }
        if (ms == null) {
            errors.add("Could not find ms decl");
        }

        List<List<Line>> doc = new ArrayList<>();
        List<Line> block = null;
        String folio = null;
        String col = null;

        for (; i < textLines.length; i++) {
            String s = textLines[i].trim();
            if (s.isEmpty()) {
                if (block != null && !block.isEmpty()) {
                    doc.add(block);
                    block = null;
                }
            } else if (s.startsWith("[") && s.endsWith("]")) {
                String inner = s.substring(1, s.length() - 1);
                if (inner.replaceAll("\\s+", "").toLowerCase().equals(ms)) continue;

                String[] parts = inner.split("\\s+");
                if (parts.length != 2) {
                    errors.add("Line " + (i + 1) + "; Header does not match 'folio col': " + s);
                    continue;
                }
                folio = parts[0];
                if (!folio.endsWith("r") && !folio.endsWith("v")) {
                    warnings.add("Line " + (i + 1) + "; Odd folio name: " + s);
                }
                col = parts[1];
                if (!(col.equals("a") || col.equals("b") || col.equals("c") || col.equals("d"))) {
                    errors.add("Line " + (i + 1) + "; Invalid column: " + s);
                }
            } else {
                if (block == null) block = new ArrayList<>();
                if (folio == null) {
                    errors.add("Line " + (i + 1) + "; Folio or ms declaration missing.");
                }
                block.add(new Line(s, i + 1, folio, col, errors));
            }
        }
        if (block != null && !block.isEmpty()) {
            doc.add(block);
        }
        return doc;
    }

    // ==================== Processing ====================

    private void process(List<List<Line>> doc) {
        // Merge adjacent blocks that together make 4 poetry lines
        for (int k = 0; k < doc.size() - 1;) {
            int n1 = countPoetry(doc.get(k));
            int n2 = countPoetry(doc.get(k + 1));
            if (n1 + n2 == 4) {
                doc.get(k).addAll(doc.get(k + 1));
                doc.remove(k + 1);
            }
            k++;
        }

        boolean interpState = false;
        for (int k = 0; k < doc.size(); k++) {
            List<Line> block = doc.get(k);

            // Set up anchor IDs
            for (Line line : block) {
                for (Tag tag : line.tags) {
                    if (tag.anchorId() != null) {
                        // already set during parsing
                    }
                }
            }

            // Resolve notes: strip note lines and attach text to anchors
            for (int idx = 0; idx < block.size();) {
                Line line = block.get(idx);
                if (line.tags.size() != 1 || !line.tags.getFirst().name().endsWith("Note")) {
                    idx++;
                    continue;
                }
                Tag noteTag = line.tags.getFirst();
                String type = noteTag.name().replace("Note", "");
                String attr = type.equals("order") ? "idref" : "n";
                String anchorId = noteTag.attrs().get(attr);
                if (anchorId == null) {
                    errors.add("Line " + line.fileLineNumber + "; Need " + attr + " attribute");
                    idx++;
                    continue;
                }

                // Find anchor in current and previous blocks
                Tag anchor = findAnchorInDoc(doc, k, type, anchorId);
                if (anchor == null) {
                    errors.add("Line " + line.fileLineNumber + "; No anchor for note");
                    idx++;
                    continue;
                }
                // Replace the anchor tag with one that has the note text
                replaceAnchorWithNote(doc, k, type, anchorId, noteTag.text());
                block.remove(idx);
            }

            // Check for broken rhymes
            for (Line line : block) {
                line.brokenRhyme = line.tags.stream().anyMatch(t -> t.name().equals("brokenRhyme"));
            }

            // Logical line numbering
            int numPoetry = 0;
            for (Line line : block) {
                if (line.type == LineType.POETRY) {
                    line.lineNumber = nextLineNumber++;
                    numPoetry++;
                }
            }
            if (numPoetry != 0 && numPoetry != 4 && k != doc.size() - 1) {
                warnings.add("Line " + block.getFirst().fileLineNumber + "; Block does not have 4 lines of poetry");
            }

            // Mark interpolated lines
            for (Line line : block) {
                if (line.interpolationStart) { line.interpolated = true; interpState = true; }
                else if (line.interpolationEnd) { line.interpolated = true; interpState = false; }
                else { line.interpolated = interpState; }
            }

            // Find and strip Lecoy numbers
            int lecoyIdx = -1;
            for (int idx = 0; idx < block.size(); idx++) {
                Line line = block.get(idx);
                Matcher m = LECOY_NUMBER.matcher(line.text);
                if (m.matches()) {
                    line.text = m.group(1).trim();
                    line.determineType();
                    line.lecoyLineNumber = Integer.parseInt(m.group(2));
                    lecoyIdx = idx;
                } else {
                    Matcher m2 = LECOY_LETTER.matcher(line.text);
                    if (m2.matches()) {
                        line.text = m2.group(1).trim();
                        line.determineType();
                        line.lecoyLineNumber = m2.group(2).charAt(0);
                        line.lecoyIsLetter = true;
                        lecoyIdx = idx;
                    }
                }
            }

            if (lecoyIdx == -1 && numPoetry > 0 && k != doc.size() - 1
                    && !block.stream().allMatch(l -> l.interpolated)) {
                warnings.add("Line " + block.getFirst().fileLineNumber + "; No lecoy number for quartet");
            }

            // Propagate Lecoy numbers
            if (lecoyIdx != -1) {
                int lecoy = block.get(lecoyIdx).lecoyLineNumber;
                for (int idx = lecoyIdx - 1; idx >= 0; idx--) {
                    Line line = block.get(idx);
                    if (line.type == LineType.POETRY && !line.interpolated) {
                        if (line.lecoyLineNumber == -1) line.lecoyLineNumber = --lecoy;
                        else lecoy = line.lecoyLineNumber;
                    }
                }
                lecoy = block.get(lecoyIdx).lecoyLineNumber;
                for (int idx = lecoyIdx + 1; idx < block.size(); idx++) {
                    Line line = block.get(idx);
                    if (line.type == LineType.POETRY && !line.interpolated) {
                        if (line.lecoyLineNumber == -1) line.lecoyLineNumber = ++lecoy;
                        else lecoy = line.lecoyLineNumber;
                    }
                }
            }
        }
    }

    private int countPoetry(List<Line> block) {
        return (int) block.stream().filter(l -> l.type == LineType.POETRY).count();
    }

    private Tag findAnchorInDoc(List<List<Line>> doc, int upToBlock, String type, String anchorId) {
        for (int n = upToBlock; n >= 0; n--) {
            for (Line l : doc.get(n)) {
                Tag t = l.findAnchor(type, anchorId);
                if (t != null) return t;
            }
        }
        return null;
    }

    private void replaceAnchorWithNote(List<List<Line>> doc, int upToBlock, String type, String anchorId, String noteText) {
        for (int n = upToBlock; n >= 0; n--) {
            for (Line l : doc.get(n)) {
                for (int i = 0; i < l.tags.size(); i++) {
                    Tag t = l.tags.get(i);
                    if (t.name().equals(type) && t.anchorId() != null && t.anchorId().equals(anchorId)) {
                        l.tags.set(i, t.withNoteText(noteText));
                        return;
                    }
                }
            }
        }
    }

    // ==================== XML Output ====================

    private void writeBlock(List<Line> block, XMLStreamWriter xml) throws XMLStreamException {
        boolean inCouplet = false;

        for (Line line : block) {
            // Emit pb/cb when folio or column changes
            String folio = (folioOverride != null) ? folioOverride : line.folio;
            if (lastFolio == null || !folio.equals(lastFolio) || !line.col.equals(lastCol)) {
                if (inCouplet) { xml.writeEndElement(); inCouplet = false; } // close lg
                xml.writeEmptyElement("pb");
                xml.writeAttribute("n", folio);
                xml.writeEmptyElement("cb");
                xml.writeAttribute("n", line.col);
                lastFolio = folio;
                lastCol = line.col;
            }

            switch (line.type) {
                case POETRY -> {
                    if (inCouplet) {
                        writeNormalLine(line, xml);
                        xml.writeEndElement(); // lg
                        inCouplet = false;
                    } else {
                        xml.writeStartElement("lg");
                        xml.writeAttribute("type", "couplet");
                        writeNormalLine(line, xml);
                        inCouplet = true;
                    }
                }
                case ABSENTLINES -> {
                    Tag tag = line.tags.getFirst();
                    xml.writeStartElement("l");
                    xml.writeStartElement("gap");
                    xml.writeStartElement("desc");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement(); // desc
                    xml.writeEndElement(); // gap
                    xml.writeEndElement(); // l
                }
                case CATCHPHRASE -> {
                    Tag tag = line.tags.getFirst();
                    xml.writeStartElement("fw");
                    xml.writeAttribute("type", "catch");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement(); // fw
                }
                case ILLUSTRATION -> {
                    for (Tag t : line.tags) {
                        xml.writeStartElement("figure");
                        xml.writeAttribute("type", "miniature");
                        xml.writeStartElement("head");
                        writeInternalText(t.text(), xml);
                        xml.writeEndElement(); // head
                        String chars = t.attrs().get("characters");
                        if (chars != null) {
                            for (String c : chars.split(",")) {
                                c = c.trim();
                                if (!c.isEmpty()) {
                                    xml.writeStartElement("note");
                                    xml.writeAttribute("type", "character");
                                    xml.writeCharacters(c);
                                    xml.writeEndElement(); // note
                                }
                            }
                        }
                        xml.writeEndElement(); // figure
                    }
                }
                case RUBRIC -> {
                    Tag tag = line.tags.getFirst();
                    xml.writeStartElement("l");
                    xml.writeStartElement("hi");
                    xml.writeAttribute("rend", "rubric");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement(); // hi
                    for (int i = 1; i < line.tags.size(); i++) {
                        writeNote(line.tags.get(i), xml);
                    }
                    xml.writeEndElement(); // l
                }
                case ANNOTATION -> {
                    Tag tag = line.tags.getFirst();
                    xml.writeStartElement("l");
                    xml.writeStartElement("note");
                    xml.writeAttribute("type", "scribalAnnotation");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement(); // note
                    xml.writeEndElement(); // l
                }
            }
        }

        if (inCouplet) {
            xml.writeEndElement(); // lg
        }
    }

    private void writeNormalLine(Line line, XMLStreamWriter xml) throws XMLStreamException {
        xml.writeStartElement("l");
        int offset = 0;
        boolean initial = false;

        for (Tag tag : line.tags) {
            if (tag.start() > offset) {
                writeText(line.text.substring(offset, tag.start()), initial, xml);
                initial = false;
            }
            switch (tag.name()) {
                case "initial" -> initial = true;
                case "nota" -> {
                    xml.writeStartElement("hi");
                    xml.writeAttribute("rend", "nota");
                    xml.writeCharacters("\u261a");
                    xml.writeEndElement();
                }
                case "added" -> {
                    xml.writeStartElement("add");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement();
                }
                case "expg" -> {
                    xml.writeStartElement("del");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement();
                }
                case "ann" -> {
                    xml.writeStartElement("note");
                    xml.writeAttribute("type", "scribalAnnotation");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement();
                }
                case "rubric" -> {
                    xml.writeStartElement("hi");
                    xml.writeAttribute("rend", "rubric");
                    writeInternalText(tag.text(), xml);
                    xml.writeEndElement();
                }
                case "brokenRhyme" -> { /* no output */ }
                default -> writeNote(tag, xml);
            }
            offset = tag.end();
        }

        if (offset < line.text.length()) {
            writeText(line.text.substring(offset), initial, xml);
        }

        // Lecoy milestone
        if (line.lecoyLineNumber != -1) {
            xml.writeEmptyElement("milestone");
            xml.writeAttribute("n", line.lecoyIsLetter
                    ? String.valueOf((char) line.lecoyLineNumber)
                    : String.valueOf(line.lecoyLineNumber));
            xml.writeAttribute("ed", "lecoy");
            xml.writeAttribute("unit", "line");
        }

        xml.writeEndElement(); // l
    }

    private void writeNote(Tag tag, XMLStreamWriter xml) throws XMLStreamException {
        if (tag.anchorId() != null) return; // Note missing, anchor without resolved text
        String noteType = switch (tag.name()) {
            case "cn" -> "critical";
            case "order" -> "order";
            case "corr" -> "correction";
            default -> null;
        };
        if (noteType == null) return;
        xml.writeStartElement("note");
        xml.writeAttribute("type", noteType);
        xml.writeCharacters(tag.text() != null ? tag.text() : "");
        xml.writeEndElement();
    }

    private void writeText(String text, boolean initial, XMLStreamWriter xml) throws XMLStreamException {
        if (initial && !text.isEmpty()) {
            xml.writeStartElement("hi");
            xml.writeAttribute("rend", "init");
            xml.writeCharacters(text.substring(0, 1));
            xml.writeEndElement();
            text = text.substring(1);
        }
        writeInternalText(text, xml);
    }

    /** Handles abbreviation marks (/text/) and scribal notation in text content. */
    private void writeInternalText(String text, XMLStreamWriter xml) throws XMLStreamException {
        if (text == null || text.isEmpty()) return;
        int offset = 0;
        while (offset < text.length()) {
            int abbrevStart = findAbbrevMark(text, offset);
            if (abbrevStart == -1) {
                writeScribalText(text.substring(offset), xml);
                break;
            }
            int abbrevEnd = findAbbrevMark(text, abbrevStart + 1);
            if (abbrevEnd == -1) {
                writeScribalText(text.substring(offset), xml);
                break;
            }
            writeScribalText(text.substring(offset, abbrevStart), xml);
            xml.writeStartElement("expan");
            writeScribalText(text.substring(abbrevStart + 1, abbrevEnd), xml);
            xml.writeEndElement();
            offset = abbrevEnd + 1;
        }
    }

    private int findAbbrevMark(String text, int offset) {
        while (true) {
            int i = text.indexOf('/', offset);
            if (i == -1) return -1;
            // Skip scribal notation </>
            if (i > 0 && text.charAt(i - 1) == '<') {
                offset = i + 1;
            } else {
                return i;
            }
        }
    }

    /** Handles scribal punctuation notation like <.>, <->, </>, <:> */
    private void writeScribalText(String text, XMLStreamWriter xml) throws XMLStreamException {
        int offset = 0;
        while (offset < text.length()) {
            int start = text.indexOf('<', offset);
            if (start == -1) {
                xml.writeCharacters(text.substring(offset));
                break;
            }
            int end = text.indexOf('>', start);
            if (end == -1) {
                xml.writeCharacters(text.substring(offset));
                break;
            }
            end++;
            xml.writeCharacters(text.substring(offset, start));
            String name = text.substring(start + 1, end - 1);
            xml.writeStartElement("note");
            xml.writeAttribute("type", "scribalPunc");
            xml.writeCharacters(name);
            xml.writeEndElement();
            offset = end;
        }
    }
}
