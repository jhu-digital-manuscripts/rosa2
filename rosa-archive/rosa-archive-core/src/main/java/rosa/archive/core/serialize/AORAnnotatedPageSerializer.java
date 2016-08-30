package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import rosa.archive.core.ArchiveConstants;
import rosa.archive.core.util.Annotations;
import rosa.archive.core.util.CachingUrlResourceResolver;
import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.ReferenceTarget;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

public class AORAnnotatedPageSerializer implements Serializer<AnnotatedPage>, ArchiveConstants,
        AORAnnotatedPageConstants {
    private static final Logger logger = Logger.getLogger(AORAnnotatedPageSerializer.class.toString());

    /** Caches DTDs for write validation */
    private static final CachingUrlResourceResolver resourceResolver = new CachingUrlResourceResolver();

    @Override
    public AnnotatedPage read(InputStream is, final List<String> errors) throws IOException {

        if (is == null) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Following three lines will validate the XML as it parses!
//            factory.setNamespaceAware(true);
//            factory.setValidating(true);
//            factory.setAttribute(JAXPConstants.JAXP_SCHEMA_LANGUAGE, JAXPConstants.W3C_XML_SCHEMA);

            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(resourceResolver);
            Document doc = builder.parse(is);
            return buildPage(doc, errors);

        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(AnnotatedPage aPage, OutputStream out) throws IOException {
        Document doc = XMLUtil.newDocument(annotationSchemaUrl);
        if (doc == null) {
            throw new IOException("Failed to write annotated page.");
        }

        Element base = doc.createElement(TAG_TRANSCRIPTION);
        base.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:noNamespaceSchemaLocation",
                annotationSchemaUrl);

        doc.appendChild(base);

        Element pageEl = newElement(TAG_PAGE, base, doc);
        setAttribute(pageEl, ATTR_FILENAME, aPage.getPage());
        setAttribute(pageEl, ATTR_PAGINATION, aPage.getPagination());
        setAttribute(pageEl, ATTR_READER, aPage.getReader());
        setAttribute(pageEl, ATTR_SIGNATURE, aPage.getSignature());

        Element annotationEl = newElement(TAG_ANNOTATION, base, doc);
        addMarginalia(aPage.getMarginalia(), annotationEl, doc);
        addUnderline(aPage.getUnderlines(), annotationEl, doc);
        addSymbol(aPage.getSymbols(), annotationEl, doc);
        addMark(aPage.getMarks(), annotationEl, doc);
        addNumeral(aPage.getNumerals(), annotationEl, doc);
        addErrata(aPage.getErrata(), annotationEl, doc);
        addDrawing(aPage.getDrawings(), annotationEl, doc);

        doc.normalizeDocument();

        // validate written document against schema, only write to file if valid
        if (validate(doc, annotationSchemaUrl)) {
            XMLUtil.write(doc, out, false);
        } else {
            throw new IOException("Failed to write AoR transcription due to previously logged errors.");
        }
    }

    private boolean validate(Document doc, String schemaUrl) throws IOException {
        DocumentBuilderFactory dbf = XMLUtil.documentBuilderFactory(schemaUrl);

        if (dbf == null) {
            return false;
        }

        Validator validator = dbf.getSchema().newValidator();
        validator.setResourceResolver(resourceResolver);

        final Set<String> errors = new HashSet<>();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) throws SAXException {

            }

            @Override
            public void error(SAXParseException e) throws SAXException {
                String message = "[ERROR] writing AoR transcription. "  + e.getLineNumber() + ":"
                        + e.getColumnNumber() + "):\n" + e.getMessage();
                logger.log(Level.SEVERE, message);
                errors.add(message);
            }

            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                String message = "[FATAL ERROR] writing AoR transcription. "  + e.getLineNumber() + ":"
                        + e.getColumnNumber() + "): \n" + e.getMessage();
                logger.log(Level.SEVERE, message);
                errors.add(message);
            }
        });

        try {
            validator.validate(new DOMSource(doc));
        } catch (SAXException e) {
            return false;
        }

        return errors.isEmpty();
    }

    private void addUnderline(List<Underline> underlines, Element parent, Document doc) {
        for (Underline underline : underlines) {
            Element u = newElement(TAG_UNDERLINE, parent, doc);
            setAttribute(u, ATTR_TEXT, underline.getReferencedText());
            setAttribute(u, ATTR_METHOD, underline.getMethod());
            setAttribute(u, ATTR_TYPE, underline.getType());
            setAttribute(u, ATTR_LANGUAGE, underline.getLanguage());
        }
    }

    private void addSymbol(List<Symbol> symbols, Element parent, Document doc) {
        for (Symbol symbol : symbols) {
            Element s = newElement(TAG_SYMBOL, parent, doc);
            setAttribute(s, ATTR_TEXT, symbol.getReferencedText());
            setAttribute(s, ATTR_NAME, symbol.getName());
            setAttribute(s, ATTR_LANGUAGE, symbol.getLanguage());
            setAttribute(s, ATTR_PLACE, symbol.getLocation().toString().toLowerCase());
        }
    }

    private void addMark(List<Mark> marks, Element parent, Document doc) {
        for (Mark mark : marks) {
            Element m = newElement(TAG_MARK, parent, doc);
            setAttribute(m, ATTR_TEXT, mark.getReferencedText());
            setAttribute(m, ATTR_NAME, mark.getName());
            setAttribute(m, ATTR_METHOD, mark.getMethod());
            setAttribute(m, ATTR_LANGUAGE, mark.getLanguage());
            setAttribute(m, ATTR_PLACE, mark.getLocation().toString().toLowerCase());
        }
    }

    private void addNumeral(List<Numeral> numerals, Element parent, Document doc) {
        for (Numeral numeral : numerals) {
            Element n = newElement(TAG_NUMERAL, parent, doc);
            setAttribute(n, ATTR_TEXT, numeral.getReferencedText());
            setAttribute(n, ATTR_PLACE, numeral.getLocation().toString().toLowerCase());
            if (numeral.getNumeral() != null) {
                n.setTextContent(numeral.getNumeral());
            }
        }
    }

    private void addErrata(List<Errata> erratas, Element parent, Document doc) {
        for (Errata errata : erratas) {
            Element e = newElement(TAG_ERRATA, parent, doc);
            setAttribute(e, ATTR_LANGUAGE, errata.getLanguage());
            setAttribute(e, ATTR_COPYTEXT, errata.getReferencedText());
            setAttribute(e, ATTR_AMENDEDTEXT, errata.getAmendedText());
        }
    }

    private void addDrawing(List<Drawing> drawings, Element parent, Document doc) {
        for (Drawing drawing : drawings) {
            Element d = newElement(TAG_DRAWING, parent, doc);
            setAttribute(d, ATTR_TEXT, drawing.getReferencedText());
            setAttribute(d, ATTR_PLACE, drawing.getLocation().toString().toLowerCase());
            setAttribute(d, ATTR_NAME, drawing.getName());
            setAttribute(d, ATTR_METHOD, drawing.getMethod());
            setAttribute(d, ATTR_LANGUAGE, drawing.getLanguage());
        }
    }

    private void addMarginalia(List<Marginalia> marginalias, Element parent, Document doc) {
        for (Marginalia marginalia : marginalias) {
            Element margEl = newElement(TAG_MARGINALIA, parent, doc);

            setAttribute(margEl, ATTR_DATE, marginalia.getDate());
            setAttribute(margEl, ATTR_HAND, marginalia.getHand());
            setAttribute(margEl, ATTR_OTHER_READER, marginalia.getOtherReader());
            setAttribute(margEl, ATTR_TOPIC, marginalia.getTopic());
            setAttribute(margEl, ATTR_ANCHOR_TEXT, marginalia.getReferencedText());

            for (MarginaliaLanguage lang : marginalia.getLanguages()) {
                Element langEl = newElement(TAG_LANGUAGE, margEl, doc);
                setAttribute(langEl, ATTR_IDENT, lang.getLang());

                for (Position pos : lang.getPositions()) {
                    Element posEl = newElement(TAG_POSITION, langEl, doc);

                    setAttribute(posEl, ATTR_PLACE, pos.getPlace().toString().toLowerCase());
                    setAttribute(posEl, ATTR_BOOK_ORIENTATION, pos.getOrientation());

                    StringBuilder sb = new StringBuilder();
                    for (String s : pos.getTexts()) {
                        sb.append(s);
                    }
                    valueElement(TAG_MARGINALIA_TEXT, sb.toString(), posEl, doc);

                    for (String person : pos.getPeople()) {
                        Element e = newElement(TAG_PERSON, posEl, doc);
                        setAttribute(e, ATTR_NAME, person == null || person.isEmpty() ? " " : person);
                    }
                    for (String book : pos.getBooks()) {
                        Element e = newElement(TAG_BOOK, posEl, doc);
                        setAttribute(e, ATTR_TITLE, book);
                    }
                    for (String location : pos.getLocations()) {
                        Element e = newElement(TAG_LOCATION, posEl, doc);
                        setAttribute(e, ATTR_NAME, location);
                    }
                    for (XRef xRef : pos.getxRefs()) {
                        Element e = newElement(TAG_X_REF, posEl, doc);
                        setAttribute(e, ATTR_PERSON, xRef.getPerson());
                        setAttribute(e, ATTR_BOOK_TITLE, xRef.getTitle());
                        setAttribute(e, ATTR_TEXT, xRef.getText());
                        setAttribute(e, ATTR_LANGUAGE, xRef.getLanguage());
                    }
                    for (Underline underline : pos.getEmphasis()) {
                        Element e = newElement(TAG_EMPHASIS, posEl, doc);
                        setAttribute(e, ATTR_TEXT, underline.getReferencedText());
                        setAttribute(e, ATTR_METHOD, underline.getMethod());
                        setAttribute(e, ATTR_TYPE, underline.getType());
                        setAttribute(e, ATTR_LANGUAGE, underline.getLanguage());
                    }
                    for (InternalReference ref : pos.getInternalRefs()) {
                        Element e = newElement(TAG_INTERNAL_REF, posEl, doc);
                        setAttribute(e, ATTR_TEXT, ref.getText());

                        for (ReferenceTarget target : ref.getTargets()) {
                            Element et = newElement(TAG_TARGET, e, doc);
                            setAttribute(et, ATTR_FILENAME, target.getFilename());
                            setAttribute(et, ATTR_BOOK_ID, target.getBookId());
                            setAttribute(et, ATTR_TEXT, target.getText());
                        }
                    }
                }
            }

            if (marginalia.getTranslation() != null && !marginalia.getTranslation().isEmpty()) {
                valueElement(TAG_TRANSLATION, marginalia.getTranslation(), margEl, doc);
            }
        }
    }

    private AnnotatedPage buildPage(Document doc, List<String> errors) {
        AnnotatedPage page = new AnnotatedPage();

        // <page>
        NodeList pageEls = doc.getElementsByTagName(TAG_PAGE);
        if (pageEls.getLength() != 1) {
            errors.add("Transcription file must have exactly ONE <page> element! Current document" +
                    " has [" + pageEls.getLength() + "]");
        } else {
            Element pageEl = (Element) pageEls.item(0);

            page.setPage(pageEl.getAttribute(ATTR_FILENAME));
            page.setPagination(pageEl.getAttribute(ATTR_PAGINATION));
            page.setReader(pageEl.getAttribute(ATTR_READER));
            page.setSignature(pageEl.getAttribute(ATTR_SIGNATURE));
        }

        // <annotation>
        NodeList annotationEls = doc.getElementsByTagName(TAG_ANNOTATION);
        if (annotationEls.getLength() != 1) {
            errors.add("Transcription file must have ONE <annotation> element! Current document " +
                    "has [" + annotationEls.getLength() + "]");
        } else {
            readAnnotations((Element) annotationEls.item(0), page);
        }

        return page;
    }

    /**
     * As annotations are read, IDs are assigned to them depending on annotation
     * type and ordering within the transcription XML.
     *
     * For all annotation types, the ID will be structures as:
     *      page-id_annotation-type_annotation-number(s)
     *
     * Example:
     *      FolgersHa2.024r.tif_underline_3
     *
     * @param annotationEl annotation XML element
     * @param page result AnnotatedPage
     * @see rosa.archive.core.util.Annotations#annotationId(String, String, int)
     */
    private void readAnnotations(Element annotationEl, AnnotatedPage page) {

        NodeList children = annotationEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element annotation = (Element) child;
            switch (annotation.getTagName()) {
                case TAG_MARGINALIA:
                    Marginalia marg = buildMarginalia(annotation, page.getPage());
                    marg.setId(Annotations.annotationId(page.getPage(), TAG_MARGINALIA, page.getMarginalia().size()));

                    page.getMarginalia().add(marg);
                    break;
                case TAG_UNDERLINE:
                    page.getUnderlines().add(new Underline(
                            Annotations.annotationId(page.getPage(), TAG_UNDERLINE, page.getUnderlines().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getAttribute(ATTR_METHOD),
                            annotation.getAttribute(ATTR_TYPE),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            Location.INTEXT
                    ));
                    break;
                case TAG_SYMBOL:
                    page.getSymbols().add(new Symbol(
                            Annotations.annotationId(page.getPage(), TAG_SYMBOL, page.getSymbols().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getAttribute(ATTR_NAME),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            Location.valueOf(
                                    annotation.getAttribute(ATTR_PLACE).toUpperCase()
                            )
                    ));
                    break;
                case TAG_MARK:
                    page.getMarks().add(new Mark(
                            Annotations.annotationId(page.getPage(), TAG_MARK, page.getMarks().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getAttribute(ATTR_NAME),
                            annotation.getAttribute(ATTR_METHOD),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            Location.valueOf(
                                    annotation.getAttribute(ATTR_PLACE).toUpperCase()
                            )
                    ));
                    break;
                case TAG_NUMERAL:
                    page.getNumerals().add(new Numeral(
                            Annotations.annotationId(page.getPage(), TAG_NUMERAL, page.getNumerals().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getTextContent(),
                            null,
                            Location.valueOf(
                                    annotation.getAttribute(ATTR_PLACE).toUpperCase()
                            )

                    ));
                    break;
                case TAG_ERRATA:
                    page.getErrata().add(new Errata(
                            Annotations.annotationId(page.getPage(), TAG_ERRATA, page.getErrata().size()),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            annotation.getAttribute(ATTR_COPYTEXT),
                            annotation.getAttribute(ATTR_AMENDEDTEXT)
                    ));
                    break;
                case TAG_DRAWING:
                    page.getDrawings().add(new Drawing(
                            Annotations.annotationId(page.getPage(), TAG_DRAWING, page.getDrawings().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            Location.valueOf(annotation.getAttribute(ATTR_PLACE).toUpperCase()),
                            annotation.getAttribute(ATTR_NAME),
                            annotation.getAttribute(ATTR_METHOD),
                            annotation.getAttribute(ATTR_LANGUAGE)
                    ));
                    break;
                default:
                    break;
            }
        }
    }

    private Marginalia buildMarginalia(Element annotation, String page) {
        Marginalia marg = new Marginalia();

        marg.setDate(annotation.getAttribute(ATTR_DATE));
        marg.setHand(annotation.getAttribute(ATTR_HAND));
        marg.setOtherReader(annotation.getAttribute(ATTR_OTHER_READER));
        marg.setTopic(annotation.getAttribute(ATTR_TOPIC));
        marg.setReferencedText(annotation.getAttribute(ATTR_ANCHOR_TEXT));

        List<MarginaliaLanguage> langs = marg.getLanguages();
        NodeList children = annotation.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) child;

            switch (el.getTagName()) {
                case TAG_LANGUAGE:
                    MarginaliaLanguage lang = new MarginaliaLanguage();
                    lang.setLang(el.getAttribute(ATTR_IDENT));

                    List<Position> p = lang.getPositions();
                    NodeList positions = el.getElementsByTagName(TAG_POSITION);
                    for (int j = 0; j < positions.getLength(); j++) {
                        Node posNode = positions.item(j);
                        if (posNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        p.add(buildPosition((Element) posNode, page));
                    }

                    langs.add(lang);
                    break;
                case TAG_TRANSLATION:
                    marg.setTranslation(hasAttribute(ATTR_TRANSLATION_TEXT, el) ?
                            el.getAttribute(ATTR_TRANSLATION_TEXT) : el.getTextContent());
                    break;
                default:
                    break;
            }
        }

        return marg;
    }

    private Position buildPosition(Element position, String page) {
        Position pos = new Position();
        pos.setPlace(Location.valueOf(position.getAttribute(ATTR_PLACE).toUpperCase().trim()));

        // book_orientation is integer value: (0|90|180|270)
        String orientation = position.getAttribute(ATTR_BOOK_ORIENTATION);
        if (orientation.matches("\\d+")) {
            pos.setOrientation(Integer.parseInt(orientation));
        }

        List<String> people = pos.getPeople();
        List<String> books = pos.getBooks();
        List<XRef> xRefs = pos.getxRefs();
        List<String> locations = pos.getLocations();
        List<Underline> underlines = pos.getEmphasis();
        List<InternalReference> internalRefs = pos.getInternalRefs();

        NodeList list = position.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) node;
            switch (el.getTagName()) {
                case TAG_PERSON:
                    people.add(hasAttribute(ATTR_NAME, el) ?
                            el.getAttribute(ATTR_NAME) : el.getAttribute(ATTR_PERSON_NAME));
                    break;
                case TAG_BOOK:
                    books.add(el.getAttribute(ATTR_TITLE));
                    break;
                case TAG_LOCATION:
                    locations.add(hasAttribute(ATTR_NAME, el) ?
                            el.getAttribute(ATTR_NAME) : el.getAttribute(ATTR_LOCATION_NAME));
                    break;
                case TAG_MARGINALIA_TEXT:
                    pos.getTexts().add(el.getTextContent());
                    break;
                case TAG_EMPHASIS:
                    StringBuilder id = new StringBuilder(page);
                    id.append('_');
                    id.append(TAG_EMPHASIS);
                    id.append('_');

                    if (pos.getEmphasis() != null) {
                        id.append(pos.getEmphasis().size() + 1);
                    } else {
                        id.append('0');
                    }

                    underlines.add(new Underline(id.toString(),
                            hasAttribute(ATTR_TEXT, el) ?
                                    el.getAttribute(ATTR_TEXT) : el.getAttribute(ATTR_EMPHASIS_TEXT),
                            el.getAttribute(ATTR_METHOD),
                            el.getAttribute(ATTR_TYPE),
                            el.getAttribute(ATTR_LANGUAGE),
                            pos.getPlace()
                    ));
                    break;
                case TAG_X_REF:
                    XRef xRef = new XRef(el.getAttribute(ATTR_PERSON),
                            hasAttribute(ATTR_BOOK_TITLE, el) ?
                                    el.getAttribute(ATTR_BOOK_TITLE) : el.getAttribute(ATTR_TITLE), el.getAttribute(ATTR_TEXT),
                                    el.getAttribute(ATTR_LANGUAGE));
                    xRefs.add(xRef);
                    break;
                case TAG_INTERNAL_REF:
                    InternalReference ref = new InternalReference();
                    ref.setText(el.getAttribute(ATTR_TEXT));

                    // Build targets
                    NodeList children = el.getChildNodes();
                    if (children == null) {
                        System.err.println("No targets found for this reference.");
                        break;
                    }

                    for (int j = 0; j < children.getLength(); j++) {
                        Node n = children.item(j);
                        if (n == null || n.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        Element child = (Element) n;

                        if (child.getTagName().equals(TAG_TARGET)) {
                            ReferenceTarget t = new ReferenceTarget(
                                    child.getAttribute(ATTR_FILENAME),
                                    child.getAttribute(ATTR_BOOK_ID),
                                    child.getAttribute(ATTR_TEXT)
                            );
                            ref.addTargets(t);
                        }
                    }

                    internalRefs.add(ref);
                default:
                    break;
            }
        }

        return pos;
    }

    @Override
    public Class<AnnotatedPage> getObjectType() {
        return AnnotatedPage.class;
    }

    private boolean hasAttribute(String attribute, Element el) {
        return el.getAttribute(attribute) != null && !el.getAttribute(attribute).isEmpty();
    }

    private Element newElement(String tagName, Element parent, Document doc) {
        Element el = doc.createElement(tagName);
        parent.appendChild(el);
        return el;
    }

    private Element valueElement(String tagName, String value, Element parent, Document doc) {
        Element el = doc.createElement(tagName);
        el.setTextContent(value);
        parent.appendChild(el);

        return el;
    }
/*
    private Element valueElement(String tagName, int value, Element parent, Document doc) {
        Element el = doc.createElement(tagName);
        el.setTextContent(String.valueOf(value));
        parent.appendChild(el);

        return el;
    }
*/
    private void setAttribute(Element tag, String attribute, String value) {
        boolean alwaysWrite = attribute.equals(ATTR_AMENDEDTEXT) || attribute.equals(ATTR_COPYTEXT)
                || (tag.getTagName().equalsIgnoreCase("x-ref") && attribute.equals(ATTR_PERSON))
                || (tag.getTagName().equals("internal_ref") && attribute.equals(ATTR_TEXT))
                || (tag.getTagName().equals("target") && attribute.equals(ATTR_TEXT));
        // Dumb hack to force writing of specific attributes even if empty...
        if (alwaysWrite) {
            tag.setAttribute(attribute, value == null ? "" : value);
        } else if (value != null && !value.isEmpty()) {
            tag.setAttribute(attribute, value);
        }
    }

    private void setAttribute(Element tag, String attribute, int value) {
        if (value != -1) {
            setAttribute(tag, attribute, String.valueOf(value));
        }
    }
}
