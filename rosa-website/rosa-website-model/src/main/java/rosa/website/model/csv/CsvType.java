package rosa.website.model.csv;

public enum CsvType {
    COLLECTION_DATA("collection_data.csv"),
    COLLECTION_BOOKS("books.csv"),
    ILLUSTRATIONS("illustration_titles.csv"),
    CHARACTERS("character_names.csv"),
    NARRATIVE_SECTIONS("narrative_sections.csv"),;

    private String key;

    CsvType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static CsvType getType(String type) {
        for (CsvType t : CsvType.values()) {
            if (t.key.equals(type)) {
                return t;
            }
        }

        return null;
    }
}
