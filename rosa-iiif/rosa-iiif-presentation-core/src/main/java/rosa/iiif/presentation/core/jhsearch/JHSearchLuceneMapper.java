package rosa.iiif.presentation.core.jhsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.archive.model.ReferenceSheet;
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
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequest;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.search.core.BaseLuceneMapper;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Index and create queries for data which becomes IIIF Presentation
 * Annotations.
 */
public class JHSearchLuceneMapper extends BaseLuceneMapper {
    private static final Logger logger = Logger.getLogger(JHSearchLuceneMapper.class.toString());

    private final IIIFPresentationRequestFormatter formatter;
    
    public JHSearchLuceneMapper(IIIFPresentationRequestFormatter formatter) {
        super(JHSearchField.values());
        this.formatter = formatter;
    }

    @Override
    public SearchField getIdentifierSearchField() {
        return JHSearchField.OBJECT_ID;
    }

    /**
     * Create and index Lucene documents for a given book within a book
     * collection.
     *
     * @param col
     *            BookCollection object
     * @param book
     *            Book object
     * @return list of documents representing the book
     * @throws IOException
     */
    public List<Document> createDocuments(BookCollection col, Book book) throws IOException {
        List<Document> result = new ArrayList<>();

        // Create annotation documents annotations associated with each image

        ImageList images = book.getImages();

        if (images.getImages() == null) {
            logger.warning("No image list found. [" + col.getId() + ":" + book.getId() + "]");
        } else {
            for (BookImage image : images.getImages()) {
                // AoR transcription
                index(col, book, image, book.getAnnotationPage(image.getId()), result);

                // TODO index other data
            }
        }

        return result;
    }

    /**
     * Index all information about the AoR transcriptions.
     *
     * <ul>
     * <li>image ID</li>
     * <li>image short name</li>
     * <li>reader</li>
     * <li>pagination</li>
     * <li>signature</li>
     * <li>underlined text</li>
     * <li>symbols</li>
     * <li>marks</li>
     * <li>marginalia translation</li>
     * <li>marginalia transcription</li>
     * <li>marginalia references to books, both internal to corpus and external
     * </li>
     * <li>marginalia references to people</li>
     * <li>marginalia references to locations</li>
     * <li>errata</li>
     * <li>drawing</li>
     * <li>numerals</li>
     * </ul>
     *
     *
     * @param col
     *            BookCollection obj
     * @param book
     *            Book obj
     * @param image
     *            this image
     * @param page
     *            transcriptions of AoR annotations on this page
     * @param result
     *            resulting list of indexed documents
     */
    private void index(BookCollection col, Book book, BookImage image, AnnotatedPage page,
            List<Document> result) {
        if (page == null) {
            return;
        }
        
        Document doc = create_document(page, col, book, image);

        page.getSymbols().forEach(a -> index(col, book, image, a, doc));
        page.getMarks().forEach(a -> index(col, book, image, a, doc));
        page.getDrawings().forEach(a -> index(col, book, image, a, doc));
        page.getErrata().forEach(a -> index(col, book, image, a, doc));
        page.getNumerals().forEach(a -> index(col, book, image, a, doc));
        page.getMarginalia().forEach(a -> index(col, book, image, a, doc));
        page.getUnderlines().forEach(a -> index(col, book, image, a, doc));
        
        result.add(doc);
    }

    // Create document with generic annotation info
    private Document create_document(AnnotatedPage page, BookCollection col, Book book, BookImage image) {
        Document doc = new Document();

        String collection_id = get_uri(col.getId(), null, col.getId(), PresentationRequestType.COLLECTION);
        String manifest_id = get_uri(col.getId(), book.getId(), null, PresentationRequestType.MANIFEST);
        String canvas_id = get_uri(col.getId(), book.getId(), image.getName(), PresentationRequestType.CANVAS);
        
        addField(doc, JHSearchField.COLLECTION_ID, collection_id);
        
        addField(doc, JHSearchField.OBJECT_ID, canvas_id);
        addField(doc, JHSearchField.OBJECT_TYPE, IIIFNames.SC_CANVAS);
        addField(doc, JHSearchField.OBJECT_LABEL, image.getName());
        
        addField(doc, JHSearchField.MANIFEST_ID, manifest_id);        
        addField(doc, JHSearchField.MANIFEST_LABEL, book.getBookMetadata("en").getCommonName());
        
        return doc;
    }

    private void index(BookCollection col, Book book, BookImage image, Symbol symbol, Document doc) {
        addField(doc, JHSearchField.SYMBOL, SearchFieldType.STRING, symbol.getName());
        addField(doc, JHSearchField.SYMBOL, get_lang(symbol), symbol.getReferencedText());
    }

    private void index(BookCollection col, Book book, BookImage image, Drawing drawing, Document doc) {
        addField(doc, JHSearchField.DRAWING, SearchFieldType.STRING, drawing.getName());
        addField(doc, JHSearchField.DRAWING, get_lang(drawing), drawing.getReferencedText());
    }

