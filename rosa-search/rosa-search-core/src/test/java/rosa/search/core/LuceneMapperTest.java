package rosa.search.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;
import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookDescription;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;
import rosa.search.model.Query;
import rosa.search.model.QueryOperation;
import rosa.search.model.SearchFieldType;
import rosa.search.model.SearchFields;

public class LuceneMapperTest {
    private LuceneMapper mapper;

    @Before
    public void setup() {
        mapper = new LuceneMapper();
    }

    /**
     * Test handling of a term query.
     */
    @Test
    public void testCreateTermQuery() {
        String bookid = "moo";

        Query query = new Query(SearchFields.BOOK_ID, bookid);

        org.apache.lucene.search.Query result = mapper.createLuceneQuery(query);

        String lucene_field = mapper.getLuceneField(SearchFields.BOOK_ID,
                SearchFields.BOOK_ID.getFieldTypes()[0]);
        org.apache.lucene.search.Query expected = new TermQuery(new Term(
                lucene_field, bookid));

        assertEquals(expected, result);
    }

    /**
     * Test that odd characters do not making query generation blowup.
     */
    @Test
    public void testCreateQueryWithOddCharacters() {
        Query query;

        query = new Query(SearchFields.TRANSCRIPTION_TEXT, "");
        mapper.createLuceneQuery(query);

        query = new Query(SearchFields.TRANSCRIPTION_TEXT, ":");
        mapper.createLuceneQuery(query);

        query = new Query(SearchFields.TRANSCRIPTION_TEXT, ":as\"");
        mapper.createLuceneQuery(query);

        query = new Query(SearchFields.TRANSCRIPTION_TEXT,
                "//\\3*3\'w;:  #$52%");
        mapper.createLuceneQuery(query);
    }

    /**
     * Test handling of a boolean query.
     */
    @Test
    public void testCreateBooleanQuery() {
        String bookid = "moo";
        String title = "Cow chews cud.";

        Query query = new Query(QueryOperation.AND, new Query(
                SearchFields.BOOK_ID, bookid), new Query(
                SearchFields.ILLUSTRATION_TITLE, title));

        org.apache.lucene.search.Query result = mapper.createLuceneQuery(query);

        String bookid_lucene_field = mapper.getLuceneField(
                SearchFields.BOOK_ID, SearchFields.BOOK_ID.getFieldTypes()[0]);
        String title_lucene_field = mapper.getLuceneField(
                SearchFields.ILLUSTRATION_TITLE,
                SearchFields.ILLUSTRATION_TITLE.getFieldTypes()[0]);

        BooleanQuery expected = new BooleanQuery();
        expected.add(new TermQuery(new Term(bookid_lucene_field, bookid)),
                Occur.MUST);
        PhraseQuery expected_title_query = new PhraseQuery();
        expected_title_query.add(new Term(title_lucene_field, "cow"));
        expected_title_query.add(new Term(title_lucene_field, "chew"));
        expected_title_query.add(new Term(title_lucene_field, "cud"));
        expected.add(expected_title_query, Occur.MUST);

        assertEquals(expected, result);
    }

    /**
     * Test creating Documents for a book and its images. This test some of the
     * data mapped to a Document.
     */
    @Test
    public void testCreateDocuments() throws IOException {
        BookCollection col = new BookCollection();
        col.setId("barn");

        Book book = new Book();
        book.setId("bessie");

        BookDescription book_desc_en = new BookDescription();
        book_desc_en
                .setDescription("<desc>A good cow is always a delight.</desc>");
        book.addBookDescription(book_desc_en, "en");

        BookDescription book_desc_fr = new BookDescription();
        book_desc_fr
                .setDescription("<desc>Une bonne vache est toujours un délice.</desc>");
        book.addBookDescription(book_desc_fr, "fr");

        col.setLanguages(new String[] { "en", "fr" });

        ImageList images = new ImageList();
        book.setImages(images);

        List<Document> results = mapper.createDocuments(col, book);

        String lucene_id_field = mapper.getLuceneField(SearchFields.ID,
                SearchFields.ID.getFieldTypes()[0]);

        for (Document doc: results) {
            String doc_id = doc.get(lucene_id_field);

            assertNotNull(doc_id);

            assertEquals(col.getId(), SearchUtil.getCollectionFromId(doc_id));
            assertNotNull(book.getId(), SearchUtil.getBookFromId(doc_id));

            if (SearchUtil.isBookId(doc_id)) {
                check_book(col, book, doc);
            } else {
                BookImage doc_image = null;
                String doc_image_id = SearchUtil.getImageFromId(doc_id);

                assertNotNull(doc_image_id);

                for (BookImage bi: images) {
                    if (doc_image_id.equals(bi.getId())) {
                        doc_image = bi;
                        break;
                    }
                }

                assertNotNull("Could not find image: " + doc_image_id,
                        doc_image);

                check_image(col, book, doc_image, doc);
            }
        }

        assertEquals(1 + images.getImages().size(), results.size());
    }

    private String get_lucene_field(SearchFields sf) {
        assertEquals(1, sf.getFieldTypes().length);

        return mapper.getLuceneField(sf, sf.getFieldTypes()[0]);
    }

    private String get_lucene_field(SearchFields sf, SearchFieldType type) {
        return mapper.getLuceneField(sf, type);
    }

    private String get_field(Document doc, SearchFields sf) {
        return doc.get(get_lucene_field(sf));
    }

    private String get_field(Document doc, SearchFields sf, SearchFieldType type) {
        return doc.get(get_lucene_field(sf, type));
    }

    private void check_field(SearchFields sf, String expected, Document doc) {
        String lucene_field = get_lucene_field(sf);

        assertEquals(1, doc.getFields(lucene_field).length);
        assertEquals(expected, get_field(doc, sf));
    }

    private void check_field(SearchFields sf, SearchFieldType type,
            String expected, Document doc) {
        String lucene_field = get_lucene_field(sf, type);

        assertEquals(1, doc.getFields(lucene_field).length);
        assertEquals(expected, get_field(doc, sf, type));
    }

    private void check_image(BookCollection col, Book book, BookImage image,
            Document doc) {
        check_field(SearchFields.ID,
                SearchUtil.createId(col.getId(), book.getId(), image.getId()),
                doc);
        check_field(SearchFields.BOOK_ID, book.getId(), doc);
        check_field(SearchFields.COLLECTION_ID, col.getId(), doc);
    }

    private void check_book(BookCollection col, Book book, Document doc) {
        check_field(SearchFields.ID,
                SearchUtil.createId(col.getId(), book.getId()), doc);
        check_field(SearchFields.BOOK_ID, book.getId(), doc);
        check_field(SearchFields.COLLECTION_ID, col.getId(), doc);

        BookDescription desc_en = book.getBookDescription("en");

        if (desc_en != null) {
            String text = desc_en.getXML().replace("<desc>", "")
                    .replace("</desc>", "");
            check_field(SearchFields.DESCRIPTION_TEXT, SearchFieldType.ENGLISH,
                    text, doc);
        }

        BookDescription desc_fr = book.getBookDescription("fr");

        if (desc_fr != null) {
            String text = desc_fr.getXML().replace("<desc>", "")
                    .replace("</desc>", "");
            check_field(SearchFields.DESCRIPTION_TEXT, SearchFieldType.FRENCH,
                    text, doc);
        }
    }
}
