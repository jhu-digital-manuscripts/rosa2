package rosa.search.core;

public class SearchUtil {
    private static final String ID_SEPARATOR = ";";

    private static String get_id_part(String id, int index) {
        String[] parts = id.split(ID_SEPARATOR);

        if (index >= parts.length) {
            return null;
        }

        return parts[index];
    }

    public static String createId(String collection, String book) {
        return collection + ID_SEPARATOR + book;
    }
    
    public static String createId(String collection, String book, String image) {
        return collection + ID_SEPARATOR + book + ID_SEPARATOR + image;
    }

    public static String createId(String collection, String book, String image, String annoName) {
        return createId(collection, book, image) + ID_SEPARATOR + annoName;
    }
    
    public static boolean isBookId(String id) {
        return id.split(ID_SEPARATOR).length == 2;
    }

    public static boolean isImageId(String id) {
        return id.split(ID_SEPARATOR).length == 3;
    }

    public static String getCollectionFromId(String id) {
        return get_id_part(id, 0);
    }

    public static String getBookFromId(String id) {
        return get_id_part(id, 1);
    }

    public static String getImageFromId(String id) {
        return get_id_part(id, 2);
    }
}
