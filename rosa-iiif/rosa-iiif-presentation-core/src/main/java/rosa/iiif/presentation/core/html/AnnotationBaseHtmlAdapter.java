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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This base class provides a starting point with useful utilities to adapt the content
 * of an annotation object to an HTML string that can be displayed to a user. The HTML
 * content will be generated and added to an XMLStreamWriter in
 * {@link #annotationAsHtml(BookCollection, Book, BookImage, Object)}
 * then stringified and sent back to the original caller.
 *
 * @param <T>
 */
public abstract class AnnotationBaseHtmlAdapter<T> implements AnnotationConstants {
    private static final Logger LOGGER = Logger.getLogger("AnnotationHtmlAdapter");
//    protected static final boolean[] NO_ORIENTATION = new boolean[] { false, false, false, false };

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
//                writer.writeCharacters("<br/>");
                writer.writeCharacters(", ");
            }

            if (ref.getText() != null && !ref.getText().isEmpty()) {
                writer.writeCharacters(StringEscapeUtils.escapeHtml4(ref.getText()));
                writer.writeCharacters(", ");
            }
            for (int j = 0; j < ref.getTargets().size(); j++) {
//            for (ReferenceTarget tar : ref.getTargets()) {
                ReferenceTarget tar = ref.getTargets().get(j);
                if (j > 0) {
                    writer.writeCharacters(", ");
                }
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
     * Mutate transcription text by inserting internal references found in an annotation into its transcription.
     *
     * Note: "source text" identifies the specific text in the current annotation that is referring to
     * another place. It seems that in the transcriptions, this text was placed on the <target text="" />
     * element.
     *
     * Another Note: A subset of internal_refs in the corpus have the source text in the internal_ref#text
     * attribute, opposite of the normal behavior - which is to have the source text in internal_ref/target#text.
     * In this anomalous behavior, we will for now ignore the target#text attribute.
     *
     * @param transcription original annotation transcription text
     * @param refs list of internal references
     * @return newly modified transcription
     */
    String addInternalRefs(BookCollection col, String transcription, List<InternalReference> refs) {
        if (transcription == null || transcription.isEmpty()) {
            return "";
        }
        if (refs == null || refs.size() == 0) {
            return transcription;
        }

        for (InternalReference ref : refs) {
            // TODO temp block preventing decoration of references with more than 1 target to avoid user confusion
            if (ref.getTargets().size() > 1) {
                continue;
            }

            boolean textInRef = ref.getText() != null && !ref.getText().isEmpty();

            for (ReferenceTarget target : ref.getTargets()) {
                String sourcePrefix;
                String sourceText;
                String sourceSuffix;
                String label;

                if (textInRef) {
                    sourcePrefix = "";
                    sourceSuffix = "";
                    sourceText = ref.getText();
                    label = target.getText();
                } else {
                    sourcePrefix = target.getTextPrefix();
                    sourceText = target.getText();
                    sourceSuffix = target.getTextSuffix();
                    label = ref.getText();
                }

                if (!resolvable(col, target.getTargetId())) {
                    continue;
                }

                transcription = decorate(transcription, sourcePrefix, sourceText, sourceSuffix,
                        target.getTargetId(), label, col);
            }
        }

        return transcription;
    }

    /**
     * Decorate the transcription with a given {@link ReferenceTarget} in an {@link InternalReference}.
     *
     * A single internal reference may have multiple targets where the same bit of text can point to
     * multiple places. In this case, the first target will decorate the transcription with an <a> tag.
     * Subsequent targets will try to match the same text, which will have been modified by the first
     * target. So instead of a simple string match, we may have to match a REGEX, matching the prefix,
     * a possible anchor tag with arbitrary attributes, the actual link text, a possible end tag, finally
     * the suffix.
     *
     * From there, we can pull that string from the original transcription. If the anchor tag is not there,
     * we can do a simple drop in replacement with an anchor tag, as {@link #buildLink(BookCollection, String, String, String)}.
     * If the anchor tag is already there, we can add a new 'data-' attribute with the new target URI. This
     * 'data-' attribute will be a 'data-targetid#' where the number is determined by how many other
     * 'data-targetid#' attributes already exist. The UI should then be able to match all 'data-targetid*'.
     *
     * @param transcription full original transcription
     * @param prefix ref prefix
     * @param text ref text
     * @param suffix ref suffix
     * @param targetId ID of the reference target
     * @param col book collection
     * @return decorated HTML-ified transcription
     */
    private String decorate(String transcription, String prefix, String text, String suffix,
                            String targetId, String label, BookCollection col) {
        // REGEX Groups:                          1                   2               3                 4               5
        Pattern p = Pattern.compile("(" + escapeRegex(prefix) + ")(<a.*>)?(" + escapeRegex(text) + ")(</a>)?(" + escapeRegex(suffix) + ")");
        Matcher matcher = p.matcher(transcription);

        String targetUri = targetId(col.getAnnotationMap().get(targetId));

        StringBuilder sb = new StringBuilder();
        int start = 0;
        int end = 0;
        while (matcher.find()) {
            sb.append(transcription.substring(start, matcher.start()));

            start = matcher.start();

            if (matcher.group(2) == null) { // No 'anchor' tag present
                sb.append(simpleReplace(matcher.group(0), prefix, text, suffix, buildLink(col, text, targetId, label)));
            } else {
                sb.append(prefix).append(replaceAnchorStart(matcher.group(2), targetUri, label));
                sb.append(text).append(matcher.group(4)).append(suffix);
            }

            end = matcher.end();
        }
        sb.append(transcription.substring(end));

        return sb.toString();

    }

    private String escapeRegex(String in) {
        if (in == null) {
            return "";
        }
        return StringEscapeUtils.escapeJava(in).replaceAll("\\[", "\\\\[");
    }

    private String replaceAnchorStart(String anchor, String targetUri, String label) {
        int targetIndex = anchor.split("data-targetid").length - 1;
        return anchor.substring(0, anchor.length() - 1) + " data-targetid" + targetIndex + "=\"" + targetUri + "\""
                + (label != null ? " data-label" + targetIndex + "=\"" + label + "\" " : "")
                + anchor.substring(anchor.length() - 1);
    }

    /**
     * This is used to decorate some text with an internal reference, if it has not already been
     * decorated.
     *
     * @param transcription full original transcription
     * @param prefix ref prefix
     * @param text ref text
     * @param suffix ref suffix
     * @param link 'text' surrounded by an anchor tag
     * @return decorated HTML-ified transcription
     */
    private String simpleReplace(String transcription, String prefix, String text, String suffix, String link) {
        StringBuilder original = new StringBuilder();
        StringBuilder modified = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) {
            original.append(prefix);
            modified.append(prefix);
        }
        original.append(text);
        modified.append(link);
        if (suffix != null && !suffix.isEmpty()) {
            original.append(suffix);
            modified.append(suffix);
        }

        return transcription.replace(original, modified);
    }

    private boolean resolvable(BookCollection col, String target) {
        return col.getAnnotationMap().containsKey(target);
    }

    private String buildLink(BookCollection col, String text, String targetId, String label) {
        AorLocation loc = col.getAnnotationMap().get(targetId);
        return "<a class=\"internal-ref\" href=\"javascript:;\" " +
                "data-targetid=\"" + targetId(loc) + "\" " +
                (label != null ? "data-label=\"" + label + "\" " : "") +
                "data-manifestid=\"" + manifestId(loc) + "\">" +
                text +
                "</a>";
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
