package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rosa.archive.core.ArchiveConstants;
import rosa.archive.core.util.CachingUrlEntityResolver;
import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Errata;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

/**
 *
 */
public class AORAnnotatedPageSerializer implements Serializer<AnnotatedPage>, ArchiveConstants {
    private static final CachingUrlEntityResolver entityResolver = new CachingUrlEntityResolver();

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
            builder.setEntityResolver(entityResolver);
            Document doc = builder.parse(is);
            return buildPage(doc, errors);

        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(AnnotatedPage aPage, OutputStream out) throws IOException {
        Document doc = XMLUtil.newDocument(annotationSchemaUrl);
//        doc.setXmlStandalone(true);
        Element base = doc.createElement("transcription");
        base.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        base.setAttribute("xsi:noNamespaceSchemaLocation", annotationSchemaUrl);

        doc.appendChild(base);

        Element pageEl = newElement("page", base, doc);
        setAttribute(pageEl, "filename", aPage.getPage());
        setAttribute(pageEl, "pagination", aPage.getPagination());
        setAttribute(pageEl, "reader", aPage.getReader());
        setAttribute(pageEl, "signature", aPage.getSignature());

        Element annotationEl = newElement("annotation", pageEl, doc);
        addMarginalia(aPage.getMarginalia(), annotationEl, doc);
        addUnderline(aPage.getUnderlines(), annotationEl, doc);
        addSymbol(aPage.getSymbols(), annotationEl, doc);
        addMark(aPage.getMarks(), annotationEl, doc);
        addNumeral(aPage.getNumerals(), annotationEl, doc);
        addErrata(aPage.getErrata(), annotationEl, doc);
        addDrawing(aPage.getDrawings(), annotationEl, doc);

        doc.normalizeDocument();
        XMLUtil.write(doc, out);
    }

    private void addUnderline(List<Underline> underlines, Element parent, Document doc) {
        for (Underline underline : underlines) {
            Element u = newElement("underline", parent, doc);
            setAttribute(u, "text", underline.getReferringText());
            setAttribute(u, "method", underline.getMethod());
            setAttribute(u, "type", underline.getType());
            setAttribute(u, "language", underline.getLanguage());
        }
    }

    private void addSymbol(List<Symbol> symbols, Element parent, Document doc) {
        for (Symbol symbol : symbols) {
            Element s = newElement("symbol", parent, doc);
            setAttribute(s, "text", symbol.getReferringText());
            setAttribute(s, "name", symbol.getName());
            setAttribute(s, "language", symbol.getLanguage());
            setAttribute(s, "place", symbol.getLocation().toString().toLowerCase());
        }
    }

    private void addMark(List<Mark> marks, Element parent, Document doc) {
        for (Mark mark : marks) {
            Element m = newElement("mark", parent, doc);
            setAttribute(m, "text", mark.getReferringText());
            setAttribute(m, "name", mark.getName());
            setAttribute(m, "method", mark.getMethod());
            setAttribute(m, "language", mark.getLanguage());
            setAttribute(m, "place", mark.getLocation().toString().toLowerCase());
        }
    }

    private void addNumeral(List<Numeral> numerals, Element parent, Document doc) {
        for (Numeral numeral : numerals) {
            Element n = newElement("numberal", parent, doc);
            setAttribute(n, "text", numeral.getReferringText());
            setAttribute(n, "place", numeral.getLocation().toString().toLowerCase());
        }
    }

    private void addErrata(List<Errata> erratas, Element parent, Document doc) {
        for (Errata errata : erratas) {
            Element e = newElement("errata", parent, doc);
            setAttribute(e, "language", errata.getLanguage());
            setAttribute(e, "copytext", errata.getCopyText());
            setAttribute(e, "amendedtext", errata.getAmendedText());
        }
    }

    private void addDrawing(List<Drawing> drawings, Element parent, Document doc) {
        for (Drawing drawing : drawings) {
            Element d = newElement("drawing", parent, doc);
            setAttribute(d, "text", drawing.getReferringText());
            setAttribute(d, "place", drawing.getLocation().toString().toLowerCase());
            setAttribute(d, "name", drawing.getName());
            setAttribute(d, "method", drawing.getMethod());
            setAttribute(d, "language", drawing.getLanguage());
        }
    }

