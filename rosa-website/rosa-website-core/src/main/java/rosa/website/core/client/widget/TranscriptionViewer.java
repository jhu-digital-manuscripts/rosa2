package rosa.website.core.client.widget;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Style.Unit;
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
import rosa.archive.model.Illustration;
import rosa.archive.model.IllustrationTagging;
import rosa.website.core.client.Labels;
import rosa.website.core.shared.ImageNameParser;

import java.util.ArrayList;
import java.util.List;
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

    public static String transcriptionUnavalableLabel = "";
    public static String illustrationLabel = "";
    public static String catchphraseLabel = "";

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
                    tabs.add(new Label(transcriptionUnavalableLabel), name);
                    LOGGER.log(Level.SEVERE, "Error parsing XML", e);
                }

            } else {
                tabs.add(new Label(transcriptionUnavalableLabel), name);
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
                addIllustrationKeywords(sb, Labels.INSTANCE.textualElements(), ill.getTextualElement());
                addIllustrationKeywords(sb, Labels.INSTANCE.initials(), ill.getInitials());
                addIllustrationKeywords(sb, Labels.INSTANCE.characterNames(), ill.getCharacters());
                addIllustrationKeywords(sb, Labels.INSTANCE.costume(), ill.getCostume());
                addIllustrationKeywords(sb, Labels.INSTANCE.objects(), ill.getObject());
                addIllustrationKeywords(sb, Labels.INSTANCE.landscape(), ill.getLandscape());
                addIllustrationKeywords(sb, Labels.INSTANCE.architecture(), ill.getArchitecture());
                addIllustrationKeywords(sb, Labels.INSTANCE.other(), ill.getOther());

                sb.append("</p>");

                descriptions.add(sb.toString().trim());
            }
        }

        return descriptions.toArray(new String[descriptions.size()]);
    }

    private static void addIllustrationKeywords(StringBuilder sb, String label, String ... text) {
        if (text == null || text.length == 0 || label == null || label.isEmpty()) {
            return;
        }

        sb.append("<span class=\"ImageDescriptionLabel\">");
        sb.append(SimpleHtmlSanitizer.sanitizeHtml(label));
        sb.append("</span>");

        sb.append("<span class=\"ImageDescriptionText\">");
        boolean isFirst = true;
        for (String str : text) {
            if (!isFirst) {
                sb.append(", ");
            }

            sb.append(SimpleHtmlSanitizer.sanitizeHtml(str));
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
                    display.appendChild(span(htmldoc, illustrationLabel + ": ", "TranscriptionExtraHeader"));
                    display.appendChild(span(htmldoc, n, "TranscriptionExtra"));
                    display.appendChild(htmldoc.createBRElement());
                    break;
                case "fw":
                    display.appendChild(span(htmldoc, catchphraseLabel + ": ", "TranscriptionExtraHeader"));
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

//    private void displayIllustrationKeywordsOnRight(final Panel container) {
//        int recto = -1;
//        int verso = -1;
//
//        ImageTagging illus = book.illustrations();
//
//        if (illus == null) {
//            String illusurl = GWT.getHostPageBaseURL() + DATA_PATH + book.illustrationsPath();
//
//            loadingdialog.display();
//
//            HttpGet.request(illusurl, new HttpGet.Callback<String>() {
//                public void failure(String error) {
//                    loadingdialog.error(error);
//                }
//
//                public void success(String result) {
//                    loadingdialog.hide();
//                    book.setIllustrations(result);
//
//                    if (book.illustrations() != null) {
//                        displayIllustrationKeywordsOnRight(container);
//                    }
//                }
//            });
//        } else {
//            if (Book.isRectoImage(selectedImageIndex)) {
//                recto = selectedImageIndex;
//
//                if (recto > 0) {
//                    verso = selectedImageIndex - 1;
//                }
//            } else {
//                if (selectedImageIndex + 1 < book.numImages()) {
//                    recto = selectedImageIndex + 1;
//                }
//
//                verso = selectedImageIndex;
//            }
//
//            TabLayoutPanel tabpanel = new TabLayoutPanel(1.5, Unit.EM);
//            tabpanel.addStyleName("ImageDescription");
//
//            if (verso != -1) {
//                displayIllustrationKeywords(tabpanel, verso);
//            }
//
//            if (recto != -1) {
//                displayIllustrationKeywords(tabpanel, recto);
//            }
//
//            if (tabpanel.getWidgetCount() > 0) {
//                tabpanel.selectTab(0);
//                container.add(tabpanel);
//            }
//        }
//    }
//
//    private void displayIllustrationKeywords(TabLayoutPanel tabpanel, int image) {
//        ImageTagging illus = book.illustrations();
//
//        int count = 1;
//        List<Integer> indexes = illus.findImageIndexes(image);
//
//        for (int i : indexes) {
//            String name = Book.shortImageName(book.imageName(image))
//                    + (indexes.size() > 1 ? " " + count++ : "");
//            tabpanel.add(new ScrollPanel(illus.displayImage(i)), name);
//        }
//    }
}
