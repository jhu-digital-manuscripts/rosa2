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
import rosa.archive.model.aor.*;

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
        aPage.getCalculations().forEach(c -> addCalculation(c, annotationEl, doc));
        aPage.getGraphs().forEach(g -> addGraph(g, annotationEl, doc));
        aPage.getTables().forEach(t -> addTable(t, annotationEl, doc));
        aPage.getLinks().forEach(l -> addPhysicalLink(l, annotationEl, doc));

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
            setAttribute(u, ATTR_ID, underline.getId());
            setAttribute(u, ATTR_TEXT, underline.getReferencedText());
            setAttribute(u, ATTR_METHOD, underline.getMethod());
            setAttribute(u, ATTR_TYPE, underline.getType());
            setAttribute(u, ATTR_LANGUAGE, underline.getLanguage());
            setAttribute(u, ATTR_COLOR, underline.getColor());
            setAttribute(u, ATTR_INTERNAL_REF, underline.getInternalRef());
        }
    }

    private void addSymbol(List<Symbol> symbols, Element parent, Document doc) {
        for (Symbol symbol : symbols) {
            Element s = newElement(TAG_SYMBOL, parent, doc);
            setAttribute(s, ATTR_ID, symbol.getId());
            setAttribute(s, ATTR_TEXT, symbol.getReferencedText());
            setAttribute(s, ATTR_NAME, symbol.getName());
            setAttribute(s, ATTR_LANGUAGE, symbol.getLanguage());
            setAttribute(s, ATTR_PLACE, symbol.getLocation().toString().toLowerCase());
        }
    }

    private void addMark(List<Mark> marks, Element parent, Document doc) {
        for (Mark mark : marks) {
            Element m = newElement(TAG_MARK, parent, doc);
            setAttribute(m, ATTR_ID, mark.getId());
            setAttribute(m, ATTR_TEXT, mark.getReferencedText());
            setAttribute(m, ATTR_NAME, mark.getName());
            setAttribute(m, ATTR_METHOD, mark.getMethod());
            setAttribute(m, ATTR_LANGUAGE, mark.getLanguage());
            setAttribute(m, ATTR_PLACE, mark.getLocation().toString().toLowerCase());
            setAttribute(m, ATTR_COLOR, mark.getColor());
        }
    }

    private void addNumeral(List<Numeral> numerals, Element parent, Document doc) {
        for (Numeral numeral : numerals) {
            Element n = newElement(TAG_NUMERAL, parent, doc);
            setAttribute(n, ATTR_ID, numeral.getId());
            setAttribute(n, ATTR_TEXT, numeral.getReferencedText());
            setAttribute(n, ATTR_LANGUAGE, numeral.getLanguage());
            setAttribute(n, ATTR_PLACE, numeral.getLocation().toString().toLowerCase());
            if (numeral.getNumeral() != null) {
                n.setTextContent(numeral.getNumeral());
            }
        }
    }

    private void addErrata(List<Errata> erratas, Element parent, Document doc) {
        for (Errata errata : erratas) {
            Element e = newElement(TAG_ERRATA, parent, doc);
            setAttribute(e, ATTR_ID, errata.getId());
            setAttribute(e, ATTR_LANGUAGE, errata.getLanguage());
            setAttribute(e, ATTR_COPYTEXT, errata.getReferencedText());
            setAttribute(e, ATTR_AMENDEDTEXT, errata.getAmendedText());
            setAttribute(e, ATTR_INTERNAL_REF, errata.getInternalRef());
        }
    }

    private void addDrawing(List<Drawing> drawings, Element parent, Document doc) {
        for (Drawing drawing : drawings) {
            Element d = newElement(TAG_DRAWING, parent, doc);

            setAttribute(d, ATTR_ID, drawing.getId());
            setAttribute(d, ATTR_ANCHOR_TEXT, drawing.getReferencedText());
            setAttribute(d, ATTR_PLACE, drawing.getLocation().toString().toLowerCase());
            setAttribute(d, ATTR_METHOD, drawing.getMethod());
            setAttribute(d, ATTR_PLACE, drawing.getLocation().toString().toLowerCase());
            setAttribute(d, ATTR_COLOR, drawing.getColor());

            drawing.getTexts().forEach(t -> addTextElement(t.getHand(), t.getLanguage(), t.getAnchor_text(),
                    t.getText(), d, doc));
            drawing.getPeople().forEach(p -> addPerson(p, d, doc));
            drawing.getBooks().forEach(b -> addBook(b, d, doc));
            drawing.getLocations().forEach(l -> addLocation(l, d, doc));
            drawing.getSymbols().forEach(s -> addSymbolInText(s, d, doc));
            drawing.getInternalRefs().forEach(r -> addInternalRef(r, d, doc));
            if (drawing.getTranslation() != null && !drawing.getTranslation().isEmpty()) {
                valueElement(TAG_TRANSLATION, drawing.getTranslation(), d, doc);
            }
        }
    }

    private void addTextElement(String hand, String language, String anchor, String text, Element parent, Document doc) {
        Element e = newElement(TAG_TEXT, parent, doc);
        setAttribute(e, ATTR_HAND, hand);
        setAttribute(e, ATTR_LANGUAGE, language);
        setAttribute(e, ATTR_ANCHOR_TEXT, anchor);
        e.setTextContent(text);
    }

    private void addCalculation(Calculation calc, Element parent, Document doc) {
        Element c = newElement(TAG_CALCULATION, parent, doc);

        setAttribute(c, ATTR_TYPE, calc.getType());
        setAttribute(c, ATTR_BOOK_ORIENTATION, calc.getOrientation());
        setAttribute(c, ATTR_PLACE, calc.getLocation().toString().toLowerCase());
        setAttribute(c, ATTR_METHOD, calc.getMethod());
        setAttribute(c, ATTR_ID, calc.getId());
        setAttribute(c, ATTR_INTERNAL_REF, calc.getInternalRef());

        calc.getData().forEach(d -> {
            Element e = newElement(TAG_CALCULATION_ANCHOR, c, doc);
            setAttribute(e, ATTR_DATA, d);
        });
    }

    private void addGraph(Graph graph, Element parent, Document doc) {
        Element g = newElement(TAG_GRAPH, parent, doc);

        setAttribute(g, ATTR_TYPE, graph.getType());
        setAttribute(g, ATTR_BOOK_ORIENTATION, graph.getOrientation());
        setAttribute(g, ATTR_PLACE, graph.getLocation().toString().toLowerCase());
        setAttribute(g, ATTR_METHOD, graph.getMethod());
        setAttribute(g, ATTR_ID, graph.getId());
        setAttribute(g, ATTR_INTERNAL_REF, graph.getInternalRef());
        setAttribute(g, ATTR_GRAPH_CONT_TO, graph.getContinuesTo());
        setAttribute(g, ATTR_GRAPH_CONT_FROM, graph.getContinuesFrom());
        setAttribute(g, ATTR_GRAPH_TO_TRANSC, graph.getToTranscription());
        setAttribute(g, ATTR_GRAPH_FROM_TRANSC, graph.getFromTranscription());

        graph.getNodes().forEach(n -> {
            Element e = newElement(TAG_NODE, g, doc);
            setAttribute(e, ATTR_ID, n.getId());
            setAttribute(e, ATTR_PERSON, n.getPerson());
            setAttribute(e, ATTR_ANCHOR_TEXT, n.getText());
            e.setTextContent(n.getContent() == null ? "" : n.getContent());
        });

        graph.getLinks().forEach(l -> {
            Element e = newElement(TAG_LINK, g, doc);
            setAttribute(e, ATTR_TO, l.getTarget());
            setAttribute(e, ATTR_FROM, l.getSource());
            setAttribute(e, ATTR_RELATIONSHIP, l.getRelationship());
        });

        graph.getGraphTexts().forEach(t -> {
            Element e = newElement(TAG_GRAPH_TEXT, g, doc);

            t.getNotes().forEach(n -> {
                Element no = newElement(TAG_NOTE, e, doc);
                setAttribute(no, ATTR_ID, n.id);
                setAttribute(no, ATTR_HAND, n.hand);
                setAttribute(no, ATTR_LANGUAGE, n.language);
                setAttribute(no, ATTR_INTERNAL_LINK, n.internalLink);
                setAttribute(no, ATTR_ANCHOR_TEXT, n.anchorText);
                no.setTextContent(n.content);
            });
            t.getPeople().forEach(p -> addPerson(p, e, doc));
            t.getBooks().forEach(b -> addBook(b, e, doc));
            t.getLocations().forEach(l -> addLocation(l, e, doc));
            t.getSymbols().forEach(s -> addSymbolInText(s, e, doc));
            t.getTranslations().forEach(tra -> valueElement(TAG_TRANSLATION, tra, e, doc));
        });

        graph.getInternalRefs().forEach(r -> addInternalRef(r, g, doc));
    }

    private void addTable(Table table, Element parent, Document doc) {
        Element t = newElement(TAG_TABLE, parent, doc);

        setAttribute(t, ATTR_TYPE, table.getType());
        setAttribute(t, ATTR_PLACE, table.getLocation().toString().toLowerCase());
        setAttribute(t, ATTR_ID, table.getId());
        setAttribute(t, ATTR_INTERNAL_REF, table.getInternalRef());
        setAttribute(t, ATTR_AGGREGATED_INFO, table.getAggregatedInfo());

        for (int i = 0; i < table.getRows().size(); i++) {
            final int r = i;
            TableRow row = table.getRow(i);

            Element rel = newElement(TAG_TR, t, doc);

            Element header = valueElement(TAG_TH, row.getHeaderContent(), rel, doc);
            setAttribute(header, ATTR_LABEL, row.getHeaderLabel());
            setAttribute(header, ATTR_ANCHOR_TEXT, row.getHeaderAnchorText());
            setAttribute(header, ATTR_ANCHOR_DATA, row.getHeaderAnchorData());

            // Getting all cells in this row
            table.getCells().stream().filter(cell -> cell.row == r)
                    .forEach(cell -> {
                        Element c = valueElement(TAG_TD, cell.content, rel, doc);
                        setAttribute(c, ATTR_ANCHOR_TEXT, cell.anchorText);
                        setAttribute(c, ATTR_ANCHOR_DATA, cell.anchorData);
                    });
        }
        table.getTexts().forEach(text -> addTextElement(text.getHand(), text.getLanguage(), text.getAnchor_text(),
                text.getText(), t, doc));
        table.getPeople().forEach(p -> addPerson(p, t, doc));
        table.getBooks().forEach(b -> addBook(b, t, doc));
        table.getLocations().forEach(l -> addLocation(l, t, doc));
        table.getSymbols().forEach(s -> addSymbolInText(s, t, doc));
        table.getInternalRefs().forEach(r -> addInternalRef(r, t, doc));
        if (table.getTranslation() != null && !table.getTranslation().isEmpty()) {
            valueElement(TAG_TRANSLATION, table.getTranslation(), t, doc);
        }
    }

    private void addPhysicalLink(PhysicalLink link, Element parent, Document doc) {
        Element e = newElement(TAG_PHYSICAL_LINK, parent, doc);

        link.getAllIds().forEach(id -> {
            Element ma = newElement(TAG_MARGINAL_ANNOTATION, e, doc);
            setAttribute(ma, ATTR_ID_REF, id);
        });
        link.getLinks().forEach(l -> {
            Element r = newElement(TAG_RELATION, e, doc);
            setAttribute(r, ATTR_FROM, l.getSource());
            setAttribute(r, ATTR_TO, l.getTarget());
            setAttribute(r, ATTR_TYPE, l.getRelationship());
        });
    }

    private void addMarginalia(List<Marginalia> marginalias, Element parent, Document doc) {
        for (Marginalia marginalia : marginalias) {
            Element margEl = newElement(TAG_MARGINALIA, parent, doc);

            setAttribute(margEl, ATTR_ID, marginalia.getId());
            setAttribute(margEl, ATTR_DATE, marginalia.getDate());
            setAttribute(margEl, ATTR_HAND, marginalia.getHand());
            setAttribute(margEl, ATTR_OTHER_READER, marginalia.getOtherReader());
            setAttribute(margEl, ATTR_TOPIC, marginalia.getTopic());
            setAttribute(margEl, ATTR_ANCHOR_TEXT, marginalia.getReferencedText());
            setAttribute(margEl, ATTR_BOOK_ID, marginalia.getBookId());
            setAttribute(margEl, ATTR_MARG_CONT_TO, marginalia.getContinuesTo());
            setAttribute(margEl, ATTR_MARG_CONT_FROM, marginalia.getContinuesFrom());
            setAttribute(margEl, ATTR_MARG_TO_TRANSC, marginalia.getToTranscription());
            setAttribute(margEl, ATTR_MARG_FROM_TRANSC, marginalia.getFromTranscription());
            setAttribute(margEl, ATTR_INTERNAL_REF, marginalia.getInternalRef());

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

                    pos.getSymbols().forEach(s -> addSymbolInText(s, posEl, doc));
                    pos.getPeople().forEach(p -> addPerson(p, posEl, doc));
                    pos.getBooks().forEach(b -> addBook(b, posEl, doc));
                    pos.getLocations().forEach(l -> addLocation(l, posEl, doc));
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
                    pos.getInternalRefs().forEach(r -> addInternalRef(r, posEl, doc));
                }
            }

            if (marginalia.getTranslation() != null && !marginalia.getTranslation().isEmpty()) {
                valueElement(TAG_TRANSLATION, marginalia.getTranslation(), margEl, doc);
            }
        }
    }

    private void addSymbolInText(String symbol, Element parent, Document doc) {
        Element e = newElement(TAG_SYMBOL_IN_TEXT, parent, doc);
        setAttribute(e, ATTR_NAME, symbol);
    }

    private void addInternalRef(InternalReference ref, Element parent, Document doc) {
        Element e = newElement(TAG_INTERNAL_REF, parent, doc);

        setAttribute(e, ATTR_TEXT, ref.getText());
        setAttribute(e, ATTR_ANCHOR_PREFIX, ref.getAnchorPrefix());
        setAttribute(e, ATTR_ANCHOR_SUFFIX, ref.getAnchorSuffix());

        ref.getTargets().forEach(target -> {
            Element t = newElement(TAG_TARGET, e, doc);
            setAttribute(t, ATTR_REF, target.getTargetId());
            setAttribute(t, ATTR_TEXT, target.getText());
            setAttribute(t, ATTR_PREFIX, target.getTextPrefix());
            setAttribute(t, ATTR_SUFFIX, target.getTextSuffix());
        });
    }

    private void addBook(String book, Element parent, Document doc) {
        Element e = newElement(TAG_BOOK, parent, doc);
        setAttribute(e, ATTR_TITLE, book);
    }

    private void addPerson(String person, Element parent, Document doc) {
        Element e = newElement(TAG_PERSON, parent, doc);
        setAttribute(e, ATTR_NAME, person);
    }

    private void addLocation(String place, Element parent, Document doc) {
        Element e = newElement(TAG_LOCATION, parent, doc);
        setAttribute(e, ATTR_NAME, place);
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
            String id = annotation.getAttribute(ATTR_ID);
            boolean hasId = id != null && !id.isEmpty();

            Location loc = getLocation(annotation.getAttribute(ATTR_PLACE));
            switch (annotation.getTagName()) {
                case TAG_MARGINALIA:
                    Marginalia marg = buildMarginalia(annotation, page.getPage());
                    marg.setId(hasId ? id : Annotations.annotationId(page.getPage(), TAG_MARGINALIA, page.getMarginalia().size()));

                    page.getMarginalia().add(marg);
                    break;
                case TAG_UNDERLINE:
                    page.getUnderlines().add(new Underline(
                            hasId ? id : Annotations.annotationId(page.getPage(), TAG_UNDERLINE, page.getUnderlines().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getAttribute(ATTR_METHOD),
                            annotation.getAttribute(ATTR_TYPE),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            Location.INTEXT
                    ));
                    break;
                case TAG_SYMBOL:
                    page.getSymbols().add(new Symbol(
                            hasId ? id : Annotations.annotationId(page.getPage(), TAG_SYMBOL, page.getSymbols().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getAttribute(ATTR_NAME),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            loc
                    ));
                    break;
                case TAG_MARK:
                    page.getMarks().add(new Mark(
                            hasId ? id : Annotations.annotationId(page.getPage(), TAG_MARK, page.getMarks().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getAttribute(ATTR_NAME),
                            annotation.getAttribute(ATTR_METHOD),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            loc
                    ));
                    break;
                case TAG_NUMERAL:
                    page.getNumerals().add(new Numeral(
                            hasId ? id : Annotations.annotationId(page.getPage(), TAG_NUMERAL, page.getNumerals().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            annotation.getTextContent(),
                            null,
                            loc
                    ));
                    break;
                case TAG_ERRATA:
                    page.getErrata().add(new Errata(
                            hasId ? id : Annotations.annotationId(page.getPage(), TAG_ERRATA, page.getErrata().size()),
                            annotation.getAttribute(ATTR_LANGUAGE),
                            annotation.getAttribute(ATTR_COPYTEXT),
                            annotation.getAttribute(ATTR_AMENDEDTEXT)
                    ));
                    break;
                case TAG_DRAWING:
                    page.getDrawings().add(new Drawing(
                            hasId ? id : Annotations.annotationId(page.getPage(), TAG_DRAWING, page.getDrawings().size()),
                            annotation.getAttribute(ATTR_TEXT),
                            loc,
                            annotation.getAttribute(ATTR_NAME),
                            annotation.getAttribute(ATTR_METHOD),
                            annotation.getAttribute(ATTR_LANGUAGE)
                    ));
                    break;
                case TAG_CALCULATION:
                    Calculation c = buildCalculation(annotation);
                    if (!hasId) {
                        c.setId(Annotations.annotationId(page.getPage(), TAG_CALCULATION, page.getCalculations().size()));
                    }
                    page.getCalculations().add(c);
                    break;
                case TAG_GRAPH:
                    Graph g = buildGraph(annotation);
                    if (!hasId) {
                        g.setId(Annotations.annotationId(page.getPage(), TAG_GRAPH, page.getGraphs().size()));
                    }
                    page.getGraphs().add(g);
                    break;
                case TAG_TABLE:
                    Table t = buildTable(annotation);
                    if (!hasId) {
                        t.setId(Annotations.annotationId(page.getPage(), TAG_TABLE, page.getTables().size()));
                    }
                    page.getTables().add(t);
                    break;
                case TAG_PHYSICAL_LINK:
                    PhysicalLink l = buildPhysicalLink(annotation);
                    if (!hasId) {
                        l.setId(Annotations.annotationId(page.getPage(), TAG_PHYSICAL_LINK, page.getLinks().size()));
                    }
                    page.getLinks().add(l);
                    break;
                default:
                    break;
            }
        }
    }

    private Location getLocation(String loc) {
        if (loc == null || loc.length() == 0) {
            return null;
        }
        for (Location l : Location.values()) {
            if (l.name().toLowerCase().equals(loc.toLowerCase())) {
                return l;
            }
        }
        return null;
    }

    private Marginalia buildMarginalia(Element annotation, String page) {
        Marginalia marg = new Marginalia();

        marg.setDate(annotation.getAttribute(ATTR_DATE));
        marg.setHand(annotation.getAttribute(ATTR_HAND));
        marg.setOtherReader(annotation.getAttribute(ATTR_OTHER_READER));
        marg.setTopic(annotation.getAttribute(ATTR_TOPIC));
        marg.setReferencedText(annotation.getAttribute(ATTR_ANCHOR_TEXT));
        marg.setContinuesTo(annotation.getAttribute(ATTR_MARG_CONT_TO));
        marg.setContinuesFrom(annotation.getAttribute(ATTR_MARG_CONT_FROM));
        marg.setToTranscription(annotation.getAttribute(ATTR_MARG_TO_TRANSC));
        marg.setFromTranscription(annotation.getAttribute(ATTR_MARG_FROM_TRANSC));
        marg.setInternalRef(annotation.getAttribute(ATTR_INTERNAL_REF));
        marg.setDate(annotation.getAttribute(ATTR_DATE));
        marg.setColor(annotation.getAttribute(ATTR_COLOR));

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

        NodeList list = position.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element el = (Element) node;
            switch (el.getTagName()) {
                case TAG_PERSON:
                    pos.getPeople().add(hasAttribute(ATTR_NAME, el) ?
                            el.getAttribute(ATTR_NAME) : el.getAttribute(ATTR_PERSON_NAME));
                    break;
                case TAG_BOOK:
                    pos.getBooks().add(el.getAttribute(ATTR_TITLE));
                    break;
                case TAG_LOCATION:
                    pos.getLocations().add(hasAttribute(ATTR_NAME, el) ?
                            el.getAttribute(ATTR_NAME) : el.getAttribute(ATTR_LOCATION_NAME));
                    break;
                case TAG_MARGINALIA_TEXT:
                    pos.getTexts().add(el.getTextContent());
                    NodeList children = el.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        if (children.item(j).getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element child = (Element) children.item(j);
                        if (child.getTagName().equals(TAG_MARGINALIA_REF)) {
//                            InternalReference r = new InternalReference();
//                            r.setText(child.getTextContent());
//                            r.addTargets();
                            throw new UnsupportedOperationException("Element not yet supported: " + TAG_MARGINALIA_REF);
                        }
                    }
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

                    pos.getEmphasis().add(new Underline(id.toString(),
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
                    pos.getxRefs().add(xRef);
                    break;
                case TAG_SYMBOL_IN_TEXT:
                    pos.getSymbols().add(el.getAttribute(ATTR_NAME));
                    break;
                case TAG_INTERNAL_REF:
                    InternalReference ref = buildInternalRef(el);
                    if (ref != null) {
                        pos.getInternalRefs().add(ref);
                    }
                default:
                    break;
            }
        }

        return pos;
    }

    /**
     * @param el internal_ref XML element
     * @return {@link InternalReference} object from the XML
     */
    private InternalReference buildInternalRef(Element el) {
        InternalReference ir = new InternalReference();

        ir.setText(el.getAttribute(ATTR_TEXT));

        // Build targets
        NodeList children = el.getChildNodes();
        if (children == null) {
            System.err.println("No targets found for this reference.");
            return null;
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
                ir.addTargets(t);
            }
        }

        return ir;
    }

    private Table buildTable(Element tableEl) {
        Table table = new Table(
                tableEl.getAttribute(ATTR_ID),
                getLocation(tableEl.getAttribute(ATTR_PLACE))
        );

        table.setType(tableEl.getAttribute(ATTR_TYPE));
        table.setAggregatedInfo(tableEl.getAttribute(ATTR_AGGREGATED_INFO));

        NodeList children = tableEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) children.item(i);
            int rowCount = 0;
            switch (child.getTagName()) {
                case TAG_TR:
                    NodeList headers = tableEl.getElementsByTagName(TAG_TH);
                    if (headers.getLength() > 0) {
                        Element h = (Element) headers.item(0);
                        table.getRows().add(new TableRow(
                                table.getRows().size(),
                                h.getAttribute(ATTR_LABEL),
                                h.getAttribute(ATTR_ANCHOR_TEXT),
                                h.getAttribute(ATTR_ANCHOR_DATA),
                                h.getTextContent()
                        ));
                    }

                    NodeList cols = tableEl.getElementsByTagName(TAG_TD);
                    for (int j = 0; j < cols.getLength(); j++) {
                        Element c = (Element) cols.item(j);
                        table.getCells().add(new TableCell(
                                // int row, int col, String anchorText, String anchorData, String content
                                rowCount,
                                j,
                                c.getAttribute(ATTR_ANCHOR_TEXT),
                                c.getAttribute(ATTR_ANCHOR_DATA),
                                c.getTextContent()
                        ));
                    }
                    rowCount++;
                    break;
                case TAG_TEXT:
                    table.getTexts().add(new TextEl(
                            child.getAttribute(ATTR_HAND),
                            child.getAttribute(ATTR_LANGUAGE),
                            child.getAttribute(ATTR_ANCHOR_TEXT),
                            child.getTextContent().trim()
                    ));
                    break;
                case TAG_PERSON:
                    table.getPeople().add(child.getAttribute(ATTR_NAME));
                    break;
                case TAG_BOOK:
                    table.getBooks().add(child.getAttribute(ATTR_TITLE));
                    break;
                case TAG_LOCATION:
                    table.getLocations().add(child.getAttribute(ATTR_NAME));
                    break;
                case TAG_SYMBOL_IN_TEXT:
                    table.getSymbols().add(child.getAttribute(ATTR_NAME));
                    break;
                case TAG_INTERNAL_REF:
                    table.getInternalRefs().add(buildInternalRef(child));
                    break;
                case TAG_TRANSLATION:
                    table.setTranslation(child.getTextContent());
                    break;
                default:
                    break;
            }
        }

        return table;
    }

    /**
     * @param graphEl graph XML element
     * @return {@link Graph} object
     */
    private Graph buildGraph(Element graphEl) {
        Graph g = new Graph(
                graphEl.getAttribute(ATTR_ID),
                graphEl.getAttribute(ATTR_TYPE),
                getOrientationAngle(graphEl.getAttribute(ATTR_BOOK_ORIENTATION)),
                getLocation(graphEl.getAttribute(ATTR_PLACE)),
                graphEl.getAttribute(ATTR_METHOD)
        );

        NodeList children = graphEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) children.item(i);
            switch (child.getTagName()) {
                case TAG_NODE:
                    g.addNode(new GraphNode(
                            child.getAttribute(ATTR_ID),
                            child.getAttribute(ATTR_PERSON),
                            child.getAttribute(ATTR_ANCHOR_TEXT),
                            child.getTextContent().trim()
                    ));
                    break;
                case TAG_LINK:
                    g.addLink(new AnnotationLink(
                            null,
                            child.getAttribute(ATTR_FROM),
                            child.getAttribute(ATTR_TO),
                            child.getAttribute(ATTR_RELATIONSHIP)
                    ));
                    break;
                case TAG_GRAPH_TEXT:
                    g.addGraphText(buildGraphText(child));
                    break;
                case TAG_INTERNAL_REF:
                    g.getInternalRefs().add(buildInternalRef(child));
                    break;
                default:
                    break;
            }
        }
        return g;
    }

    private GraphText buildGraphText(Element el) {
        GraphText gt = new GraphText();

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) children.item(i);
            switch (child.getTagName()) {
                case TAG_NOTE:
                    gt.addNote(new GraphNote(
                            child.getAttribute(ATTR_ID),
                            child.getAttribute(ATTR_HAND),
                            child.getAttribute(ATTR_LANGUAGE),
                            child.getAttribute(ATTR_INTERNAL_LINK),
                            child.getAttribute(ATTR_ANCHOR_TEXT),
                            child.getTextContent()
                    ));
                    break;
                case TAG_PERSON:
                    gt.addPerson(child.getAttribute(ATTR_NAME));
                    break;
                case TAG_BOOK:
                    gt.addBook(child.getAttribute(ATTR_TITLE));
                    break;
                case TAG_LOCATION:
                    gt.addLocation(child.getAttribute(ATTR_NAME));
                    break;
                case TAG_SYMBOL_IN_TEXT:
                    gt.addSymbol(child.getAttribute(ATTR_NAME));
                    break;
                case TAG_TRANSLATION:
                    gt.addTranslation(child.getTextContent());
                    break;
                default:
                    break;
            }
        }

        return gt;
    }

    /**
     * @param calcEl calculation XML element
     * @return {@link Calculation} object from the XML
     */
    private Calculation buildCalculation(Element calcEl) {
        Calculation calc = new Calculation(
                calcEl.getAttribute(ATTR_ID),
                calcEl.getAttribute(ATTR_TYPE),
                getOrientationAngle(calcEl.getAttribute(ATTR_BOOK_ORIENTATION)),
                getLocation(calcEl.getAttribute(ATTR_PLACE)),
                calcEl.getAttribute(ATTR_METHOD),
                calcEl.getAttribute(ATTR_INTERNAL_REF)
        );

        NodeList children = calcEl.getElementsByTagName(TAG_CALCULATION_ANCHOR);
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                calc.addData(((Element) children.item(i)).getAttribute(ATTR_DATA));
            }
        }

        return calc;
    }

    private int getOrientationAngle(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private PhysicalLink buildPhysicalLink(Element el) {
        PhysicalLink link = new PhysicalLink();

        NodeList relations = el.getElementsByTagName(TAG_RELATION);
        for (int i = 0; i < relations.getLength(); i++) {
            if (relations.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element)relations.item(i);
                link.getLinks().add(new AnnotationLink(
                        null,
                        e.getAttribute(ATTR_FROM),
                        e.getAttribute(ATTR_TO),
                        e.getAttribute(ATTR_TYPE)
                ));
            }
        }

        return link;
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
