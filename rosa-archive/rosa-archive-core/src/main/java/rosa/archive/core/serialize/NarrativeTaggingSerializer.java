package rosa.archive.core.serialize;

import org.apache.commons.io.IOUtils;
import rosa.archive.core.RoseConstants;
import rosa.archive.core.util.CSV;
import rosa.archive.model.BookScene;
import rosa.archive.model.NarrativeTagging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see rosa.archive.model.NarrativeTagging
 */
public class NarrativeTaggingSerializer implements Serializer<NarrativeTagging> {

    public NarrativeTaggingSerializer() {  }

    @Override
    public NarrativeTagging read(InputStream is) throws IOException {

        List<String> errors = new ArrayList<>();
        List<String> lines = IOUtils.readLines(is, RoseConstants.CHARSET);

        // Guess if it is a .csv file or a .txt file. Each must be parsed differently.
        // Narrative Tagging CSV file will have 8 columns.
        String[] firstRow = lines.get(0).split(",");
        if (firstRow.length == 8) {
            return readCSVFormat(lines, errors);
        }

        return readTextFormat(lines, errors);
    }

    @Override
    public void write(NarrativeTagging object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Code copied from old
     * <a href="https://github.com/jhu-digital-manuscripts/rosa/blob/master/rosa-core/src/main/java/rosa/core/NarrativeMapping.java">
     *     NarrativeMapping class</a> from rosa1 project.
     *
     * @param lines lines from data file
     * @param errors container for errors
     * @return {@link rosa.archive.model.NarrativeTagging} representation of data
     * @throws IOException
     */
    private NarrativeTagging readCSVFormat(List<String> lines, List<String> errors) throws IOException {
        NarrativeTagging tagging = new NarrativeTagging();
        List<BookScene> scenes = tagging.getScenes();

        int lineCount = 1;
        for (String row : lines) {
            String[] csv = CSV.parse(row);

            if (csv.length != 8) {
                // TODO log
                continue;
            }

            BookScene scene = createBookScene(
                    csv[0], csv[1], csv[2], csv[3], csv[4], csv[5], errors, lineCount,
                    Boolean.parseBoolean(csv[6]), Integer.parseInt(csv[7])
            );

            if (scene != null) {
                scenes.add(scene);
            }

            lineCount++;
        }

        return tagging;
    }

    /**
     * Code copied from old
     * <a href="https://github.com/jhu-digital-manuscripts/rosa/blob/master/rosa-core/src/main/java/rosa/core/NarrativeMapping.java">
     *     NarrativeMapping class</a> from rosa1 project.
     *     
     * @param lines lines from data file
     * @param errors container for errors
     * @return {@link rosa.archive.model.NarrativeTagging} representation of data
     * @throws IOException
     */
    private NarrativeTagging readTextFormat(List<String> lines, List<String> errors) throws IOException {
        NarrativeTagging tagging = new NarrativeTagging();
        List<BookScene> scenes = tagging.getScenes();

        int lineCount = 0;
        String[] parts = null;
        Pattern lecoypat = Pattern.compile("^(.*)\\s+L?(\\d+)\\s*$");

        for (String line : lines) {
            line = line.trim();
            lineCount++;

            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                // Info about pages/cols/lines surrounded by [ and ]
                parts = line.substring(1, line.length() - 1).split("\\s+");

                if (parts.length != 5) {
                    errors.add("Line " + lineCount + ": Malformed: " + line);
                    parts = null;
                    continue;
                }
            } else if (line.endsWith("]") || line.startsWith("[")) {
                errors.add("Line " + lineCount + ": Missing [ or ]: " + line);
                parts = null;
            } else {
                if (parts == null) {
                    errors.add("Line " + lineCount + ": Double transcriptions? " + line);
                    continue;
                }

                Matcher m = lecoypat.matcher(line);

                if (!m.matches()) {
                    errors.add("Line " + lineCount + ": Missing lecoy " + line);
                    continue;
                }

                String transcription = m.group(1).trim();
                Integer lecoy = Integer.parseInt(m.group(2));

                BookScene scene = createBookScene(parts[0], parts[1], parts[2],
                        parts[3], parts[4], transcription, errors, lineCount, true, lecoy);
                parts = null;

                if (scene != null) {
                    scenes.add(scene);
                }
            }
        }
        return tagging;
    }

    /**
     * Code taken from rose1 project,
     * <a href="https://github.com/jhu-digital-manuscripts/rosa/blob/master/rosa-core/src/main/java/rosa/core/NarrativeMapping.java">
     *     NarrativeMapping</a>
     *
     * @param idinfo ID
     * @param startinfo start page and column
     * @param startlineoffsetinfo line offset in start page/column
     * @param endinfo end page and column
     * @param endlineoffsetinfo line offset in end page/column
     * @param trans start page of transcription
     * @param errors container for errors
     * @param line integer line in data
     * @param correct correct
     * @param start_lecoy start in critical edition
     * @return {@link rosa.archive.model.BookScene} representation
     */
    private BookScene createBookScene(String idinfo, String startinfo, String startlineoffsetinfo,
                                      String endinfo, String endlineoffsetinfo, String trans,
                                      List<String> errors, int line, boolean correct, int start_lecoy) {
        // Initialize vars
        String id = null;
        String start_folio = null;
        String start_folio_col = null;
        int start_line_offset = 0;
        String end_folio = null;
        String end_folio_col = null;
        int end_line_offset = 0;

        // Folio info -> folio and column info separated by a period '.'
        String[] start = startinfo.split("\\.");
        String[] end = endinfo.split("\\.");

        if (start.length != 2 || end.length != 2) {
            errors.add("Line " + line + ": Malformed folio.col: [" + startinfo + "][" + endinfo + "]");
            return null;
        }

        // Normalize spacing in ID
        id = idinfo.replaceAll("\\s+", "");

        start_folio = start[0];
        start_folio_col = start[1];
        end_folio = end[0];
        end_folio_col = end[1];

        try {
            start_line_offset = Integer.parseInt(startlineoffsetinfo);
            end_line_offset = Integer.parseInt(endlineoffsetinfo);
        } catch (NumberFormatException e) {
            errors.add("Line " + line + ": Error parsing line offset [" + start_line_offset + "][" + end_line_offset + "]");
            return null;
        }

        // TODO specifies up to only 4 columns on any page, change to arbitrary columns?
        if (!start_folio_col.matches("a|b|c|d")
                || !end_folio_col.matches("a|b|c|d")) {
            errors.add("Line " + line + ": Malformed column: [" + start_folio_col + "][" + end_folio_col + "]");
            return null;
        }

        // Create the BookScene object
        BookScene scene = new BookScene();

        scene.setId(id);
        scene.setStartTranscription(trans);
        scene.setCorrect(correct);

        scene.setStartPage(start_folio);
        scene.setStartLineOffset(start_line_offset);
        scene.setStartPageCol(start_folio_col);
        scene.setStartCriticalEdition(start_lecoy);

        scene.setEndPage(end_folio);
        scene.setEndLineOffset(end_line_offset);
        scene.setEndPageCol(end_folio_col);

        return scene;
    }
}
