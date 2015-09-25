package rosa.website.viewer.server;

import rosa.archive.model.Book;
import rosa.archive.model.BookImage;
import rosa.archive.model.ImageList;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.Map;

public class FSISerializer implements FSIConstants {
    private static final String UTF8 = "UTF-8";

    private Map<String, String> fsi_shares;

    /**
     * @param fsi_shares mapping archive collection IDs to FSI share names
     */
    public FSISerializer(Map<String, String> fsi_shares) {
        this.fsi_shares = fsi_shares;
    }

    /**
     * Write out a *.pages.fsi XML to an output stream. This XML file configures
     * the FSI flash viewer object to show a book with openings in reading order.
     *
     * @param collection collection ID
     * @param book book loaded from archive
     * @param out output stream to write XML
     * @return TRUE if XML has written successfully, FALSE if there was an error
     * @throws XMLStreamException .
     */
    public boolean fsiPagesDoc(String collection, Book book, OutputStream out) throws XMLStreamException {
        XMLStreamWriter writer = getXMLStreamWriter(out);
        if (writer == null) {
            return false;
        }

        ImageList images = book.getCroppedImages() == null ? book.getImages() : book.getCroppedImages();
        int start = -1;
        int end = -1;
        String frontcover = null;
        String backcover = null;
        boolean hasCrop = book.getCroppedImages() != null;

        for (int i = 0; i < images.getImages().size(); i++) {
            BookImage image = images.getImages().get(i);
            if (image.getRole() == null) {
                continue;
            }

            switch (image.getRole()) {
                case FRONT_COVER:
                    if (!image.isMissing()) {
                        frontcover = image.getId();
                    }
                    start = i + 1;
                    break;
                case BACK_COVER:
                    if (!image.isMissing()) {
                        backcover = image.getId();
                    }
                    end = i;
                    break;
                default:break;
            }
        }

        writer.writeStartDocument(UTF8, "1.0");
        writer.writeStartElement(TAG_FSI_PARAMETER);
        writer.writeStartElement(TAG_PLUGINS);

        writer.writeStartElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "pages");

        writer.writeEmptyElement("BendEffectIntensity");
        writer.writeAttribute(ATTR_VALUE, "0");

        writer.writeEmptyElement("Print");
        writer.writeAttribute(ATTR_VALUE, "0");

        writer.writeEmptyElement("Save");
        writer.writeAttribute(ATTR_VALUE, "0");

        writer.writeEmptyElement("Search");
        writer.writeAttribute(ATTR_VALUE, "0");

        writer.writeEmptyElement("FrontCoverImage");
        writer.writeAttribute(ATTR_VALUE, buildFsiShare(collection, book.getId(), frontcover, hasCrop));

        writer.writeEmptyElement("BackCoverImage");
        writer.writeAttribute(ATTR_VALUE, buildFsiShare(collection, book.getId(), backcover, hasCrop));

        writer.writeEndElement(); // </PLUGIN>

