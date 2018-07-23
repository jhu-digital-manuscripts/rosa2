package rosa.iiif.presentation.core.util;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.Table;
import rosa.archive.model.aor.TextEl;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.jhsearch.JHSearchField;

import javax.xml.stream.XMLStreamException;

public class TableHtmlAdapter extends AnnotationBaseHtmlAdapter<Table> {

    @Inject
    public TableHtmlAdapter(PresentationUris pres_uris) {
        super(pres_uris);
    }

    @Override
    Class<Table> getAnnotationType() {
        return Table.class;
    }

    @Override
    void annotationAsHtml(BookCollection col, Book book, BookImage page, Table annotation) throws XMLStreamException {
        writer.writeStartElement("p");
//            addSimpleElement(writer, "span", "Table", "class", "annotation-title");
        if (isNotEmpty(annotation.getType())) {
            writer.writeCharacters(" " + annotation.getType().replaceAll("_", " "));
        }

        writer.writeStartElement("p");
        addSimpleElement(writer, "span", "Text:", "class", "emphasize");
        for (TextEl txt : annotation.getTexts()) {
            writer.writeEmptyElement("br");
            writer.writeCharacters(txt.getText());
        }
        writer.writeEndElement();

        addTranslation(annotation.getTranslation(), writer);
        addListOfValues("Symbols:", annotation.getSymbols(), writer);
        addSearchableList("People:", annotation.getPeople(), JHSearchField.PEOPLE, pres_uris.getCollectionURI(col.getId()), writer);
        addListOfValues("Books:", annotation.getBooks(), writer);
        addListOfValues("Locations:", annotation.getLocations(), writer);
        addInternalRefs(col, annotation.getInternalRefs(), writer);

        writer.writeEndElement();
    }
}
