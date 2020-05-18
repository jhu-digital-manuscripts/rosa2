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
        writer.writeStartElement(ANNOTATION_ELEMENT);

        int orientation = 0;
        try {
            orientation = Integer.parseInt(annotation.getOrientation());
        } catch (NumberFormatException e) {}

        assembleLocationIcon(orientation(orientation), new Location[] {annotation.getLocation()}, writer);
//            addSimpleElement(writer, "span", "Drawing", "class", "annotation-title");
        writer.writeCharacters(" " + annotation.getType().replaceAll("_", " "));

        addTranslation(annotation.getTranslation(), writer);

        addListOfValues(SYMBOLS_LABEL, annotation.getSymbols(), writer);
        addSearchableList(PEOPLE_LABEL, annotation.getPeople(), JHSearchField.PEOPLE, pres_uris.getCollectionURI(col.getId()), writer);
        addListOfValues(BOOKS_LABEL, annotation.getBooks(), writer);
        addListOfValues(LOCATIONS_LABEL, annotation.getLocations(), writer);

        addInternalRefs(col, book, annotation, annotation.getInternalRefs(), writer);

        writer.writeEndElement();
    }
}
