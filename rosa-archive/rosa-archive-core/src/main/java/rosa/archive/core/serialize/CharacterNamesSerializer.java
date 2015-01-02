package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

import rosa.archive.core.util.CSV;
import rosa.archive.core.util.CSVSpreadSheet;
import rosa.archive.model.CharacterName;
import rosa.archive.model.CharacterNames;

/**
 * @see rosa.archive.model.CharacterNames
 */
public class CharacterNamesSerializer implements Serializer<CharacterNames> {
    private static final int MIN_COLS = 1;
    private static final int MAX_COLS = 200;

    private enum Column {
        ID("ID"),
        SITE_NAME("site name"),
        FRENCH_NAME("French variant"),
        ENGLISH_NAME("English name");

        private String name;

        Column(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Override
    public CharacterNames read(InputStream is, List<String> errors) throws IOException{
        CharacterNames names = new CharacterNames();

        try (InputStreamReader reader = new InputStreamReader(is, UTF_8)) {

            CSVSpreadSheet table = new CSVSpreadSheet(reader, MIN_COLS, MAX_COLS, errors);
            List<String> headers = table.size() > 0 ?
                    new ArrayList<>(Arrays.asList(table.row(0))) : new ArrayList<String>();

            // Navigate the rows, skip the 1st row, which contains the headers
            for (int i = 1; i < table.size(); i++) {
                CharacterName name = new CharacterName();
                String[] row = table.row(i);

                name.setId(row[Column.ID.ordinal()]);
                // Navigate the columns in a row, skip 1st column which is the row ID
                for (int j = 1; j < row.length; j++) {
                    name.addName(row[j], headers.get(j));
                }

                if (names.getAllCharacterIds().contains(name.getId())) {
                    errors.add("ID [" + name.getId() + "] already exists.");
                }
                names.addCharacterName(name);
            }

        }

        return names;
    }

    @Override
    public void write(CharacterNames names, OutputStream out) throws IOException {
        final String header = "ID,Site name,French variant,English name\n";
        IOUtils.write(header, out, UTF_8);

        for (String id : names.getAllCharacterIds()) {
            StringBuilder sb = new StringBuilder(id);

            sb.append(',');
            sb.append(CSV.escape(
                    names.getNameInLanguage(id, Column.SITE_NAME.getName())
            ));

            sb.append(',');
            sb.append(CSV.escape(names.getNameInLanguage(id, "fr")));
            sb.append(',');
            sb.append(CSV.escape(names.getNameInLanguage(id, "en")));
            sb.append('\n');

            IOUtils.write(sb, out, UTF_8);
        }

    }

    @Override
    public Class<CharacterNames> getObjectType() {
        return CharacterNames.class;
    }
}
