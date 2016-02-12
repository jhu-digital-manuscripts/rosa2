package rosa.iiif.presentation.core.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookMetadata;
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
import rosa.search.core.BaseLuceneMapper;
import rosa.search.core.SearchUtil;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

/**
 * Index and create queries for data which becomes IIIF Presentation
 * Annotations.
 */
public class IIIFSearchLuceneMapper extends BaseLuceneMapper {
    private static final Logger logger = Logger.getLogger(IIIFSearchLuceneMapper.class.toString());

    public IIIFSearchLuceneMapper() {
        super(IIIFSearchFields.values());
    }

    @Override
    public SearchField getIdentifierSearchField() {
        return IIIFSearchFields.ID;
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
     * <li>marginalia transcription?</li>
     * <li>marginalia references to books, both internal to corpus and external
     * </li>
     * <li>marginalia references to people</li>
     * <li>marginalia references to locations</li>
     * <li>marginalia internal references (references within this corpus)</li>
     * <li>errata</li>
     * <li>drawing</li>
     * <li>numerals</li>
     * </ul>
     *
     * TODO handle different languages better?
     *
     * @param col
     *            BookCollection obj
     * @param book
     *            Book obj
     * @param image
     *            this image
     * @param annotatedPage
     *            transcriptions of AoR annotations on this page
     * @param result
     *            resulting list of indexed documents
     */
    private void index(BookCollection col, Book book, BookImage image, AnnotatedPage annotatedPage,
            List<Document> result) {
        if (annotatedPage == null) {
            return;
        }

        annotatedPage.getSymbols().forEach(a -> index(col, book, image, a, result));
        annotatedPage.getMarks().forEach(a -> index(col, book, image, a, result));
        annotatedPage.getDrawings().forEach(a -> index(col, book, image, a, result));
        annotatedPage.getErrata().forEach(a -> index(col, book, image, a, result));
        annotatedPage.getNumerals().forEach(a -> index(col, book, image, a, result));
        annotatedPage.getMarginalia().forEach(a -> index(col, book, image, a, result));
        annotatedPage.getUnderlines().forEach(a -> index(col, book, image, a, result));
    }

    private boolean is_empty(String str) {
        return str == null || str.isEmpty();
    }

    // Create document with generic annotation info
    private Document create_document(Annotation anno, BookCollection col, Book book, BookImage image) {
        Document doc = new Document();

        addField(doc, IIIFSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image.getId(), anno.getId()));
        addField(doc, IIIFSearchFields.COLLECTION, col.getId());
        addField(doc, IIIFSearchFields.BOOK, book.getId());
        addField(doc, IIIFSearchFields.IMAGE, image.getId());

        BookMetadata md = book.getBookMetadata("en");
        addField(doc, IIIFSearchFields.TARGET_LABEL, md.getCommonName() + ": " + image.getName());

        return doc;
    }

    private void index(BookCollection col, Book book, BookImage image, Symbol symbol, List<Document> result) {
        if (is_empty(symbol.getName())) {
            return;
        }

        Document doc = create_document(symbol, col, book, image);

        addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, symbol.getName());
        addField(doc, IIIFSearchFields.TYPE, IIIFSearchFieldType.SYMBOL.name());
        addField(doc, IIIFSearchFields.LABEL, "Symbol");

        result.add(doc);
    }

    private void index(BookCollection col, Book book, BookImage image, Drawing drawing,
            List<Document> result) {
        if (is_empty(drawing.getName())) {
            return;
        }

        Document doc = create_document(drawing, col, book, image);

        addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, drawing.getName());
        addField(doc, IIIFSearchFields.TYPE, IIIFSearchFieldType.DRAWING.name());
        addField(doc, IIIFSearchFields.LABEL, "Drawing");

        result.add(doc);
    }

    private void index(BookCollection col, Book book, BookImage image, Errata errata, List<Document> result) {
        if (is_empty(errata.getAmendedText()) || is_empty(errata.getCopyText())) {
            return;
        }

        Document doc = create_document(errata, col, book, image);

        addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH,
                errata.getAmendedText() + " " + errata.getCopyText());
        addField(doc, IIIFSearchFields.TYPE, IIIFSearchFieldType.ERRATA.name());
        addField(doc, IIIFSearchFields.LABEL, "Errata");

        result.add(doc);
    }

    private void index(BookCollection col, Book book, BookImage image, Mark mark, List<Document> result) {
        if (is_empty(mark.getName())) {
            return;
        }
        Document doc = create_document(mark, col, book, image);

        addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, mark.getName());
        addField(doc, IIIFSearchFields.TYPE, IIIFSearchFieldType.MARK.name());
        addField(doc, IIIFSearchFields.LABEL, "Mark");

        result.add(doc);
    }

    private void index(BookCollection col, Book book, BookImage image, Numeral numeral, List<Document> result) {
        if (is_empty(numeral.getReferringText())) {
            return;
        }
        Document doc = create_document(numeral, col, book, image);

        // TODO Use correct lang
        addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, numeral.getReferringText());
        addField(doc, IIIFSearchFields.TYPE, IIIFSearchFieldType.NUMERAL.name());
        addField(doc, IIIFSearchFields.LABEL, "Numeral");

        result.add(doc);
    }

    private void index(BookCollection col, Book book, BookImage image, Underline underline, List<Document> result) {
        if (is_empty(underline.getReferringText())) {
            return;
        }
        Document doc = create_document(underline, col, book, image);

        // TODO User correct lang
        addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, underline.getReferringText());
        addField(doc, IIIFSearchFields.TYPE, IIIFSearchFieldType.UNDERLINE.name());
        addField(doc, IIIFSearchFields.LABEL, "Underline");

        result.add(doc);
    }

    private void index(BookCollection col, Book book, BookImage image, Marginalia marg, List<Document> result) {
        Document doc = create_document(marg, col, book, image);

        if (!is_empty(marg.getReferringText())) {
            // TODO Use correct lang
            addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, marg.getReferringText());
        }

        StringBuilder transcription = new StringBuilder();
        StringBuilder notes = new StringBuilder();

        SearchFieldType marg_lang_type = SearchFieldType.ENGLISH;

        for (MarginaliaLanguage lang : marg.getLanguages()) {
            marg_lang_type = getSearchFieldTypeForLang(lang.getLang());

            for (Position pos : lang.getPositions()) {
                transcription.append(to_string(pos.getTexts()));

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
            addField(doc, IIIFSearchFields.TEXT, marg_lang_type, transcription.toString());
        }

        if (!is_empty(marg.getTranslation())) {
            addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, marg.getTranslation());
        }

        if (notes.length() > 0) {
            addField(doc, IIIFSearchFields.TEXT, SearchFieldType.ENGLISH, notes.toString());
        }
        
        addField(doc, IIIFSearchFields.TYPE, IIIFSearchFieldType.MARGINALIA.name());
        addField(doc, IIIFSearchFields.LABEL, "Marginalia");

        result.add(doc);
    }

    private String to_string(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            sb.append(str.trim());
            sb.append(' ');
        }
        return sb.toString();
    }
}
