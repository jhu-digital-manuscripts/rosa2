package rosa.archive.core.serialize;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import rosa.archive.core.util.XMLUtil;
import rosa.archive.model.BookDescription;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class BookDescriptionSerializer implements Serializer<BookDescription> {

    @Override
    public BookDescription read(InputStream is, List<String> errors) throws IOException {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(is);

            return buildDescription(doc);
        } catch (ParserConfigurationException e) {
            errors.add("Error configuring XML parser.");
            throw new IOException(e);
        } catch (SAXException e) {
            errors.add("Error parsing XML.");
            throw new IOException(e);
        }
    }

    @Override
    public void write(BookDescription object, OutputStream out) throws IOException {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Class<BookDescription> getObjectType() {
        return BookDescription.class;
    }

    private BookDescription buildDescription(Document doc) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XMLUtil.write(doc, out, false);

        BookDescription bookDescription = new BookDescription();
        try {
            bookDescription.setDescription(out.toString("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // If UTF-8 is not supported for whatever reason, fall back to system default
            bookDescription.setDescription(out.toString());
        }
        return bookDescription;
    }

}
