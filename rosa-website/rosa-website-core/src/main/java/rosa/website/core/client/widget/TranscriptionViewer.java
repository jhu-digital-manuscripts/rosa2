package rosa.website.core.client.widget;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.HtmlSanitizer;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;
import com.google.gwt.xml.client.impl.DOMParseException;
import rosa.archive.model.BookScene;
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.archive.model.NarrativeScene;
import rosa.archive.model.NarrativeSections;
import rosa.archive.model.NarrativeTagging;
import rosa.website.core.client.Labels;
import rosa.website.core.shared.ImageNameParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO update refactor this
// Whenever text is added to the DOM, must normalize spacing

/**
 * Adapted from old Rosa1 code.
 * https://github.com/jhu-digital-manuscripts/rosa/blob/master/rosa-website-common/src/main/java/rosa/gwt/common/client/TranscriptionViewer.java
 */
public class TranscriptionViewer {
    private static final Logger LOGGER = Logger.getLogger(TranscriptionViewer.class.toString());
    private static final HtmlSanitizer HTML_SANITIZER = SimpleHtmlSanitizer.getInstance();

    /**
     *
     * @param transxml array of XML fragments with transcription data
     * @param transnames array of labels for the XML fragments
     * @param lecoy .
     * @return a Widget displaying the data
     * @throws NullPointerException if {@param transxml} or if {@param transnames} is NULL
     */
    public static TabLayoutPanel createTranscriptionViewer(String[] transxml, final String[] transnames,
                                                           boolean lecoy) {
        final TabLayoutPanel tabs = new TabLayoutPanel(1.5, Unit.EM);
        tabs.addStyleName("Transcription");

        for (int i = 0; i < transxml.length; i++) {
            String xml = transxml[i];
            String name = transnames[i];

            if (xml != null) {
                try {
                    Document doc = XMLParser.parse(xml);

                    com.google.gwt.dom.client.Document htmldoc = com.google.gwt.dom.client.Document.get();

                    displayTranscription(tabs, htmldoc, null, name, doc.getDocumentElement(), lecoy);
                } catch (DOMParseException e) {
                    tabs.add(new Label(Labels.INSTANCE.transcriptionUnavailable()), name);
                    LOGGER.log(Level.SEVERE, "Error parsing XML", e);
                }

            } else {
                tabs.add(new Label(Labels.INSTANCE.transcriptionUnavailable()), name);
            }
        }

        tabs.selectTab(0);

        return tabs;
    }

    /**
     *
     * @param selectedPages names of all pages currently in view
     * @param illustrations illustration tagging data
     * @return a Widget displaying the relevant illustration data
     */
    public static TabLayoutPanel createIllustrationTaggingViewer(String[] selectedPages,
                                                                 IllustrationTagging illustrations) {
        if (selectedPages == null || selectedPages.length == 0) {
            return null;
        }

        TabLayoutPanel display = new TabLayoutPanel(1.5, Unit.EM);
        display.addStyleName("Transcription");

        for (String page : selectedPages) {
            int count = 1;

            for (String content : getIllustrationDescriptions(page, illustrations)) {
                String tabLabel = page + " " + count++;
                display.add(new ScrollPanel(new HTML(content)), tabLabel);
            }
        }

        return display;
    }

    public static TabLayoutPanel createNarrativeTaggingViewer(String[] selectedPages,
                                                              NarrativeTagging tagging, NarrativeSections sections) {
        if (sections == null || tagging == null
                || selectedPages == null || selectedPages.length == 0) {
            LOGGER.warning("No data found while looking for narrative tagging.");
            return null;
        }

        TabLayoutPanel display = new TabLayoutPanel(1.5, Unit.EM);
        display.addStyleName("Transcription");

        for (String page : selectedPages) {
            narrativeTagging(page, tagging, sections, display);
        }

        return display;
    }

