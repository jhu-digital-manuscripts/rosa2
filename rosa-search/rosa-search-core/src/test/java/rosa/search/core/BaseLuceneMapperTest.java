package rosa.search.core;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.junit.Before;
import org.junit.Test;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.search.model.Query;
import rosa.search.model.SearchField;
import rosa.search.model.SearchFieldType;

public class BaseLuceneMapperTest {
    private BaseLuceneMapper mapper;

    private enum SearchFields implements SearchField {
        ID(false, false, SearchFieldType.STRING),
//        BOOK(false, true, SearchFieldType.STRING),
        TEXT(true, true, SearchFieldType.ENGLISH, SearchFieldType.FRENCH, SearchFieldType.LATIN),
        EN_TEXT(true, true, SearchFieldType.ENGLISH),
        FR_TEXT(true, true, SearchFieldType.OLD_FRENCH);

        private final SearchFieldType[] types;
        private final boolean context;
        private final boolean include;

        SearchFields(boolean context, boolean include, SearchFieldType... types) {
            this.types = types;
            this.context = context;
            this.include = include;
        }

        @Override
        public boolean isContext() {
            return context;
        }

        @Override
        public boolean includeValue() {
            return include;
        }

        @Override
        public SearchFieldType[] getFieldTypes() {
            return types;
        }

        @Override
        public String getFieldName() {
            return name();
        }
    }

    @Before
    public void setup() {
        mapper = new BaseLuceneMapper(SearchFields.values()) {
            @Override
            public SearchField getIdentifierSearchField() {
                return SearchFields.ID;
            }

            @Override
            public List<Document> createDocuments(BookCollection col, Book book) throws IOException {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    @Test
    public void testCreateLuceneQuerySimple() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery(new Query(SearchFields.ID, "bessie"));
        
        assertNotNull(result);
        
        String lucene_query = result.toString();       
        
        assertEquals("ID.STRING:bessie", lucene_query);
    }
    
    @Test
    public void testCreateLuceneQuerySimplePhrase() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery(new Query(SearchFields.TEXT, "bessie \"good cow\""));
        
        assertNotNull(result);
        
        String lucene_query = result.toString();        
        
        assertTrue(lucene_query.contains("TEXT.ENGLISH:\"good cow\""));
        assertTrue(lucene_query.contains("TEXT.FRENCH:\"good cow\""));
    }
    
    @Test
    public void testCreateLuceneQueryFromAnyString() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery("bessie \"good cow\"");
        
        assertNotNull(result);
        
        String lucene_query = result.toString();        
        
        assertTrue(lucene_query.contains("TEXT.ENGLISH:\"good cow\""));
        assertTrue(lucene_query.contains("TEXT.FRENCH:\"good cow\""));
    }

    @Test
    public void testOldFrenchSpelling() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery(
                new Query(SearchFields.FR_TEXT, "bessie, bessje, and bessye are the same cow.")
        );

        String lucene_query = result.toString();

        assertNotNull(result);

        assertTrue(lucene_query.contains("besi"));
        assertFalse(lucene_query.contains("besy"));
        assertFalse(lucene_query.contains("besj"));
    }

    @Test
    public void testEnglishSpelling() {
        org.apache.lucene.search.Query result = mapper.createLuceneQuery(
                new Query(SearchFields.EN_TEXT, "bessie, bessje, and bessye are the same cow.")
        );

        String lucene_query = result.toString();
        System.out.println(lucene_query);

        assertNotNull(result);

        assertTrue(lucene_query.contains("bessi"));
        assertTrue(lucene_query.contains("bessy"));
        assertTrue(lucene_query.contains("bessj"));
    }
}
