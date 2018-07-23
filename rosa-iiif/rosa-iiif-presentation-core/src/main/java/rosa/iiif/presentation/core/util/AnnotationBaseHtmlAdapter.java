package rosa.iiif.presentation.core.util;

import org.apache.commons.lang3.StringEscapeUtils;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.AorLocation;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.search.model.SearchField;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public abstract class AnnotationBaseHtmlAdapter<T> {
    protected final PresentationUris pres_uris;

    protected XMLStreamWriter writer;

    public AnnotationBaseHtmlAdapter(PresentationUris pres_uris) {
        this.pres_uris = pres_uris;
    }

    public String adapt(BookCollection col, Book book, BookImage page, T annotation) {
        XMLOutputFactory outF = newOutputFactory();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            this.writer = outF.createXMLStreamWriter(output);
            annotationAsHtml(col, book, page, annotation);
            return output.toString("UTF-8");
        } catch (XMLStreamException | UnsupportedEncodingException e) {
            return "";
        }
    }

    abstract Class<T> getAnnotationType();

    /**
     *
     * @param col
     * @param book
     * @param page
     * @param annotation
     * @throws XMLStreamException
     */
    abstract void annotationAsHtml(BookCollection col, Book book, BookImage page, T annotation) throws XMLStreamException;

    boolean isNotEmpty(String[] str) {
        return str != null && str.length > 0;
    }

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
                                   XMLStreamWriter writer) throws XMLStreamException {
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

    void addInternalRefs(BookCollection col, List<InternalReference> refs, XMLStreamWriter writer) throws XMLStreamException {
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
