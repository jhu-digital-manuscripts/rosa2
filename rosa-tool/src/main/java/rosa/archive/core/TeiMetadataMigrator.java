package rosa.archive.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.model.BiblioData;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.ObjectRef;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Migrates book metadata from TEI description files into the custom XML metadata format.
 *
 * <p>For each book, this utility scans for language-specific TEI description files
 * (e.g., {@code <bookId>.description_en.xml}), parses each to extract metadata fields,
 * consolidates multilingual fields into a single {@link BookMetadata} object, and writes
 * the result as {@code <bookId>.metadata.xml}.</p>
 */
public final class TeiMetadataMigrator {

    private final Path archivePath;

    /**
     * Creates a migrator operating on the given archive directory.
     *
     * @param archivePath the root archive directory
     */
    public TeiMetadataMigrator(Path archivePath) {
        this.archivePath = archivePath;
    }

    /**
     * Migrates TEI description files for all books in the specified collection.
     *
     * @param collectionId the collection identifier
     * @param errors       list to collect error messages
     */
    public void migrateCollection(String collectionId, List<String> errors) {
        Path collectionDir = archivePath.resolve(collectionId);
        if (!Files.isDirectory(collectionDir)) {
            errors.add("Collection directory does not exist: " + collectionId);
            return;
        }

        List<String> bookIds = listBooks(collectionDir, errors);
        for (String bookId : bookIds) {
            migrateBook(collectionId, bookId, errors);
        }
    }

    /**
     * Migrates TEI description files for a single book into a consolidated metadata XML file.
     *
     * <p>If {@code <bookId>.metadata.xml} already exists, the book is skipped and a
     * warning is printed to stderr.</p>
     *
     * @param collectionId the collection identifier
     * @param bookId       the book identifier
     * @param errors       list to collect error messages
     */
    public void migrateBook(String collectionId, String bookId, List<String> errors) {
        Path bookDir = archivePath.resolve(collectionId).resolve(bookId);
        if (!Files.isDirectory(bookDir)) {
            errors.add("Book directory does not exist: " + collectionId + "/" + bookId);
            return;
        }

        Path metadataFile = bookDir.resolve(bookId + ".metadata.xml");
        if (Files.exists(metadataFile)) {
            System.err.println("Warning: metadata.xml already exists for " + bookId + ", skipping");
            return;
        }

        // Find all description files for this book
        Map<String, Path> descriptionFiles = findDescriptionFiles(bookDir, bookId);
        if (descriptionFiles.isEmpty()) {
            // No description files found — nothing to migrate
            return;
        }

        BookMetadata metadata = new BookMetadata();
        metadata.setId(bookId);
        Map<String, BiblioData> biblioMap = new HashMap<>();

        for (Map.Entry<String, Path> entry : descriptionFiles.entrySet()) {
            String lang = entry.getKey();
            Path descFile = entry.getValue();

            try {
                BiblioData biblio = parseTeiDescription(descFile, metadata, errors);
                if (biblio != null) {
                    biblio.setLanguage(lang);
                    biblioMap.put(lang, biblio);
                }
            } catch (IOException e) {
                errors.add("Failed to parse TEI description: " + descFile + " - " + e.getMessage());
            }
        }

        metadata.setBiblioDataMap(biblioMap);

        // Write the consolidated metadata
        try {
            writeMetadataXml(metadata, metadataFile);
        } catch (IOException e) {
            errors.add("Failed to write metadata XML for " + bookId + ": " + e.getMessage());
        }
    }

