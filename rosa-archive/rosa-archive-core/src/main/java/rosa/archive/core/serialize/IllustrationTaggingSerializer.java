package rosa.archive.core.serialize;

import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;

import rosa.archive.core.ArchiveConfig;
import rosa.archive.core.util.CSV;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Serializes image tagging data.
 *
 * @see rosa.archive.model.IllustrationTagging
 */
public class IllustrationTaggingSerializer implements Serializer<IllustrationTagging> {

    private ArchiveConfig config;

    @Inject
    IllustrationTaggingSerializer(ArchiveConfig config) {
        this.config = config;
    }

    @Override
    public IllustrationTagging read(InputStream is, List<String> errors) throws IOException{
        IllustrationTagging tagging = new IllustrationTagging();

        List<String> linesIn = IOUtils.readLines(is, config.getEncoding());

        for (int i = 1; i < linesIn.size(); i++) {
            String[] row = CSV.parse(linesIn.get(i));

            if (row.length < 1 || row.length > 11) {
                errors.add("Malformed row in Image Tagging: [" + Arrays.toString(row) + "]"
                        + " bad number of columns! (" + row.length + ")");
                continue;
            }

            Illustration illustration = new Illustration();

            illustration.setId(row[0]);
            illustration.setPage(row.length > 1 ? row[1] : "");
            illustration.setTitles(row.length > 2 ? row[2].split("\\s*,\\s*") : new String[] {});
            illustration.setTextualElement(row.length > 3 ? row[3] : "");
            illustration.setInitials(row.length > 4 ? row[4] : "");
            illustration.setCharacters(row.length > 5 ? row[5].split("\\s*,\\s*") : new String[] {});
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
    public void write(IllustrationTagging tagging, OutputStream out) throws IOException {
        final String header = "id,Folio,Illustration title,Textual elements,Initials,Characters,Costume,Objects,Landscape,Architecture,Other\n";
        IOUtils.write(header, out, Charset.forName(config.getEncoding()));

        for (Illustration ill : tagging) {
            String line =
                    ill.getId() + ','
                    + ill.getPage() + ','
                    + CSV.escape(arrayToString(ill.getTitles())) + ','
                    + CSV.escape(ill.getTextualElement()) + ','
                    + CSV.escape(ill.getInitials()) + ','
                    + CSV.escape(arrayToString(ill.getCharacters())) + ','
                    + CSV.escape(ill.getCostume()) + ','
                    + CSV.escape(ill.getObject()) + ','
                    + CSV.escape(ill.getLandscape()) + ','
                    + CSV.escape(ill.getArchitecture()) + ','
                    + CSV.escape(ill.getOther()) + '\n';

            IOUtils.write(line, out, Charset.forName(config.getEncoding()));
        }
    }

    private String arrayToString(String[] arr) {
        if (arr == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean notFirst = false;
        for (String str : arr) {
            if (notFirst) {
                sb.append(',');
            }
            sb.append(str);
            notFirst = true;
        }

        return sb.toString();
    }

    @Override
    public Class<IllustrationTagging> getObjectType() {
       return IllustrationTagging.class;
    }
}
