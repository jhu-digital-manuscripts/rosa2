package rosa.iiif.presentation.core.search;

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


public class AnnotationLuceneMapper extends BaseLuceneMapper {
    private static final Logger logger = Logger.getLogger(AnnotationLuceneMapper.class.toString());
    
    public AnnotationLuceneMapper() {
        super(AnnotationSearchFields.values());
    }

    @Override
    public SearchField getIdentifierSearchField() {
        return AnnotationSearchFields.ID;
    }
    
    /**
     * Create and index Lucene documents for a given book within a book
     * collection.
     *
     * @param col BookCollection object
     * @param book Book object
     * @return list of documents representing the book
     * @throws IOException
     */
    public List<Document> createDocuments(BookCollection col, Book book)
            throws IOException {
        List<Document> result = new ArrayList<>();

        // Create annotation documents annotations associated with each image

        ImageList images = book.getImages();

        if (images.getImages() == null) {
            logger.warning("No image list found. [" + col.getId() + ":" + book.getId() + "]");
        } else {
            for (BookImage image : images.getImages()) {
                // AoR transcription
                index(col, book, image, book.getAnnotationPage(image.getId()), result);
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
     * <li>marginalia references to books, both internal to corpus and external</li>
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
     * @param col BookCollection obj
     * @param book Book obj
     * @param image this image
     * @param annotatedPage transcriptions of AoR annotations on this page
     * @param result resulting list of indexed documents
     */
    private void index(BookCollection col, Book book, BookImage image, AnnotatedPage annotatedPage,
                       List<Document> result) {
        if (annotatedPage == null) {
            return;
        }

        Document pageDoc = new Document();

        addField(pageDoc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image.getId()));
        addField(pageDoc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(pageDoc, AnnotationSearchFields.BOOK, book.getId());
        addField(pageDoc, AnnotationSearchFields.IMAGE, image.getName());
        addField(pageDoc, AnnotationSearchFields.AUTHOR, annotatedPage.getReader());
        addField(pageDoc, AnnotationSearchFields.TARGET, annotatedPage.getPagination());
        addField(pageDoc, AnnotationSearchFields.TARGET, annotatedPage.getSignature());

        // Symbols
        for (Symbol s : annotatedPage.getSymbols()) {
            docForTranscription(col, book, image.getId(), s, result);
        }

        // Marks
        for (Mark m : annotatedPage.getMarks()) {
            docForTranscription(col, book, image.getId(), m, result);
        }

        // Errata
        for (Errata e : annotatedPage.getErrata()) {
            docForTranscription(col, book, image.getId(), e, result);
        }

        // Drawing
        for (Drawing d : annotatedPage.getDrawings()) {
            docForTranscription(col, book, image.getId(), d, result);
        }

        // Numeral
        for (Numeral n : annotatedPage.getNumerals()) {
            docForTranscription(col, book, image.getId(), n, result);
        }

        // Underlines
        for (Underline u : annotatedPage.getUnderlines()) {
            docForTranscription(col, book, image.getId(), u, result);
        }

        // Marginalia
        for (Marginalia m : annotatedPage.getMarginalia()) {
            docForTranscription(col, book, image.getId(), m, result);
        }
    }

    private boolean is_empty(String str) {
        return str == null || str.isEmpty();
    }

    private void docForTranscription(BookCollection col, Book book, String image, Symbol symbol,
                                         List<Document> result) {
        if (is_empty(symbol.getName())) {
            return;
        }
        Document doc = new Document();

        addField(doc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image, symbol.getId()));
        addField(doc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(doc, AnnotationSearchFields.BOOK, book.getId());
        addField(doc, AnnotationSearchFields.IMAGE, image);
        addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, symbol.getName());
        addField(doc, AnnotationSearchFields.TYPE, AnnotationType.SYMBOL.name());

        result.add(doc);
    }

    private void docForTranscription(BookCollection col, Book book, String image, Drawing drawing,
                                         List<Document> result) {
        if (is_empty(drawing.getName())) {
            return;
        }
        Document doc = new Document();

        addField(doc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image, drawing.getId()));
        addField(doc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(doc, AnnotationSearchFields.BOOK, book.getId());
        addField(doc, AnnotationSearchFields.IMAGE, image);
        addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, drawing.getName());
                addField(doc, AnnotationSearchFields.TYPE, AnnotationType.DRAWING.name());

        result.add(doc);
    }

    private void docForTranscription(BookCollection col, Book book, String image, Errata errata,
                                         List<Document> result) {
        if (is_empty(errata.getAmendedText()) || is_empty(errata.getCopyText())) {
            return;
        }
        Document doc = new Document();

        addField(doc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image, errata.getId()));
        addField(doc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(doc, AnnotationSearchFields.BOOK, book.getId());
        addField(doc, AnnotationSearchFields.IMAGE, image);
        addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, errata.getAmendedText() + " " + errata.getCopyText());
        addField(doc, AnnotationSearchFields.TYPE, AnnotationType.ERRATA.name());

        result.add(doc);
    }

