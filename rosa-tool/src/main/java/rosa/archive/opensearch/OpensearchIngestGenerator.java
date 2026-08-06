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
import rosa.archive.model.BookReferenceSheet;
import rosa.archive.model.BookText;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.HTMLAnnotations;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.ObjectRef;
import rosa.archive.model.ReferenceSheet;
import rosa.archive.model.Transcription;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphNote;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TableCell;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Generates Opensearch bulk ingest NDJSON files from archive data.
 *
 * <p>Produces files in the Opensearch Bulk API format (alternating action and document lines)
 * with one output file per collection. Documents are generated for the {@code manifest},
 * {@code canvas}, and {@code annotation} indexes.
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

            if (bookIds.isEmpty()) {
                continue;
            }

            Path outputFile = outputDir.resolve(collectionId + ".bulk.json");

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                for (String bookId : bookIds) {
                    Book book = store.loadBook(collection, bookId);

                    // Manifest document
                    ObjectNode manifestDoc = generateManifestDocument(collection, book);
                    writeActionDocumentPair(writer, "manifest", book.getId(), manifestDoc);

                    // Process each image (canvas)
                    ImageList imageList = book.getImages();
                    if (imageList == null) {
                        continue;
                    }

                    // Split the transcription XML once per book into per-page fragments
                    Transcription transcription = book.getTranscription();
                    Map<String, String> transcriptionPages = Collections.emptyMap();
                    if (transcription != null && transcription.getXML() != null) {
                        transcriptionPages = TranscriptionSplitter.split(transcription.getXML());
                    }

                    List<BookImage> images = imageList.getImages();
                    for (int i = 0; i < images.size(); i++) {
                        BookImage image = images.get(i);
                        int position = i + 1;

                        // Canvas document
                        ObjectNode canvasDoc = generateCanvasDocument(collection, book, image, position);
                        String canvasId = collection.getId() + "." + book.getId() + "." + image.getId();
                        writeActionDocumentPair(writer, "canvas", canvasId, canvasDoc);

                        // AoR annotations for this page
                        AnnotatedPage annotatedPage = book.getAnnotationPage(image.getId());
                        if (annotatedPage != null) {
                            String reader = annotatedPage.getReader();
                            for (Annotation annotation : annotatedPage.getAnnotations()) {
                                ObjectNode annotationDoc = generateAnnotationDocument(
                                        collection, book, image, annotation, reader);
                                String annotDocId = annotationDoc.has("id") ? annotationDoc.get("id").asText() : "";
                                writeActionDocumentPair(writer, "annotation", annotDocId, annotationDoc);
                            }
                        }

                        // Transcription for this page
                        if (!transcriptionPages.isEmpty()) {
                            String normalizedPage = TranscriptionSplitter.normalizePageName(image.getName());
                            String pageFragment = transcriptionPages.get(normalizedPage);
                            if (pageFragment != null && !pageFragment.isBlank()) {
                                ObjectNode transcriptionDoc = generateTranscriptionDocument(
                                        collection, book, image, pageFragment);
                                String transId = transcriptionDoc.has("id") ? transcriptionDoc.get("id").asText() : "";
                                writeActionDocumentPair(writer, "annotation", transId, transcriptionDoc);
                            }
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
                                writeActionDocumentPair(writer, "annotation", illusId, illustrationDoc);
                            }
                        }
                    }
                }
            }
        }
    }

    private void writeActionDocumentPair(BufferedWriter writer, String index, String docId, ObjectNode doc) throws IOException {
        ObjectNode action = mapper.createObjectNode();
        ObjectNode indexNode = mapper.createObjectNode();
        indexNode.put("_index", index);
        indexNode.put("_id", docId);
        action.set("index", indexNode);

        writer.write(mapper.writeValueAsString(action));
        writer.newLine();
        writer.write(mapper.writeValueAsString(doc));
        writer.newLine();
    }

    // ========== Manifest ==========

    /**
     * Generates a manifest document for the {@code manifest} index.
     */
    public ObjectNode generateManifestDocument(BookCollection collection, Book book) {
        ObjectNode doc = mapper.createObjectNode();

        doc.put("id", book.getId());
        doc.put("collection_id", collection.getId());

        BookMetadata metadata = book.getBookMetadata();
        BiblioData biblio = book.getBiblioData("en");

        String title = null;
        if (biblio != null) {
            title = biblio.getCommonName() != null ? biblio.getCommonName() : biblio.getTitle();
        }
        doc.put("title", title != null ? title : book.getId());

        // Titles array: common name + BookText titles
        ArrayNode titlesArray = doc.putArray("titles");
        if (title != null && !title.isBlank()) {
            titlesArray.add(title);
        }
        if (metadata != null && metadata.getBookTexts() != null) {
            for (BookText bt : metadata.getBookTexts()) {
                if (bt.getTitle() != null && !bt.getTitle().isBlank()) {
                    titlesArray.add(bt.getTitle());
                }
            }
        }

        String repository = biblio != null ? biblio.getRepository() : null;
        doc.put("repository", repository != null ? repository : "");

        String shelfmark = biblio != null ? biblio.getShelfmark() : null;
        doc.put("shelfmark", shelfmark != null ? shelfmark : "");

        String date = biblio != null ? biblio.getDateLabel() : null;
        doc.put("date", date != null ? date : "");

        String origin = biblio != null ? biblio.getOrigin() : null;
        doc.put("origin", origin != null ? origin : "");

        int numPages = metadata != null ? metadata.getNumberOfPages() : 0;
        doc.put("num_pages", numPages >= 0 ? numPages : 0);

        ArrayNode languagesArray = doc.putArray("languages");
        String[] supportedLangs = collection.getAllSupportedLanguages();
        if (supportedLangs != null) {
            for (String lang : supportedLangs) {
                languagesArray.add(lang);
            }
        }

        // Description: BookDescription doesn't exist in new code, use empty string
        doc.put("description", "");

        // Authors: from BiblioData + BookText authors
        ArrayNode authorsArray = doc.putArray("authors");
        if (biblio != null && biblio.getAuthors() != null) {
            for (ObjectRef author : biblio.getAuthors()) {
                if (author.getName() != null && !author.getName().isBlank()) {
                    authorsArray.add(author.getName());
                }
            }
        }
        if (metadata != null && metadata.getBookTexts() != null) {
            for (BookText bt : metadata.getBookTexts()) {
                if (bt.getAuthors() != null) {
                    for (String author : bt.getAuthors()) {
                        if (author != null && !author.isBlank() && !arrayContains(authorsArray, author)) {
                            authorsArray.add(author);
                        }
                    }
                }
            }
        }

        String type = biblio != null ? biblio.getType() : null;
        doc.put("type", type != null ? type : "");

        String currentLocation = biblio != null ? biblio.getCurrentLocation() : null;
        doc.put("current_location", currentLocation != null ? currentLocation : "");

        ArrayNode websitesArray = doc.putArray("websites");
        if (biblio != null && biblio.getWebsites() != null) {
            for (String website : biblio.getWebsites()) {
                if (website != null && !website.isBlank()) {
                    websitesArray.add(website);
                }
            }
        }

        int yearStart = metadata != null ? metadata.getYearStart() : -1;
        int yearEnd = metadata != null ? metadata.getYearEnd() : -1;
        doc.put("year_start", yearStart >= 0 ? yearStart : 0);
        doc.put("year_end", yearEnd >= 0 ? yearEnd : 0);

        int numIllustrations = metadata != null ? metadata.getNumberOfIllustrations() : 0;
        doc.put("number_of_illustrations", numIllustrations >= 0 ? numIllustrations : 0);

        String material = biblio != null ? biblio.getMaterial() : null;
        doc.put("material", material != null ? material : "");

        boolean hasTranscription = book.getTranscription() != null;
        doc.put("has_transcription", hasTranscription);

        return doc;
    }

    // ========== Canvas ==========

    /**
     * Generates a canvas document for the {@code canvas} index.
     * Uses pagination/signature from AnnotatedPage for the label when available.
     */
    public ObjectNode generateCanvasDocument(BookCollection collection, Book book, BookImage image, int position) {
        ObjectNode doc = mapper.createObjectNode();

        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());

        // Label: prefer pagination from AnnotatedPage, then signature, then image name
        String label = image.getName() != null ? image.getName() : image.getId();
        AnnotatedPage ap = book.getAnnotationPage(image.getId());
        if (ap != null) {
            if (ap.getPagination() != null && !ap.getPagination().isBlank()) {
                label = ap.getPagination();
            } else if (ap.getSignature() != null && !ap.getSignature().isBlank()) {
                label = ap.getSignature();
            }
        }
        doc.put("label", label);
        doc.put("image_name", image.getId());
        doc.put("position", position);

        return doc;
    }

    // ========== Annotation (dispatcher) ==========

    /**
     * Generates an annotation document for the {@code annotation} index.
     */
    public ObjectNode generateAnnotationDocument(BookCollection collection, Book book, BookImage image,
                                                  Annotation annotation, String reader) {
        ObjectNode doc = mapper.createObjectNode();

        String annotationId = annotation.getId() != null ? annotation.getId() : "";
        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId() + "." + annotationId);
        doc.put("canvas_id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());
        doc.put("image_name", image.getId());

        if (reader != null && !reader.isBlank()) {
            doc.put("annotator", reader);
        }

        String language = annotation.getLanguage();
        if (language != null && !language.isBlank()) {
            doc.put("language", language);
        }

        switch (annotation) {
            case Marginalia m -> generateMarginaliaFields(doc, m, collection);
            case Underline u -> generateUnderlineFields(doc, u);
            case Mark mk -> generateMarkFields(doc, mk);
            case Symbol sym -> generateSymbolFields(doc, sym);
            case Drawing d -> generateDrawingFields(doc, d, collection);
            case Errata e -> generateErrataFields(doc, e);
            case Numeral n -> generateNumeralFields(doc, n);
            case Calculation c -> generateCalculationFields(doc, c);
            case Graph g -> generateGraphFields(doc, g, collection);
            case Table t -> generateTableFields(doc, t, collection);
            default -> {
                doc.put("type", annotation.getClass().getSimpleName().toLowerCase());
                if (annotation.getReferencedText() != null && !annotation.getReferencedText().isBlank()) {
                    String lang = language != null ? language : "en";
                    LanguageFieldRouter.routeField(doc, "text", lang,
                            stripTranscriberMarks(annotation.getReferencedText()));
                }
            }
        }

        return doc;
    }

    // ========== Transcription ==========

    /**
     * Generates a transcription annotation document. Parses the XML fragment to extract
     * text content and routes it to appropriate language sub-fields.
     */
    public ObjectNode generateTranscriptionDocument(BookCollection collection, Book book,
                                                     BookImage image, String content) {
        ObjectNode doc = mapper.createObjectNode();

        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId() + ".transcription");
        doc.put("canvas_id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());
        doc.put("image_name", image.getId());
        doc.put("type", "transcription");

        // Parse the transcription XML to extract text by category
        TranscriptionXmlExtractor.Result extracted = TranscriptionXmlExtractor.extract(content);

        // Poetry, rubric, catchphrase → Old French
        StringBuilder ofrText = new StringBuilder();
        if (!extracted.poetry().isBlank()) {
            ofrText.append(extracted.poetry()).append(" ");
        }
        if (!extracted.rubric().isBlank()) {
            ofrText.append(extracted.rubric()).append(" ");
        }
        if (!extracted.catchphrase().isBlank()) {
            ofrText.append(extracted.catchphrase()).append(" ");
        }
        if (!ofrText.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "text", "ofr", ofrText.toString().trim());
            doc.put("language", "ofr");
        }

        // Illustration, notes, lecoy, line numbers → English
        StringBuilder enText = new StringBuilder();
        if (!extracted.illustration().isBlank()) {
            enText.append(extracted.illustration()).append(" ");
        }
        if (!extracted.note().isBlank()) {
            enText.append(extracted.note()).append(" ");
        }
        if (!extracted.lecoy().isBlank()) {
            enText.append(extracted.lecoy()).append(" ");
        }
        if (!extracted.line().isBlank()) {
            enText.append(extracted.line()).append(" ");
        }
        if (!enText.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "text", "en", enText.toString().trim());
            if (!doc.has("language")) {
                doc.put("language", "en");
            }
        }

        return doc;
    }

    // ========== Illustration ==========

    /**
     * Generates an illustration annotation document. Resolves title IDs and character IDs
     * using collection reference data.
     */
    public ObjectNode generateIllustrationDocument(BookCollection collection, Book book,
                                                    BookImage image, Illustration illustration) {
        ObjectNode doc = mapper.createObjectNode();

        String illustrationId = illustration.getId() != null ? illustration.getId() : "";
        doc.put("id", collection.getId() + "." + book.getId() + "." + image.getId() + ".illustration." + illustrationId);
        doc.put("canvas_id", collection.getId() + "." + book.getId() + "." + image.getId());
        doc.put("manifest_id", book.getId());
        doc.put("collection_id", collection.getId());
        doc.put("image_name", image.getId());
        doc.put("type", "illustration");
        doc.put("language", "en");

        StringBuilder textBuilder = new StringBuilder();
        List<String> peopleList = new ArrayList<>();

        // Resolve titles via IllustrationTitles
        IllustrationTitles titles = collection.getIllustrationTitles();
        if (illustration.getTitles() != null) {
            for (String titleId : illustration.getTitles()) {
                if (titleId == null || titleId.isBlank()) {
                    continue;
                }
                if (titles == null) {
                    throw new IllegalStateException(
                            "Cannot resolve illustration title ID '" + titleId
                                    + "': no illustration_titles.csv loaded for collection '"
                                    + collection.getId() + "'");
                }
                String resolvedTitle = titles.getTitleById(titleId);
                if (resolvedTitle == null || resolvedTitle.isBlank()) {
                    throw new IllegalStateException(
                            "Cannot resolve illustration title ID '" + titleId
                                    + "' in collection '" + collection.getId()
                                    + "', book '" + book.getId()
                                    + "', page '" + image.getId() + "'");
                }
                textBuilder.append(resolvedTitle).append(", ");
            }
        }

        // Resolve characters via CharacterNames
        CharacterNames charNames = collection.getCharacterNames();
        if (illustration.getCharacters() != null) {
            for (String charId : illustration.getCharacters()) {
                if (charId == null || charId.isBlank()) {
                    continue;
                }
                if (charNames == null) {
                    throw new IllegalStateException(
                            "Cannot resolve character ID '" + charId
                                    + "': no character_names.csv loaded for collection '"
                                    + collection.getId() + "'");
                }
                CharacterName charName = charNames.getCharacterName(charId);
                if (charName == null) {
                    throw new IllegalStateException(
                            "Cannot resolve character ID '" + charId
                                    + "' in collection '" + collection.getId()
                                    + "', book '" + book.getId()
                                    + "', page '" + image.getId() + "'");
                }
                for (String name : charName.getAllNames()) {
                    textBuilder.append(name).append(", ");
                    if (!peopleList.contains(name)) {
                        peopleList.add(name);
                    }
                }
            }
        }

        // Textual element
        appendIfPresent(textBuilder, illustration.getTextualElement());
        // Architecture, costume, object, landscape, other
        appendIfPresent(textBuilder, illustration.getArchitecture());
        appendIfPresent(textBuilder, illustration.getCostume());
        appendIfPresent(textBuilder, illustration.getObject());
        appendIfPresent(textBuilder, illustration.getLandscape());
        appendIfPresent(textBuilder, illustration.getOther());

        // HTML annotations about this illustration
        HTMLAnnotations htmlAnnotations = collection.getHTMLAnnotations();
        if (htmlAnnotations != null) {
            String htmlAnno = htmlAnnotations.getAnnotation(image.getId());
            if (htmlAnno != null && !htmlAnno.isBlank()) {
                // Strip HTML tags
                textBuilder.append(htmlAnno.replaceAll("<.*?>", " ")).append(" ");
            }
        }

        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "text", "en", textBuilder.toString().trim());
        }

        addArrayField(doc, "people", peopleList);

        return doc;
    }

    // ========== Marginalia ==========

    private void generateMarginaliaFields(ObjectNode doc, Marginalia m, BookCollection collection) {
        doc.put("type", "marginalia");

        if (m.getTopic() != null && !m.getTopic().isBlank()) {
            doc.put("topic", m.getTopic());
        }
        if (m.getHand() != null && !m.getHand().isBlank()) {
            doc.put("hand", m.getHand());
        }
        if (m.getColor() != null && !m.getColor().isBlank()) {
            doc.put("color", m.getColor());
        }

        // Index referenced text (text in the book that is being annotated)
        String baseLang = m.getLanguage() != null ? m.getLanguage() : "en";
        if (m.getReferencedText() != null && !m.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", baseLang, stripTranscriberMarks(m.getReferencedText()));
        }

        // Index otherReader as annotator (overrides page-level reader for this annotation)
        if (m.getOtherReader() != null && !m.getOtherReader().isBlank()) {
            doc.put("annotator", m.getOtherReader());
        }

        List<String> allPeople = new ArrayList<>();
        List<String> allBooks = new ArrayList<>();
        List<String> allLocations = new ArrayList<>();
        List<String> allSymbols = new ArrayList<>();

        ReferenceSheet peopleRef = collection.getPeopleRef();
        ReferenceSheet bookRef = collection.getBooksRef();
        ReferenceSheet locationRef = collection.getLocationsRef();

        String marg_lang_type = "en";

        for (MarginaliaLanguage ml : m.getLanguages()) {
            String langCode = ml.getLang();
            if (langCode == null || langCode.isBlank()) {
                langCode = "en";
            }
            marg_lang_type = langCode;

            if (m.getLanguages().indexOf(ml) == 0) {
                doc.put("language", langCode);
            }

            for (Position pos : ml.getPositions()) {
                // Transcription text
                if (pos.getTexts() != null && !pos.getTexts().isEmpty()) {
                    String joinedText = stripTranscriberMarks(String.join(" ", pos.getTexts()));
                    if (joinedText != null && !joinedText.isBlank()) {
                        LanguageFieldRouter.routeField(doc, "text", langCode, joinedText);
                    }
                }

                // Emphasis
                if (pos.getEmphasis() != null && !pos.getEmphasis().isEmpty()) {
                    StringJoiner emphJoiner = new StringJoiner(" ");
                    for (Underline emph : pos.getEmphasis()) {
                        if (emph.getReferencedText() != null && !emph.getReferencedText().isBlank()) {
                            emphJoiner.add(stripTranscriberMarks(emph.getReferencedText()));
                        }
                    }
                    String emphText = emphJoiner.toString();
                    if (!emphText.isBlank()) {
                        LanguageFieldRouter.routeField(doc, "emphasis", langCode, emphText);
                    }
                }

                // Cross-references (XRef)
                if (pos.getXRefs() != null && !pos.getXRefs().isEmpty()) {
                    for (XRef xref : pos.getXRefs()) {
                        if (xref.person() != null && !xref.person().isBlank()) {
                            LanguageFieldRouter.routeField(doc, "cross_reference", "en", xref.person());
                        }
                        if (xref.title() != null && !xref.title().isBlank()) {
                            LanguageFieldRouter.routeField(doc, "cross_reference", "en", xref.title());
                        }
                        if (xref.text() != null && !xref.text().isBlank()) {
                            String xrefLang = xref.language() != null ? xref.language() : langCode;
                            LanguageFieldRouter.routeField(doc, "cross_reference", xrefLang,
                                    stripTranscriberMarks(xref.text()));
                        }
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

        // Translation always routed to English (translations are into English)
        if (m.getTranslation() != null && !m.getTranslation().isBlank()) {
            LanguageFieldRouter.routeField(doc, "translation", "en", m.getTranslation());
        }

        // Add reference sheet alternates and write arrays
        addArrayField(doc, "people", addRefListAlternates(allPeople, peopleRef));
        addArrayField(doc, "books", addRefListAlternates(allBooks, bookRef));
        addArrayField(doc, "locations", addRefListAlternates(allLocations, locationRef));
        addArrayField(doc, "symbols", allSymbols);
    }

    // ========== Underline ==========

    private void generateUnderlineFields(ObjectNode doc, Underline u) {
        doc.put("type", "underline");

        if (u.getMethod() != null && !u.getMethod().isBlank()) {
            doc.put("method", u.getMethod());
        }
        if (u.getColor() != null && !u.getColor().isBlank()) {
            doc.put("color", u.getColor());
        }

        String lang = u.getLanguage() != null ? u.getLanguage() : "en";
        if (u.getReferencedText() != null && !u.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, stripTranscriberMarks(u.getReferencedText()));
        }
    }

    // ========== Mark ==========

    private void generateMarkFields(ObjectNode doc, Mark mk) {
        doc.put("type", "mark");

        if (mk.getMethod() != null && !mk.getMethod().isBlank()) {
            doc.put("method", mk.getMethod());
        }
        if (mk.getColor() != null && !mk.getColor().isBlank()) {
            doc.put("color", mk.getColor());
        }
        // Mark name goes to mark_name field (NOT hand)
        if (mk.getName() != null && !mk.getName().isBlank()) {
            doc.put("mark_name", mk.getName());
        }

        String lang = mk.getLanguage() != null ? mk.getLanguage() : "en";
        if (mk.getReferencedText() != null && !mk.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, stripTranscriberMarks(mk.getReferencedText()));
        }
    }

    // ========== Symbol ==========

    private void generateSymbolFields(ObjectNode doc, Symbol sym) {
        doc.put("type", "symbol");

        if (sym.getName() != null && !sym.getName().isBlank()) {
            ArrayNode symbolsArray = doc.putArray("symbols");
            symbolsArray.add(sym.getName());
        }

        String lang = sym.getLanguage() != null ? sym.getLanguage() : "en";
        if (sym.getReferencedText() != null && !sym.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, stripTranscriberMarks(sym.getReferencedText()));
        }
    }

    // ========== Drawing ==========

    private void generateDrawingFields(ObjectNode doc, Drawing d, BookCollection collection) {
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

        // Hand from TextEl elements
        if (d.getTexts() != null && !d.getTexts().isEmpty()) {
            String hand = d.getTexts().stream()
                    .map(TextEl::hand)
                    .filter(h -> h != null && !h.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
            if (!hand.isBlank()) {
                doc.put("hand", hand);
            }

            // Text content from TextEl elements
            StringJoiner textJoiner = new StringJoiner(" ");
            for (TextEl textEl : d.getTexts()) {
                String textLang = textEl.language() != null ? textEl.language() : lang;
                String combined = "";
                if (textEl.text() != null && !textEl.text().isBlank()) {
                    combined += textEl.text();
                }
                if (textEl.anchorText() != null && !textEl.anchorText().isBlank()) {
                    combined += " " + textEl.anchorText();
                }
                if (!combined.isBlank()) {
                    LanguageFieldRouter.routeField(doc, "text", textLang, combined.trim());
                }
            }
        }

        // Referenced text
        if (d.getReferencedText() != null && !d.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", lang, stripTranscriberMarks(d.getReferencedText()));
        }

        // Translation routed to English
        if (d.getTranslation() != null && !d.getTranslation().isBlank()) {
            LanguageFieldRouter.routeField(doc, "translation", "en", d.getTranslation());
        }

        // People, books, locations with reference sheet alternates
        List<String> people = addRefListAlternates(d.getPeople(), collection.getPeopleRef());
        List<String> books = addRefListAlternates(d.getBooks(), collection.getBooksRef());
        List<String> locations = addRefListAlternates(d.getLocations(), collection.getLocationsRef());
        addArrayField(doc, "people", people);
        addArrayField(doc, "books", books);
        addArrayField(doc, "locations", locations);
        addArrayField(doc, "symbols", d.getSymbols());
    }

    // ========== Errata ==========

    private void generateErrataFields(ObjectNode doc, Errata e) {
        doc.put("type", "errata");

        String lang = e.getLanguage() != null ? e.getLanguage() : "en";

        StringBuilder textBuilder = new StringBuilder();
        if (e.getReferencedText() != null && !e.getReferencedText().isBlank()) {
            textBuilder.append(stripTranscriberMarks(e.getReferencedText()));
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

    // ========== Numeral ==========

    private void generateNumeralFields(ObjectNode doc, Numeral n) {
        doc.put("type", "numeral");

        String lang = n.getLanguage() != null ? n.getLanguage() : "en";

        // Index BOTH referenced text AND numeral value
        StringBuilder textBuilder = new StringBuilder();
        if (n.getReferencedText() != null && !n.getReferencedText().isBlank()) {
            textBuilder.append(stripTranscriberMarks(n.getReferencedText()));
        }
        if (n.getNumeral() != null && !n.getNumeral().isBlank()) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append(" ");
            }
            textBuilder.append(n.getNumeral());
        }
        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "text", lang, textBuilder.toString());
        }
    }

    // ========== Calculation ==========

    private void generateCalculationFields(ObjectNode doc, Calculation c) {
        doc.put("type", "calculation");

        if (c.getMethod() != null && !c.getMethod().isBlank()) {
            doc.put("method", c.getMethod());
        }

        // Build text from data list and content
        StringBuilder textBuilder = new StringBuilder();
        if (c.getData() != null && !c.getData().isEmpty()) {
            textBuilder.append(String.join(", ", c.getData()));
        }
        if (c.getContent() != null && !c.getContent().isBlank()) {
            if (!textBuilder.isEmpty()) {
                textBuilder.append(" ");
            }
            textBuilder.append(c.getContent());
        }
        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "text", "en", textBuilder.toString());
        }
    }

    // ========== Graph ==========

    private void generateGraphFields(ObjectNode doc, Graph g, BookCollection collection) {
        doc.put("type", "graph");

        if (g.getMethod() != null && !g.getMethod().isBlank()) {
            doc.put("method", g.getMethod());
        }

        String lang = g.getLanguage() != null ? g.getLanguage() : "en";

        List<String> allPeople = new ArrayList<>();
        List<String> allBooks = new ArrayList<>();
        List<String> allLocations = new ArrayList<>();
        List<String> allSymbols = new ArrayList<>();
        List<String> allHands = new ArrayList<>();

        // Process GraphText elements
        for (GraphText text : g.getGraphTexts()) {
            // Hand from notes
            for (GraphNote note : text.getNotes()) {
                if (note.hand() != null && !note.hand().isBlank() && !allHands.contains(note.hand())) {
                    allHands.add(note.hand());
                }
            }

            // People, books, locations, symbols
            for (String p : text.getPeople()) {
                if (p != null && !p.isBlank() && !allPeople.contains(p)) {
                    allPeople.add(p);
                }
            }
            for (String b : text.getBooks()) {
                if (b != null && !b.isBlank() && !allBooks.contains(b)) {
                    allBooks.add(b);
                }
            }
            for (String l : text.getLocations()) {
                if (l != null && !l.isBlank() && !allLocations.contains(l)) {
                    allLocations.add(l);
                }
            }
            for (String s : text.getSymbols()) {
                if (s != null && !s.isBlank() && !allSymbols.contains(s)) {
                    allSymbols.add(s);
                }
            }

            // Translations → English
            if (text.getTranslations() != null && !text.getTranslations().isEmpty()) {
                String translations = String.join(", ", text.getTranslations());
                if (!translations.isBlank()) {
                    LanguageFieldRouter.routeField(doc, "text", "en", translations);
                }
            }
        }

        // Set hand
        if (!allHands.isEmpty()) {
            doc.put("hand", String.join(", ", allHands));
        }

        // Process GraphNode elements: text + content + person
        if (g.getNodes() != null && !g.getNodes().isEmpty()) {
            StringJoiner nodeTextJoiner = new StringJoiner(" ");
            for (GraphNode node : g.getNodes()) {
                if (node.text() != null && !node.text().isBlank()) {
                    nodeTextJoiner.add(node.text());
                }
                if (node.content() != null && !node.content().isBlank()) {
                    nodeTextJoiner.add(node.content());
                }
                if (node.person() != null && !node.person().isBlank() && !allPeople.contains(node.person())) {
                    allPeople.add(node.person());
                }
            }
            String nodeText = nodeTextJoiner.toString();
            if (!nodeText.isBlank()) {
                LanguageFieldRouter.routeField(doc, "text", lang, nodeText);
            }
        }

        // Write arrays with reference sheet alternates
        addArrayField(doc, "people", addRefListAlternates(allPeople, collection.getPeopleRef()));
        addArrayField(doc, "books", addRefListAlternates(allBooks, collection.getBooksRef()));
        addArrayField(doc, "locations", addRefListAlternates(allLocations, collection.getLocationsRef()));
        addArrayField(doc, "symbols", allSymbols);
    }

    // ========== Table ==========

    private void generateTableFields(ObjectNode doc, Table t, BookCollection collection) {
        doc.put("type", "table");

        String lang = t.getLanguage() != null ? t.getLanguage() : "en";

        // Hand from TextEl elements
        if (t.getTexts() != null && !t.getTexts().isEmpty()) {
            String hand = t.getTexts().stream()
                    .map(TextEl::hand)
                    .filter(h -> h != null && !h.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
            if (!hand.isBlank()) {
                doc.put("hand", hand);
            }

            // Text from TextEl elements (anchorText + text)
            StringJoiner textJoiner = new StringJoiner(", ");
            for (TextEl txt : t.getTexts()) {
                StringBuilder sb = new StringBuilder();
                if (txt.anchorText() != null && !txt.anchorText().isBlank()) {
                    sb.append(txt.anchorText());
                }
                if (txt.text() != null && !txt.text().isBlank()) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(txt.text());
                }
                if (!sb.isEmpty()) {
                    textJoiner.add(sb.toString());
                }
            }
            String textsContent = textJoiner.toString();
            if (!textsContent.isBlank()) {
                LanguageFieldRouter.routeField(doc, "text", lang, textsContent);
            }
        }

        // Translation → English
        if (t.getTranslation() != null && !t.getTranslation().isBlank()) {
            LanguageFieldRouter.routeField(doc, "translation", "en", t.getTranslation());
        }

        // Aggregated info
        if (t.getAggregatedInfo() != null && !t.getAggregatedInfo().isBlank()) {
            LanguageFieldRouter.routeField(doc, "text", "en", t.getAggregatedInfo());
        }

        // Table cells (anchorData + anchorText + content)
        if (t.getCells() != null && !t.getCells().isEmpty()) {
            StringJoiner cellJoiner = new StringJoiner(" ");
            for (TableCell cell : t.getCells()) {
                StringBuilder sb = new StringBuilder();
                if (cell.anchorData() != null && !cell.anchorData().isBlank()) {
                    sb.append(cell.anchorData());
                }
                if (cell.anchorText() != null && !cell.anchorText().isBlank()) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(cell.anchorText());
                }
                if (cell.content() != null && !cell.content().isBlank()) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(cell.content());
                }
                if (!sb.isEmpty()) {
                    cellJoiner.add(sb.toString());
                }
            }
            String cellText = cellJoiner.toString();
            if (!cellText.isBlank()) {
                LanguageFieldRouter.routeField(doc, "text", "en", cellText);
            }
        }

        // People, books, locations, symbols with reference sheet alternates
        addArrayField(doc, "people", addRefListAlternates(t.getPeople(), collection.getPeopleRef()));
        addArrayField(doc, "books", addRefListAlternates(t.getBooks(), collection.getBooksRef()));
        addArrayField(doc, "locations", addRefListAlternates(t.getLocations(), collection.getLocationsRef()));
        addArrayField(doc, "symbols", t.getSymbols());
    }

    // ========== Utility methods ==========

    /**
     * Adds a string array field to the document if the list is non-empty.
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

    /**
     * Expands a list of reference values with alternates from a reference sheet.
     * Returns a new list containing original values plus all alternates.
     */
    private List<String> addRefListAlternates(List<String> values, ReferenceSheet reference) {
        if (values == null || values.isEmpty()) {
            return values != null ? values : List.of();
        }
        if (reference == null) {
            return values;
        }
        List<String> expanded = new ArrayList<>(values);
        for (String v : values) {
            if (v != null && reference.hasAlternates(v)) {
                for (String alt : reference.getAlternates(v)) {
                    if (alt != null && !alt.isBlank() && !expanded.contains(alt)) {
                        expanded.add(alt);
                    }
                }
            }
        }
        return expanded;
    }

    /**
     * Strips transcriber marks ([ and ]) from text.
     */
    static String stripTranscriberMarks(String s) {
        if (s == null) {
            return null;
        }
        return s.replace("[", "").replace("]", "");
    }

    /**
     * Appends a value to a StringBuilder if it is non-null and non-blank, followed by ", ".
     */
    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(", ");
        }
    }

    /**
     * Checks if a Jackson ArrayNode already contains a specific string value.
     */
    private boolean arrayContains(ArrayNode array, String value) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).asText().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