    /**
     * Scans the book directory for description files matching the pattern
     * {@code <bookId>.description_<lang>.xml}.
     */
    private Map<String, Path> findDescriptionFiles(Path bookDir, String bookId) {
        Map<String, Path> result = new HashMap<>();
        String prefix = bookId + ".description_";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, prefix + "*.xml")) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();
                // Extract language code: <bookId>.description_<lang>.xml
                String langPart = filename.substring(prefix.length(), filename.length() - 4);
                if (!langPart.isEmpty()) {
                    result.put(langPart, file);
                }
            }
        } catch (IOException e) {
            // Directory not readable — will be reported elsewhere
        }

        return result;
    }

    /**
     * Parses a TEI description file and extracts metadata fields.
     * Updates the shared BookMetadata with numeric/dimension fields from the first file parsed.
     *
     * @return a BiblioData containing language-specific text fields, or null on parse failure
     */
    private BiblioData parseTeiDescription(Path descFile, BookMetadata metadata, List<String> errors) throws IOException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(Files.newInputStream(descFile));
            Element top = doc.getDocumentElement();

            BiblioData biblio = new BiblioData();

            // Extract title from <title> or <bibl>/<title>
            biblio.setTitle(firstElementValue(top, "title"));
            biblio.setCommonName(noteValue(top, "commonName"));
            biblio.setMaterial(noteValue(top, "material"));
            biblio.setType(noteValue(top, "format"));

            // Origin from <pubPlace>
            biblio.setOrigin(firstElementValue(top, "pubPlace"));

            // Repository, shelfmark, currentLocation from <msIdentifier>
            biblio.setCurrentLocation(firstElementValue(top, "settlement"));
            biblio.setRepository(firstElementValue(top, "repository"));
            String shelfmark = firstElementValue(top, "shelfmark");
            if (shelfmark == null || shelfmark.isEmpty()) {
                shelfmark = firstElementValue(top, "idno");
            }
            biblio.setShelfmark(shelfmark);

            // Date label
            String dateLabel = firstElementValue(top, "date");
            biblio.setDateLabel(dateLabel);

            // Update shared numeric fields (from first file that has them)
            updateNumericFields(top, metadata);

            // Texts from <msItem> elements
            updateBookTexts(top, metadata);

            return biblio;
        } catch (ParserConfigurationException | SAXException e) {
            errors.add("Failed to parse TEI description: " + descFile + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts numeric/dimension fields from the TEI document and sets them on metadata
     * if they haven't been set yet (i.e., still at -1 default).
     */
    private void updateNumericFields(Element top, BookMetadata metadata) {
        // Dimensions
        if (metadata.getWidth() == -1) {
            int width = parseIntQuietly(firstElementValue(top, "width"));
            if (width > 0) {
                metadata.setWidth(width);
            }
        }
        if (metadata.getHeight() == -1) {
            int height = parseIntQuietly(firstElementValue(top, "height"));
            if (height > 0) {
                metadata.setHeight(height);
            }
        }
        if (metadata.getDimensionUnits() == null || metadata.getDimensionUnits().isEmpty()) {
            NodeList heightEls = top.getElementsByTagName("height");
            if (heightEls.getLength() > 0) {
                String unit = ((Element) heightEls.item(0)).getAttribute("unit");
                if (unit != null && !unit.isEmpty()) {
                    metadata.setDimensionUnits(unit);
                }
            }
        }

        // Number of pages from <measure quantity="N">
        if (metadata.getNumberOfPages() == -1) {
            NodeList measureEls = top.getElementsByTagName("measure");
            if (measureEls.getLength() > 0) {
                String quantity = ((Element) measureEls.item(0)).getAttribute("quantity");
                int pages = parseIntQuietly(quantity);
                if (pages > 0) {
                    metadata.setNumberOfPages(pages);
                }
            }
        }

        // Number of illustrations
        if (metadata.getNumberOfIllustrations() == -1) {
            String illus = noteValue(top, "illustrations");
            int numIllus = parseIntQuietly(illus);
            if (numIllus >= 0) {
                metadata.setNumberOfIllustrations(numIllus);
            }
        }

        // Year start/end from <date notBefore="..." notAfter="...">
        if (metadata.getYearStart() == -1) {
            NodeList dateEls = top.getElementsByTagName("date");
            if (dateEls.getLength() > 0) {
                Element dateEl = (Element) dateEls.item(0);
                int yearStart = parseIntQuietly(dateEl.getAttribute("notBefore"));
                int yearEnd = parseIntQuietly(dateEl.getAttribute("notAfter"));
                if (yearStart > 0) {
                    metadata.setYearStart(yearStart);
                }
                if (yearEnd > 0) {
                    metadata.setYearEnd(yearEnd);
                }
            }
        }
    }

    /**
     * Extracts book text entries from {@code <msItem>} elements and sets them on metadata
     * if not already populated.
     */
    private void updateBookTexts(Element top, BookMetadata metadata) {
        if (!metadata.getBookTexts().isEmpty()) {
            return;
        }

        NodeList msItems = top.getElementsByTagName("msItem");
        if (msItems.getLength() == 0) {
            return;
        }

        List<BookText> texts = new ArrayList<>();
        for (int i = 0; i < msItems.getLength(); i++) {
            if (msItems.item(i).getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) msItems.item(i);
            BookText text = new BookText();

            text.setTitle(firstElementValue(el, "title"));
            text.setLinesPerColumn(parseIntQuietly(noteValue(el, "linesPerColumn")));
            text.setColumnsPerPage(parseIntQuietly(noteValue(el, "columnsPerFolio")));
            text.setLeavesPerGathering(parseIntQuietly(noteValue(el, "leavesPerGathering")));
            text.setNumberOfIllustrations(parseIntQuietly(noteValue(el, "illustrations")));
            text.setNumberOfPages(parseIntQuietly(noteValue(el, "folios")));

            // Locus from/to
            NodeList locusEls = el.getElementsByTagName("locus");
            if (locusEls.getLength() > 0) {
                Element locus = (Element) locusEls.item(0);
                text.setFirstPage(locus.getAttribute("from"));
                text.setLastPage(locus.getAttribute("to"));
            }

            // Authors from <note type="author">
            NodeList notes = el.getElementsByTagName("note");
            for (int j = 0; j < notes.getLength(); j++) {
                if (notes.item(j).getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element noteEl = (Element) notes.item(j);
                if ("author".equals(noteEl.getAttribute("type"))) {
                    String author = noteEl.getTextContent();
                    if (author != null && !author.isBlank()) {
                        text.addAuthor(author.trim());
                    }
                }
            }

            texts.add(text);
        }

        metadata.setBookTexts(texts);
    }

    /**
     * Gets the text content of the first element with the given tag name.
     */
    private String firstElementValue(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() == 0) {
            return null;
        }
        String text = list.item(0).getTextContent();
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    /**
     * Finds a {@code <note type="name">} element and returns its text content.
     */
    private String noteValue(Element parent, String type) {
        NodeList notes = parent.getElementsByTagName("note");
        for (int i = 0; i < notes.getLength(); i++) {
            if (notes.item(i).getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element note = (Element) notes.item(i);
            if (type.equals(note.getAttribute("type"))) {
                String text = note.getTextContent();
                return (text == null || text.isBlank()) ? null : text.trim();
            }
        }
        return null;
    }

    /**
     * Parses an integer quietly, returning -1 on failure.
     */
    private int parseIntQuietly(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Writes the consolidated BookMetadata as XML using the custom format.
     */
    private void writeMetadataXml(BookMetadata metadata, Path outputFile) throws IOException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("book");
            doc.appendChild(root);

            // License
            Element license = doc.createElement("license");
            root.appendChild(license);
            appendTextElement(doc, license, "url", metadata.getLicenseUrl() != null ? metadata.getLicenseUrl() : "");
            appendTextElement(doc, license, "logo", metadata.getLicenseLogo() != null ? metadata.getLicenseLogo() : "");

            // Illustrations and pages
            appendTextElement(doc, root, "illustrations", String.valueOf(metadata.getNumberOfIllustrations()));
            appendTextElement(doc, root, "totalPages", String.valueOf(metadata.getNumberOfPages()));

            // Dimensions
            Element dimensions = doc.createElement("dimensions");
            root.appendChild(dimensions);
            dimensions.setAttribute("units", metadata.getDimensionUnits() != null ? metadata.getDimensionUnits() : "");
            appendTextElement(doc, dimensions, "width", String.valueOf(metadata.getWidth()));
            appendTextElement(doc, dimensions, "height", String.valueOf(metadata.getHeight()));

            // Dates
            Element dates = doc.createElement("dates");
            root.appendChild(dates);
            appendTextElement(doc, dates, "startDate", String.valueOf(metadata.getYearStart()));
            appendTextElement(doc, dates, "endDate", String.valueOf(metadata.getYearEnd()));

            // Texts
            Element texts = doc.createElement("texts");
            root.appendChild(texts);
            for (BookText t : metadata.getBookTexts()) {
                Element text = doc.createElement("text");
                texts.appendChild(text);

                appendTextElement(doc, text, "language", t.getLanguage() != null ? t.getLanguage() : "");
                appendTextElement(doc, text, "title", t.getTitle() != null ? t.getTitle() : "");

                Element pages = appendTextElement(doc, text, "pages", String.valueOf(t.getNumberOfPages()));
                pages.setAttribute("start", t.getFirstPage() != null ? t.getFirstPage() : "");
                pages.setAttribute("end", t.getLastPage() != null ? t.getLastPage() : "");

                appendTextElement(doc, text, "illustrations", String.valueOf(t.getNumberOfIllustrations()));
                appendTextElement(doc, text, "linesPerColumn", String.valueOf(t.getLinesPerColumn()));
                appendTextElement(doc, text, "leavesPerGathering", String.valueOf(t.getLeavesPerGathering()));
                appendTextElement(doc, text, "columnsPerPage", String.valueOf(t.getColumnsPerPage()));
            }

            // Bibliographies
            Element bibs = doc.createElement("bibliographies");
            root.appendChild(bibs);
            for (Map.Entry<String, BiblioData> entry : metadata.getBiblioDataMap().entrySet()) {
                String lang = entry.getKey();
                BiblioData data = entry.getValue();

                Element bib = doc.createElement("bibliography");
                bibs.appendChild(bib);
                bib.setAttribute("lang", lang);

                appendTextElement(doc, bib, "title", data.getTitle() != null ? data.getTitle() : "");
                appendTextElement(doc, bib, "commonName", data.getCommonName() != null ? data.getCommonName() : "");
                appendTextElement(doc, bib, "dateLabel", data.getDateLabel() != null ? data.getDateLabel() : "");
                appendTextElement(doc, bib, "type", data.getType() != null ? data.getType() : "");
                appendTextElement(doc, bib, "material", data.getMaterial() != null ? data.getMaterial() : "");
                appendTextElement(doc, bib, "origin", data.getOrigin() != null ? data.getOrigin() : "");
                appendTextElement(doc, bib, "currentLocation", data.getCurrentLocation() != null ? data.getCurrentLocation() : "");
                appendTextElement(doc, bib, "repository", data.getRepository() != null ? data.getRepository() : "");
                appendTextElement(doc, bib, "shelfmark", data.getShelfmark() != null ? data.getShelfmark() : "");

                for (ObjectRef author : data.getAuthors()) {
                    Element authorEl = doc.createElement("author");
                    bib.appendChild(authorEl);
                    appendTextElement(doc, authorEl, "name", author.getName() != null ? author.getName() : "");
                    appendTextElement(doc, authorEl, "id", author.getUri() != null ? author.getUri() : "");
                }
                for (ObjectRef reader : data.getReaders()) {
                    Element readerEl = doc.createElement("reader");
                    bib.appendChild(readerEl);
                    appendTextElement(doc, readerEl, "name", reader.getName() != null ? reader.getName() : "");
                    appendTextElement(doc, readerEl, "id", reader.getUri() != null ? reader.getUri() : "");
                }
                for (String website : data.getWebsites()) {
                    appendTextElement(doc, bib, "website", website != null ? website : "");
                }
                for (String detail : data.getDetails()) {
                    appendTextElement(doc, bib, "detail", detail != null ? detail : "");
                }
                for (String note : data.getNotes()) {
                    appendTextElement(doc, bib, "note", note != null ? note : "");
                }
            }

            // Write the document
            try (OutputStream out = Files.newOutputStream(outputFile)) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException("Failed to write metadata XML", e);
        }
    }

    /**
     * Appends a text element as a child of the given parent and returns the new element.
     */
    private Element appendTextElement(Document doc, Element parent, String tagName, String text) {
        Element el = doc.createElement(tagName);
        el.setTextContent(text);
        parent.appendChild(el);
        return el;
    }

    /**
     * Lists book directories (subdirectories) within a collection directory.
     */
    private List<String> listBooks(Path collectionDir, List<String> errors) {
        List<String> books = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionDir, Files::isDirectory)) {
            for (Path dir : stream) {
                books.add(dir.getFileName().toString());
            }
        } catch (IOException e) {
            errors.add("Failed to list books in " + collectionDir + ": " + e.getMessage());
        }
        books.sort(String::compareTo);
        return books;
    }
}
