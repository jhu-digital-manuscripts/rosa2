package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.RoseConstants;
import rosa.archive.core.util.CSV;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Serializes image tagging data.
 *
 * @see rosa.archive.model.IllustrationTagging
 */
public class IllustrationTaggingSerializer implements Serializer<IllustrationTagging> {

    public IllustrationTaggingSerializer() {  }

    @Override
    public IllustrationTagging read(InputStream is) throws IOException{
        IllustrationTagging tagging = new IllustrationTagging();

        List<String> linesIn = IOUtils.readLines(is, RoseConstants.CHARSET);
//        String[] headers = CSV.parse(linesIn.get(0));

        for (int i = 1; i < linesIn.size(); i++) {
            String[] row = CSV.parse(linesIn.get(i));

            if (row.length < 1 || row.length > 11) {
                // TODO log malformed row
                continue;
            }

            Illustration illustration = new Illustration();

            illustration.setId(row[0]);
            illustration.setPage(row.length > 1 ? row[1] : "");
            illustration.setTitles(row.length > 2 ? row[2].split("\\s+,\\s+") : new String[] {});
            illustration.setTextualElement(row.length > 3 ? row[3] : "");
            illustration.setInitials(row.length > 4 ? row[4] : "");
            illustration.setCharacters(row.length > 5 ? row[5].split("\\s+,\\s+") : new String[] {});
            illustration.setCostume(row.length > 6 ? row[6] : "");
            illustration.setObject(row.length > 7 ? row[7] : "");
            illustration.setLandscape(row.length > 8 ? row[8] : "");
            illustration.setArchitecture(row.length > 9 ? row[9] : "");
            illustration.setOther(row.length > 10 ? row[10] : "");

            tagging.addIllustrationData(illustration);
        }

        return tagging;
    }

    @Override
    public void write(IllustrationTagging object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
