package rosa.archive.core.serialize;

import com.google.inject.Inject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import rosa.archive.core.config.AppConfig;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Numeral;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;
import rosa.archive.model.aor.XRef;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 */
public class AORAnnotatedPageSerializer implements Serializer<AnnotatedPage> {

    private AppConfig config;

    @Inject
    AORAnnotatedPageSerializer(AppConfig config) {
        this.config = config;
    }

    @Override
    public AnnotatedPage read(InputStream is, final List<String> errors) throws IOException {
        // Error reporting for validation
//        ErrorHandler errorHandler = new ErrorHandler() {
//            @Override
//            public void warning(SAXParseException e) throws SAXException {
//
//            }
//
//            @Override
//            public void error(SAXParseException e) throws SAXException {
//                errors.add("[Error] " + e.getLineNumber() + ":"
//                        + e.getColumnNumber() + ": " + e.getMessage());
//            }
//
//            @Override
//            public void fatalError(SAXParseException e) throws SAXException {
//                errors.add("[Fatal Error]: " + e.getLineNumber() + ":"
//                        + e.getColumnNumber() + ": " + e.getMessage());
//            }
//        };

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Following three lines will validate the XML as it parses!
//            factory.setNamespaceAware(true);
//            factory.setValidating(true);
//            factory.setAttribute(JAXPConstants.JAXP_SCHEMA_LANGUAGE, JAXPConstants.W3C_XML_SCHEMA);

            DocumentBuilder builder = factory.newDocumentBuilder();
//            builder.setErrorHandler(errorHandler);

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
        List<Marginalia> marginalia = page.getMarginalia();
        List<Mark> marks = page.getMarks();
        List<Numeral> numerals = page.getNumerals();
        List<Symbol> symbols = page.getSymbols();
        List<Underline> underlines = page.getUnderlines();

        NodeList children = annotationEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element annotation = (Element) child;
            switch (annotation.getTagName()) {
                case "marginalia":
                    marginalia.add(buildMarginalia(annotation));
                    break;
                case "underline":
                    Underline underline = new Underline();

                    underline.setMethod(annotation.getAttribute("method"));
                    underline.setType(annotation.getAttribute("type"));
                    underline.setLanguage(annotation.getAttribute("language"));
                    underline.setReferringText(annotation.getAttribute("text"));

                    underlines.add(underline);
                    break;
                case "symbol":
                    Symbol symbol = new Symbol();

                    symbol.setName(annotation.getAttribute("name"));
                    symbol.setPlace(annotation.getAttribute("place"));
                    symbol.setReferringText(annotation.getAttribute("text"));

                    symbols.add(symbol);
                    break;
                case "mark":
                    Mark mark = new Mark();

                    mark.setName(annotation.getAttribute("name"));
                    mark.setPlace(annotation.getAttribute("place"));
                    mark.setMethod(annotation.getAttribute("method"));
                    mark.setLanguage(annotation.getAttribute("language"));
                    mark.setReferringText(annotation.getAttribute("text"));

                    marks.add(mark);
                    break;
                case "numeral":
                    Numeral numeral = new Numeral();

                    numeral.setPlace(annotation.getAttribute("place"));
                    numeral.setReferringText(annotation.getAttribute("text"));

                    numerals.add(numeral);
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
        pos.setPlace(position.getAttribute("place"));

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
                    Underline underline = new Underline();
                    underline.setMethod(el.getAttribute("method"));
                    underline.setReferringText(el.getAttribute("emphasis_text"));

                    underlines.add(underline);
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
}
