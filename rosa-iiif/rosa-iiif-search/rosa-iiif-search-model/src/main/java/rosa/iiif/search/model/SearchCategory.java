package rosa.iiif.search.model;

import rosa.search.model.SearchFields;

/**
 * Maps user facing search categories into Lucene search fields
 */
public enum SearchCategory {
    SYMBOLS(SearchFields.AOR_SYMBOLS),
    MARKS(SearchFields.AOR_MARKS),
    MARGINALIA(SearchFields.AOR_MARGINALIA_BOOKS, SearchFields.AOR_MARGINALIA_PEOPLE,
            SearchFields.AOR_MARGINALIA_LOCATIONS, SearchFields.AOR_MARGINALIA_TRANSCRIPTIONS,
            SearchFields.AOR_MARGINALIA_TRANSLATIONS, SearchFields.AOR_MARGINALIA_INTERNAL_REFS),
    UNDERLINES(SearchFields.AOR_UNDERLINES),
    ALL(/*SearchFields.COLLECTION_ID, SearchFields.BOOK_ID, SearchFields.IMAGE_NAME,
            SearchFields.DESCRIPTION_TEXT,*/
            SearchFields.AOR_READER, SearchFields.AOR_PAGINATION, SearchFields.AOR_SIGNATURE,
            SearchFields.AOR_MARGINALIA_BOOKS, SearchFields.AOR_MARGINALIA_PEOPLE,
            SearchFields.AOR_MARGINALIA_LOCATIONS, SearchFields.AOR_MARGINALIA_TRANSCRIPTIONS,
            SearchFields.AOR_MARGINALIA_TRANSLATIONS, SearchFields.AOR_MARGINALIA_INTERNAL_REFS,
            SearchFields.AOR_MARKS, SearchFields.AOR_SYMBOLS, SearchFields.AOR_UNDERLINES,
            SearchFields.AOR_ERRATA, SearchFields.AOR_DRAWINGS, SearchFields.AOR_NUMERALS);

    public final SearchFields[] luceneFields;

    SearchCategory(SearchFields... luceneFields) {
        this.luceneFields = luceneFields;
    }
}
