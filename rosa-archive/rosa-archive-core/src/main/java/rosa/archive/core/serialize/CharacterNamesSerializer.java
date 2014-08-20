package rosa.archive.core.serialize;

import rosa.archive.core.RoseConstants;
import rosa.archive.core.util.CSVSpreadSheet;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.CharacterName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @see rosa.archive.model.CharacterNames
 */
public class CharacterNamesSerializer implements Serializer<CharacterNames> {
    private static final int MIN_COLS = 1;
    private static final int MAX_COLS = 200;

    private enum Column {
        ID, SITE_NAME, FRENCH_NAME, ENGLISH_NAME
    }

    public CharacterNamesSerializer() {  }

    @Override
    public CharacterNames read(InputStream is, List<String> errors) throws IOException{
        CharacterNames names = new CharacterNames();

        try (InputStreamReader reader = new InputStreamReader(is, RoseConstants.CHARSET)) {

            CSVSpreadSheet table = new CSVSpreadSheet(reader, MIN_COLS, MAX_COLS, errors);
            List<String> headers = new ArrayList<>(Arrays.asList(table.row(0)));

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
    public void write(CharacterNames object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet!");
    }
}
