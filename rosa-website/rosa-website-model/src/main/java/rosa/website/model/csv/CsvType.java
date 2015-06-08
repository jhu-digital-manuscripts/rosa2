package rosa.website.model.csv;

public enum CsvType {
    COLLECTION_DATA("collection_data.csv"),
    COLLECTION_BOOKS("books.csv"),
    ILLUSTRATIONS("illustration_titles.csv"),
    CHARACTERS("character_names.csv"),
    NARRATIVE_SECTIONS("narrative_sections.csv"),
    WORKS("works.csv");

    private String key;

    CsvType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
