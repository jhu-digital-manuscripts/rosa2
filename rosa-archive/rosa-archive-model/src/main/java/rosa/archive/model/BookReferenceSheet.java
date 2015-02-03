package rosa.archive.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BookReferenceSheet extends ReferenceSheet implements HasId, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public List<String> getAlternates(String key) {
        if (!hasAlternates(key)) {
            return null;
        }

        List<String> result = new ArrayList<>();
        int len = getLine(key).size();
        for (int i = 1; i < len; i++) {
            if (i >= 6) {
                continue;
            }

            String val = getCell(key, i);
            if (val != null && !val.isEmpty()) {
                result.add(val);
            }
        }

        return result;
    }

    public List<String> getAuthors(String key) {
        String authors = getCell(key, 6);
        if (authors == null || authors.isEmpty()) {
            return null;
        }

        return Arrays.asList(authors.split(","));
    }

    public String getFullTitle(String key) {
        String title = getCell(key, 7);
        if (title == null || title.isEmpty()) {
            return null;
        }

        return title;
    }
}