        writer.writeEmptyElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "fullscreen");

        writer.writeEmptyElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "resize");

        writer.writeStartElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "jsbridge");
        writer.writeAttribute(TAG_CALLBACK, "true");

        writer.writeEmptyElement(TAG_ALLOWDOMAINS);
        writer.writeAttribute(ATTR_VALUE, ALLOW_DOMAINS);
        writer.writeEndElement();

        writer.writeEndElement(); // </PLUGINS>

        writer.writeStartElement(TAG_IMAGES);

        // Do not include front/back cover because they are specified above.
        // This will also exclude MISC images...
        for (int i = start; i < end; i++) {
            BookImage image = images.getImages().get(i);
            writer.writeStartElement(TAG_IMAGE);
            writer.writeAttribute(ATTR_LABEL, image.getName());

            writer.writeStartElement(TAG_FPX);
            writer.writeEmptyElement(TAG_SRC);
            if (image.isMissing()) {
                writer.writeAttribute(ATTR_VALUE, missingImageFsiShare(collection));
            } else {
                writer.writeAttribute(ATTR_VALUE, buildFsiShare(collection, book.getId(), image.getId(), hasCrop));
            }

            writer.writeEndElement();// </FPX>
            writer.writeEndElement();// </Image>
        }

        writer.writeEndElement(); // </Images>
        writer.writeEndElement(); // </fsi_parameter>
        writer.writeEndDocument();

        return true;
    }

    /**
     * Write out a *.showcase.fsi to an output stream. This XML file configures
     * the FSI flash viewer object to show a book with thumbnails.
     *
     * @param collection collection ID
     * @param book book loaded from archive
     * @param out output stream to write XML
     * @return TRUE if XML has written successfully, FALSE otherwise
     * @throws XMLStreamException .
     */
    public boolean fsiShowcaseDoc(String collection, Book book, OutputStream out) throws XMLStreamException {
        XMLStreamWriter writer = getXMLStreamWriter(out);
        if (writer == null) {
            return false;
        }

        writer.writeStartElement(TAG_FSI_PARAMETER);
        writer.writeStartElement(TAG_PLUGINS);

        writer.writeStartElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "showcase");

        writer.writeEmptyElement(TAG_LABEL_TEXT_SIZE);
        writer.writeAttribute(ATTR_VALUE, "11");

        writer.writeEmptyElement(TAG_LABEL_MARGIN_TOP);
        writer.writeAttribute(ATTR_VALUE, "2");

        writer.writeEmptyElement(TAG_LABEL_CONTENT);
        writer.writeAttribute(ATTR_VALUE, "<b><ImageLabel /></b>");

        writer.writeEmptyElement(TAG_LABEL_TEXT_COLOR);
        writer.writeAttribute(ATTR_VALUE, "000000");

        writer.writeEmptyElement(TAG_LAYOUT);
        writer.writeAttribute(ATTR_VALUE, "float");

        writer.writeEmptyElement(TAG_DRAG_MENU_WIDTH);
        writer.writeAttribute(ATTR_VALUE, "15");

        writer.writeEmptyElement(TAG_DRAG_BAR_WIDTH);
        writer.writeAttribute(ATTR_VALUE, "15");

        writer.writeEmptyElement(TAG_BACKGROUND_ALPHA);
        writer.writeAttribute(ATTR_VALUE, "75");

        writer.writeEmptyElement(TAG_TOOLTIPS);
        writer.writeAttribute(ATTR_VALUE, "true");

        writer.writeEmptyElement(TAG_ALIGN);
        writer.writeAttribute(ATTR_VALUE, "bottom");

        writer.writeEmptyElement(TAG_THUMB_FACE);
        writer.writeAttribute(ATTR_VALUE, "CCCCCC");

        writer.writeEmptyElement(TAG_THUMB_ACTIVE_FACE);
        writer.writeAttribute(ATTR_VALUE, "F2984C");

        writer.writeEmptyElement(TAG_THUMB_SELECTED_FACE);
        writer.writeAttribute(ATTR_VALUE, "FFF0AA");

        writer.writeEndElement(); // </PLUGIN> (showcase plugin)

        writer.writeEmptyElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "fullscreen");

        writer.writeEmptyElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "resize");

        writer.writeStartElement(TAG_PLUGIN);
        writer.writeAttribute(ATTR_SRC, "jsbridge");
        writer.writeAttribute(TAG_CALLBACK, "true");

        writer.writeEmptyElement(TAG_ALLOWDOMAINS);
        writer.writeAttribute(ATTR_VALUE, ALLOW_DOMAINS);
        writer.writeEndElement();

        writer.writeEndElement(); // </PLUGINS>

        writer.writeStartElement(TAG_IMAGES);
        boolean hasCrop = book.getCroppedImages() != null;

        for (BookImage image : book.getImages()) {
            // Only for images that are present (exclude missing images)
            if (image.isMissing()) {
                continue;
            }

            writer.writeStartElement(TAG_IMAGE);
            writer.writeAttribute(ATTR_LABEL, image.getName());
            writer.writeStartElement(TAG_FPX);

            writer.writeEmptyElement(TAG_SRC);
            writer.writeAttribute(ATTR_VALUE, buildFsiShare(collection, book.getId(), image.getId(), hasCrop));

            writer.writeEndElement(); // </FPX>
            writer.writeEndElement(); // </Image>
        }

        writer.writeEndElement(); // </Images>
        writer.writeEndElement(); // </fsi_parameter>
        writer.writeEndDocument();

        return true;
    }

    private String missingImageFsiShare(String collection) {
        return fsi_shares.get(collection) + "/missing_image.tif";
    }

    private String buildFsiShare(String collection, String book, String image, boolean hasCrop) {
        return fsi_shares.get(collection) + "/" + book + "/" + (hasCrop ? "cropped" + "/" : "") + image;
    }

    private XMLStreamWriter getXMLStreamWriter(OutputStream out) throws XMLStreamException {
        return XMLOutputFactory.newFactory().createXMLStreamWriter(out, UTF8);
    }

}