    private void docForTranscription(BookCollection col, Book book, String image, Mark mark,
                                         List<Document> result) {
        if (is_empty(mark.getName())) {
            return;
        }
        Document doc = new Document();

        addField(doc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image, mark.getId()));
        addField(doc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(doc, AnnotationSearchFields.BOOK, book.getId());
        addField(doc, AnnotationSearchFields.IMAGE, image);
        addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, mark.getName());
        addField(doc, AnnotationSearchFields.TYPE, AnnotationType.MARK.name());

        result.add(doc);
    }

    private void docForTranscription(BookCollection col, Book book, String image, Numeral numeral,
                                         List<Document> result) {
        if (is_empty(numeral.getReferringText())) {
            return;
        }
        Document doc = new Document();

        addField(doc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image, numeral.getId()));
        addField(doc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(doc, AnnotationSearchFields.BOOK, book.getId());
        addField(doc, AnnotationSearchFields.IMAGE, image);
        
        // TODO Use correct lang
        addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, numeral.getReferringText());
        
        addField(doc, AnnotationSearchFields.TYPE, AnnotationType.NUMERAL.name());

        result.add(doc);
    }

    private void docForTranscription(BookCollection col, Book book, String image, Underline underline,
                                         List<Document> result) {
        if (is_empty(underline.getReferringText())) {
            return;
        }
        Document doc = new Document();

        addField(doc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image, underline.getId()));
        addField(doc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(doc, AnnotationSearchFields.BOOK, book.getId());
        addField(doc, AnnotationSearchFields.IMAGE, image);
        
        // TODO User correct lang
        addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, underline.getReferringText());
        
        addField(doc, AnnotationSearchFields.TYPE, AnnotationType.UNDERLINE.name());

        result.add(doc);
    }
    
    private void docForTranscription(BookCollection col, Book book, String image, Marginalia marg,
                                         List<Document> result) {
        Document doc = new Document();

        addField(doc, AnnotationSearchFields.ID, SearchUtil.createId(col.getId(), book.getId(), image, marg.getId()));
        addField(doc, AnnotationSearchFields.COLLECTION, col.getId());
        addField(doc, AnnotationSearchFields.BOOK, book.getId());
        addField(doc, AnnotationSearchFields.IMAGE, image);
        
        if (!is_empty(marg.getReferringText())) {
            // TODO Use correct lang
            addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, marg.getReferringText());
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
            addField(doc, AnnotationSearchFields.TEXT, marg_lang_type, transcription.toString());
        }
        
        if (!is_empty(marg.getTranslation())) {
            addField(doc, AnnotationSearchFields.TEXT, SearchFieldType.ENGLISH, marg.getTranslation());
        }
        
        if (notes.length() > 0) {
            addField(doc, AnnotationSearchFields.NOTE, SearchFieldType.ENGLISH, notes.toString());
        }

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
