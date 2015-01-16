package rosa.archive.core.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rosa.archive.core.util.CachingUrlEntityResolver;
import rosa.archive.model.aor.AnnotatedPage;
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
public class AORAnnotatedPageSerializer implements Serializer<AnnotatedPage> {
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
    public void write(AnnotatedPage object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not Implemented");
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
                            Location.valueOf(
                                    annotation.getAttribute("place").toUpperCase()
                            )

                    ));
                    break;
                case "errata":
                    page.getErrata().add(new Errata(
                            annotation.getAttribute("copytext"),
                            annotation.getAttribute("amendedtext")
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
                    marg.setTranslation(el.getAttribute("translation_text"));
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
                    people.add(el.getAttribute("person_name"));
                    break;
                case "book":
                    books.add(el.getAttribute("title"));
                    break;
                case "location":
                    locations.add(el.getAttribute("location_name"));
                    break;
                case "marginalia_text":
                    pos.getTexts().add(el.getTextContent());
                    break;
                case "emphasis":
                    underlines.add(new Underline(
                            el.getAttribute("emphasis_text"),
                            el.getAttribute("method"),
                            el.getAttribute("type"),
                            el.getAttribute("language"),
                            pos.getPlace()
                    ));
                    break;
                case "X-ref":
                    XRef xRef = new XRef(el.getAttribute("person"), el.getAttribute("title"));
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
}
