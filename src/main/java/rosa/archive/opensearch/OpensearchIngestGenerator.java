package rosa.archive.opensearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import rosa.archive.core.ArchiveStore;
import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.ImageList;
import rosa.archive.model.ObjectRef;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Generates Opensearch bulk ingest NDJSON files from archive data.
 *
 * <p>Produces files in the Opensearch Bulk API format (alternating action and document lines)
 * with one output file per collection. Documents are generated for the {@code manifests},
 * {@code canvases}, and {@code annotations} indexes.
 */
public final class OpensearchIngestGenerator {

    private final ObjectMapper mapper;

    /**
     * Creates an OpensearchIngestGenerator with a default Jackson ObjectMapper.
     */
    public OpensearchIngestGenerator() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Generates bulk ingest files for all collections in the archive.
     *
     * <p>Produces one NDJSON file per collection named {@code <collectionId>.bulk.json}
     * in the output directory. Each file contains alternating action/document line pairs
     * for the manifests, canvases, and annotations indexes.
     *
     * @param store     the archive store to read collections and books from
     * @param outputDir the output directory for bulk ingest files
     * @throws IOException if reading archive data or writing output fails
     */
    public void generate(ArchiveStore store, Path outputDir) throws IOException {
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        List<String> collectionIds = store.listCollections();

        for (String collectionId : collectionIds) {
            BookCollection collection = store.loadCollection(collectionId);
            List<String> bookIds = store.listBooks(collectionId);

            Path outputFile = outputDir.resolve(collectionId + ".bulk.json");

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                for (String bookId : bookIds) {
                    Book book = store.loadBook(collection, bookId);

                    // Manifest document
                    ObjectNode manifestDoc = generateManifestDocument(collection, book);
                    writeActionDocumentPair(writer, "manifests", book.getId(), manifestDoc);

                    // Process each image (canvas)
                    ImageList imageList = book.getImages();
                    if (imageList == null) {
                        continue;
                    }

                    List<BookImage> images = imageList.getImages();
                    for (int i = 0; i < images.size(); i++) {
                        BookImage image = images.get(i);
                        int position = i + 1; // 1-based

                        // Canvas document
                        ObjectNode canvasDoc = generateCanvasDocument(collection, book, image, position);
                        String canvasId = collection.getId() + "." + book.getId() + "." + image.getId();
                        writeActionDocumentPair(writer, "canvases", canvasId, canvasDoc);

                        // AoR annotations for this page
                        AnnotatedPage annotatedPage = book.getAnnotationPage(image.getId());
                        if (annotatedPage != null) {
                            String reader = annotatedPage.getReader();
                            for (Annotation annotation : annotatedPage.getAnnotations()) {
                                ObjectNode annotationDoc = generateAnnotationDocument(
                                        collection, book, image, annotation, reader);
                                String annotDocId = annotationDoc.has("id") ? annotationDoc.get("id").asText() : "";
                                writeActionDocumentPair(writer, "annotations", annotDocId, annotationDoc);
                            }
                        }

                        // Transcription for this page
                        Transcription transcription = book.getTranscription();
                        if (transcription != null && transcription.getXML() != null) {
                            // The transcription content is stored as a single XML blob;
                            // generate one transcription document per page
                            ObjectNode transcriptionDoc = generateTranscriptionDocument(
                                    collection, book, image, transcription.getXML());
                            String transId = transcriptionDoc.has("id") ? transcriptionDoc.get("id").asText() : "";
                            writeActionDocumentPair(writer, "annotations", transId, transcriptionDoc);
                        }

                        // Illustration tagging for this page
                        IllustrationTagging illustrationTagging = book.getIllustrationTagging();
                        if (illustrationTagging != null) {
                            List<Integer> illustrationIndices = illustrationTagging.findImageIndices(book, image.getId());
                            for (int idx : illustrationIndices) {
                                Illustration illustration = illustrationTagging.getIllustrationData(idx);
                                ObjectNode illustrationDoc = generateIllustrationDocument(
                                        collection, book, image, illustration);
                                String illusId = illustrationDoc.has("id") ? illustrationDoc.get("id").asText() : "";
                                writeActionDocumentPair(writer, "annotations", illusId, illustrationDoc);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Writes an action/document pair in Opensearch bulk API NDJSON format.
     *
     * <p>The action line specifies the index operation with the target index name and document ID.
     * The document line is the compact JSON representation of the document.
     *
     * @param writer  the writer to write to
     * @param index   the target index name
     * @param docId   the document ID
     * @param doc     the document content
     * @throws IOException if writing fails
     */
    private void writeActionDocumentPair(BufferedWriter writer, String index, String docId, ObjectNode doc) throws IOException {
        // Action line: {"index":{"_index":"<index>","_id":"<docId>"}}
        ObjectNode action = mapper.createObjectNode();
        ObjectNode indexNode = mapper.createObjectNode();
        indexNode.put("_index", index);
        indexNode.put("_id", docId);
        action.set("index", indexNode);

        writer.write(mapper.writeValueAsString(action));
        writer.newLine();

        // Document line
        writer.write(mapper.writeValueAsString(doc));
        writer.newLine();
    }

    /**
     * Generates a manifest document for the {@code manifests} index.
     *
     * <p>Contains bibliographic metadata and structural information for a single book.
     * Fields include: id, collection_id, title, repository, shelfmark, date, origin,
     * num_pages, languages, description, authors, type, current_location, websites,
     * year_start, year_end, number_of_illustrations, material, has_transcription.
     *
     * @param collection the collection containing the book
     * @param book       the book to generate a manifest document for
     * @return the manifest document as a Jackson ObjectNode
     */
    public ObjectNode generateManifestDocument(BookCollection collection, Book book) {
        ObjectNode doc = mapper.createObjectNode();

        doc.put("id", book.getId());
        doc.put("collection_id", collection.getId());

        BookMetadata metadata = book.getBookMetadata();
        BiblioData biblio = book.getBiblioData("en");

        // Title: prefer commonName from biblio data, fall back to book id
        String title = null;
        if (biblio != null) {
            title = biblio.getCommonName() != null ? biblio.getCommonName() : biblio.getTitle();
        }
        doc.put("title", title != null ? title : book.getId());

        // Repository
        String repository = biblio != null ? biblio.getRepository() : null;
        doc.put("repository", repository != null ? repository : "");

        // Shelfmark
        String shelfmark = biblio != null ? biblio.getShelfmark() : null;
        doc.put("shelfmark", shelfmark != null ? shelfmark : "");

        // Date
        String date = biblio != null ? biblio.getDateLabel() : null;
        doc.put("date", date != null ? date : "");

        // Origin
        String origin = biblio != null ? biblio.getOrigin() : null;
        doc.put("origin", origin != null ? origin : "");

        // Num pages
        int numPages = metadata != null ? metadata.getNumberOfPages() : 0;
        doc.put("num_pages", numPages >= 0 ? numPages : 0);

        // Languages
        ArrayNode languagesArray = doc.putArray("languages");
        String[] supportedLangs = collection.getAllSupportedLanguages();
        for (String lang : supportedLangs) {
            languagesArray.add(lang);
        }

        // Description
        String description = collection.getDescription();
        doc.put("description", description != null ? description : "");

        // Authors
        ArrayNode authorsArray = doc.putArray("authors");
        if (biblio != null && biblio.getAuthors() != null) {
            for (ObjectRef author : biblio.getAuthors()) {
                if (author.getName() != null && !author.getName().isBlank()) {
                    authorsArray.add(author.getName());
                }
            }
        }

        // Type
        String type = biblio != null ? biblio.getType() : null;
        doc.put("type", type != null ? type : "");

        // Current location
        String currentLocation = biblio != null ? biblio.getCurrentLocation() : null;
        doc.put("current_location", currentLocation != null ? currentLocation : "");

        // Websites
        ArrayNode websitesArray = doc.putArray("websites");
        if (biblio != null && biblio.getWebsites() != null) {
            for (String website : biblio.getWebsites()) {
                if (website != null && !website.isBlank()) {
                    websitesArray.add(website);
                }
            }
        }

        // Year start / year end
        int yearStart = metadata != null ? metadata.getYearStart() : -1;
        int yearEnd = metadata != null ? metadata.getYearEnd() : -1;
        doc.put("year_start", yearStart >= 0 ? yearStart : 0);
        doc.put("year_end", yearEnd >= 0 ? yearEnd : 0);

        // Number of illustrations
        int numIllustrations = metadata != null ? metadata.getNumberOfIllustrations() : 0;
        doc.put("number_of_illustrations", numIllustrations >= 0 ? numIllustrations : 0);

        // Material
        String material = biblio != null ? biblio.getMaterial() : null;
        doc.put("material", material != null ? material : "");

        // Has transcription
        boolean hasTranscription = book.getTranscription() != null;
        doc.put("has_transcription", hasTranscription);

        return doc;
    }

    /**
     * Generates a canvas document for the {@code canvases} index.
     *
     * <p>Contains page-level data including the image name and 1-based sequential position.
     *
     * @param collection the collection containing the book
     * @param book       the book containing the image
     * @param image      the book image representing this canvas/page
     * @param position   the 1-based sequential position of this page within the book
     * @return the canvas document as a Jackson ObjectNode
     */
    public ObjectNode generateCanvasDocument(BookCollection collection, Book book, BookImage image, int position) {
        ObjectNode doc = mapper.createObjectNode();

        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());

        // Label: prefer image name, fall back to image id
        String label = image.getName() != null ? image.getName() : image.getId();
        doc.put("label", label);

        // Image name: the short image identifier
        doc.put("image_name", image.getId());

        // Position: 1-based integer
        doc.put("position", position);

        return doc;
    }

    /**
     * Generates an annotation document for the {@code annotations} index.
     *
     * <p>Handles all annotation types: marginalia, underline, mark, symbol, drawing,
     * errata, numeral, transcription, and illustration. Routes text content into
     * language-specific sub-fields using {@link LanguageFieldRouter}.
     *
     * @param collection the collection containing the book
     * @param book       the book containing the annotation
     * @param image      the book image (page) on which the annotation appears
     * @param annotation the annotation to generate a document for
     * @param reader     the reader (annotator) from the AnnotatedPage, or null
     * @return the annotation document as a Jackson ObjectNode
     */
    public ObjectNode generateAnnotationDocument(BookCollection collection, Book book, BookImage image,
                                                  Annotation annotation, String reader) {
        ObjectNode doc = mapper.createObjectNode();

        // Common fields
        String annotationId = annotation.getId() != null ? annotation.getId() : "";
        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId() + "." + annotationId);
        doc.put("canvas_id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());
        doc.put("image_name", image.getId());

        // Annotator
        if (reader != null && !reader.isBlank()) {
            doc.put("annotator", reader);
        }

        // Language (base language of annotation)
        String language = annotation.getLanguage();
        if (language != null && !language.isBlank()) {
            doc.put("language", language);
        }

        // Dispatch based on annotation type
        switch (annotation) {
            case Marginalia m -> generateMarginaliaFields(doc, m);
            case Underline u -> generateUnderlineFields(doc, u);
            case Mark mk -> generateMarkFields(doc, mk);
            case Symbol sym -> generateSymbolFields(doc, sym);
            case Drawing d -> generateDrawingFields(doc, d);
            case Errata e -> generateErrataFields(doc, e);
            case Numeral n -> generateNumeralFields(doc, n);
            default -> {
                // For other annotation types (PhysicalLink, Calculation, Graph, Table),
                // set a generic type
                doc.put("type", annotation.getClass().getSimpleName().toLowerCase());
                if (annotation.getReferencedText() != null && !annotation.getReferencedText().isBlank()) {
                    String lang = language != null ? language : "en";
                    LanguageFieldRouter.routeField(doc, "text", lang, annotation.getReferencedText());
                }
            }
        }

        return doc;
    }

    /**
     * Generates a transcription annotation document for the {@code annotations} index.
     *
     * <p>Transcription documents contain the XML content of the transcription for a specific page.
     *
     * @param collection the collection containing the book
     * @param book       the book with the transcription
     * @param image      the book image representing the page
     * @param content    the transcription XML content for this page
     * @return the transcription annotation document as a Jackson ObjectNode
     */
    public ObjectNode generateTranscriptionDocument(BookCollection collection, Book book, BookImage image, String content) {
        ObjectNode doc = mapper.createObjectNode();

        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId() + ".transcription");
        doc.put("canvas_id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());
        doc.put("image_name", image.getId());
        doc.put("type", "transcription");

        // Determine language from collection supported languages, default to "en"
        String[] langs = collection.getAllSupportedLanguages();
        String lang = (langs != null && langs.length > 0) ? langs[0] : "en";
        doc.put("language", lang);

        if (content != null && !content.isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, content);
        }

        return doc;
    }

    /**
     * Generates an illustration annotation document for the {@code annotations} index.
     *
     * <p>Illustration documents contain title/description information for a tagged illustration.
     *
     * @param collection   the collection containing the book
     * @param book         the book with the illustration
     * @param image        the book image representing the page
     * @param illustration the illustration data
     * @return the illustration annotation document as a Jackson ObjectNode
     */
    public ObjectNode generateIllustrationDocument(BookCollection collection, Book book, BookImage image,
                                                    Illustration illustration) {
        ObjectNode doc = mapper.createObjectNode();

        String illustrationId = illustration.getId() != null ? illustration.getId() : "";
        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId() + ".illustration." + illustrationId);
        doc.put("canvas_id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());
        doc.put("image_name", image.getId());
        doc.put("type", "illustration");

        // Determine language from collection
        String[] langs = collection.getAllSupportedLanguages();
        String lang = (langs != null && langs.length > 0) ? langs[0] : "en";
        doc.put("language", lang);

        // Build text from illustration titles and description fields
        StringBuilder textBuilder = new StringBuilder();
        if (illustration.getTitles() != null) {
            for (String title : illustration.getTitles()) {
                if (title != null && !title.isBlank()) {
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append(" ");
                    }
                    textBuilder.append(title);
                }
            }
        }
        if (illustration.getTextualElement() != null && !illustration.getTextualElement().isBlank()) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append(" ");
            }
            textBuilder.append(illustration.getTextualElement());
        }

        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "text", lang, textBuilder.toString());
        }

