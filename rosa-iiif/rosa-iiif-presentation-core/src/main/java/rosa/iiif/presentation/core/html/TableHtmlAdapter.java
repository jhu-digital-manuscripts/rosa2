package rosa.iiif.presentation.core.html;

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
        writer.writeStartElement(ANNOTATION_ELEMENT);
//            addSimpleElement(writer, "span", "Table", "class", "annotation-title");
        if (isNotEmpty(annotation.getType())) {
            writer.writeCharacters(" " + annotation.getType().replaceAll("_", " "));
        }

        writer.writeStartElement(TRANSCRIPTION_ELEMENT);
        addSimpleElement(writer, TABLE_TEXT_ELEMENT, TEXT_LABEL, CSS_CLASS, CSS_CLASS_EMPHASIZE);
        for (TextEl txt : annotation.getTexts()) {
            writer.writeEmptyElement("br");
            writer.writeCharacters(txt.getText());
        }
        writer.writeEndElement();

        addTranslation(annotation.getTranslation(), writer);
        addListOfValues(SYMBOLS_LABEL, annotation.getSymbols(), writer);
        addSearchableList(PEOPLE_LABEL, annotation.getPeople(), JHSearchField.PEOPLE, pres_uris.getCollectionURI(col.getId()), writer);
        addListOfValues(BOOKS_LABEL, annotation.getBooks(), writer);
        addListOfValues(LOCATIONS_LABEL, annotation.getLocations(), writer);
        addInternalRefs(col, book, annotation, annotation.getInternalRefs(), writer);

        writer.writeEndElement();
    }
}
