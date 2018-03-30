package rosa.iiif.presentation.core.transform.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
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
import rosa.archive.model.aor.*;
import rosa.iiif.presentation.core.IIIFPresentationRequestFormatter;
import rosa.iiif.presentation.core.extras.DecoratorParserHandler;
import rosa.iiif.presentation.core.extras.ExternalResourceDb;
import rosa.iiif.presentation.core.extras.HtmlDecorator;
import rosa.iiif.presentation.core.extras.ISNIResourceDb;
import rosa.iiif.presentation.core.transform.Transformer;
import rosa.iiif.presentation.model.HtmlValue;
import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.annotation.Annotation;
import rosa.iiif.presentation.model.annotation.AnnotationSource;
import rosa.iiif.presentation.model.annotation.AnnotationTarget;
import rosa.iiif.presentation.model.selector.FragmentSelector;

import java.io.UnsupportedEncodingException;

public class AnnotationTransformer extends BasePresentationTransformer implements Transformer<Annotation>,
        AORAnnotatedPageConstants {

    private ArchiveNameParser nameParser;
    private HtmlDecorator decorator;
    private ISNIResourceDb isni_db;

    @Inject
    public AnnotationTransformer(@Named("formatter.presentation") IIIFPresentationRequestFormatter presRequestFormatter,
                                 ArchiveNameParser nameParser) {
        super(presRequestFormatter);
        this.nameParser = nameParser;
        this.decorator = new HtmlDecorator();
    }

    @Override
    public Annotation transform(BookCollection collection, Book book, String name) {
        // Find annotation in book
        rosa.archive.model.aor.Annotation archiveAnno = Annotations.getArchiveAnnotation(book, name);

        // Transform archive anno -> iiif anno
        return adaptAnnotation(collection, book, archiveAnno, null);
    }

    public Annotation transform(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno) {
        return adaptAnnotation(collection, book, anno, null);
    }

    @Override
    public Class<Annotation> getType() {
        return Annotation.class;
    }

    public Annotation transform(BookCollection col, Book book, BookImage image, rosa.archive.model.aor.Annotation anno) {
        return adaptAnnotation(col, book, anno, image);
    }

    private Annotation adaptAnnotation(BookCollection collection, Book book, rosa.archive.model.aor.Annotation anno, BookImage image) {
        if (isni_db == null) {
            isni_db = new ISNIResourceDb(collection);
        } else {
            isni_db.setCollection(collection);
        }
        if (anno == null) {
            return null;
        } else if (anno instanceof Marginalia) {
            return adaptMarginalia(collection, book, (Marginalia) anno, image);
        }

        String language = anno.getLanguage() != null && !anno.getLanguage().isEmpty() ? anno.getLanguage() : "en";
        String locationIcon = locationToHtml(anno.getLocation());

        Annotation a = new Annotation();

        a.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), anno.getId()));
        a.setType(IIIFNames.OA_ANNOTATION);
        a.setMotivation(IIIFNames.SC_PAINTING);
        a.getMetadata().put("type", new HtmlValue(anno.getClass().getSimpleName()));

        if (anno instanceof Drawing) {
            a.setDefaultSource(new AnnotationSource(
                    "moo", DC_TEXT, "text/html", drawingToDisplayHtml(collection, (Drawing) anno), language));
        } else if (anno instanceof Table) {
            a.setDefaultSource(new AnnotationSource(
                    "moo", DC_TEXT, "text/html", tableToDisplayHtml(collection, (Table) anno), language));
        } else if (anno instanceof Graph) {
            a.setDefaultSource(new AnnotationSource(
                    "moo", DC_TEXT, "text/html", graphToDisplayHtml(collection, (Graph) anno), language));
        } else {
            a.setDefaultSource(new AnnotationSource("URI", IIIFNames.DC_TEXT, "text/html",
                    locationIcon + " " + anno.toPrettyString(), language));
        }

        if (image == null) {
            image = getPageImage(book.getImages(), getAnnotationPage(anno.getId()));
        }
        AnnotationTarget target = locationOnCanvas(image, Location.FULL_PAGE);
        target.setUri(pres_uris.getCanvasURI(
                collection.getId(),
                book.getId(),
                image.getName()
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
    private Annotation adaptMarginalia(BookCollection collection, Book book, Marginalia marg, BookImage image) {
        String lang = marg.getLanguages() != null && marg.getLanguages().size() > 0
                ? marg.getLanguages().get(0).getLang() : "en";

        Annotation anno = new Annotation();

        anno.setId(pres_uris.getAnnotationURI(collection.getId(), book.getId(), marg.getId()));
        anno.setMotivation(IIIFNames.SC_PAINTING);
        anno.getMetadata().put("type", new HtmlValue("Marginalia"));
        anno.setDefaultSource(new AnnotationSource("URI", IIIFNames.DC_TEXT, "text/html",
                marginaliaToDisplayHtml(collection, marg), lang));

        /*
            #getAnnotationPage(String) -- This will not work if an annotation has a pre-defined ID
            (( only works with our home-baked IDs ))
         */
        if (image == null) {
            image = getPageImage(book.getImages(), getAnnotationPage(marg.getId()));
        }
        AnnotationTarget target = locationOnCanvas(image, Location.FULL_PAGE);
        target.setUri(pres_uris.getCanvasURI(
                collection.getId(),
                book.getId(),
                image.getName()
        ));

        anno.setDefaultTarget(target); // TODO actual position(s)
        anno.setLabel(marg.getId(), "en");

        return anno;
    }

    private String graphToDisplayHtml(BookCollection col, Graph graph) {
        StringBuilder people = new StringBuilder();
        StringBuilder books = new StringBuilder();
        StringBuilder locs = new StringBuilder();
        StringBuilder symbols = new StringBuilder();
        StringBuilder notes = new StringBuilder();      // Notes that target the graph, not an individual node
        StringBuilder notesTr = new StringBuilder();

        for (GraphText gt : graph.getGraphTexts()) {
            add(people, gt.getPeople(), ", ");
            add(books, gt.getBooks(), ", ");
            add(locs, gt.getLocations(), ", ");
            add(symbols, gt.getSymbols(), ", ");
            gt.getNotes().forEach(note -> {
                notes.append(note.content);
                if (note.internalLink == null || note.internalLink.isEmpty()) {
                    notes.append(note.content).append(", ");
                }
            });
            add(notesTr, gt.getTranslations(), ", ");
        }

        // ----------------------------------------------------------------------------------------------
        // ----- Write XML ------------------------------------------------------------------------------
        XMLOutputFactory outF = newOutputFactory();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            XMLStreamWriter writer = outF.createXMLStreamWriter(output);

            writer.writeStartElement("p");

            assembleLocationIcon(orientation(graph.getOrientation()), new Location[] { graph.getLocation() }, writer);

            writer.writeStartElement("p");      // Nodes
            addSimpleElement(writer, "span", "Nodes: ", "class", "emphasize");
            for (int i = 0; i < graph.getNodes().size(); i++) {
                if (i > 0) {
                    writer.writeEmptyElement("br");
                }
                GraphNode node = graph.getNodes().get(i);

                writer.writeCharacters(node.getContent());
                if (isNotEmpty(node.getPerson())) {
                    writer.writeCharacters(" (" + node.getPerson() + ")");
                }

                String note = getNoteForGraphNode(node.getId(), graph);
                if (isNotEmpty(note)) {
                    writer.writeCharacters(" Note: " + note);
                }
            }
            writer.writeEndElement();

            addListOfValues("Notes:", notes.toString(), writer);
            addListOfValues("Notes (translated):", notesTr.toString(), writer);
            addListOfValues("People:", people.toString(), writer, isni_db);
            addListOfValues("Books:", books.toString(), writer);
            addListOfValues("Locations:", locs.toString(), writer);
            addListOfValues("Symbols:", symbols.toString(), writer);

            addInternalRefs(col, graph.getInternalRefs(), writer);

            writer.writeEndElement();
            return output.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            return "Failed to write out graph as HTML.";
        }
    }

    private String getNoteForGraphNode(String nodeId, Graph graph) {
        if (nodeId == null || nodeId.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        // Each graph text, find any a note with an 'internal_link' matching 'nodeId'.
        // Add each of those to the final result.
        graph.getGraphTexts().stream()
                .map(GraphText::getNotes)
                .forEach(notes ->
                    result.append(notes.stream().filter(note -> nodeId.equals(note.internalLink))
                            .map(moo -> moo.content)
                            .findFirst()
                            .orElse("")).append(' ')
                );
        return result.toString();
    }

    private String drawingToDisplayHtml(BookCollection col, Drawing drawing) {
        XMLOutputFactory outF = newOutputFactory();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            XMLStreamWriter writer = outF.createXMLStreamWriter(output);
            writer.writeStartElement("p");

            int orientation = 0;
            try {
                orientation = Integer.parseInt(drawing.getOrientation());
            } catch (NumberFormatException e) {}

            assembleLocationIcon(orientation(orientation), new Location[] {drawing.getLocation()}, writer);
//            addSimpleElement(writer, "span", "Drawing", "class", "annotation-title");
            writer.writeCharacters(" " + drawing.getType().replaceAll("_", " "));

            addTranslation(drawing.getTranslation(), writer);

            addListOfValues("Symbols:", drawing.getSymbols(), ", ", writer);
            addListOfValues("People:", drawing.getPeople(), ", ", writer, isni_db);
            addListOfValues("Books:", drawing.getBooks(), ", ", writer);
            addListOfValues("Locations:", drawing.getLocations(), ", ", writer);

            addInternalRefs(col, drawing.getInternalRefs(), writer);

            writer.writeEndElement();
            return output.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            return "Failed to write out drawing as HTML.";
        }
    }

    private String tableToDisplayHtml(BookCollection col, Table table) {
        XMLOutputFactory outF = newOutputFactory();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            XMLStreamWriter writer = outF.createXMLStreamWriter(output);

            writer.writeStartElement("p");
//            addSimpleElement(writer, "span", "Table", "class", "annotation-title");
            if (isNotEmpty(table.getType())) {
                writer.writeCharacters(" " + table.getType().replaceAll("_", " "));
            }

            writer.writeStartElement("p");
            addSimpleElement(writer, "span", "Text:", "class", "emphasize");
            for (TextEl txt : table.getTexts()) {
                writer.writeEmptyElement("br");
                writer.writeCharacters(txt.getText());
            }
            writer.writeEndElement();

            addTranslation(table.getTranslation(), writer);
            addListOfValues("Symbols:", table.getSymbols(), ", ", writer);
            addListOfValues("People:", table.getPeople(), ", ", writer, isni_db);
            addListOfValues("Books:", table.getBooks(), ", ", writer);
            addListOfValues("Locations:", table.getLocations(), ", ", writer);
            addInternalRefs(col, table.getInternalRefs(), writer);

            writer.writeEndElement();
            return output.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            return "Failed to write out table as HTML.";
        }
    }

    // Must make sure to escape text appropriately
    private String marginaliaToDisplayHtml(BookCollection col, Marginalia marg) {
        StringBuilder transcription = new StringBuilder();
        StringBuilder people = new StringBuilder();
        StringBuilder books = new StringBuilder();
        StringBuilder locs = new StringBuilder();
        StringBuilder symb = new StringBuilder();

        // Left, top, right, bottom
        boolean[] orientation = new boolean[4];
        List<Location> positions = new ArrayList<>();
        List<XRef> xrefs = new ArrayList<>();
        List<InternalReference> iRefs = new ArrayList<>();

        for (MarginaliaLanguage lang : marg.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                add(transcription, pos.getTexts(), " ");
                add(people, pos.getPeople(), ", ");
                add(books, pos.getBooks(), ", ");
                add(locs, pos.getLocations(), ", ");
                add(symb, pos.getSymbols(), ", ");
                xrefs.addAll(pos.getxRefs());
                iRefs.addAll(pos.getInternalRefs());
                
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

        XMLOutputFactory outF = newOutputFactory();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            XMLStreamWriter writer = outF.createXMLStreamWriter(output);

            writer.writeStartElement("p");

            // ------ Add orientation + location icons ------
            assembleLocationIcon(orientation, positions.toArray(new Location[positions.size()]), writer);

            if (isNotEmpty(marg.getOtherReader())) {
                writer.writeStartElement("p");
                writer.writeCharacters("Reader: " + marg.getOtherReader());
                writer.writeEndElement();
            }

            // Add transcription
            writer.writeStartElement("p");
            writer.writeCharacters(StringEscapeUtils.escapeHtml4(transcription.toString()));
            writer.writeEndElement();

            // Add translation
            addTranslation(marg.getTranslation(), writer);

            addListOfValues("Symbols:", symb.toString(), writer);
            addListOfValues("People:", people.toString(), writer, isni_db);
            addListOfValues("Books:", books.toString(), writer);
            addListOfValues("Locations:", locs.toString(), writer);

            // Add list of X-refs
            addXRefs(xrefs, writer);
            addInternalRefs(col, iRefs, writer);

            writer.writeEndElement();
            return output.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            return "Failed to write out marginalia.";
        }
    }

    private boolean[] orientation(int orientation) {
        switch (orientation) {
            case 0:
                return new boolean[] { false, true, false, false };
            case 90:
                return new boolean[] { true, false, false, false };
            case 180:
                return new boolean[] { false, false, false, true };
            case 270:
                return new boolean[] { false, false, true, false };
            default:
                return new boolean[] { false, false, false, false };
        }
    }

    private void assembleLocationIcon(boolean[] orientation, Location[] locations, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("span");
        writer.writeAttribute("class", "aor-icon-container");
        if (orientation[0]) {   // Left
            addSimpleElement(writer, "i",null,  "class", "orientation arrow-left");
        }
        if (orientation[1]) {   // Up
            addSimpleElement(writer, "i",null,  "class", "orientation arrow-top");
        }
        writeLocationAsHtml(writer, locations);
        if (orientation[2]) {   // Right
            addSimpleElement(writer, "i",null,  "class", "orientation arrow-right");
        }
        if (orientation[3]) {   // Down
            addSimpleElement(writer, "i", null, "class", "orientation arrow-bottom");
        }
        writer.writeEndElement();
    }

    private void addTranslation(String translation, XMLStreamWriter writer) throws XMLStreamException {
        if (isNotEmpty(translation)) {
            String content = "[" + StringEscapeUtils.escapeHtml4(translation) + "]";
            addSimpleElement(writer, "p", content, "class", "italic");
        }
    }

    private void addListOfValues(String label, List<String> vals, String separator, XMLStreamWriter writer, ExternalResourceDb ... externalDbs) throws XMLStreamException {
        StringBuilder str = new StringBuilder();
        add(str, vals, separator);
        addListOfValues(label, str.toString(), writer);
    }

    private void addListOfValues(String label, String listStr, XMLStreamWriter writer, ExternalResourceDb ... externalDbs) throws XMLStreamException {
        if (isNotEmpty(listStr)) {
            writer.writeStartElement("p");
            addSimpleElement(writer, "span", label, "class", "emphasize");
            if (externalDbs == null || externalDbs.length == 0) {
                writer.writeCharacters(" " + StringEscapeUtils.escapeHtml4(listStr));
            } else {
                addDecoratedText(decorator.decorate(listStr.trim(), externalDbs), writer);
            }
            writer.writeEndElement();
        }
    }

    private void addXRefs(List<XRef> xRefs, XMLStreamWriter writer) throws XMLStreamException {
        if (xRefs == null || xRefs.isEmpty()) {
            return;
        }

        writer.writeStartElement("p");
        addSimpleElement(writer, "span", "Cross-references:", "class", "emphasize");
        writer.writeCharacters(" ");
        for (int i = 0; i < xRefs.size(); i++) {
            XRef xref = xRefs.get(i);
            if (i > 0) {
                writer.writeCharacters("; ");
            }
            writer.writeCharacters(StringEscapeUtils.escapeHtml4(xref.getPerson()) + ", ");
            addSimpleElement(writer, "span", StringEscapeUtils.escapeHtml4(xref.getTitle()), "class", "italic");
            if (isNotEmpty(xref.getText())) {
                writer.writeCharacters(" &quot;" + StringEscapeUtils.escapeHtml4(xref.getText()) + "&quot;");
            }
            writer.writeCharacters("; ");
        }

        writer.writeEndElement();
    }

    private void addInternalRefs(BookCollection col, List<InternalReference> refs, XMLStreamWriter writer) throws XMLStreamException {
        if (refs == null || refs.isEmpty()) {
            return;
        }
        writer.writeStartElement("p");

        addSimpleElement(writer, "span", "Internal References:", "class", "emphasize");
        writer.writeCharacters(" ");

        for (int i = 0; i < refs.size(); i++) {
            InternalReference ref = refs.get(i);
            if (i > 0) {
                writer.writeCharacters(", ");
            }

            writer.writeCharacters(StringEscapeUtils.escapeHtml4(ref.getText()));
            for (int j = 0; j < ref.getTargets().size(); j++) {
                ReferenceTarget tar = ref.getTargets().get(j);

                AorLocation location = col.getAnnotationMap().get(tar.getTargetId());
                String targetId = targetId(location);
                if (targetId != null) {
                    writer.writeCharacters("(");
                    writer.writeStartElement("a");
                    writer.writeAttribute("class", "internal-ref");
                    writer.writeAttribute("href", "javascript:;");
                    writer.writeAttribute("data-targetId", targetId);
                    writer.writeAttribute("data-manifestid", manifestId(location));
                    writer.writeCharacters("[" + tar.getText() + "]");
                    writer.writeEndElement();
                    writer.writeCharacters(")");
                } else {
                    writer.writeCharacters("[" + tar.getText() + "]");
                }
            }
        }

        writer.writeEndElement();
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

    // Add strings to builder separated by the given string
    private void add(StringBuilder sb, List<String> list, String sep) {
        for (int i = 0; i < list.size(); i++) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(list.get(i));
        }
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
                    xml.writeCharacters(sb_names.toString());
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

    private XMLOutputFactory newOutputFactory() {
        XMLOutputFactory outF = XMLOutputFactory.newInstance();
        outF.setProperty("escapeCharacters", false);
        return outF;
    }

    private String targetId(AorLocation loc) {
        if (loc == null) {
            return null;
        }

        String uri = null;

        String page = loc.getPage();
        while (page.startsWith("0")) {
            page = page.substring(1);
        }

        if (isNotEmpty(loc.getAnnotation())) {
//            uri = pres_uris.getAnnotationURI(loc.getCollection(), loc.getBook(), loc.getAnnotation());
            uri = pres_uris.getCanvasURI(loc.getCollection(), loc.getBook(), page);
        } else if (isNotEmpty(loc.getPage())) {
            uri = pres_uris.getCanvasURI(loc.getCollection(), loc.getBook(), page);
        } else if (isNotEmpty(loc.getBook())) {
            uri = pres_uris.getManifestURI(loc.getCollection(), loc.getBook());
        } else if (isNotEmpty(loc.getCollection())) {
            uri = pres_uris.getCollectionURI(loc.getCollection());
        }

        if (isNotEmpty(uri)) {
            return uri;
        } else {
            return null;
        }
    }

    private String manifestId(AorLocation loc) {
        if (loc == null || !isNotEmpty(loc.getBook())) {
            return null;
        }
        return pres_uris.getManifestURI(loc.getCollection(), loc.getBook());
    }

}
