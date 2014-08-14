package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import rosa.archive.core.RoseConstants;
import rosa.archive.core.util.CSV;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class NarrativeSectionsSerializer implements Serializer<NarrativeSections> {

    NarrativeSectionsSerializer() {  }

    @Override
    public NarrativeSections read(InputStream is) throws IOException {
        NarrativeSections sections = new NarrativeSections();

        List<NarrativeScene> scenes = sections.asScenes();
        List<String> errors = new ArrayList<>();

        List<String> lines = IOUtils.readLines(is, RoseConstants.CHARSET);
        String[] headers = CSV.parse(lines.get(0));

        for (int i = 1; i < lines.size(); i++) {
            String[] row = CSV.parse(lines.get(i));

            if (row.length < 4 || row.length > headers.length) {
                errors.add("Malformed row [" + i + "]: " + Arrays.toString(row));
            }

            // TODO move this into scene creation method?
            checkNumberRange(row[1], i, errors);
            checkNumberRange(row[2], i, errors);

            NarrativeScene scene = createScene(row);
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

    private void checkNumberRange(String data, int lineInData, List<String> errors) {
        if (StringUtils.isBlank(data) || data.equals("a-j")) {
            return;
        }

        if (!data.matches("\\d+-\\d+")) {
            errors.add("Row [" + lineInData + "] has bad range: [" + data + "]");
        }
    }

    private int[] getRangeValue(String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }

        String[] parts = data.split("-");
        try {
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);

            return new int[] { start, end };
        } catch (IndexOutOfBoundsException e) {
            // TODO log malformed data
            return null;
        } catch (NumberFormatException e) {
            if (!data.equals("a-j")) {
                // TODO log non-lecoy error
            }
            return null;
        }
    }

    private NarrativeScene createScene(String[] row) {
        int[] lecoy = getRangeValue(row[2]);
        int[] lines = getRangeValue(row[1]);

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