    private void addMarginalia(List<Marginalia> marginalias, Element parent, Document doc) {
        for (Marginalia marginalia : marginalias) {
            Element margEl = newElement("marginalia", parent, doc);

            setAttribute(margEl, "date", marginalia.getDate());
            setAttribute(margEl, "hand", marginalia.getHand());
            setAttribute(margEl, "other_reader", marginalia.getOtherReader());
            setAttribute(margEl, "topic", marginalia.getTopic());
            setAttribute(margEl, "anchor_text", marginalia.getAnchorText());

            if (marginalia.getTranslation() != null && !marginalia.getTranslation().isEmpty()) {
                valueElement("translation", marginalia.getTranslation(), margEl, doc);
            }

            for (MarginaliaLanguage lang : marginalia.getLanguages()) {
                Element langEl = newElement("language", margEl, doc);
                setAttribute(langEl, "ident", lang.getLang());

                for (Position pos : lang.getPositions()) {
                    Element posEl = newElement("position", langEl, doc);

                    setAttribute(posEl, "place", pos.getPlace().toString().toLowerCase());
                    setAttribute(posEl, "book_orientation", pos.getOrientation());

                    StringBuilder sb = new StringBuilder();
                    for (String s : pos.getTexts()) {
                        sb.append(s);
                    }
                    valueElement("marginalia_text", sb.toString(), posEl, doc);

                    for (String person : pos.getPeople()) {
                        Element e = newElement("person", posEl, doc);
                        setAttribute(e, "name", person);
                    }
                    for (String book : pos.getBooks()) {
                        Element e = newElement("book", posEl, doc);
                        setAttribute(e, "title", book);
                    }
                    for (XRef xRef : pos.getxRefs()) {
                        Element e = newElement("X-ref", posEl, doc);
                        setAttribute(e, "person", xRef.getPerson());
                        setAttribute(e, "title", xRef.getTitle());
                    }
                    for (String location : pos.getLocations()) {
                        Element e = newElement("location", posEl, doc);
                        setAttribute(e, "name", location);
                    }
                    for (Underline underline : pos.getEmphasis()) {
                        Element e = newElement("emphasis", posEl, doc);
                        setAttribute(e, "text", underline.getReferringText());
                        setAttribute(e, "method", underline.getMethod());
                        setAttribute(e, "type", underline.getType());
                        setAttribute(e, "language", underline.getLanguage());
                    }
                }
            }
        }
    }

    private AnnotatedPage buildPage(Document doc, List<String> errors) {
        AnnotatedPage page = new AnnotatedPage();

        // <page>
        NodeList pageEls = doc.getElementsByTagName("page");
        if (pageEls.getLength() != 1) {
            errors.add("Transcription file must have exactly ONE <page> element! Current document" +
                    " has [" + pageEls.getLength() + "]");
        } else {
            Element pageEl = (Element) pageEls.item(0);

            page.setPage(pageEl.getAttribute("filename"));
            page.setPagination(pageEl.getAttribute("pagination"));
            page.setReader(pageEl.getAttribute("reader"));
            page.setSignature(pageEl.getAttribute("signature"));
        }

        // <annotation>
        NodeList annotationEls = doc.getElementsByTagName("annotation");
        if (annotationEls.getLength() != 1) {
            errors.add("Transcription file must have ONE <annotation> element! Current document " +
                    "has [" + annotationEls.getLength() + "]");
        } else {
            readAnnotations((Element) annotationEls.item(0), page);
        }

        return page;
    }

