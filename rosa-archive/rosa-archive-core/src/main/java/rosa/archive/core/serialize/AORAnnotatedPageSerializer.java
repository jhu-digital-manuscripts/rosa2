package rosa.archive.core.serialize;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import rosa.archive.model.aor.AnnotatedPage;
import rosa.archive.model.aor.Annotation;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.Mark;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.Symbol;
import rosa.archive.model.aor.Underline;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AORAnnotatedPageSerializer implements Serializer<AnnotatedPage> {


    @Override
    public AnnotatedPage read(InputStream is, List<String> errors) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            // TODO need to grab the DTD if DOCTYPE is missing
//            builder.setEntityResolver(new EntityResolver()
//            {
//                public InputSource resolveEntity(String publicId, String systemId)
//                        throws SAXException, IOException
//                {
//                    return new InputSource(null);
//                }
//            });

            Document doc = builder.parse(is);
            return buildPage(doc, errors);

        } catch (ParserConfigurationException e) {
            String reason = "Failed to build Document.";
            errors.add(reason);
            throw new IOException(reason, e);
        } catch (SAXException e) {
            String reason = "Failed to parse input stream.";
            errors.add(reason);
            throw new IOException(reason, e);
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

            page.setId(pageEl.getAttribute("filename"));
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
            page.setAnnotations(readAnnotations((Element) annotationEls.item(0)));
        }

        return page;
    }

    private List<Annotation> readAnnotations(Element annotationEl) {
        List<Annotation> annotations = new ArrayList<>();

        NodeList children = annotationEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element annotation = (Element) child;
            switch (annotation.getTagName()) {
                case "marginalia":
                    annotations.add(buildMarginalia(annotation));
                    break;
                case "underline":
                    Underline underline = new Underline();

                    underline.setMethod(annotation.getAttribute("method"));
                    underline.setType(annotation.getAttribute("type"));
                    underline.setReferringText(annotation.getAttribute("text"));

                    annotations.add(underline);
                    break;
                case "symbol":
                    Symbol symbol = new Symbol();

                    symbol.setName(annotation.getAttribute("name"));
                    symbol.setPlace(annotation.getAttribute("place"));
                    symbol.setReferringText(annotation.getAttribute("text"));

                    annotations.add(symbol);
                    break;
                case "mark":
                    Mark mark = new Mark();

                    mark.setName(annotation.getAttribute("name"));
                    mark.setPlace(annotation.getAttribute("place"));
                    mark.setMethod(annotation.getAttribute("method"));
                    mark.setReferringText(annotation.getAttribute("text"));

                    annotations.add(mark);
                    break;
                default:
                    break;
            }
        }

        return annotations;
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
                    pos.setText(el.getTextContent());
                    break;
                case "emphasis":
                    Underline underline = new Underline();
                    underline.setMethod(el.getAttribute("method"));
                    underline.setReferringText(el.getAttribute("emphasis_text"));

                    underlines.add(underline);
                    break;
                default:
                    break;
            }
        }

        return pos;
    }
}
