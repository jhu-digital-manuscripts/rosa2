package rosa.iiif.presentation.core.transform.impl;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringEscapeUtils;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import rosa.archive.core.ArchiveNameParser;
import rosa.archive.core.serialize.AORAnnotatedPageConstants;
import rosa.archive.core.util.Annotations;
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
import rosa.archive.model.aor.Substitution;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.extres.*;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;

import java.io.UnsupportedEncodingException;
import java.io.IOException;

public class AnnotationTransformer extends BasePresentationTransformer implements Transformer<Annotation>,
        AORAnnotatedPageConstants {

    private ArchiveNameParser nameParser;
    private HtmlDecorator decorator;
    private ExternalResourceDb pleaides_db;
    private ExternalResourceDb perseus_db;
    private ExternalResourceDb isni_db;

    @Inject
    public AnnotationTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter,
                                 ArchiveNameParser nameParser) throws IOException {
        super(presRequestFormatter);
        this.nameParser = nameParser;
        this.decorator = new HtmlDecorator();
        this.pleaides_db = new PleaidesGazetteer();
        this.perseus_db = new PerseusDictionary();
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
        isni_db = new ISNIResourceDb(collection);
        if (anno == null) {
            return null;
        } else if (anno instanceof Marginalia) {
            return adaptMarginalia(collection, book, (Marginalia) anno);
        }

        String locationIcon = locationToHtml(anno.getLocation());

        Annotation a = new Annotation();

        a.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), anno.getId()));

        String text = locationIcon + " " + anno.toPrettyString();
        if (anno instanceof Substitution) {
            text = getSubstitutionString((Substitution) anno);
        }

        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.SC_PAINTING);
        a.setDefaultSource(new AnnotationSource(
                "URI", IIIFNames.DC_TEXT, "text/html",
                text,
                (anno.getLanguage() != null && !anno.getLanguage().isEmpty() ? anno.getLanguage() : "en")
        ));

        AnnotationTarget target = locationOnCanvas(
                getPageImage(book.getImages(), getAnnotationPage(anno.getId())),
                Location.FULL_PAGE);
        target.setUri(pres_uris.getCanvasURI(
                collection.getId(),
                book.getId(),
                getAnnotationPage(anno.getId())
        ));

        a.setDefaultTarget(target);

        for (String lang : collection.getAllSupportedLanguages()) {
            a.setLabel(anno.getId(), lang);
        }

        return a;
    }

    private String getSubstitutionString(Substitution sub) {
        StringBuilder res = new StringBuilder();

        String label = sub.getType().substring(0, 1).toUpperCase() + sub.getType().substring(1);

        res.append("<div class=\"substitution\">");
        res.append("<div class=\"sub-title\">")
                .append(StringEscapeUtils.escapeHtml4(sub.getSignature()))
                .append(" : ").append(label).append(':').append("</div>");

        String copy = StringEscapeUtils.escapeHtml4(sub.getCopyText());
        if (copy != null && !copy.isEmpty()) {
            copy = copy.replace("|", "<br/>");
            res.append("<div class=\"section\"><span class=\"italic full-width\">Before:</span>")
                    .append(copy).append("</div>");
        }
        String amended = StringEscapeUtils.escapeHtml4(sub.getAmendedText());
        if (amended != null && !amended.isEmpty()) {
            amended = amended.replace("|", "<br/>");
            res.append("<div class=\"section\"><span class=\"italic full-width\">After:</span>")
                    .append(amended).append("</div>");
        } else if (sub.getType().equals("deletion")) {
            res.append("<div class=\"section\"><span class=\"italic full-width\">After:</span> (deleted)</div>");
        }
        res.append("</div>");

        return res.toString();
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

        anno.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), marg.getId()));
        anno.setMotivation(IIIFNames.SC_PAINTING);
        anno.setDefaultSource(new AnnotationSource("URI", IIIFNames.DC_TEXT, "text/html",
                marginaliaToDisplayHtml(marg), lang));

        AnnotationTarget target = locationOnCanvas(
                getPageImage(book.getImages(), getAnnotationPage(marg.getId())),
                Location.FULL_PAGE);
        target.setUri(pres_uris.getCanvasURI(
                collection.getId(),
                book.getId(),
                getAnnotationPage(anno.getId())
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
                addSimpleElement(writer, "i",null,  "class", "orientation arrow-left");
            }
            if (orientation[1]) {   // Up
                addSimpleElement(writer, "i",null,  "class", "orientation arrow-top");
            }
            writeLocationAsHtml(writer, positions.toArray(new Location[positions.size()]));
            if (orientation[2]) {   // Right
                addSimpleElement(writer, "i",null,  "class", "orientation arrow-right");
            }
            if (orientation[3]) {   // Down
                addSimpleElement(writer, "i", null, "class", "orientation arrow-bottom");
            }
            writer.writeEndElement();
            // ------------

            // Add transcription
            writer.writeStartElement("p");
            addDecoratedText(decorator.decorate(transcription.toString(), pleaides_db, perseus_db), writer);
            writer.writeEndElement();

            // Add translation
            if (isNotEmpty(marg.getTranslation())) {
                writer.writeStartElement("p");
                writer.writeAttribute("class", "italic");
                writer.writeCharacters("[");
                addDecoratedText(decorator.decorate(marg.getTranslation(), pleaides_db, perseus_db, isni_db), writer);
                writer.writeCharacters("]");
                writer.writeEndElement();
            }

            // Add list of People
            if (people.length() > 0) {
                writer.writeStartElement("p");
                addSimpleElement(writer, "span", "People:", "class", "emphasize");
                writer.writeCharacters(" ");
                addDecoratedText(decorator.decorate(trim_right(people, 2), perseus_db, isni_db), writer);
                writer.writeEndElement();
            }

            // Add list of books
            if (books.length() > 0) {
                writer.writeStartElement("p");
                addSimpleElement(writer, "span", "Books:", "class", "emphasize");
                writer.writeCharacters(" " + StringEscapeUtils.escapeHtml4(trim_right(books, 2)));
                writer.writeEndElement();
            }

            // Add list of Locations
            if (locs.length() > 0) {
                writer.writeStartElement("p");
                addSimpleElement(writer, "span", "Locations:", "class", "emphasize");
                writer.writeCharacters(" ");
                addDecoratedText(decorator.decorate(trim_right(locs, 2), pleaides_db), writer);
                writer.writeEndElement();
            }

            // Add list of X-refs
            if (xrefs.size() > 0) {
                writer.writeStartElement("p");
                addSimpleElement(writer, "span", "Cross-references:", "class", "emphasize");
                writer.writeCharacters(" ");
                for (XRef xref : xrefs) {
//                    writer.writeCharacters(StringEscapeUtils.escapeHtml4(xref.getPerson()) + ", ");
                    addDecoratedText(decorator.decorate(xref.getPerson(), isni_db) + ", ", writer);

                    addSimpleElement(writer, "span", StringEscapeUtils.escapeHtml4(xref.getTitle()), "class", "italic");
                    if (isNotEmpty(xref.getText())) {
                        writer.writeCharacters(" &quot;" + StringEscapeUtils.escapeHtml4(xref.getText()) + "&quot;");
                    }
                    writer.writeCharacters("; ");
                }

                writer.writeEndElement();
            }

            writer.writeEndElement();
            return output.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            return "Failed to write out marginalia.";
        }
    }

    /**
     * Create an simple element that may have attributes and may have simple string content.
     * This element cannot have nested elements.
     *
     * @param writer xml stream writer
     * @param element element name
     * @param content simple String content
     * @param attrs array of attributes for the new element, always attribute label followed by attribute value
     *              IF PRESENT, attrs MUST have even number of elements
     */
    private void addSimpleElement(XMLStreamWriter writer, String element, String content, String ... attrs)
            throws XMLStreamException {
        writer.writeStartElement(element);
        if (attrs != null && attrs.length % 2 == 0) {
            for (int i = 0; i < attrs.length - 1;) {
                writer.writeAttribute(attrs[i++], attrs[i++]);
            }
        }
        if (isNotEmpty(content)) {
            writer.writeCharacters(content);
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
            addSimpleElement(writer, "i", null, "class", "inner");
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
            if (image.getName().equals(page) || nameParser.shortName(image.getId()).equals(page) ||
                    image.getId().equals(page)) {
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

        transcription = adapter.toHtml(transcription, (String t) -> decorator.decorate(t, perseus_db));

        if (isNotEmpty(transcription)) {
            Annotation ann = new Annotation();

            ann.setLabel("Transcription for page " + page, "en");
            ann.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), name));
            ann.setMotivation(SC_PAINTING);
            ann.setType(OA_ANNOTATION);

            ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));
            ann.setDefaultSource(new AnnotationSource("ID", IIIFNames.DC_TEXT, "text/html", transcription, "en"));

            return Collections.singletonList(ann);
        } else {
            return Collections.emptyList();
        }
    }

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
            ann.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), anno_name));
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

            String content;
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                XMLStreamWriter xml = XMLOutputFactory.newInstance().createXMLStreamWriter(output);

                addSimpleElement(xml, "p", "Illustration", "class", "annotation-title");

                if (isNotEmpty(ill.getTitles())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Titles:", "class", "bold");
                    xml.writeCharacters(sb_titles.toString());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getCharacters())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Characters:", "class", "bold");
                    addDecoratedText(decorator.decorate(sb_names.toString(), perseus_db), xml);
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getTextualElement())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Textual Elements:", "class", "bold");
                    xml.writeCharacters(ill.getTextualElement());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getCostume())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Costume:", "class", "bold");
                    xml.writeCharacters(ill.getCostume());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getInitials())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Initials:", "class", "bold");
                    xml.writeCharacters(ill.getInitials());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getObject())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Object:", "class", "bold");
                    xml.writeCharacters(ill.getObject());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getLandscape())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Landscape:", "class", "bold");
                    xml.writeCharacters(ill.getLandscape());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getArchitecture())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Architecture:", "class", "bold");
                    xml.writeCharacters(ill.getArchitecture());
                    xml.writeEndElement();
                }
                if (isNotEmpty(ill.getOther())) {
                    xml.writeStartElement("p");
                    addSimpleElement(xml, "span", "Other:", "class", "bold");
                    xml.writeCharacters(ill.getOther());
                    xml.writeEndElement();
                }

                content = output.toString("UTF-8");
            } catch (XMLStreamException | UnsupportedEncodingException e) {
                content = "";
            }

            ann.setDefaultSource(new AnnotationSource("ID", IIIFNames.DC_TEXT, "text/html", content, "en"));
            ann.setDefaultTarget(locationOnCanvas(image, Location.INTEXT));
            anns.add(ann);
        }

        return anns;
    }

    /**
     * Add string content without escaping any potentially included HTML tags.
     * The string must be parsed first. Any included tags will then have to be added to the
     * final document normally through the {@link XMLStreamWriter} (Trying to write a simple
     * String using {@link XMLStreamWriter#writeCharacters(String)} will properly escape
     * any included HTML).
     *
     * Notes: String content must be padded with dummy tags at the beginning and end
     * in order to parse a well-formatted XML fragment, otherwise the parser will die.
     *
     * @param text decorated text, potentially with HTML anchors
     * @param writer xml output
     */
    protected void addDecoratedText(String text, XMLStreamWriter writer) {
        text = "<zz>" + text + "</zz>";

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            saxParser.parse(new InputSource(new StringReader(text)),
                    new DecoratorParserHandler(writer)
            );
        } catch (ParserConfigurationException | SAXException | IOException e) {}
    }

    private boolean isNotEmpty(String[] str) {
        return str != null && str.length > 0;
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

}
