package rosa.iiif.presentation.core.transform.impl;

import java.util.ArrayList;
import java.util.List;

import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.BookImageLocation;
import rosa.archive.model.BookMetadata;
import rosa.archive.model.BookText;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.IllustrationTitles;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.model.Range;
import rosa.iiif.presentation.model.TextValue;
import rosa.iiif.presentation.model.ViewingHint;

public class RangeTransformer implements TransformerConstants {
    private final PresentationUris pres_uris;
    
    public RangeTransformer(PresentationUris pres_uris) {
        this.pres_uris = pres_uris;
    }

    public Range transform(BookCollection collection, Book book, String name) {
        String[] parts = name.split("\\.");

        if (parts.length != 2) {
            return null;
        }

        String type = parts[0];
        String id = parts[1];

        if (type.equals(ILLUSTRATION_RANGE_TYPE)) {
            return buildIllustrationRange(collection, book, id);
        } else if (type.equals(IMAGE_RANGE_TYPE)) {
            return buildImageRange(collection, book, id);
        } else if (type.equals(TEXT_RANGE_TYPE)) {
            return buildTextRange(collection, book, id);
        } else {
            return null;
        }
    }

    private Range range(BookCollection col, Book book, String name) {
        return transform(col, book, name);
    }

    private String constructRangeName(String type, String id) {
        return type + "." + id;
    }

    private String constructRangeURI(BookCollection col, Book book, String range_type, String range_id) {
        return pres_uris.getRangeURI(col.getId(), book.getId(), constructRangeName(range_type, range_id));
    }

    public List<Range> topRanges(BookCollection col, Book book) {
        List<Range> result = new ArrayList<>();

        // TODO Looks like ranges need to be embedded, add nicer mechanism to generate all ranges
        result.add(range(col, book, constructRangeName(IMAGE_RANGE_TYPE, TOP_RANGE_ID)));
        result.add(range(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_FRONTMATTER_ID)));
        result.add(range(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_BODYMATTER_ID)));
        result.add(range(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_ENDMATTER_ID)));
        //result.add(range(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_BINDING_ID)));
        //result.add(range(col, book, constructRangeName(IMAGE_RANGE_TYPE, IMAGE_RANGE_MISC_ID)));

