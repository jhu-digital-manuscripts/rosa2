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
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;
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
        super(JHSearchFields.values());
        this.formatter = formatter;
    }

    @Override
    public SearchField getIdentifierSearchField() {
        return JHSearchFields.OBJECT_ID;
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

    private boolean is_empty(String str) {
        return str == null || str.isEmpty();
    }

    // Create document with generic annotation info
    private Document create_document(AnnotatedPage page, BookCollection col, Book book, BookImage image) {
        Document doc = new Document();

        String collection_id = get_uri(col.getId(), null, col.getId(), PresentationRequestType.COLLECTION);
        String manifest_id = get_uri(col.getId(), book.getId(), null, PresentationRequestType.MANIFEST);
        String canvas_id = get_uri(col.getId(), book.getId(), image.getName(), PresentationRequestType.CANVAS);
        
        addField(doc, JHSearchFields.COLLECTION_ID, collection_id);
        
        addField(doc, JHSearchFields.OBJECT_ID, canvas_id);
        addField(doc, JHSearchFields.OBJECT_TYPE, IIIFNames.SC_CANVAS);
        addField(doc, JHSearchFields.OBJECT_LABEL, image.getName());
        
        addField(doc, JHSearchFields.MANIFEST_ID, manifest_id);        
        addField(doc, JHSearchFields.MANIFEST_LABEL, book.getBookMetadata("en").getCommonName());
        
        return doc;
    }

    private void index(BookCollection col, Book book, BookImage image, Symbol symbol, Document doc) {
        addField(doc, JHSearchFields.SYMBOL, SearchFieldType.STRING, symbol.getName());
    }

    private void index(BookCollection col, Book book, BookImage image, Drawing drawing, Document doc) {
        addField(doc, JHSearchFields.DRAWING, SearchFieldType.STRING, drawing.getName());
    }

    private void index(BookCollection col, Book book, BookImage image, Errata errata, Document doc) {
        SearchFieldType type = getSearchFieldTypeForLang(errata.getLanguage());
        
        addField(doc, JHSearchFields.ERRATA, type, errata.getAmendedText());
        addField(doc, JHSearchFields.ERRATA, type, errata.getReferencedText());
    }

    private void index(BookCollection col, Book book, BookImage image, Mark mark, Document doc) {
        addField(doc, JHSearchFields.MARK, SearchFieldType.STRING, mark.getName());
        addField(doc, JHSearchFields.MARK, get_lang(mark), mark.getReferencedText());
    }

    private void index(BookCollection col, Book book, BookImage image, Numeral numeral, Document doc) {
        addField(doc, JHSearchFields.NUMERAL, get_lang(numeral), numeral.getReferencedText());
    }

    private void index(BookCollection col, Book book, BookImage image, Underline underline, Document doc) {
        addField(doc, JHSearchFields.UNDERLINE, get_lang(underline), underline.getReferencedText());
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
        addField(doc, JHSearchFields.MARGINALIA, get_lang(marg), marg.getReferencedText());

        StringBuilder transcription = new StringBuilder();
        StringBuilder notes = new StringBuilder();

        SearchFieldType marg_lang_type = SearchFieldType.ENGLISH;

        for (MarginaliaLanguage lang : marg.getLanguages()) {
            marg_lang_type = getSearchFieldTypeForLang(lang.getLang());

            for (Position pos : lang.getPositions()) {
                transcription.append(to_string(pos.getTexts()));

                // TODO Variants 
                notes.append(to_string(pos.getBooks()));
                notes.append(to_string(pos.getPeople()));
                notes.append(to_string(pos.getLocations()));

                for (InternalReference internalRef : pos.getInternalRefs()) {
                    for (ReferenceTarget target : internalRef.getTargets()) {
                        notes.append(target.getBookId());
                        notes.append(' ');
                        notes.append(target.getFilename());
                        notes.append(' ');
                    }
                }

                pos.getTexts();
            }
        }

        if (transcription.length() > 0) {
            addField(doc, JHSearchFields.MARGINALIA, marg_lang_type, transcription.toString());
        }

        if (!is_empty(marg.getTranslation())) {
            addField(doc, JHSearchFields.MARGINALIA, SearchFieldType.ENGLISH, marg.getTranslation());
        }

        if (notes.length() > 0) {
            addField(doc, JHSearchFields.MARGINALIA, SearchFieldType.ENGLISH, notes.toString());
        }        
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
