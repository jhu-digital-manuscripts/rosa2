package rosa.iiif.presentation.core.html;

import org.apache.commons.lang3.StringEscapeUtils;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.AorLocation;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.extras.ExternalResourceDb;
import rosa.iiif.presentation.core.util.AnnotationLocationUtil;
import rosa.search.model.SearchField;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This base class provides a starting point with useful utilities to adapt the content
 * of an annotation object to an HTML string that can be displayed to a user. The HTML
 * content will be generated and added to an XMLStreamWriter in
 * {@link #annotationAsHtml(BookCollection, Book, BookImage, Object)}
 * then stringified and sent back to the original caller.
 *
 * @param <T>
 */
public abstract class AnnotationBaseHtmlAdapter<T> {
    private static final Logger LOGGER = Logger.getLogger("AnnotationHtmlAdapter");
    protected static final boolean[] NO_ORIENTATION = new boolean[] { false, false, false, false };

    protected final PresentationUris pres_uris;
    private List<ExternalResourceDb> externalDbs;

    protected XMLStreamWriter writer;

    AnnotationBaseHtmlAdapter(PresentationUris pres_uris) {
        this.pres_uris = pres_uris;
        this.externalDbs = new ArrayList<>();
    }

    private void setExternalDbs(ExternalResourceDb ... externalDbs) {
        this.externalDbs.clear();
        this.externalDbs.addAll(Arrays.asList(externalDbs));
    }

    /**
     * Adapt the contents of an annotation object to an HTML string
     *
     * @param col book collection object
     * @param book book object
     * @param page information for specific page
     * @param annotation annotation to adapt
     * @param externalDbs external DBs to lookup related URIs
     * @return Stringified HTML
     */
    public String adapt(BookCollection col, Book book, BookImage page, T annotation, ExternalResourceDb ... externalDbs) {
        XMLOutputFactory outF = newOutputFactory();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        setExternalDbs(externalDbs);

        try {
            this.writer = outF.createXMLStreamWriter(output);
            annotationAsHtml(col, book, page, annotation);
            return output.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * @return Class for annotation that an implementation will adapt
     */
    abstract Class<T> getAnnotationType();

    /**
     * Adapt the given annotation to HTML as XML using the class' XMLStreamWriter.
     *
     * @param col book collection object
     * @param book book object
     * @param page info for the page
     * @param annotation annotation to adapt
     * @throws XMLStreamException .
     */
    abstract void annotationAsHtml(BookCollection col, Book book, BookImage page, T annotation) throws XMLStreamException;

    boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    // Add strings to builder separated by the given string
    void add(StringBuilder sb, List<String> list, String sep) {
        for (int i = 0; i < list.size(); i++) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(list.get(i));
        }
    }

    /**
     * Add a formatted translation string to the current XML chunk
     *
     * @param translation translation string to add
     * @param writer XML writer
     * @throws XMLStreamException .
     */
    void addTranslation(String translation, XMLStreamWriter writer) throws XMLStreamException {
        if (isNotEmpty(translation)) {
            String content = "[" + StringEscapeUtils.escapeHtml4(translation) + "]";
            addSimpleElement(writer, "p", content, "class", "italic");
        }
    }

    void addListOfValues(String label, List<String> vals, XMLStreamWriter writer) throws XMLStreamException {
        StringBuilder str = new StringBuilder();
        add(str, vals, ", ");
        addListOfValues(label, str.toString(), writer);
    }

    void addListOfValues(String label, String listStr, XMLStreamWriter writer) throws XMLStreamException {
        if (isNotEmpty(listStr)) {
            writer.writeStartElement("p");
            addSimpleElement(writer, "span", label, "class", "emphasize");
            writer.writeCharacters(" " + StringEscapeUtils.escapeHtml4(listStr));
            writer.writeEndElement();
        }
    }

    void addSearchableList(String label, List<String> vals, SearchField searchField, String withinUri,
                                   XMLStreamWriter writer, Class<?> ... desiredExternalDbs) throws XMLStreamException {
        if (vals == null || vals.size() == 0) {
            return;
        }

        writer.writeStartElement("p");
        addSimpleElement(writer, "span", label, "class", "emphasize");
        for (int i = 0; i < vals.size(); i++) {
            String val = vals.get(i);

            writer.writeStartElement("a");
            writer.writeAttribute("href", "javascript:;");
            writer.writeAttribute("class", "searchable");
            writer.writeAttribute("data-searchfield", searchField.getFieldName());
            writer.writeAttribute("data-searchwithin", withinUri);

            if (desiredExternalDbs != null && desiredExternalDbs.length > 0 && externalDbs != null) {
                Arrays.stream(desiredExternalDbs).forEach(dbClass ->
                    externalDbs.stream().filter(db -> dbClass.equals(db.getClass())).forEach(db -> {
                        try {
                            URI result = db.lookup(val);
                            if (result != null) {
                                writer.writeAttribute("data-" + db.label(), result.toString());
                            }
                        } catch (XMLStreamException e) {
                            /* If error occurs, just skip it */
                            LOGGER.log(Level.WARNING, "External DB lookup failed ", e);
                        }
                    })
                );
            }

            writer.writeCharacters((i == 0 ? " " : ", ") + StringEscapeUtils.escapeHtml4(val));
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    void addXRefs(List<XRef> xRefs, XMLStreamWriter writer) throws XMLStreamException {
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

    void addInternalRefs(BookCollection col, Annotation a, List<InternalReference> refs, XMLStreamWriter writer) throws XMLStreamException {
        if (refs == null || refs.isEmpty()) {
            return;
        }
        writer.writeStartElement("p");

        addSimpleElement(writer, "span", "Internal References:", "class", "emphasize");
        writer.writeCharacters(" ");

        if (hasExtraInternalRef(col, a)) {
            InternalReference annoRef = fromInternalRefAttr(a);
            writeInternalRef(col, annoRef.getTargets().get(0), writer);
        }

        for (int i = 0; i < refs.size(); i++) {
            InternalReference ref = refs.get(i);
            if (i > 0 || hasExtraInternalRef(col, a)) {
                writer.writeCharacters(", ");
            }

            writer.writeCharacters(StringEscapeUtils.escapeHtml4(ref.getText()));
            for (ReferenceTarget tar : ref.getTargets()) {
                writeInternalRef(col, tar, writer);
            }
        }

        writer.writeEndElement();
    }

    private void writeInternalRef(BookCollection col, ReferenceTarget target, XMLStreamWriter writer) throws XMLStreamException {
        AorLocation loc = col.getAnnotationMap().get(target.getTargetId());

        if (targetId(loc) != null) {
            writer.writeStartElement("a");
            writer.writeAttribute("class", "internal-ref");
            writer.writeAttribute("href", "javascript:;");
            writer.writeAttribute("data-targetId", targetId(loc));
            writer.writeAttribute("data-manifestid", manifestId(loc));
            writer.writeCharacters(target.getText());
            writer.writeEndElement();
        } else {
            writer.writeCharacters(target.getText());
        }
    }

    private boolean hasExtraInternalRef(BookCollection col, Annotation a) {
        return isNotEmpty(a.getInternalRef()) && col.getAnnotationMap().containsKey(a.getInternalRef());
    }

    private InternalReference fromInternalRefAttr(Annotation a) {
        String text = a.getReferencedText();
        return new InternalReference(text, Collections.singletonList(new ReferenceTarget(a.getInternalRef(), text)));
    }

    /**
     * Insert internal references found in an annotation into its transcription.
     *
     * Note this impl won't handle the case where two references have the same source, though
     * ideally, this case will not appear, as we can have a reference with the same source and
     * multiple targets.
     *
     * @param transcription original annotation transcription text
     * @param refs list of internal references
     * @return newly modified transcription
     */
    String addInternalRefs(String transcription, List<InternalReference> refs) {
        if (transcription == null || transcription.isEmpty()) {
            return "";
        }
        if (refs == null || refs.size() == 0) {
            return transcription;
        }

        for (InternalReference ref : refs) {
            // Source text with identifying prefix/suffix
            final String fullSource = (ref.getAnchorPrefix() != null && !ref.getAnchorPrefix().isEmpty() ? ref.getAnchorPrefix() : "") +
                                        ref.getAnchor() +
                                        (ref.getAnchorSuffix() != null && !ref.getAnchorSuffix().isEmpty() ? ref.getAnchorSuffix() : "");

            for (ReferenceTarget target : ref.getTargets()) {
                final String start = "<a href=\"" + target.getTargetId() + "\" target=\"_blank\">";
                final String end = "</a>";

                // Split by source text, then insert text on split as needed
                String[] parts = transcription.split(fullSource);

                StringBuilder result = new StringBuilder();
                if (parts.length > 0) {
                    result.append(parts[0]);
                    for (int i = 1; i < parts.length; i++) {
                        result.append(ref.getAnchorPrefix()).append(start).append(ref.getAnchor())
                                .append(end).append(ref.getAnchorSuffix());
                        result.append(parts[i]);
                    }

                    // "Full Source" appears at the end of the transcription
                    if (transcription.lastIndexOf(fullSource) + fullSource.length() == transcription.length()) {
                        result.append(ref.getAnchorPrefix()).append(start).append(ref.getAnchor())
                                .append(end).append(ref.getAnchorSuffix());
                    }
                } else { // Transcription should be exactly equal to "fullSource"
                    result.append(ref.getAnchorPrefix()).append(start).append(ref.getAnchor())
                            .append(end).append(ref.getAnchorSuffix());
                }

                transcription = result.toString();
            }
        }

        return transcription;
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
    void addSimpleElement(XMLStreamWriter writer, String element, String content, String ... attrs)
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

    boolean[] orientation(int orientation) {
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

    void assembleLocationIcon(boolean[] orientation, Location[] locations, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("span");
        writer.writeAttribute("class", "aor-icon-container");
        if (orientation[0]) {   // Left
            addSimpleElement(writer, "i",null,  "class", "orientation arrow-left");
        }
        if (orientation[1]) {   // Up
            addSimpleElement(writer, "i",null,  "class", "orientation arrow-top");
        }
        AnnotationLocationUtil.writeLocationAsHtml(writer, locations);
        if (orientation[2]) {   // Right
            addSimpleElement(writer, "i",null,  "class", "orientation arrow-right");
        }
        if (orientation[3]) {   // Down
            addSimpleElement(writer, "i", null, "class", "orientation arrow-bottom");
        }
        writer.writeEndElement();
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

    private XMLOutputFactory newOutputFactory() {
        XMLOutputFactory outF = XMLOutputFactory.newInstance();
        outF.setProperty("escapeCharacters", false);
        return outF;
    }
}
