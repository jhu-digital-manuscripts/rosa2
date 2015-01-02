package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import rosa.archive.model.BookStructure;
import rosa.archive.model.redtag.Blank;
import rosa.archive.model.redtag.Heading;
import rosa.archive.model.redtag.Image;
import rosa.archive.model.redtag.Initial;
import rosa.archive.model.redtag.Item;
import rosa.archive.model.redtag.Rubric;
import rosa.archive.model.redtag.StructureColumn;
import rosa.archive.model.redtag.StructurePage;
import rosa.archive.model.redtag.StructurePageSide;

/**
 * Associated with Reduced Tagging.
 *
 * @see rosa.archive.model.BookStructure
 */
public class BookStructureSerializer implements Serializer<BookStructure> {
    @Override
    public BookStructure read(InputStream is, List<String> errors) throws IOException {
        BookStructure structure = new BookStructure();

        List<StructurePage> pages = structure.pages();
        List<Item> items = new ArrayList<>();

        int n = 0;
        int linesInColumn = -1;
        StructureColumn col = null;

        List<String> inputLine = IOUtils.readLines(is, UTF_8);
        for (String line : inputLine) {
            line = line.trim();
            n++;

            if (line.length() == 0 || line.startsWith("#")) {
                // The line is blank or a comment. Ignore.
                continue;
            }

            String normalizedLine = line;
            if (line.startsWith("[") && line.endsWith("]")) {
                normalizedLine = line.substring(1, line.length() - 1);
            }

            normalizedLine = normalizedLine.replaceAll(":", " ");
            String[] parts = normalizedLine.split("\\s+");

            if (parts.length == 0) {
                continue;
            }

            try {
                if (parts[0].equalsIgnoreCase("rubric") || parts[0].equalsIgnoreCase("r")) {
                    if (parts.length < 3) {
                        errors.add("Line " + n + ": " + "Rubric missing lines and text: " + line);
                        continue;
                    }

                    int lines = Integer.parseInt(parts[1]);

                    if (items != null) {
                        String text = normalizedLine.replaceFirst("^\\s*[Rr][a-z]*\\s*\\d+\\s*", "").trim();

                        Rubric rubric = new Rubric();
                        rubric.setLines(lines);
                        rubric.setText(text);

                        items.add(rubric);
                    }
                } else if (parts[0].equalsIgnoreCase("heading") || parts[0].equalsIgnoreCase("h")) {
                    if (parts.length < 3) {
                        errors.add("Line " + n + ": " + "Heading missing lines and text: " + line);
                        continue;
                    }

                    int lines = Integer.parseInt(parts[1]);

                    if (items != null) {
                        Heading heading = new Heading();

                        heading.setText(normalizedLine.substring(normalizedLine.indexOf(' ')).trim());
                        heading.setLines(lines);

                        items.add(heading);
                    }
                } else if (parts[0].equalsIgnoreCase("lecoy")) {
                    if (parts.length < 3) {
                        errors.add("Line " + n + ": " + "Lecoy missing text and number: " + line);
                        continue;
                    }

                    if (col == null) {
                        errors.add("Line " + n + ": " + "No folio yet: " + line);
                        continue;
                    }

                    col.setFirstLineCriticalEdition(Integer.parseInt(parts[parts.length - 1]));
                    col.setFirstLineTranscribed(normalizedLine.substring(normalizedLine.indexOf(' '),
                            normalizedLine.lastIndexOf(' ')).trim());

                } else if (parts[0].equalsIgnoreCase("columnlines")) {
                    if (parts.length != 2) {
                        errors.add("Line " + n + ": " + "Column without lines: " + line);
                        continue;
                    }

                    linesInColumn = Integer.parseInt(parts[1]);
                } else if (parts[0].equalsIgnoreCase("image") || parts[0].equalsIgnoreCase("m")) {
                    if (parts.length != 2) {
                        errors.add("Line " + n + ": " + "Image without lines: " + line);
                        continue;
                    }

                    int lines = Integer.parseInt(parts[1]);

                    if (items != null) {
                        Image image = new Image();
                        image.setLines(lines);

                        items.add(image);
                    }
                } else if (parts[0].equalsIgnoreCase("blank") || parts[0].equalsIgnoreCase("b")) {
                    if (parts.length != 2) {
                        errors.add("Line " + n + ": " + "Blank without lines: " + line);
                        continue;
                    }

                    int lines = Integer.parseInt(parts[1]);

                    if (items != null) {
                        Blank blank = new Blank();
                        blank.setLines(lines);

                        items.add(blank);
                    }
                } else if (parts[0].equalsIgnoreCase("initial") || parts[0].equalsIgnoreCase("i")) {
                    if (parts.length < 2) {
                        errors.add("Line " + n + ": " + "Initial without letter: " + line);
                        continue;
                    }

                    int lines = 0;
                    boolean empty = false;

                    if (parts.length == 3) {
                        empty = parts[2].equalsIgnoreCase("(empty)");

                        if (!empty) {
                            lines = Integer.parseInt(parts[2]);
                        }
                    }

                    if (parts[1].length() > 1) {
                        errors.add("Line " + n + ": Initial more than one character: " + parts[1]);
                        continue;
                    }

                    if (items != null) {
                        Initial initial = new Initial();

                        initial.setLines(lines);
                        initial.setCharacter(parts[1]);
                        initial.setEmpty(empty);

                        items.add(initial);
                    }
                } else if (parts[0].equalsIgnoreCase("folio") || parts[0].equalsIgnoreCase("page")
                        || parts[0].equalsIgnoreCase("f")) {
                    if (parts.length != 3) {
                        errors.add("Line " + n + ": " + "Folio needs name and column: " + line);
                        continue;
                    }

                    String sidename = parts[1].toLowerCase();

                    if (!sidename.endsWith("r") && !sidename.endsWith("v")) {
                        errors.add("Folio malformed: " + sidename);
                        continue;
                    }

                    String leafname = sidename.substring(0, sidename.length() - 1);
                    StructurePage leaf = null;

                    for (StructurePage l : pages) {
                        if (l.getName().equals(leafname)) {
                            leaf = l;
                            break;
                        }
                    }

                    if (leaf == null) {
                        leaf = new StructurePage(leafname, linesInColumn);
                        pages.add(leaf);
                    }

                    StructurePageSide side = null;

                    if (sidename.endsWith("r")) {
                        side = leaf.getRecto();
                    } else {
                        side = leaf.getVerso();
                    }

                    String c = parts[2].toLowerCase();

                    if (c.equals("a") || c.equals("c")) {
                        col = side.columns().get(0);
                        items = col.getItems();
                    } else if (c.equals("b") || c.equals("d")) {
                        col = side.columns().get(1);
                        items = col.getItems();
                    } else if (c.equals("ab") || c.equals("cd")) {
                        items = side.spanning();
                    } else {
                        errors.add("Line " + n + ": Malformed column: " + line);
                    }
                } else {
                    errors.add("Line " + n + ": Malformed: " + line);
                    continue;
                }
            } catch (NumberFormatException e) {
                errors.add("Line " + n + ": Error parsing number: " + line);
            }
        }

        if (linesInColumn == -1) {
            errors.add("columnlines must be set");
        }

        // Insert missing folios. Only works for ms.
        if (pages.size() > 0 && !pages.get(0).getName().matches("\\d+")) {
            // Not a manuscript
            return new BookStructure();
        }

        for (int i = 0; i < pages.size();) {
            StructurePage f2 = pages.get(i);

            int n2 = Integer.parseInt(f2.getName());
            int n1 = 0;

            if (i > 0) {
                StructurePage f1 = pages.get(i - 1);

                n1 = Integer.parseInt(f1.getName());
            }

            while (++n1 < n2) {
                StructurePage missing = new StructurePage("" + n1, linesInColumn);
                pages.add(i++, missing);
            }

            i++;
        }

        return structure;
    }

    @Override
    public void write(BookStructure object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Class<BookStructure> getObjectType() {
        return BookStructure.class;
    }
}
