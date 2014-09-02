package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.config.AppConfig;
import rosa.archive.core.util.CSV;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * @see rosa.archive.model.NarrativeSections
 */
public class NarrativeSectionsSerializer implements Serializer<NarrativeSections> {

    private AppConfig config;

    @Inject
    NarrativeSectionsSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public NarrativeSections read(InputStream is, List<String> errors) throws IOException {
        NarrativeSections sections = new NarrativeSections();

        List<NarrativeScene> scenes = sections.asScenes();

        List<String> lines = IOUtils.readLines(is, config.getCHARSET());
        String[] headers = CSV.parse(lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            String[] row = CSV.parse(lines.get(i));

            if (row.length < 4 || row.length > headers.length) {
                errors.add("Malformed row in narrative sections [" + i + "]: " + Arrays.toString(row));
            }

            NarrativeScene scene = createScene(row, errors);
            if (scene != null) {
                scenes.add(scene);
            }
        }

        return sections;
    }

    @Override
    public void write(NarrativeSections object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Check the format of data to make sure it represents a numberical range, ex: 5-10.
     * If it is determined that the data does not represent a range, it is reported in
     * {@param errors}.
     *
     * @param data a single cell from the CSV
     * @param lineInData the row number that the data is in
     * @param errors container to hold error messages
     */
    private void checkNumberRange(String data, int lineInData, List<String> errors) {
        if (StringUtils.isBlank(data) || data.equals("a-j")) {
            return;
        }

        if (!data.matches("\\d+-\\d+")) {
            errors.add("Row [" + lineInData + "] has bad range: [" + data + "]");
        }
    }

    /**
     * Find the start and end of a range.
     *
     * @param data data from CSV that represents a numerical range
     * @return integer array with start and end numbers
     */
    private int[] getRangeValue(String data, List<String> errors) {
        if (StringUtils.isBlank(data)) {
            return null;
        }

        String[] parts = data.split("-");
        try {
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);

            return new int[] { start, end };
        } catch (IndexOutOfBoundsException e) {
            errors.add("Malformed data in narrative sections: [" + data + "]. " +
                    "Should contain two numbers separated by '-'");
            return null;
        } catch (NumberFormatException e) {
            if (!data.equals("a-j")) {
                errors.add("Error parsing as integer or Lecoy in narrative sections: [" + data + "]");
            }
            return null;
        }
    }

    /**
     * Create a {@link rosa.archive.model.NarrativeScene} from a row in the CSV.
     *
     * @param row row from CSV data
     * @return the scene
     */
    private NarrativeScene createScene(String[] row, List<String> errors) {
        int[] lecoy = getRangeValue(row[2], errors);
        int[] lines = getRangeValue(row[1], errors);

        if (lecoy == null || lines == null) {
            return null;
        }

        NarrativeScene scene = new NarrativeScene();

        scene.setId(row[0]);
        scene.setDescription(row[3]);
        scene.setCriticalEditionStart(lecoy[0]);
        scene.setCriticalEditionEnd(lecoy[1]);
        scene.setRel_line_start(lines[0]);
        scene.setRel_line_end(lines[1]);

        return scene;
    }
}
