package rosa.iiif.presentation.core.html;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.Calculation;
import rosa.archive.model.aor.Location;
import rosa.iiif.presentation.core.PresentationUris;

import javax.xml.stream.XMLStreamException;

public class CalculationHtmlAdapter extends AnnotationBaseHtmlAdapter<Calculation> {

    @Inject
    public CalculationHtmlAdapter(PresentationUris pres_uris) {
        super(pres_uris);
    }

    @Override
    Class<Calculation> getAnnotationType() {
        return Calculation.class;
    }

    @Override
    void annotationAsHtml(BookCollection col, Book book, BookImage page, Calculation calc) throws XMLStreamException {
        writer.writeStartElement("p");

        assembleLocationIcon(orientation(calc.getOrientation()), new Location[] {calc.getLocation()}, writer);

        if (isNotEmpty(calc.getContent())) {
            writer.writeStartElement("ul");
            for (String part : massageContent(calc.getContent())) {
                if (part.trim().isEmpty()) {
                    continue;
                }
                writer.writeStartElement("li");
                writer.writeCharacters(part.trim());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    /**
     * To make the content more readable, split on new lines, so each line can be properly placed on a new line
     * in output HTML.
     *
     * @param content calculation element content
     * @return content split by (in-XML) line
     */
    private String[] massageContent(String content) {
        return content.replaceAll("\\r", "").split("\\n");
    }
}