//        result.add(range(col, book, constructRangeName(ILLUSTRATION_RANGE_TYPE, TOP_RANGE_ID)));
        result.add(range(col, book, constructRangeName(TEXT_RANGE_TYPE, TOP_RANGE_ID)));

        Range range = range(col, book, constructRangeName(ILLUSTRATION_RANGE_TYPE, TOP_RANGE_ID));
        int index = 0;

        while (range != null) {
            result.add(range);
            range = range(col, book, constructRangeName(ILLUSTRATION_RANGE_TYPE, "" + index++));
        }

        return result;
    }

    // TODO Better error handling in class
    // Range name is  RANGE_TYPE "." RANGE_ID
    private Range buildTextRange(BookCollection col, Book book, String range_id) {
        Range result = new Range();

        result.setId(constructRangeURI(col, book, TEXT_RANGE_TYPE, range_id));

        String lang_code = "en";
        BookMetadata metadata = book.getBookMetadata();
        
        if (metadata == null || range_id == null || range_id.isEmpty()) {
            return null;
        }

        List<String> ranges = new ArrayList<>();

        int text_count = 0;
        for (BookText text : metadata.getBookTexts()) {
            text_count++;
            String text_id = String.valueOf(text_count);
            
            if (range_id.equals(TOP_RANGE_ID)) {
                // If TOP range, collect all BookText text_id's for sub-ranges
                result.setViewingHint(ViewingHint.TOP);
                result.setLabel("Text Type", lang_code);

                ranges.add(constructRangeURI(col, book, TEXT_RANGE_TYPE, text_id));
            } else if (text_id.equals(range_id)) {
                // If specific range, return canvases for specific BookText text_id
                result.setLabel("Text Type: " + range_id, lang_code);

                String start_page = text.getFirstPage();
                String end_page = text.getLastPage();

                List<String> canvases = new ArrayList<>();
                for (BookImage image : book.getImages()) {
                    String this_page = image.getName();
                    // If this_page lies within the range of pages of this BookText, add it to the range
                    if (this_page.compareToIgnoreCase(start_page) >= 0
                            && this_page.compareToIgnoreCase(end_page) <= 0) {
                        canvases.add(pres_uris.getCanvasURI(col.getId(), book.getId(), this_page));
                    }
                }
                result.setCanvases(canvases);
                return result;
            }
        }

        // Set list of range URIs after collecting them in a list
        result.setRanges(ranges);
        return result;
    }

    private Range buildImageRange(BookCollection col, Book book, String range_id) {
        Range result = new Range();

        result.setId(constructRangeURI(col, book, IMAGE_RANGE_TYPE, range_id));
        List<String> uris = new ArrayList<>();

        switch (range_id) {
            case TOP_RANGE_ID:
                result.setViewingHint(ViewingHint.TOP);
                result.setLabel(new TextValue("Image Type", "en"));

                uris.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_FRONTMATTER_ID));
                uris.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_BODYMATTER_ID));
                uris.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_ENDMATTER_ID));

                // TODO Ranges must nest?
                //ranges.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_BINDING_ID));
                //ranges.add(constructRangeURI(col, book, IMAGE_RANGE_TYPE, IMAGE_RANGE_MISC_ID));

                result.setRanges(uris);
                break;
            case IMAGE_RANGE_FRONTMATTER_ID:
                result.setLabel(new TextValue("Front matter", "en"));
                addCanvasUris(col, book, BookImageLocation.FRONT_MATTER, uris);
                result.setCanvases(uris);
                break;
            case IMAGE_RANGE_ENDMATTER_ID:
                result.setLabel(new TextValue("End matter", "en"));
                addCanvasUris(col, book, BookImageLocation.END_MATTER, uris);
                result.setCanvases(uris);
                break;
            case IMAGE_RANGE_BINDING_ID:
                result.setLabel(new TextValue("Binding", "en"));
                addCanvasUris(col, book, BookImageLocation.BINDING, uris);
                result.setCanvases(uris);
                break;
            case IMAGE_RANGE_BODYMATTER_ID:
                result.setLabel(new TextValue("Body matter", "en"));
                addCanvasUris(col, book, BookImageLocation.BODY_MATTER, uris);
                result.setCanvases(uris);
                break;
            case IMAGE_RANGE_MISC_ID:
                result.setLabel(new TextValue("Misc", "en"));
                addCanvasUris(col, book, BookImageLocation.MISC, uris);
                result.setCanvases(uris);
                break;
            default:
                break;
        }

        return result;
    }

    private void addCanvasUris(BookCollection collection, Book book, BookImageLocation targetType, List<String> uris) {
        for (BookImage image : book.getImages()) {
            if (image.getLocation() == targetType) {
                uris.add(pres_uris.getCanvasURI(collection.getId(), book.getId(), image.getName()));
            }
        }
    }

    private Range buildIllustrationRange(BookCollection col, Book book, String range_id) {
        IllustrationTagging tags = book.getIllustrationTagging();

        if (tags == null) {
            return null;
        }

        Range result = new Range();

        result.setId(constructRangeURI(col, book, ILLUSTRATION_RANGE_TYPE, range_id));

        if (range_id.equals(TOP_RANGE_ID)) {
            result.setViewingHint(ViewingHint.TOP);
            result.setLabel("Illustrations", "en");

            List<String> ranges = new ArrayList<>();

            for (int i = 0; i < tags.size(); i++) {
                ranges.add(constructRangeURI(col, book, ILLUSTRATION_RANGE_TYPE, "" + i));
            }

            result.setRanges(ranges);
        } else {
            int index;

            try {
                index = Integer.parseInt(range_id);
            } catch (NumberFormatException e) {
                return null;
            }

            if (index < 0 || index >= tags.size()) {
                return null;
            }

            Illustration illus = tags.getIllustrationData(index);

            IllustrationTitles titles = col.getIllustrationTitles();

            if (titles == null) {
                return null;
            }

            String label = "";

            for (String title_id: illus.getTitles()) {
                String title = titles.getTitleById(title_id);

                if (title != null) {
                    label += (label.isEmpty() ? "" : "; ") + title;
                }
            }

            List<String> canvases = new ArrayList<>();
            canvases.add(pres_uris.getCanvasURI(col.getId(), book.getId(), illus.getPage()));

            result.setLabel(label, "en");
            result.setCanvases(canvases);
        }

        return result;
    }
}