        return doc;
    }

    /**
     * Generates fields specific to marginalia annotations.
     *
     * <p>Iterates over MarginaliaLanguage elements to route per-language text, translation,
     * emphasis, and cross-reference content. Collects people, books, locations, and symbols
     * from all Position elements.
     *
     * @param doc the document node to populate
     * @param m   the marginalia annotation
     */
    private void generateMarginaliaFields(ObjectNode doc, Marginalia m) {
        doc.put("type", "marginalia");

        // Topic
        if (m.getTopic() != null && !m.getTopic().isBlank()) {
            doc.put("topic", m.getTopic());
        }

        // Hand
        if (m.getHand() != null && !m.getHand().isBlank()) {
            doc.put("hand", m.getHand());
        }

        // Color
        if (m.getColor() != null && !m.getColor().isBlank()) {
            doc.put("color", m.getColor());
        }

        // Collect all people, books, locations, symbols across all languages/positions
        List<String> allPeople = new ArrayList<>();
        List<String> allBooks = new ArrayList<>();
        List<String> allLocations = new ArrayList<>();
        List<String> allSymbols = new ArrayList<>();

        // Iterate over MarginaliaLanguage elements for per-language text routing
        for (MarginaliaLanguage ml : m.getLanguages()) {
            String langCode = ml.getLang();
            if (langCode == null || langCode.isBlank()) {
                langCode = "en";
            }

            // Update the language field to include all languages found
            // The primary language is set from the first MarginaliaLanguage
            if (m.getLanguages().indexOf(ml) == 0) {
                doc.put("language", langCode);
            }

            for (Position pos : ml.getPositions()) {
                // Text: join all texts from this position
                if (pos.getTexts() != null && !pos.getTexts().isEmpty()) {
                    String joinedText = String.join(" ", pos.getTexts());
                    if (!joinedText.isBlank()) {
                        LanguageFieldRouter.routeField(doc, "text", langCode, joinedText);
                    }
                }

                // Emphasis: underline elements in position
                if (pos.getEmphasis() != null && !pos.getEmphasis().isEmpty()) {
                    StringJoiner emphJoiner = new StringJoiner(" ");
                    for (Underline emph : pos.getEmphasis()) {
                        if (emph.getReferencedText() != null && !emph.getReferencedText().isBlank()) {
                            emphJoiner.add(emph.getReferencedText());
                        }
                    }
                    String emphText = emphJoiner.toString();
                    if (!emphText.isBlank()) {
                        LanguageFieldRouter.routeField(doc, "emphasis", langCode, emphText);
                    }
                }

                // Cross-references: people and titles from XRef elements
                if (pos.getXRefs() != null && !pos.getXRefs().isEmpty()) {
                    StringJoiner xrefJoiner = new StringJoiner(" ");
                    for (XRef xref : pos.getXRefs()) {
                        if (xref.person() != null && !xref.person().isBlank()) {
                            xrefJoiner.add(xref.person());
                        }
                        if (xref.title() != null && !xref.title().isBlank()) {
                            xrefJoiner.add(xref.title());
                        }
                    }
                    String xrefText = xrefJoiner.toString();
                    if (!xrefText.isBlank()) {
                        LanguageFieldRouter.routeField(doc, "cross_reference", langCode, xrefText);
                    }
                }

                // Collect people, books, locations, symbols
                if (pos.getPeople() != null) {
                    for (String person : pos.getPeople()) {
                        if (person != null && !person.isBlank() && !allPeople.contains(person)) {
                            allPeople.add(person);
                        }
                    }
                }
                if (pos.getBooks() != null) {
                    for (String book : pos.getBooks()) {
                        if (book != null && !book.isBlank() && !allBooks.contains(book)) {
                            allBooks.add(book);
                        }
                    }
                }
                if (pos.getLocations() != null) {
                    for (String loc : pos.getLocations()) {
                        if (loc != null && !loc.isBlank() && !allLocations.contains(loc)) {
                            allLocations.add(loc);
                        }
                    }
                }
                if (pos.getSymbols() != null) {
                    for (String sym : pos.getSymbols()) {
                        if (sym != null && !sym.isBlank() && !allSymbols.contains(sym)) {
                            allSymbols.add(sym);
                        }
                    }
                }
            }
        }

        // Translation at marginalia level
        if (m.getTranslation() != null && !m.getTranslation().isBlank()) {
            String langCode = "en"; // translation is typically in English
            if (!m.getLanguages().isEmpty() && m.getLanguages().getFirst().getLang() != null) {
                langCode = m.getLanguages().getFirst().getLang();
            }
            LanguageFieldRouter.routeField(doc, "translation", langCode, m.getTranslation());
        }

        // Write arrays
        addArrayField(doc, "people", allPeople);
        addArrayField(doc, "books", allBooks);
        addArrayField(doc, "locations", allLocations);
        addArrayField(doc, "symbols", allSymbols);
    }

    /**
     * Generates fields specific to underline annotations.
     *
     * @param doc the document node to populate
     * @param u   the underline annotation
     */
    private void generateUnderlineFields(ObjectNode doc, Underline u) {
        doc.put("type", "underline");

        if (u.getMethod() != null && !u.getMethod().isBlank()) {
            doc.put("method", u.getMethod());
        }
        if (u.getColor() != null && !u.getColor().isBlank()) {
            doc.put("color", u.getColor());
        }

        // Route text by language
        String lang = u.getLanguage() != null ? u.getLanguage() : "en";
        if (u.getReferencedText() != null && !u.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, u.getReferencedText());
        }
    }

    /**
     * Generates fields specific to mark annotations.
     *
     * @param doc the document node to populate
     * @param mk  the mark annotation
     */
    private void generateMarkFields(ObjectNode doc, Mark mk) {
        doc.put("type", "mark");

        if (mk.getMethod() != null && !mk.getMethod().isBlank()) {
            doc.put("method", mk.getMethod());
        }
        if (mk.getColor() != null && !mk.getColor().isBlank()) {
            doc.put("color", mk.getColor());
        }
        if (mk.getName() != null && !mk.getName().isBlank()) {
            doc.put("hand", mk.getName());
        }

        // Route referenced text by language
        String lang = mk.getLanguage() != null ? mk.getLanguage() : "en";
        if (mk.getReferencedText() != null && !mk.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, mk.getReferencedText());
        }
    }

    /**
     * Generates fields specific to symbol annotations.
     *
     * @param doc the document node to populate
     * @param sym the symbol annotation
     */
    private void generateSymbolFields(ObjectNode doc, Symbol sym) {
        doc.put("type", "symbol");

        // Route referenced text by language
        String lang = sym.getLanguage() != null ? sym.getLanguage() : "en";
        if (sym.getReferencedText() != null && !sym.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, sym.getReferencedText());
        }

        // Symbols array with symbol name
        if (sym.getName() != null && !sym.getName().isBlank()) {
            ArrayNode symbolsArray = doc.putArray("symbols");
            symbolsArray.add(sym.getName());
        }
    }

    /**
     * Generates fields specific to drawing annotations.
     *
     * @param doc the document node to populate
     * @param d   the drawing annotation
     */
    private void generateDrawingFields(ObjectNode doc, Drawing d) {
        doc.put("type", "drawing");

        if (d.getMethod() != null && !d.getMethod().isBlank()) {
            doc.put("method", d.getMethod());
        }
        if (d.getColor() != null && !d.getColor().isBlank()) {
            doc.put("color", d.getColor());
        }
        if (d.getOrientation() != null && !d.getOrientation().isBlank()) {
            doc.put("orientation", d.getOrientation());
        }

        String lang = d.getLanguage() != null ? d.getLanguage() : "en";

        // Text from drawing texts
        if (d.getTexts() != null && !d.getTexts().isEmpty()) {
            StringJoiner textJoiner = new StringJoiner(" ");
            for (TextEl textEl : d.getTexts()) {
                if (textEl.text() != null && !textEl.text().isBlank()) {
                    textJoiner.add(textEl.text());
                }
            }
            String textContent = textJoiner.toString();
            if (!textContent.isBlank()) {
                LanguageFieldRouter.routeField(doc, "text", lang, textContent);
            }

            // Anchor text from drawing TextEl elements
            StringJoiner anchorJoiner = new StringJoiner(" ");
            for (TextEl textEl : d.getTexts()) {
                if (textEl.anchorText() != null && !textEl.anchorText().isBlank()) {
                    anchorJoiner.add(textEl.anchorText());
                }
            }
            String anchorContent = anchorJoiner.toString();
            if (!anchorContent.isBlank()) {
                LanguageFieldRouter.routeField(doc, "anchor_text", lang, anchorContent);
            }
        }

        // Translation
        if (d.getTranslation() != null && !d.getTranslation().isBlank()) {
            LanguageFieldRouter.routeField(doc, "translation", lang, d.getTranslation());
        }

        // People, books, locations, symbols
        addArrayField(doc, "people", d.getPeople());
        addArrayField(doc, "books", d.getBooks());
        addArrayField(doc, "locations", d.getLocations());
        addArrayField(doc, "symbols", d.getSymbols());
    }

    /**
     * Generates fields specific to errata annotations.
     *
     * @param doc the document node to populate
     * @param e   the errata annotation
     */
    private void generateErrataFields(ObjectNode doc, Errata e) {
        doc.put("type", "errata");

        String lang = e.getLanguage() != null ? e.getLanguage() : "en";

        // Text = copy text + amended text
        StringBuilder textBuilder = new StringBuilder();
        if (e.getReferencedText() != null && !e.getReferencedText().isBlank()) {
            textBuilder.append(e.getReferencedText());
        }
        if (e.getAmendedText() != null && !e.getAmendedText().isBlank()) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append(" ");
            }
            textBuilder.append(e.getAmendedText());
        }
        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "text", lang, textBuilder.toString());
        }
    }

    /**
     * Generates fields specific to numeral annotations.
     *
     * @param doc the document node to populate
     * @param n   the numeral annotation
     */
    private void generateNumeralFields(ObjectNode doc, Numeral n) {
        doc.put("type", "numeral");

        String lang = n.getLanguage() != null ? n.getLanguage() : "en";

        // Text = numeral content
        String content = n.getNumeral() != null ? n.getNumeral() : n.getReferencedText();
        if (content != null && !content.isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, content);
        }
    }

    /**
     * Adds a string array field to the document if the list is non-empty.
     *
     * @param doc       the document node
     * @param fieldName the field name
     * @param values    the list of values
     */
    private void addArrayField(ObjectNode doc, String fieldName, List<String> values) {
        if (values != null && !values.isEmpty()) {
            ArrayNode array = doc.putArray(fieldName);
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    array.add(value);
                }
            }
        }
    }
}