    private void readAnnotations(Element annotationEl, AnnotatedPage page) {

        NodeList children = annotationEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element annotation = (Element) child;
            switch (annotation.getTagName()) {
                case "marginalia":
                    page.getMarginalia().add(
                            buildMarginalia(annotation)
                    );
                    break;
                case "underline":
                    page.getUnderlines().add(new Underline(
                            annotation.getAttribute("text"),
                            annotation.getAttribute("method"),
                            annotation.getAttribute("type"),
                            annotation.getAttribute("language"),
                            Location.INTEXT
                    ));
                    break;
                case "symbol":
                    page.getSymbols().add(new Symbol(
                            annotation.getAttribute("text"),
                            annotation.getAttribute("name"),
                            annotation.getAttribute("language"),
                            Location.valueOf(
                                    annotation.getAttribute("place").toUpperCase()
                            )
                    ));
                    break;
                case "mark":
                    page.getMarks().add(new Mark(
                            annotation.getAttribute("text"),
                            annotation.getAttribute("name"),
                            annotation.getAttribute("method"),
                            annotation.getAttribute("language"),
                            Location.valueOf(
                                    annotation.getAttribute("place").toUpperCase()
                            )
                    ));
                    break;
                case "numeral":
                    page.getNumerals().add(new Numeral(
                            annotation.getAttribute("text"),
                            null,
                            Location.valueOf(
                                    annotation.getAttribute("place").toUpperCase()
                            )

                    ));
                    break;
                case "errata":
                    page.getErrata().add(new Errata(
                            annotation.getAttribute("language"),
                            annotation.getAttribute("copytext"),
                            annotation.getAttribute("amendedtext")
                    ));
                    break;
                case "drawing":
                    page.getDrawings().add(new Drawing(
                            annotation.getAttribute("text"),
                            Location.valueOf(annotation.getAttribute("place").toUpperCase()),
                            annotation.getAttribute("name"),
                            annotation.getAttribute("method"),
                            annotation.getAttribute("language")
                    ));
                    break;
                default:
                    break;
            }
        }
    }

    private Marginalia buildMarginalia(Element annotation) {
        Marginalia marg = new Marginalia();

        marg.setDate(annotation.getAttribute("date"));
        marg.setHand(annotation.getAttribute("hand"));
        marg.setOtherReader(annotation.getAttribute("other_reader"));
        marg.setTopic(annotation.getAttribute("topic"));
        marg.setAnchorText(annotation.getAttribute("anchor_text"));

        List<MarginaliaLanguage> langs = marg.getLanguages();
        NodeList children = annotation.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) child;

            switch (el.getTagName()) {
                case "language":
                    MarginaliaLanguage lang = new MarginaliaLanguage();
                    lang.setLang(el.getAttribute("ident"));

                    List<Position> p = lang.getPositions();
                    NodeList positions = el.getElementsByTagName("position");
                    for (int j = 0; j < positions.getLength(); j++) {
                        Node posNode = positions.item(j);
                        if (posNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        p.add(buildPosition((Element) posNode));
                    }

                    langs.add(lang);
                    break;
                case "translation":
                    marg.setTranslation(hasAttribute("translation_text", el) ?
                            el.getAttribute("translation_text") : el.getTextContent());
                    break;
                default:
                    break;
            }
        }

        return marg;
    }

    private Position buildPosition(Element position) {
        Position pos = new Position();
        pos.setPlace(Location.valueOf(position.getAttribute("place").toUpperCase().trim()));

        // book_orientation is integer value: (0|90|180|270)
        String orientation = position.getAttribute("book_orientation");
        if (orientation.matches("\\d+")) {
            pos.setOrientation(Integer.parseInt(orientation));
        }

        List<String> people = pos.getPeople();
        List<String> books = pos.getBooks();
        List<XRef> xRefs = pos.getxRefs();
        List<String> locations = pos.getLocations();
        List<Underline> underlines = pos.getEmphasis();

        NodeList list = position.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) node;
            switch (el.getTagName()) {
                case "person":
                    people.add(hasAttribute("name", el) ?
                            el.getAttribute("name") : el.getAttribute("person_name"));
                    break;
                case "book":
                    books.add(el.getAttribute("title"));
                    break;
                case "location":
                    locations.add(hasAttribute("name", el) ?
                            el.getAttribute("name") : el.getAttribute("location_name"));
                    break;
                case "marginalia_text":
                    pos.getTexts().add(el.getTextContent());
                    break;
                case "emphasis":
                    underlines.add(new Underline(
                            hasAttribute("text", el) ?
                                    el.getAttribute("text") : el.getAttribute("emphasis_text"),
                            el.getAttribute("method"),
                            el.getAttribute("type"),
                            el.getAttribute("language"),
                            pos.getPlace()
                    ));
                    break;
                case "X-ref":
                    XRef xRef = new XRef(el.getAttribute("person"),
                            hasAttribute("book_title", el) ?
                                    el.getAttribute("book_title") : el.getAttribute("title"));
                    xRefs.add(xRef);
                    break;
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

    private Element valueElement(String tagName, int value, Element parent, Document doc) {
        Element el = doc.createElement(tagName);
        el.setTextContent(String.valueOf(value));
        parent.appendChild(el);

        return el;
    }

    private void setAttribute(Element tag, String attribute, String value) {
        if (value != null && !value.isEmpty()) {
            tag.setAttribute(attribute, value);
        }
    }

    private void setAttribute(Element tag, String attribute, int value) {
        if (value != -1) {
            setAttribute(tag, attribute, String.valueOf(value));
        }
    }
}
