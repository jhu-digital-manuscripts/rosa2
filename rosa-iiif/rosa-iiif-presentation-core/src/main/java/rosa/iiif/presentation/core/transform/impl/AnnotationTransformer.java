package rosa.iiif.presentation.core.transform.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.serialize.AORAnnotatedPageConstants;
import rosa.archive.core.util.RoseTranscriptionAdapter;
import rosa.archive.core.util.TranscriptionSplitter;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.CharacterNames;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTitles;
import rosa.archive.model.ImageList;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.archive.core.util.Annotations;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationRequestType;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringEscapeUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class AnnotationTransformer extends BasePresentationTransformer implements Transformer<Annotation>,
        AORAnnotatedPageConstants {

    private ArchiveNameParser nameParser;

    @Inject
    public AnnotationTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter,
                                 ArchiveNameParser nameParser) {
        super(presRequestFormatter);
        this.nameParser = nameParser;
    }

    @Override
    public Annotation transform(BookCollection collection, Book book, String name) {
        // Find annotation in book
        rosa.archive.model.aor.Annotation archiveAnno = Annotations.getArchiveAnnotation(book, name);

        // Transform archive anno -> iiif anno
        return adaptAnnotation(collection, book, archiveAnno);
    }

    public Annotation transform(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno) {
        return adaptAnnotation(collection, book, anno);
    }

    @Override
    public Class<Annotation> getType() {
        return Annotation.class;
    }



    private Annotation adaptAnnotation(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno) {
        if (anno == null) {
            return null;
        } else if (anno instanceof Marginalia) {
            return adaptMarginalia(collection, book, (Marginalia) anno);
        }

        String locationIcon = locationToHtml(anno.getLocation());

        Annotation a = new Annotation();

        a.setId(urlId(collection.getId(), book.getId(), anno.getId(), PresentationRequestType.ANNOTATION));
        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.SC_PAINTING);
        a.setDefaultSource(new AnnotationSource(
                "URI", IIIFNames.DC_TEXT, "text/html",
                locationIcon + " " + anno.toPrettyString(),
                (anno.getLanguage() != null && !anno.getLanguage().isEmpty() ? anno.getLanguage() : "en")
        ));

        AnnotationTarget target = locationOnCanvas(
                getPageImage(book.getImages(), getAnnotationPage(anno.getId())),
                Location.FULL_PAGE);
        target.setUri(urlId(
                collection.getId(),
                book.getId(),
                getAnnotationPage(anno.getId()),
                PresentationRequestType.CANVAS
        ));

        a.setDefaultTarget(target);

        for (String lang : collection.getAllSupportedLanguages()) {
            a.setLabel(anno.getId(), lang);
        }

        return a;
    }

    /**
     * Transform marginalia data into a list of annotations that are associated
     * with a canvas.
     *
     * Marginalia is split into potentially several languages, each of which are
     * split into potentially several locations. Currently, each piece is treated
     * as a new and separate IIIF annotation. TODO these pieces must be linked somehow
     *
     * Marginalia ID structure:
     *
     * @param collection book collection obj
     * @param book book obj
     * @param marg AoR marginalia
     * @return list of annotations
     */
    private Annotation adaptMarginalia(BookCollection collection, Book book, Marginalia marg) {
        String lang = marg.getLanguages() != null && marg.getLanguages().size() > 0
                ? marg.getLanguages().get(0).getLang() : "en";

        Annotation anno = new Annotation();

        anno.setId(urlId(collection.getId(), book.getId(), marg.getId(), PresentationRequestType.ANNOTATION));
        anno.setMotivation(IIIFNames.SC_PAINTING);
        anno.setDefaultSource(new AnnotationSource("URI", IIIFNames.DC_TEXT, "text/html",
                marginaliaToDisplayHtml(marg), lang));

        AnnotationTarget target = locationOnCanvas(
                getPageImage(book.getImages(), getAnnotationPage(marg.getId())),
                Location.FULL_PAGE);
        target.setUri(urlId(
                collection.getId(),
                book.getId(),
                getAnnotationPage(anno.getId()),
                PresentationRequestType.CANVAS
        ));

        anno.setDefaultTarget(target); // TODO actual position(s)

        anno.setLabel(marg.getId(), "en");

        return anno;
    }

    // Must make sure to escape text appropriately
    private String marginaliaToDisplayHtml(Marginalia marg) {
        StringBuilder transcription = new StringBuilder();
        StringBuilder people = new StringBuilder();
        StringBuilder books = new StringBuilder();
        StringBuilder locs = new StringBuilder();

        // Left, top, right, bottom
        boolean[] orientation = new boolean[4];
        List<Location> positions = new ArrayList<>();
        List<XRef> xrefs = new ArrayList<>();

        for (MarginaliaLanguage lang : marg.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                add(transcription, pos.getTexts(), " ");
                add(people, pos.getPeople(), ", ");
                add(books, pos.getBooks(), ", ");
                add(locs, pos.getLocations(), ", ");
                xrefs.addAll(pos.getxRefs());
                
                // No default case. If orientation is not 0, 90, 180, 270 then do nothing
                switch (pos.getOrientation()) {
                    case 0:
                        orientation[1] = true;
                        break;
                    case 90:
                        orientation[0] = true;
                        break;
                    case 180:
                        orientation[3] = true;
                        break;
                    case 270:
                        orientation[2] = true;
                        break;
                }

                // Add icon for position(s) on page
                positions.add(pos.getPlace());
            }
        }

        XMLOutputFactory outF = XMLOutputFactory.newInstance();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            XMLStreamWriter writer = outF.createXMLStreamWriter(output);

            writer.writeStartElement("div");

            // ------ Add orientation + location icons ------
            writer.writeStartElement("span");
            writer.writeAttribute("class", "aor-icon-container");
            if (orientation[0]) {   // Left
                addElementWithAttributes(writer, "i", "class", "orientation arrow-left");
            }
            if (orientation[1]) {   // Up
                addElementWithAttributes(writer, "i", "class", "orientation arrow-top");
            }
            writeLocationAsHtml(writer, positions.toArray(new Location[positions.size()]));
            if (orientation[2]) {   // Right
                addElementWithAttributes(writer, "i", "class", "orientation arrow-right");
            }
            if (orientation[3]) {   // Down
                addElementWithAttributes(writer, "i", "class", "orientation arrow-bottom");
            }
            writer.writeEndElement();
            // ------------

            // Add transcription
            writer.writeStartElement("p");
            writer.writeCharacters(StringEscapeUtils.escapeHtml4(transcription.toString()));
            writer.writeEndElement();

            // Add translation
            if (marg.getTranslation() != null && !marg.getTranslation().isEmpty()) {
                writer.writeStartElement("p");
                writer.writeAttribute("class", "italic");
                writer.writeCharacters("[" + StringEscapeUtils.escapeHtml4(marg.getTranslation()) + "]");
                writer.writeEndElement();
            }

            // Add list of People
            if (people.length() > 0) {
                writer.writeStartElement("p");

                writer.writeStartElement("span");
                writer.writeAttribute("class", "emphasize");
                writer.writeCharacters("People:");
                writer.writeEndElement();

                writer.writeCharacters(" " + StringEscapeUtils.escapeHtml4(trim_right(people, 2)));
                writer.writeEndElement();
            }

            // Add list of books
            if (books.length() > 0) {
                writer.writeStartElement("p");

                writer.writeStartElement("span");
                writer.writeAttribute("class", "emphasize");
                writer.writeCharacters("Books:");
                writer.writeEndElement();

                writer.writeCharacters(" " + StringEscapeUtils.escapeHtml4(trim_right(books, 2)));
                writer.writeEndElement();
            }

            // Add list of Locations
            if (locs.length() > 0) {
                writer.writeStartElement("p");

                writer.writeStartElement("span");
                writer.writeAttribute("class", "emphasize");
                writer.writeCharacters("Locations:");
                writer.writeEndElement();

                writer.writeCharacters(" " + StringEscapeUtils.escapeHtml4(trim_right(locs, 2)));
                writer.writeEndElement();
            }

            // Add list of X-refs
            if (xrefs.size() > 0) {
                writer.writeStartElement("p");

                writer.writeStartElement("span");
                writer.writeAttribute("class", "emphasize");
                writer.writeCharacters("Cross-references:");
                writer.writeEndElement();

                writer.writeCharacters(" ");
                for (XRef xref : xrefs) {
                    writer.writeCharacters(StringEscapeUtils.escapeHtml4(xref.getPerson()) + ", ");
                    writer.writeStartElement("span");
                    writer.writeAttribute("class", "italic");
                    writer.writeCharacters(StringEscapeUtils.escapeHtml4(xref.getTitle()));
                    writer.writeEndElement();

                    if (xref.getText() != null && !xref.getText().isEmpty()) {
                        writer.writeCharacters(" &quot;" + StringEscapeUtils.escapeHtml4(xref.getText()) + "&quot;");
                    }
                    writer.writeCharacters("; ");
                }

                writer.writeEndElement();
            }

            writer.writeEndElement();

        } catch (XMLStreamException e) {
            return "Failed to write out marginalia.";
        }
        return output.toString();
    }

    /**
     * Create an empty element that may have attributes.
     *
     * @param writer xml stream writer
     * @param element element name
     * @param attrs array of attributes for the new element, always attribute label followed by attribute value
     *              IF PRESENT, attrs MUST have even number of elements
     */
    private void addElementWithAttributes(XMLStreamWriter writer, String element, String ... attrs) throws XMLStreamException {
        writer.writeStartElement(element);
        if (attrs != null && attrs.length % 2 == 0) {
            for (int i = 0; i < attrs.length - 1;) {
                writer.writeAttribute(attrs[i++], attrs[i++]);
            }
        }
        writer.writeEndElement();
    }

    /**
     * Guess at the location on a canvas based on the limited location information.
     *
     * @param image canvas
     * @param location location on the canvas
     * @return the annotation target
     */
    private AnnotationTarget locationOnCanvas(BookImage image, Location... location) {
        if (location == null || location.length == 0) {
            return new AnnotationTarget(image.getId(), null);
        }

        double margin_guess = 0.10;

        int x = 0;
        int y = 0;
        int w = image.getWidth();
        int h = image.getHeight();

        AnnotationTarget target = new AnnotationTarget(image.getId());

        // This will overwrite the previous locations...
        for (Location loc : location) {
            switch (loc) {
                case HEAD:
                    h = (int) (image.getHeight() * margin_guess);
                    break;
                case TAIL:
                    y = (int) (image.getHeight() * (1 - margin_guess));
                    h = (int) (image.getHeight() * margin_guess);
                    break;
                case LEFT_MARGIN:
                    w = (int) (image.getWidth() * margin_guess);
                    break;
                case RIGHT_MARGIN:
                    x = (int) (image.getWidth() * (1 - margin_guess));
                    w = (int) (image.getWidth() * margin_guess);
                    break;
                case INTEXT:
                    x = (int) (image.getWidth() * margin_guess);
                    y = (int) (image.getHeight() * margin_guess);
                    w = (int) (image.getWidth() * (1 - 2 * margin_guess));
                    h = (int) (image.getHeight() * (1 - 2 * margin_guess));
                    break;
                case FULL_PAGE:
                    // Where Full Page shouldn't need to have a region defined,
                    // Return the full page region because Mirador is misbehaving........................... :)
//                    return new AnnotationTarget(image.getId(), null);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Location. [" + loc + "]");
            }
        }

        target.setSelector(new FragmentSelector(x, y, w, h));

        return target;
    }

    String locationToHtml(Location ... locations) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
            writeLocationAsHtml(writer, locations);

            return out.toString();
        } catch (XMLStreamException e) {
            return "";
        }
    }

    /**
     *
     * @param writer xml stream writer
     * @param locations list of zero or more locations on the page
     * @throws XMLStreamException .
     */
    private void writeLocationAsHtml(XMLStreamWriter writer, Location ... locations) throws XMLStreamException {
        if (locations == null || locations.length == 0) {
            return;
        }

        StringBuilder styleClass = new StringBuilder("aor-icon ");

        // For each distinct location value, append appropriate CSS class
        Stream.of(locations)
                .distinct()
                .map(this::locationToClass)
                .forEach(styleClass::append);

        writer.writeStartElement("i");
        writer.writeAttribute("class", styleClass.toString());

        if (Stream.of(locations).anyMatch(loc -> loc.equals(Location.INTEXT))) {
            addElementWithAttributes(writer, "i", "class", "inner");
        }

        writer.writeEndElement();
    }

    private String locationToClass(Location location) {
        switch (location) {
            default:
                return "";
            case HEAD:
                return "side-top ";
            case TAIL:
                return "side-bottom ";
            case LEFT_MARGIN:
                return "side-left ";
            case RIGHT_MARGIN:
                return "side-right ";
            case INTEXT:
                return "side-within ";
            case FULL_PAGE:
                return "full-page ";
        }
    }

    // Add strings to builder separated by the given string and ending with the separator
    private void add(StringBuilder sb, List<String> list, String sep) {
        list.forEach(s -> {
            sb.append(s);
            sb.append(sep);
        });
    }
    
    private String trim_right(StringBuilder sb, int n) {
        if (sb.length() < n) {
            return "";
        }
        
        return sb.substring(0, sb.length() - n);
    }

    private BookImage getPageImage(ImageList images, String page) {
        for (BookImage image : images) {
            if (image.getName().equals(page) || image.getId().equals(page)) {
                return image;
            }
        }

        return null;
    }

    private String getAnnotationPage(String name) {
        if (name.contains("_")) {
            name = name.split("_")[0];
        }
        return nameParser.shortName(split_id(name)[0]);
    }

    private String[] split_id(String id) {
        return id.split("_");
    }

    // TODO need better way of getting standard name... refer to how it is done in the transcription splitter
    // Ripped from WebsiteLuceneMapper#getStandardPage(String)
    private String getStandardPage(BookImage image) {
        String start = image.getName();
        if (start.length() == 2) {
            return "00" + start;
        } else if (start.length() == 3) {
            return "0" + start;
        } else {
            return start;
        }
    }

    // TODO stub for rose transcription -> annotation transform
    List<Annotation> roseTranscriptionOnPage(BookCollection collection, Book book, BookImage image) {
        if (image == null) {
            return null;
        }
        String page = getStandardPage(image);
        String name = image.getName() + ".transcription";

        // TODO only want to do this once, not once PER PAGE
        Map<String, String> transcriptionMap = TranscriptionSplitter.split(book.getTranscription());

        String transcription = transcriptionMap.get(page);

        if (transcription == null || transcription.isEmpty()) {
            return null;
        }

        RoseTranscriptionAdapter adapter = new RoseTranscriptionAdapter();
        transcription = adapter.toHtml(transcription, page);

        Annotation ann = new Annotation();

        ann.setLabel("Transcription for page " + page, "en");
        ann.setId(urlId(collection.getId(), book.getId(), name, PresentationRequestType.ANNOTATION));
        ann.setMotivation(SC_PAINTING);
        ann.setType(OA_ANNOTATION);

        ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));
        ann.setDefaultSource(new AnnotationSource("ID", IIIFNames.DC_TEXT, "text/html", transcription, "en"));

        return Collections.singletonList(ann);
    }

    // TODO rewrite with actual XML handling, instead of string manipulation
    List<Annotation> illustrationsForPage(BookCollection collection, Book book, BookImage image) {
        String page = image.getName();
        if (book.getIllustrationTagging() == null) {
            return null;
        }

        List<Annotation> anns = new ArrayList<>();
        for (Illustration ill : book.getIllustrationTagging()) {
            String illusPage = ill.getPage();
            if (!illusPage.equals(page)) {
                continue;
            }
            String anno_name = page + ".illustration_" + ill.getId();

            Annotation ann = new Annotation();
            ann.setLabel("Illustration(s) on " + page, "en");
            ann.setId(urlId(collection.getId(), book.getId(), anno_name, PresentationRequestType.ANNOTATION));
            ann.setMotivation(SC_PAINTING);
            ann.setType(OA_ANNOTATION);

            CharacterNames names = collection.getCharacterNames();
            IllustrationTitles titles = collection.getIllustrationTitles();

            // Resolve character name IDs (should be done in archive layer)
            StringBuilder sb_names = new StringBuilder();
            for (String name_id : ill.getCharacters()) {
                String name = names.getNameInLanguage(name_id, "en");

                sb_names.append(name == null ? name_id : name);
                if (!sb_names.toString().isEmpty()) {
                    sb_names.append(", ");
                } else {
                    sb_names.append(' ');
                }
            }

            // Resolve illustration title IDs (should be done in archive layer)
            StringBuilder sb_titles = new StringBuilder();
            for (String title_id : ill.getTitles()) {
                String title = titles.getTitleById(title_id);

                sb_titles.append(title == null ? title_id : title);
                if (!sb_titles.toString().isEmpty()) {
                    sb_titles.append(", ");
                } else {
                    sb_titles.append(' ');
                }
            }

            StringBuilder html = new StringBuilder("<p class=\"annotation-title\">Illustration</p>");

            if (isNotEmpty(ill.getTitles())) {
                html.append("<p><span class=\"bold\">Titles:</span> ")
                        .append(sb_titles.toString()).append("</p>");
            }
            if (isNotEmpty(ill.getCharacters())) {
                html.append("<p><span class=\"bold\">Characters:</span> ")
                        .append(sb_names.toString()).append("</p>");
            }
            if (isNotEmpty(ill.getTextualElement())) {
                html.append("<p><span class=\"bold\">Textual Elements:</span> ")
                        .append(ill.getTextualElement()).append("</p>");
            }
            if (isNotEmpty(ill.getCostume())) {
                html.append("<p><span class=\"bold\">Costume:</span> ")
                        .append(ill.getCostume()).append("</p>");
            }
            if (isNotEmpty(ill.getInitials())) {
                html.append("<p><span class=\"bold\">Initials:</span> ")
                        .append(ill.getInitials()).append("</p>");
            }
            if (isNotEmpty(ill.getObject())) {
                html.append("<p><span class=\"bold\">Object:</span> ")
                        .append(ill.getObject()).append("</p>");
            }
            if (isNotEmpty(ill.getLandscape())) {
                html.append("<p><span class=\"bold\">Landscape:</span> ")
                        .append(ill.getLandscape()).append("</p>");
            }
            if (isNotEmpty(ill.getArchitecture())) {
                html.append("<p><span class=\"bold\">Architecture:</span> ")
                        .append(ill.getArchitecture()).append("</p>");
            }
            if (isNotEmpty(ill.getOther())) {
                html.append("<p><span class=\"bold\">Other:</span> ")
                        .append(ill.getOther()).append("</p>");
            }

            ann.setDefaultSource(new AnnotationSource("ID", IIIFNames.DC_TEXT, "text/html", html.toString(), "en"));
            ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));

            anns.add(ann);
        }

        return anns;
    }

    private boolean isNotEmpty(String[] str) {
        return str != null && str.length > 0;
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

}