    private void index(BookCollection col, Book book, BookImage image, Errata errata, Document doc) {
        SearchFieldType type = get_lang(errata);
        
        addField(doc, JHSearchField.ERRATA, type, errata.getAmendedText());
        addField(doc, JHSearchField.ERRATA, type, errata.getReferencedText());
    }

    private void index(BookCollection col, Book book, BookImage image, Mark mark, Document doc) {
        addField(doc, JHSearchField.MARK, SearchFieldType.STRING, mark.getName());
        addField(doc, JHSearchField.MARK, get_lang(mark), mark.getReferencedText());
    }

    private void index(BookCollection col, Book book, BookImage image, Numeral numeral, Document doc) {
        SearchFieldType type = get_lang(numeral);
        
        addField(doc, JHSearchField.NUMERAL, type, numeral.getReferencedText());
        addField(doc, JHSearchField.NUMERAL, type, numeral.getNumeral());
    }

    private void index(BookCollection col, Book book, BookImage image, Underline underline, Document doc) {
        addField(doc, JHSearchField.UNDERLINE, get_lang(underline), underline.getReferencedText());
    }

    private SearchFieldType get_lang(Annotation a) {
        SearchFieldType type = null;
        
        if (a.getLanguage() != null) {
            type = getSearchFieldTypeForLang(a.getLanguage());
        }
        
        if (type == null) {
            // TODO Check book metadata for lang
            return SearchFieldType.ENGLISH;
        }
        
        return type;
    }
    
    private void index(BookCollection col, Book book, BookImage image, Marginalia marg, Document doc) {
        addField(doc, JHSearchField.MARGINALIA, get_lang(marg), marg.getReferencedText());

        StringBuilder transcription = new StringBuilder();
        StringBuilder notes = new StringBuilder();
        StringBuilder emphasis = new StringBuilder();
        StringBuilder xrefs = new StringBuilder();

        SearchFieldType marg_lang_type = SearchFieldType.ENGLISH;

        ReferenceSheet peopleRefs = col.getPeopleRef();
        ReferenceSheet bookRefs = col.getBooksRef();
        ReferenceSheet locationRefs = col.getLocationsRef();

        for (MarginaliaLanguage lang : marg.getLanguages()) {
            marg_lang_type = getSearchFieldTypeForLang(lang.getLang());

            for (Position pos : lang.getPositions()) {
                transcription.append(to_string(pos.getTexts()));

                //Index <person> tags + variants
                StringBuilder people = new StringBuilder("People: ");
                pos.getPeople().forEach(person -> {
                    people.append(person).append(" ");
                    if (peopleRefs != null && peopleRefs.hasAlternates(person)) {
                        peopleRefs.getAlternates(person).forEach(alt -> people.append(alt).append(" "));
                    }
                });
                notes.append(people.toString());

                // Index <book> tags + variants
                StringBuilder books = new StringBuilder("Books: ");
                pos.getBooks().forEach(bookRef -> {
                    books.append(bookRef).append(" ");
                    if (bookRefs != null && bookRefs.hasAlternates(bookRef)) {
                        bookRefs.getAlternates(bookRef).forEach(alt -> books.append(alt).append(" "));
                    }
                });
                notes.append(books.toString());

                // Index <location> tags + variants
                StringBuilder locs = new StringBuilder("Locations: ");
                pos.getLocations().forEach(location -> {
                    locs.append(location).append(" ");
                    if (locationRefs != null && locationRefs.hasAlternates(location)) {
                        locationRefs.getAlternates(location).forEach(alt -> locs.append(alt).append(" "));
                    }
                });
                notes.append(locs.toString());

                pos.getEmphasis().forEach(underline -> emphasis.append(underline.getReferencedText()).append(" "));

                for (XRef xref : pos.getxRefs()) {
                    if (xref.getPerson() != null) {
                        xrefs.append(xref.getPerson()).append(" ");
                    }

                    if (xref.getTitle() != null) {
                        xrefs.append(xref.getTitle()).append(" ");
                    }
                }
            }
        }

        addField(doc, JHSearchField.MARGINALIA, marg_lang_type, transcription.toString());
        addField(doc, JHSearchField.MARGINALIA, SearchFieldType.ENGLISH, marg.getTranslation());
        addField(doc, JHSearchField.MARGINALIA, SearchFieldType.ENGLISH, notes.toString());
        addField(doc, JHSearchField.CROSS_REFERENCE, SearchFieldType.ENGLISH, xrefs.toString());
        addField(doc, JHSearchField.EMPHASIS, marg_lang_type, emphasis.toString());
    }

    private String to_string(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            sb.append(str.trim());
            sb.append(' ');
        }
        return sb.toString();
    }
    
    // TODO Duplicated from BasePresentationTransformer. Must be put into separate service.
    
    private String get_uri(String collection, String book, String name, PresentationRequestType type) {
        return formatter.format(get_request(collection, book, name, type));
    }

    private String get_id(String collection, String book) {
        return collection + (book == null || book.isEmpty() ? "" : "." + book);
    }

    private PresentationRequest get_request(String collection, String book, String name,
                                                    PresentationRequestType type) {
        return new PresentationRequest(get_id(collection, book), name, type);
    }
}