    private static void narrativeTagging(String page, NarrativeTagging tagging,
                                         NarrativeSections sections, TabLayoutPanel container) {
        if (page == null || page.isEmpty()) {
            LOGGER.warning("No page given, so no matching narrative tagging can be found.");
            return;
        }
        page = ImageNameParser.toStandardName(page);
        boolean isRecto = page.endsWith("r") || page.endsWith("R");

        // Column -> list of scenes
        Map<String, List<BookScene>> columnMap = new HashMap<>();
        if (isRecto) {
            columnMap.put("c", new ArrayList<BookScene>());
            columnMap.put("d", new ArrayList<BookScene>());
        } else {
            columnMap.put("a", new ArrayList<BookScene>());
            columnMap.put("b", new ArrayList<BookScene>());
        }

        // Map scenes on this page to appropriate columns
        for (BookScene scene : tagging) {
            String start = ImageNameParser.toStandardName(scene.getStartPage());
            String end = ImageNameParser.toStandardName(scene.getEndPage());

            // If page falls within scene range TODO seems to be wrong...
            if (page.compareToIgnoreCase(start) > 0 && page.compareToIgnoreCase(end) < 0) {
                if (isRecto) {
                    columnMap.get("c").add(scene);
                    columnMap.get("d").add(scene);
                } else {
                    columnMap.get("a").add(scene);
                    columnMap.get("b").add(scene);
                }
            } else if (page.compareToIgnoreCase(start) == 0) {
                if (isRecto && scene.getStartPageCol().equals("d")) {
                    columnMap.get("d").add(scene);
                } else if (!isRecto && scene.getStartPageCol().equals("b")) {
                    columnMap.get("b").add(scene);
                } else if (isRecto) {
                    columnMap.get("c").add(scene);
                    columnMap.get("d").add(scene);
                } else {
                    columnMap.get("a").add(scene);
                    columnMap.get("b").add(scene);
                }
            } else if (page.compareToIgnoreCase(end) == 0) {
                if (isRecto && scene.getEndPageCol().equals("c")) {
                    columnMap.get("c").add(scene);
                } else if (!isRecto && scene.getEndPageCol().equals("a")) {
                    columnMap.get("a").add(scene);
                } else if (isRecto) {
                    columnMap.get("c").add(scene);
                    columnMap.get("d").add(scene);
                } else {
                    columnMap.get("a").add(scene);
                    columnMap.get("b").add(scene);
                }
            }
        }

        for (Entry<String, List<BookScene>> entry : columnMap.entrySet()) {
            final String tabName = page + "." + entry.getKey();     // page.column

            StringBuilder sb = new StringBuilder();
            for (BookScene scene : entry.getValue()) {
                sb.append("<p>");

                sb.append("<span class=\"ImageDescriptionLabel\">");
                sb.append(HTML_SANITIZER.sanitize(scene.getId()).asString());
                sb.append(' ');
                sb.append(scene.isCorrect() ? ":" : "?");
                sb.append("</span><br/>");

                if (sections.findIndexOfSceneById(scene.getId()) > sections.asScenes().size()) {
                    continue;
                }

                NarrativeScene s = sections.asScenes().get(sections.findIndexOfSceneById(scene.getId()));
                if (s.getDescription() != null && !s.getDescription().isEmpty()) {
                    sb.append(HTML_SANITIZER.sanitize(s.getDescription()).asString());
                }
                if (s.getCriticalEditionStart() > 0) {
                    sb.append("<span class=\"TranscriptionLecoy\">");
                    sb.append(" L");
                    sb.append(HTML_SANITIZER.sanitize(String.valueOf(s.getCriticalEditionStart())).asString());
                    sb.append("</span>");
                }
                sb.append("</p>");
            }

            container.add(new ScrollPanel(new HTML(sb.toString())), tabName);
        }
    }

