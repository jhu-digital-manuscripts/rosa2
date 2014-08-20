package rosa.archive.core.serialize;

import rosa.archive.core.RoseConstants;
import rosa.archive.core.util.CSVSpreadSheet;
import rosa.archive.model.IllustrationTitles;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @see rosa.archive.model.IllustrationTitles
 */
public class IllustrationTitlesSerializer implements Serializer<IllustrationTitles> {

    private enum Column {
        ID, TITLE
    }

    public IllustrationTitlesSerializer() {  }

    @Override
    public IllustrationTitles read(InputStream is, List<String> errors) throws IOException{

        IllustrationTitles titles = new IllustrationTitles();

        try (InputStreamReader reader = new InputStreamReader(is, RoseConstants.CHARSET)) {

            CSVSpreadSheet data = new CSVSpreadSheet(reader, 2, 2, errors);
            Map<String, String> dataMap = new HashMap<>();

            for (int row = 1; row < data.size(); row++) {
                String id = data.get(row, Column.ID.ordinal());
                String title = data.get(row, Column.TITLE.ordinal());

                if (dataMap.containsKey(id)) {
                    errors.add("ID [" + id + "] already exists.");
                }

                dataMap.put(id, title);
            }

            titles.setData(dataMap);
        }

        return titles;
    }

    @Override
    public void write(IllustrationTitles object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Write not implemented yet!");
    }
}
