package rosa.archive.core.serialize;

import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;

import rosa.archive.core.config.AppConfig;
import rosa.archive.core.util.CSV;
import rosa.archive.core.util.CSVSpreadSheet;
import rosa.archive.model.IllustrationTitles;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
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

    private AppConfig config;

    @Inject
    IllustrationTitlesSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public IllustrationTitles read(InputStream is, List<String> errors) throws IOException {

        IllustrationTitles titles = new IllustrationTitles();

        try (InputStreamReader reader = new InputStreamReader(is, config.getCHARSET())) {

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
    public void write(IllustrationTitles titles, OutputStream out) throws IOException {
        final String header = "Id,Title\n";
        IOUtils.write(header, out, Charset.forName(config.getCHARSET()));

        for (String id : titles.getAllIds()) {
            String line = id + ',' + CSV.escape(titles.getTitleById(id)) + '\n';
            IOUtils.write(line, out, Charset.forName(config.getCHARSET()));
        }

    }

    @Override
    public Class<IllustrationTitles> getObjectType() {
        return IllustrationTitles.class;
    }
}