    /**
     * Get the HTML to be displayed about all of the illustrations that appear
     * on a given page.
     *
     * @param page page name
     * @param illustrations Illustration Tagging
     * @return array of Strings of HTML content
     */
    private static String[] getIllustrationDescriptions(String page, IllustrationTagging illustrations) {
        List<String> descriptions = new ArrayList<>();

        for (int i = 0; i < illustrations.size(); i++) {
            Illustration ill = illustrations.getIllustrationData(i);

            if (ImageNameParser.toStandardName(ill.getPage()).equals(
                    ImageNameParser.toStandardName(page))) {
                StringBuilder sb = new StringBuilder("<p>");

                addIllustrationKeywords(sb, Labels.INSTANCE.illustrationTitles(), ill.getTitles());
                if (isNotEmpty(ill.getTextualElement())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.textualElements(), ill.getTextualElement());
                }
                if (isNotEmpty(ill.getInitials())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.initials(), ill.getInitials());
                }
                if (isNotEmpty(ill.getCharacters())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.characterNames(), ill.getCharacters());
                }
                if (isNotEmpty(ill.getCostume())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.costume(), ill.getCostume());
                }
                if (isNotEmpty(ill.getObject())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.objects(), ill.getObject());
                }
                if (isNotEmpty(ill.getLandscape())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.landscape(), ill.getLandscape());
                }
                if (isNotEmpty(ill.getArchitecture())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.architecture(), ill.getArchitecture());
                }
                if (isNotEmpty(ill.getOther())) {
                    addIllustrationKeywords(sb, Labels.INSTANCE.other(), ill.getOther());
                }

                sb.append("</p>");

                descriptions.add(sb.toString().trim());
            }
        }

        return descriptions.toArray(new String[descriptions.size()]);
    }

    /**
     * Return TRUE if the array is not NULL and contains one or more non-empty string.
     *
     * @param strings array of strings
     * @return does this array have data?
     */
    private static boolean isNotEmpty(String[] strings) {
        if (strings == null || strings.length == 0) {
            return false;
        }

        boolean isNotEmpty = false;
        for (String str : strings) {
            if (isNotEmpty(str)) {
                isNotEmpty = true;
                break;
            }
        }

        return isNotEmpty;
    }

    private static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static void addIllustrationKeywords(StringBuilder sb, String label, String ... text) {
        if (text == null || text.length == 0 || label == null || label.isEmpty()) {
            return;
        }

        sb.append("<span class=\"ImageDescriptionLabel\">");
        sb.append(HTML_SANITIZER.sanitize(label).asString());
        sb.append(": </span>");

        sb.append("<span class=\"ImageDescriptionText\">");
        boolean isFirst = true;
        for (String str : text) {
            if (!isFirst) {
                sb.append(", ");
            }

            sb.append(HTML_SANITIZER.sanitize(str).asString());
            isFirst = false;
        }
        sb.append("</span>");

        sb.append("<br/>");
    }

    private static boolean attributeEquals(String attribute, String value, Element el) {
        return el != null && el.hasAttribute(attribute)
                && el.getAttribute(attribute).equals(value);
    }

	private static void displayTranscriptionPoetry(com.google.gwt.dom.client.Document htmldoc,
			com.google.gwt.dom.client.Element display, Node parent, boolean lecoy) {

		for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
			String name = n.getNodeName();

			Element e = n.getNodeType() == Node.ELEMENT_NODE ? (Element) n : null;

            switch (name) {
                case "note":
                    if (attributeEquals("type", "scribalPunc", e)) {
                        display.appendChild(span(htmldoc, n, null));
                    } else {
                        AnchorElement anchor = htmldoc.createAnchorElement();
                        anchor.setClassName("Tooltip");
                        anchor.setHref("#");
                        anchor.setAttribute("onClick", "return false;");
                        anchor.appendChild(htmldoc.createTextNode("*"));
                        anchor.appendChild(span(htmldoc, n, null));
                        display.appendChild(anchor);
                    }
                    break;
                case "expan":
                    display.appendChild(span(htmldoc, n, "TranscriptionExpan"));
                    break;
                case "hi":
                    if (attributeEquals("rend", "init", e)) {
                        display.appendChild(span(htmldoc, n, "TranscriptionInitial"));
                    } else if (attributeEquals("rend", "rubric", e)) {
                        com.google.gwt.dom.client.Element span = htmldoc.createSpanElement();
                        span.setClassName("TranscriptionRubric");

                        displayTranscriptionPoetry(htmldoc, span, n, lecoy);
                        display.appendChild(span);
                    } else if (attributeEquals("rend", "nota", e)) {
                        display.appendChild(span(htmldoc, n, null));
                    } else {
                        if (e != null) {
                            LOGGER.warning("Unknown <hi> rend [" + e.getAttribute("rend") + "]");
                        }
                    }

                    break;
                case "add":
                    display.appendChild(span(htmldoc, n, "TranscriptionAdd"));
                    break;
                case "del":
                    display.appendChild(span(htmldoc, n, "TranscriptionDel"));
                    break;
                case "milestone":
                    if (lecoy && e != null) {
                        String num = e.getAttribute("n");
                        display.appendChild(span(htmldoc, " L" + num, "TranscriptionLecoy"));
                    }
                    break;
                case "gap":
                    break;
                default:
                    if (n.getNodeType() == Node.TEXT_NODE) {
                        String s = extractText(n).replaceAll("\\s+", " ");
                        display.appendChild(htmldoc.createTextNode(s));
                    } else {
                        LOGGER.warning("Unhandled node [" + name + "]");
                    }
            }
		}
	}

	private static com.google.gwt.dom.client.Element span(com.google.gwt.dom.client.Document htmldoc,
                                                          String text, String domclass) {
		com.google.gwt.dom.client.Element span = htmldoc.createSpanElement();

		if (domclass != null) {
			span.setClassName(domclass);
		}

		span.setInnerText(text.replaceAll("\\s+", " "));

		return span;
	}

	private static com.google.gwt.dom.client.Element span(com.google.gwt.dom.client.Document htmldoc,
                                                          Node node, String domclass) {
		return span(htmldoc, extractText(node), domclass);
	}

	// Return current node in the node being appended to
	private static com.google.gwt.dom.client.Element displayTranscription(TabLayoutPanel tabs,
                  com.google.gwt.dom.client.Document htmldoc, com.google.gwt.dom.client.Element display,
                  String imagename, Node parent, boolean lecoy) {

		for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}

			Element el = (Element) n;
			String name = el.getNodeName();

            switch (name) {
                case "cb":
                    SimplePanel w = new SimplePanel();
                    ScrollPanel tabpanel = new ScrollPanel(w);

                    display = w.getElement();
                    tabs.add(tabpanel, imagename + " " + el.getAttribute("n"));

                    continue;
                case "lg":
                    display = displayTranscription(tabs, htmldoc, display,
                            imagename, el, lecoy);
                    continue;
                case "pb":
                    continue;
                default:
                    break;
            }

			// Handle case of content before cb
			if (display == null) {
				SimplePanel w = new SimplePanel();
				ScrollPanel tabpanel = new ScrollPanel(w);
				display = w.getElement();
				tabs.add(tabpanel, imagename);
			}

            switch (name) {
                case "div":
                    display.appendChild(span(htmldoc, Labels.INSTANCE.illustration() + ": ", "TranscriptionExtraHeader"));
                    display.appendChild(span(htmldoc, n, "TranscriptionExtra"));
                    display.appendChild(htmldoc.createBRElement());
                    break;
                case "fw":
                    display.appendChild(span(htmldoc, Labels.INSTANCE.catchphrase() + ": ", "TranscriptionExtraHeader"));
                    display.appendChild(span(htmldoc, n, "TranscriptionExtra"));
                    display.appendChild(htmldoc.createBRElement());
                    break;
                case "l":
                    displayTranscriptionPoetry(htmldoc, display, el, lecoy);

                    // Hack to deal with milestone appearing as sibling rather than child of l
                    for (Node next = n.getNextSibling();; next = next.getNextSibling()) {
                        if (next == null) {
                            display.appendChild(htmldoc.createBRElement());
                            break;
                        }

                        if (next.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        // milestone will add the line break
                        if (next.getNodeName().equals("milestone")) {
                            break;
                        } else {
                            display.appendChild(htmldoc.createBRElement());
                            break;
                        }
                    }
                    break;
                case "milestone":
                    if (lecoy) {
                        String num = el.getAttribute("n");
                        display.appendChild(span(htmldoc, " L" + num, "TranscriptionLecoy"));
                    }

                    display.appendChild(htmldoc.createBRElement());
                    break;
                default:
                    LOGGER.warning("Unknown element [" + name + "]");
                    break;
            }
		}

		return display;
	}

    private static String extractText(Node n) {
        StringBuffer buf = new StringBuffer();
        extractText(n, buf);

        return buf.toString();
    }

    private static void extractText(Node n, StringBuffer buf) {
        if (n.getNodeType() == Node.TEXT_NODE) {
            buf.append(n.getNodeValue());
        }

        for (n = n.getFirstChild(); n != null; n = n.getNextSibling()) {
            extractText(n, buf);
        }
    }

}
