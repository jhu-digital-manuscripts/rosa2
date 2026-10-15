package rosa.archive.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import rosa.archive.core.ArchiveStore;
import rosa.archive.core.util.IllustrationRefs;
import rosa.archive.model.BiblioData;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.HTMLAnnotations;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
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
import rosa.archive.model.aor.TableHeader;
import rosa.archive.model.aor.TextEl;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Generates Opensearch bulk ingest NDJSON files from archive data.
 *
 * <p>Produces files in the Opensearch Bulk API format (alternating action and document lines)
 * with one output file per collection. Documents are generated for the {@code manifest}
 * and {@code canvas} indexes. All annotation data targeting a canvas is merged into the
 * canvas document with per-type independently searchable fields.
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
                    String manifestId = collection.getId() + "." + book.getId();
                    writeActionDocumentPair(writer, "manifest", manifestId, manifestDoc);

                    // Process each image (canvas) with merged annotations
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

                        // Generate canvas document with all annotations merged in
                        ObjectNode canvasDoc = generateCanvasDocument(
                                collection, book, image, i, transcriptionPages);
                        String canvasId = collection.getId() + "." + stripExtension(image.getId());
                        writeActionDocumentPair(writer, "canvas", canvasId, canvasDoc);
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

    // ========== Collection ID helpers ==========

    /**
     * Computes ancestor collection IDs using just the collection (no store needed for recursion
     * since parent IDs are already available on the collection object).
     */
    private List<String> getAncestorCollectionIds(BookCollection collection) {
        List<String> ids = new ArrayList<>();
        ids.add(collection.getId());
        String[] parents = collection.getParentCollections();
        if (parents != null) {
            for (String parent : parents) {
                if (parent != null && !parent.isBlank() && !ids.contains(parent)) {
                    ids.add(parent);
                }
            }
        }
        return ids;
    }

    // ========== Manifest ==========

    /**
     * Generates a manifest document for the {@code manifest} index.
     */
    public ObjectNode generateManifestDocument(BookCollection collection, Book book) {
        ObjectNode doc = mapper.createObjectNode();

        doc.put("id", collection.getId() + "." + book.getId());

        // Collection IDs: immediate + all ancestors
        List<String> collectionIds = getAncestorCollectionIds(collection);
        ArrayNode collIdArray = doc.putArray("collection_id");
        for (String cid : collectionIds) {
            collIdArray.add(cid);
        }

        BookMetadata metadata = book.getBookMetadata();
        BiblioData biblio = book.getBiblioData("en");

        // Label: the common name of the book
        String label = biblio != null ? biblio.getCommonName() : null;
        doc.put("label", bookLabel(book));

        // Title: combine common name + BookText titles
        StringBuilder titleBuilder = new StringBuilder();
        if (label != null && !label.isBlank()) {
            titleBuilder.append(label);
        }
        if (metadata != null && metadata.getBookTexts() != null) {
            for (BookText bt : metadata.getBookTexts()) {
                if (bt.getTitle() != null && !bt.getTitle().isBlank()) {
                    if (!titleBuilder.isEmpty()) {
                        titleBuilder.append(" ");
                    }
                    titleBuilder.append(bt.getTitle());
                }
            }
        }
        doc.put("title", !titleBuilder.isEmpty() ? titleBuilder.toString() : book.getId());

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

        doc.put("description", "");

        // Description: use English description if available
        rosa.archive.model.BookDescription bookDesc = book.getDescription("en");
        if (bookDesc != null) {
            String descText = bookDesc.getFullText();
            if (descText != null && !descText.isBlank()) {
                doc.put("description", descText);
            }
        }

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

        int numIllustrations = metadata != null ? metadata.getNumberOfIllustrations() : 0;
        doc.put("num_illustrations", numIllustrations >= 0 ? numIllustrations : 0);

        String material = biblio != null ? biblio.getMaterial() : null;
        doc.put("material", material != null ? material : "");

        boolean hasTranscription = book.getTranscription() != null;
        doc.put("has_transcription", hasTranscription);

        // Thumbnail: first 3 non-missing, non-front-matter, non-binding pages as objects
        // Each object has iiif_image_id (for IIIF Image API requests) and page_num (for canvas URI construction)
        ArrayNode thumbnailArray = doc.putArray("thumbnail");
        ImageList imageList = book.getImages();
        if (imageList != null) {
            // Determine if cropped images are available for this book
            ImageList croppedImages = book.getCroppedImages();
            boolean hasCropped = croppedImages != null && !croppedImages.getImages().isEmpty();

            int count = 0;
            List<BookImage> images = imageList.getImages();
            for (int i = 0; i < images.size(); i++) {
                if (count >= 3) break;
                BookImage img = images.get(i);
                if (img.isMissing()) continue;
                BookImageLocation loc = img.getLocation();
                if (loc == BookImageLocation.FRONT_MATTER || loc == BookImageLocation.BINDING) continue;

                // Build iiif_image_id: collection/book/[cropped/]image_id_no_ext
                String imageIdNoExt = stripExtension(img.getId());
                String iiifImageId = collection.getId() + "/" + book.getId()
                        + "/" + (hasCropped ? "cropped/" : "") + imageIdNoExt;

                ObjectNode thumbObj = mapper.createObjectNode();
                thumbObj.put("iiif_image_id", iiifImageId);
                thumbObj.put("page_num", i);
                thumbnailArray.add(thumbObj);
                count++;
            }
        }

        // Logo
        String logo;
        if ("aor".equals(collection.getId())) {
            // AOR: first reader name, lowercased, spaces to underscores, + .jpg
            logo = collection.getId() + ".jpg"; // default fallback
            if (biblio != null && biblio.getReaders() != null && biblio.getReaders().length > 0) {
                String readerName = biblio.getReaders()[0].getName();
                if (readerName != null && !readerName.isBlank()) {
                    logo = readerName.toLowerCase().replace(' ', '_') + ".jpg";
                }
            }
        } else {
            logo = collection.getId() + ".jpg";
        }
        doc.put("logo", logo);

        normalizeWhitespaceValues(doc);
        return doc;
    }

    /**
     * Returns the display label of a book: its English common name, or the book ID when
     * there is none. Used for the manifest {@code label} and the canvas {@code manifest_label}.
     */
    static String bookLabel(Book book) {
        BiblioData biblio = book.getBiblioData("en");
        String label = biblio != null ? biblio.getCommonName() : null;
        return label != null ? label : book.getId();
    }

    // ========== Canvas ==========

    /**
     * Generates a canvas document for the {@code canvas} index with all annotations merged in.
     * All annotations targeting this canvas are accumulated into per-type fields.
     */
    public ObjectNode generateCanvasDocument(BookCollection collection, Book book,
                                              BookImage image, int pageNum,
                                              Map<String, String> transcriptionPages) {
        ObjectNode doc = mapper.createObjectNode();

        String canvasId = collection.getId() + "." + stripExtension(image.getId());
        doc.put("id", canvasId);
        doc.put("manifest_id", collection.getId() + "." + book.getId());
        doc.put("manifest_label", bookLabel(book));

        // Collection IDs: immediate + all ancestors
        List<String> collectionIds = getAncestorCollectionIds(collection);
        ArrayNode collIdArray = doc.putArray("collection_id");
        for (String cid : collectionIds) {
            collIdArray.add(cid);
        }

        // IIIF Image ID: collection/book/[cropped/]image_id_no_ext
        ImageList croppedImages = book.getCroppedImages();
        boolean hasCropped = croppedImages != null && !croppedImages.getImages().isEmpty();
        String imageIdNoExt = stripExtension(image.getId());
        String iiifImageId = collection.getId() + "/" + book.getId()
                + "/" + (hasCropped ? "cropped/" : "") + imageIdNoExt;
        doc.put("iiif_image_id", iiifImageId);

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
        doc.put("page_num", pageNum);

        // ===== Merge all annotations into this canvas document =====

        // Determine default language: prefer the text language from the page's underlines
        // (which carry the printed book's text language), then fall back to BookText language,
        // then to "en".
        BookMetadata metadata2 = book.getBookMetadata();

        String defaultLang = "en";
        if (metadata2 != null && metadata2.getBookTexts() != null && !metadata2.getBookTexts().isEmpty()) {
            String textLang = metadata2.getBookTexts().get(0).getLanguage();
            if (textLang != null && !textLang.isBlank()) {
                defaultLang = textLang.strip().toLowerCase();
            }
        }
        // Override with underline language from the page if available, since underlines
        // explicitly carry the language of the printed text
        if (ap != null && !ap.getUnderlines().isEmpty()) {
            String ulLang = ap.getUnderlines().get(0).getLanguage();
            if (ulLang != null && !ulLang.isBlank()) {
                defaultLang = ulLang.strip().toLowerCase();
            }
        }

        // Keyword array accumulators
        Set<String> allPeople = new HashSet<>();
        Set<String> allBooks = new HashSet<>();
        Set<String> allLocations = new HashSet<>();
        Set<String> allMethods = new HashSet<>();
        Set<String> allHands = new HashSet<>();
        Set<String> allAnnotators = new HashSet<>();
        Set<String> allLanguages = new HashSet<>();
        Set<String> allMargLanguages = new HashSet<>();
        Set<String> allTopics = new HashSet<>();
        Set<String> allCharNames = new HashSet<>();

        // AoR annotations for this page
        if (ap != null) {
            String reader = ap.getReader();
            if (reader != null && !reader.isBlank()) {
                allAnnotators.add(reader);
            }

            for (Annotation annotation : ap.getAnnotations()) {
                String lang = annotation.getLanguage();
                if (lang != null && !lang.isBlank()) {
                    allLanguages.add(lang.strip().toLowerCase());
                }

                switch (annotation) {
                    case Marginalia m -> indexMarginalia(doc, m, collection, defaultLang,
                            allPeople, allBooks, allLocations,
                            allMethods, allHands, allAnnotators, allLanguages,
                            allMargLanguages, allTopics);
                    case Underline u -> indexUnderline(doc, u, defaultLang, allMethods);
                    case Mark mk -> indexMark(doc, mk, defaultLang, allMethods);
                    case Symbol sym -> indexSymbol(doc, sym, defaultLang);
                    case Drawing d -> indexDrawing(doc, d, collection, defaultLang,
                            allPeople, allBooks, allLocations, allHands, allMethods);
                    case Errata e -> indexErrata(doc, e, defaultLang);
                    case Numeral n -> indexNumeral(doc, n, defaultLang);
                    case Calculation c -> indexCalculation(doc, c, allMethods);
                    case Graph g -> indexGraph(doc, g, collection, defaultLang,
                            allPeople, allBooks, allLocations, allHands);
                    case Table t -> indexTable(doc, t, collection, defaultLang,
                            allPeople, allBooks, allLocations, allHands);
                    default -> {
                        // Unknown annotation type: index referenced text into marginalia field
                        if (annotation.getReferencedText() != null && !annotation.getReferencedText().isBlank()) {
                            String defLang = langOr(lang, defaultLang);
                            LanguageFieldRouter.routeField(doc, "marginalia", defLang,
                                    stripTranscriberMarks(annotation.getReferencedText()));
                        }
                    }
                }
            }
        }

        // Transcription for this page
        if (!transcriptionPages.isEmpty()) {
            String normalizedPage = TranscriptionSplitter.normalizePageName(image.getName());
            String pageFragment = transcriptionPages.get(normalizedPage);
            if (pageFragment != null && !pageFragment.isBlank()) {
                indexTranscription(book.getId(), doc, pageFragment);
            }
        }

        // Illustration tagging for this page
        IllustrationTagging illustrationTagging = book.getIllustrationTagging();
        if (illustrationTagging != null) {
            List<Integer> illustrationIndices = illustrationTagging.findImageIndices(book, image.getId());
            for (int idx : illustrationIndices) {
                Illustration illustration = illustrationTagging.getIllustrationData(idx);
                indexIllustration(doc, collection, book, image, illustration, allCharNames);
            }
        }

        // Write keyword arrays (only if non-empty)
        addSetField(doc, "people", allPeople);
        addSetField(doc, "books", allBooks);
        addSetField(doc, "locations", allLocations);
        addSetField(doc, "method", allMethods);
        addSetField(doc, "hand", allHands);
        addSetField(doc, "annotator", allAnnotators);
        addSetField(doc, "language", allLanguages);
        addSetField(doc, "marginalia_language", allMargLanguages);
        addSetField(doc, "topic", allTopics);
        addSetField(doc, "char_name", allCharNames);

        normalizeWhitespaceValues(doc);
        return doc;
    }

    // ========== Annotation type indexers ==========

    private void indexMarginalia(ObjectNode doc, Marginalia m, BookCollection collection,
                                  String defaultLang,
                                  Set<String> allPeople, Set<String> allBooks,
                                  Set<String> allLocations,
                                  Set<String> allMethods, Set<String> allHands,
                                  Set<String> allAnnotators, Set<String> allLanguages,
                                  Set<String> allMargLanguages, Set<String> allTopics) {
        if (m.getTopic() != null && !m.getTopic().isBlank()) {
            allTopics.add(m.getTopic());
        }
        if (m.getHand() != null && !m.getHand().isBlank()) {
            allHands.add(m.getHand());
        }
        if (m.getOtherReader() != null && !m.getOtherReader().isBlank()) {
            allAnnotators.add(m.getOtherReader());
        }
        if (m.getMethod() != null && !m.getMethod().isBlank()) {
            allMethods.add(m.getMethod().toLowerCase());
        }

        // Referenced text (anchor_text attribute from XML) → anchor_text field
        String baseLang = langOr(m.getLanguage(), defaultLang);
        if (m.getReferencedText() != null && !m.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "anchor_text", baseLang,
                    stripTranscriberMarks(m.getReferencedText()));
        }

        ReferenceSheet peopleRef = collection.getPeopleRef();
        ReferenceSheet bookRef = collection.getBooksRef();
        ReferenceSheet locationRef = collection.getLocationsRef();

        for (MarginaliaLanguage ml : m.getLanguages()) {
            String langCode = ml.getLang();
            if (langCode == null || langCode.isBlank()) {
                langCode = defaultLang;
            }
            langCode = langCode.strip().toLowerCase();
            allLanguages.add(langCode);
            allMargLanguages.add(langCode);

            for (Position pos : ml.getPositions()) {
                // Transcription text
                if (pos.getTexts() != null && !pos.getTexts().isEmpty()) {
                    String joinedText = stripTranscriberMarks(String.join(" ", pos.getTexts()));
                    if (!joinedText.isBlank()) {
                        LanguageFieldRouter.routeField(doc, "marginalia", langCode, joinedText);
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
                            String xrefLang = langOr(xref.language(), langCode);
                            LanguageFieldRouter.routeField(doc, "cross_reference", xrefLang,
                                    stripTranscriberMarks(xref.text()));
                        }
                    }
                }

                // Symbols named in the marginal text
                addSymbolKeywords(doc, pos.getSymbols());

                // Collect people, books, locations
                if (pos.getPeople() != null) {
                    addRefAlternates(pos.getPeople(), peopleRef, allPeople);
                }
                if (pos.getBooks() != null) {
                    addRefAlternates(pos.getBooks(), bookRef, allBooks);
                }
                if (pos.getLocations() != null) {
                    addRefAlternates(pos.getLocations(), locationRef, allLocations);
                }
            }
        }

        // Translation always routed to English
        if (m.getTranslation() != null && !m.getTranslation().isBlank()) {
            LanguageFieldRouter.routeField(doc, "marginalia", "en", m.getTranslation());
        }
    }

    private void indexUnderline(ObjectNode doc, Underline u, String defaultLang, Set<String> allMethods) {
        if (u.getMethod() != null && !u.getMethod().isBlank()) {
            allMethods.add(u.getMethod().toLowerCase());
        }

        String lang = langOr(u.getLanguage(), defaultLang);
        if (u.getReferencedText() != null && !u.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "underline", lang,
                    stripTranscriberMarks(u.getReferencedText()));
        }
    }

    private void indexMark(ObjectNode doc, Mark mk, String defaultLang, Set<String> allMethods) {
        if (mk.getMethod() != null && !mk.getMethod().isBlank()) {
            allMethods.add(mk.getMethod().toLowerCase());
        }

        // Mark name goes to mark.keyword sub-field
        if (mk.getName() != null && !mk.getName().isBlank()) {
            appendKeywordSubField(doc, "mark", mk.getName());
        }

        // Mark referenced text goes to mark.<lang> sub-field
        String lang = langOr(mk.getLanguage(), defaultLang);
        if (mk.getReferencedText() != null && !mk.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "mark", lang,
                    stripTranscriberMarks(mk.getReferencedText()));
        }
    }

    private void indexSymbol(ObjectNode doc, Symbol sym, String defaultLang) {
        // Symbol name goes to symbol.keyword sub-field
        if (sym.getName() != null && !sym.getName().isBlank()) {
            appendKeywordSubField(doc, "symbol", sym.getName());
        }

        // Symbol referenced text goes to symbol.<lang> sub-field
        String lang = langOr(sym.getLanguage(), defaultLang);
        if (sym.getReferencedText() != null && !sym.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "symbol", lang,
                    stripTranscriberMarks(sym.getReferencedText()));
        }
    }

    private void indexDrawing(ObjectNode doc, Drawing d, BookCollection collection,
                              String defaultLang,
                              Set<String> allPeople, Set<String> allBooks,
                              Set<String> allLocations,
                              Set<String> allHands, Set<String> allMethods) {
        // Drawing type goes to drawing.keyword sub-field
        if (d.getType() != null && !d.getType().isBlank()) {
            appendKeywordSubField(doc, "drawing", d.getType());
        }

        if (d.getMethod() != null && !d.getMethod().isBlank()) {
            allMethods.add(d.getMethod().toLowerCase());
        }

        String lang = langOr(d.getLanguage(), defaultLang);

        // Hand from TextEl elements
        if (d.getTexts() != null && !d.getTexts().isEmpty()) {
            for (TextEl textEl : d.getTexts()) {
                if (textEl.hand() != null && !textEl.hand().isBlank()) {
                    allHands.add(textEl.hand());
                }
                // Text content from TextEl elements
                String textLang = langOr(textEl.language(), lang);
                StringBuilder combined = new StringBuilder();
                if (textEl.text() != null && !textEl.text().isBlank()) {
                    combined.append(textEl.text());
                }
                if (textEl.anchorText() != null && !textEl.anchorText().isBlank()) {
                    if (!combined.isEmpty()) combined.append(" ");
                    combined.append(textEl.anchorText());
                    LanguageFieldRouter.routeField(doc, "anchor_text", textLang, textEl.anchorText());
                }
                if (!combined.isEmpty()) {
                    LanguageFieldRouter.routeField(doc, "drawing", textLang, combined.toString());
                }
            }
        }

        // Referenced text
        if (d.getReferencedText() != null && !d.getReferencedText().isBlank()) {
            LanguageFieldRouter.routeField(doc, "drawing", lang,
                    stripTranscriberMarks(d.getReferencedText()));
        }

        // Translation routed to English
        if (d.getTranslation() != null && !d.getTranslation().isBlank()) {
            LanguageFieldRouter.routeField(doc, "translation", "en", d.getTranslation());
        }

        // People, books, locations with reference sheet alternates
        addRefAlternates(d.getPeople(), collection.getPeopleRef(), allPeople);
        addRefAlternates(d.getBooks(), collection.getBooksRef(), allBooks);
        addRefAlternates(d.getLocations(), collection.getLocationsRef(), allLocations);

        // Symbols named in the drawing's text
        addSymbolKeywords(doc, d.getSymbols());
    }

    private void indexErrata(ObjectNode doc, Errata e, String defaultLang) {
        String lang = langOr(e.getLanguage(), defaultLang);

        StringBuilder textBuilder = new StringBuilder();
        if (e.getReferencedText() != null && !e.getReferencedText().isBlank()) {
            textBuilder.append(stripTranscriberMarks(e.getReferencedText()));
        }
        if (e.getAmendedText() != null && !e.getAmendedText().isBlank()) {
            if (!textBuilder.isEmpty()) textBuilder.append(" ");
            textBuilder.append(e.getAmendedText());
        }
        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "errata", lang, textBuilder.toString());
        }
    }

    private void indexNumeral(ObjectNode doc, Numeral n, String defaultLang) {
        String lang = langOr(n.getLanguage(), defaultLang);

        StringBuilder textBuilder = new StringBuilder();
        if (n.getReferencedText() != null && !n.getReferencedText().isBlank()) {
            textBuilder.append(stripTranscriberMarks(n.getReferencedText()));
        }
        if (n.getNumeral() != null && !n.getNumeral().isBlank()) {
            if (!textBuilder.isEmpty()) textBuilder.append(" ");
            textBuilder.append(n.getNumeral());
        }
        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "numeral", lang, textBuilder.toString());
        }
    }

    private void indexCalculation(ObjectNode doc, Calculation c, Set<String> allMethods) {
        // Calculation type goes to calculation.keyword sub-field
        if (c.getType() != null && !c.getType().isBlank()) {
            appendKeywordSubField(doc, "calculation", c.getType());
        }

        if (c.getMethod() != null && !c.getMethod().isBlank()) {
            allMethods.add(c.getMethod().toLowerCase());
        }

        StringBuilder textBuilder = new StringBuilder();
        if (c.getData() != null && !c.getData().isEmpty()) {
            textBuilder.append(String.join(", ", c.getData()));
        }
        if (c.getContent() != null && !c.getContent().isBlank()) {
            if (!textBuilder.isEmpty()) textBuilder.append(" ");
            textBuilder.append(c.getContent());
        }
        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "calculation", "en", textBuilder.toString());
        }
    }

    private void indexGraph(ObjectNode doc, Graph g, BookCollection collection,
                            String defaultLang,
                            Set<String> allPeople, Set<String> allBooks,
                            Set<String> allLocations,
                            Set<String> allHands) {
        // Graph type goes to graph.keyword sub-field
        if (g.getType() != null && !g.getType().isBlank()) {
            appendKeywordSubField(doc, "graph", g.getType());
        }

        String lang = langOr(g.getLanguage(), defaultLang);

        // Process GraphText elements
        for (GraphText text : g.getGraphTexts()) {
            // Notes: hand, note text in its own language, and anchor text
            for (GraphNote note : text.getNotes()) {
                if (note.hand() != null && !note.hand().isBlank()) {
                    allHands.add(note.hand());
                }
                String noteLang = langOr(note.language(), lang);
                if (note.content() != null && !note.content().isBlank()) {
                    LanguageFieldRouter.routeField(doc, "graph", noteLang,
                            stripTranscriberMarks(note.content()));
                }
                if (note.anchorText() != null && !note.anchorText().isBlank()) {
                    LanguageFieldRouter.routeField(doc, "anchor_text", lang,
                            stripTranscriberMarks(note.anchorText()));
                }
            }

            // People, books, locations, symbols
            addRefAlternates(text.getPeople(), collection.getPeopleRef(), allPeople);
            addRefAlternates(text.getBooks(), collection.getBooksRef(), allBooks);
            addRefAlternates(text.getLocations(), collection.getLocationsRef(), allLocations);
            addSymbolKeywords(doc, text.getSymbols());

            // Translations → routed to English in graph field
            if (text.getTranslations() != null && !text.getTranslations().isEmpty()) {
                String translations = String.join(", ", text.getTranslations());
                if (!translations.isBlank()) {
                    LanguageFieldRouter.routeField(doc, "graph", "en", translations);
                }
            }
        }

        // Process GraphNode elements: text + content + person
        if (g.getNodes() != null && !g.getNodes().isEmpty()) {
            StringJoiner nodeTextJoiner = new StringJoiner(" ");
            for (GraphNode node : g.getNodes()) {
                // A node's text is its anchor text in the printed book
                if (node.text() != null && !node.text().isBlank()) {
                    nodeTextJoiner.add(node.text());
                    LanguageFieldRouter.routeField(doc, "anchor_text", lang,
                            stripTranscriberMarks(node.text()));
                }
                if (node.content() != null && !node.content().isBlank()) {
                    nodeTextJoiner.add(node.content());
                }
                if (node.person() != null && !node.person().isBlank()) {
                    allPeople.add(node.person());
                }
            }
            String nodeText = nodeTextJoiner.toString();
            if (!nodeText.isBlank()) {
                LanguageFieldRouter.routeField(doc, "graph", lang, nodeText);
            }
        }
    }

    private void indexTable(ObjectNode doc, Table t, BookCollection collection,
                            String defaultLang,
                            Set<String> allPeople, Set<String> allBooks,
                            Set<String> allLocations,
                            Set<String> allHands) {
        // Table type goes to table.keyword sub-field
        if (t.getType() != null && !t.getType().isBlank()) {
            appendKeywordSubField(doc, "table", t.getType());
        }

        String lang = langOr(t.getLanguage(), defaultLang);

        // Hand from TextEl elements
        if (t.getTexts() != null && !t.getTexts().isEmpty()) {
            for (TextEl txt : t.getTexts()) {
                if (txt.hand() != null && !txt.hand().isBlank()) {
                    allHands.add(txt.hand());
                }
            }

            // Text from TextEl elements, each in its own language (anchorText + text)
            for (TextEl txt : t.getTexts()) {
                String textLang = langOr(txt.language(), lang);
                StringBuilder sb = new StringBuilder();
                if (txt.anchorText() != null && !txt.anchorText().isBlank()) {
                    sb.append(txt.anchorText());
                    LanguageFieldRouter.routeField(doc, "anchor_text", lang, txt.anchorText());
                }
                if (txt.text() != null && !txt.text().isBlank()) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(stripTranscriberMarks(txt.text()));
                }
                if (!sb.isEmpty()) {
                    LanguageFieldRouter.routeField(doc, "table", textLang, sb.toString());
                }
            }
        }

        // Column headers (label + content)
        if (t.getColHeaders() != null) {
            StringJoiner headerJoiner = new StringJoiner(" ");
            for (TableHeader th : t.getColHeaders()) {
                if (th.label() != null && !th.label().isBlank()) headerJoiner.add(th.label());
                if (th.content() != null && !th.content().isBlank()) headerJoiner.add(th.content());
            }
            if (headerJoiner.length() > 0) {
                LanguageFieldRouter.routeField(doc, "table", "en", headerJoiner.toString());
            }
        }

        // Translation → English
        if (t.getTranslation() != null && !t.getTranslation().isBlank()) {
            LanguageFieldRouter.routeField(doc, "translation", "en", t.getTranslation());
        }

        // Aggregated info
        if (t.getAggregatedInfo() != null && !t.getAggregatedInfo().isBlank()) {
            LanguageFieldRouter.routeField(doc, "table", "en", t.getAggregatedInfo());
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
                LanguageFieldRouter.routeField(doc, "table", "en", cellText);
            }
        }

        // People, books, locations with reference sheet alternates
        addRefAlternates(t.getPeople(), collection.getPeopleRef(), allPeople);
        addRefAlternates(t.getBooks(), collection.getBooksRef(), allBooks);
        addRefAlternates(t.getLocations(), collection.getLocationsRef(), allLocations);

        // Symbols named in the table's text
        addSymbolKeywords(doc, t.getSymbols());
    }

    // ========== Transcription ==========

    private void indexTranscription(String name, ObjectNode doc, String content) {
        TranscriptionXmlExtractor.Result extracted = TranscriptionXmlExtractor.extract(name, content);

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
            LanguageFieldRouter.routeField(doc, "transcription", "ofr", ofrText.toString().trim());
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
            LanguageFieldRouter.routeField(doc, "transcription", "en", enText.toString().trim());
        }
    }

    // ========== Illustration ==========

    private void indexIllustration(ObjectNode doc, BookCollection collection, Book book,
                                    BookImage image, Illustration illustration,
                                    Set<String> allCharNames) {
        StringBuilder textBuilder = new StringBuilder();

        // Resolve titles via IllustrationTitles
        for (String title : IllustrationRefs.resolveTitles(collection, book, illustration)) {
            textBuilder.append(title).append(", ");
        }

        // Resolve characters via CharacterNames, indexing every name variant
        for (String name : IllustrationRefs.resolveAllCharacterNames(collection, book, illustration)) {
            textBuilder.append(name).append(", ");
            allCharNames.add(name);
        }

        // Textual element, architecture, costume, object, landscape, other
        appendIfPresent(textBuilder, illustration.getTextualElement());
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
                textBuilder.append(htmlAnno.replaceAll("<.*?>", " ")).append(" ");
            }
        }

        if (!textBuilder.isEmpty()) {
            LanguageFieldRouter.routeField(doc, "illustration", "en", textBuilder.toString().trim());
        }
    }

    // ========== Utility methods ==========

    /**
     * Adds a keyword value to the dot-notation keyword sub-field of a multi-field.
     * The dot notation {@code fieldName.keyword} maps to the keyword sub-field in the index.
     *
     * <p>Values are stored as an array of distinct values, in first-seen order. Each array
     * element is indexed as its own term, so an exact match on any one value (e.g.
     * {@code drawing.keyword: pyramid}) finds a page that has several drawings. Joining the
     * values into one string would produce a single term that matches none of them.
     */
    private void appendKeywordSubField(ObjectNode doc, String fieldName, String value) {
        if (value == null || value.isBlank()) return;
        String key = fieldName + ".keyword";
        ArrayNode array = doc.get(key) instanceof ArrayNode existing ? existing : doc.putArray(key);
        for (JsonNode v : array) {
            if (v.asText().equals(value)) return;
        }
        array.add(value);
    }

    /**
     * Returns a lowercased, trimmed language code, or the fallback when the code is null or
     * blank. XML readers return "" for a missing attribute, so a null check alone would route
     * every annotation without a language to {@code .en} instead of the page's language.
     */
    static String langOr(String code, String fallback) {
        return code == null || code.isBlank() ? fallback : code.strip().toLowerCase();
    }

    /**
     * Adds symbols named within an annotation's text (e.g. {@code <symbol_in_text>}) to
     * {@code symbol.keyword}, alongside the names of symbol annotations, so that an exact
     * symbol search finds both.
     */
    private void addSymbolKeywords(ObjectNode doc, List<String> symbols) {
        if (symbols == null) return;
        for (String sym : symbols) {
            appendKeywordSubField(doc, "symbol", sym);
        }
    }

    /**
     * Adds a Set of values as a keyword array field on the document.
     */
    private void addSetField(ObjectNode doc, String fieldName, Set<String> values) {
        if (values == null || values.isEmpty()) return;
        ArrayNode array = doc.putArray(fieldName);
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                array.add(value);
            }
        }
    }

    /**
     * Expands a list of reference values with alternates from a reference sheet,
     * adding all results to the target set.
     * 
     * <p>Values can be either:
     * <ul>
     *   <li>Standard names that match a key in the reference sheet</li>
     *   <li>ISNI identifiers (format: "0000 0000 0000 0000") which are resolved to names</li>
     * </ul>
     * 
     * <p>Values that cannot be resolved are logged as warnings and not added to the output.
     * ISNI identifiers are never included in the output, even if they appear as alternates.
     */
    private void addRefAlternates(List<String> values, ReferenceSheet reference, Set<String> target) {
        if (values == null || values.isEmpty()) return;
        for (String v : values) {
            if (v == null || v.isBlank()) continue;
            
            String resolvedKey = v;
            
            // Check if this looks like an ISNI (4 groups of 4 digits separated by spaces)
            if (looksLikeIsni(v)) {
                if (reference != null) {
                    // ISNI is at column index 6 in people.csv
                    String key = reference.findKeyByColumnValue(v, 6);
                    if (key != null) {
                        resolvedKey = key;
                    } else {
                        // Could not resolve ISNI - warn and skip
                        System.err.println("Warning: Could not resolve ISNI identifier to a person name: " + v);
                        continue;
                    }
                } else {
                    // No reference sheet available - warn and skip
                    System.err.println("Warning: Could not resolve ISNI identifier (no reference sheet): " + v);
                    continue;
                }
            }
            
            target.add(resolvedKey);
            if (reference != null && reference.hasAlternates(resolvedKey)) {
                for (String alt : reference.getAlternates(resolvedKey)) {
                    // Filter out ISNI-like values from alternates - they shouldn't be indexed as names
                    if (alt != null && !alt.isBlank() && !looksLikeIsni(alt)) {
                        target.add(alt);
                    }
                }
            }
        }
    }

    /**
     * Checks if a value looks like an ISNI identifier.
     * ISNI format: 4 groups of 4 characters (digits or X) separated by spaces,
     * e.g., "0000 0001 2278 5193" or "0000 0001 2145 201X".
     */
    private static boolean looksLikeIsni(String value) {
        if (value == null || value.length() != 19) {
            return false;
        }
        // Pattern: #### #### #### #### where # is digit or X
        for (int i = 0; i < 19; i++) {
            char c = value.charAt(i);
            if (i == 4 || i == 9 || i == 14) {
                // Should be space
                if (c != ' ') return false;
            } else {
                // Should be digit or 'X' (check digit can be X)
                if (!Character.isDigit(c) && c != 'X') return false;
            }
        }
        return true;
    }

    /**
     * Strips the file extension from a filename.
     * Uses lastIndexOf('.') so multi-dot filenames (e.g. "Ha2.binding.frontcover.tif")
     * are handled correctly → "Ha2.binding.frontcover".
     */
    private static String stripExtension(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return (dot > 0) ? filename.substring(0, dot) : filename;
    }

    /**
     * Strips transcriber marks ([ and ]) from text.
     */
    static String stripTranscriberMarks(String s) {
        if (s == null) return null;
        return s.replace("[", "").replace("]", "");
    }

    /**
     * Recursively normalizes whitespace on all string values in a document node.
     *
     * <p>Every textual value (including strings nested inside arrays and objects) has all
     * runs of whitespace collapsed to a single space and leading/trailing whitespace trimmed.
     * This removes stray newlines and indentation carried over from source XML transcriptions.
     */
    static void normalizeWhitespaceValues(JsonNode node) {
        if (node instanceof ObjectNode obj) {
            List<String> fieldNames = new ArrayList<>();
            obj.fieldNames().forEachRemaining(fieldNames::add);
            for (String name : fieldNames) {
                JsonNode child = obj.get(name);
                if (child.isTextual()) {
                    obj.put(name, normalizeWhitespace(child.asText()));
                } else if (child.isContainerNode()) {
                    normalizeWhitespaceValues(child);
                }
            }
        } else if (node instanceof ArrayNode arr) {
            for (int i = 0; i < arr.size(); i++) {
                JsonNode child = arr.get(i);
                if (child.isTextual()) {
                    arr.set(i, TextNode.valueOf(normalizeWhitespace(child.asText())));
                } else if (child.isContainerNode()) {
                    normalizeWhitespaceValues(child);
                }
            }
        }
    }

    /**
     * Collapses all runs of whitespace to a single space and trims leading/trailing whitespace.
     */
    static String normalizeWhitespace(String s) {
        if (s == null) return null;
        return s.replaceAll("\\s+", " ").trim();
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
