package rosa.iiif.presentation.core.html;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.Drawing;
import rosa.archive.model.aor.Location;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.jhsearch.JHSearchField;

import javax.xml.stream.XMLStreamException;

public class DrawingHtmlAdapter extends AnnotationBaseHtmlAdapter<Drawing> {

    @Inject
    public DrawingHtmlAdapter(PresentationUris pres_uris) {
        super(pres_uris);
    }

    @Override
    Class<Drawing> getAnnotationType() {
        return Drawing.class;
    }

    @Override
    void annotationAsHtml(BookCollection col, Book book, BookImage page, Drawing annotation) throws XMLStreamException {
        writer.writeStartElement("p");

        int orientation = 0;
        try {
            orientation = Integer.parseInt(annotation.getOrientation());
        } catch (NumberFormatException e) {}

        assembleLocationIcon(orientation(orientation), new Location[] {annotation.getLocation()}, writer);
//            addSimpleElement(writer, "span", "Drawing", "class", "annotation-title");
        writer.writeCharacters(" " + annotation.getType().replaceAll("_", " "));

        addTranslation(annotation.getTranslation(), writer);

        addListOfValues("Symbols:", annotation.getSymbols(), writer);
        addSearchableList("People:", annotation.getPeople(), JHSearchField.PEOPLE, pres_uris.getCollectionURI(col.getId()), writer);
        addListOfValues("Books:", annotation.getBooks(), writer);
        addListOfValues("Locations:", annotation.getLocations(), writer);

        addInternalRefs(col, annotation.getInternalRefs(), writer);

        writer.writeEndElement();
    }
}
