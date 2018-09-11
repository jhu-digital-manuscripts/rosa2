package rosa.iiif.presentation.core.html;

import com.google.inject.Inject;
import org.apache.commons.lang3.StringEscapeUtils;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.InternalReference;
import rosa.archive.model.aor.Location;
import rosa.archive.model.aor.Marginalia;
import rosa.archive.model.aor.MarginaliaLanguage;
import rosa.archive.model.aor.Position;
import rosa.archive.model.aor.XRef;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.extras.ISNIResourceDb;
import rosa.iiif.presentation.core.jhsearch.JHSearchField;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class MarginaliaHtmlAdapter extends AnnotationBaseHtmlAdapter<Marginalia> {

    @Inject
    public MarginaliaHtmlAdapter(PresentationUris pres_uris) {
        super(pres_uris);
    }

    @Override
    Class<Marginalia> getAnnotationType() {
        return Marginalia.class;
    }

    @Override
    void annotationAsHtml(BookCollection col, Book book, BookImage page, Marginalia annotation) throws XMLStreamException{
        final String COLLECTION_URI = pres_uris.getCollectionURI(col.getId());

        StringBuilder transcription = new StringBuilder();
        List<String> people = new ArrayList<>();
        List<String> books = new ArrayList<>();
        List<String> locs = new ArrayList<>();
        List<String> symb = new ArrayList<>();

        // Left, top, right, bottom
        boolean[] orientation = new boolean[4];
        List<Location> positions = new ArrayList<>();
        List<XRef> xrefs = new ArrayList<>();
        List<InternalReference> iRefs = new ArrayList<>();

        for (MarginaliaLanguage lang : annotation.getLanguages()) {
            for (Position pos : lang.getPositions()) {
                pos.getTexts().forEach(t -> transcription.append(t).append(' '));
                people.addAll(pos.getPeople());
                books.addAll(pos.getBooks());
                locs.addAll(pos.getLocations());
                symb.addAll(pos.getSymbols());

                xrefs.addAll(pos.getxRefs());
                iRefs.addAll(pos.getInternalRefs());

                // No default case. If orientation is not 0, 90, 180, 270 then do nothing
                switch (pos.getOrientation()) {
                    case 0:
                        orientation[1] = true;
                        break;
                    case 90:
                        orientation[0] = true;
                        break;
                    case 180:
                        orientation[3] = true;
                        break;
                    case 270:
                        orientation[2] = true;
                        break;
                }

                // Add icon for position(s) on page
                positions.add(pos.getPlace());
            }
        }

        writer.writeStartElement("p");

        // ------ Add orientation + location icons ------
        assembleLocationIcon(orientation, positions.toArray(new Location[0]), writer);

        if (isNotEmpty(annotation.getOtherReader())) {
            writer.writeStartElement("p");
            writer.writeAttribute("class", "other-reader " + annotation.getOtherReader());
            writer.writeCharacters("Reader: " + annotation.getOtherReader());
            writer.writeEndElement();
        }

        // Add transcription
        writer.writeStartElement("p");
//        writer.writeCharacters(StringEscapeUtils.escapeHtml4(transcription.toString()));
        writer.writeCharacters(addInternalRefs(
                col,
                StringEscapeUtils.escapeHtml4(transcription.toString()),
                iRefs
        ));
        writer.writeEndElement();

        // Add translation
        addTranslation(annotation.getTranslation(), writer);

        addListOfValues("Symbols:", symb, writer);

        addSearchableList("People:", people, JHSearchField.PEOPLE, COLLECTION_URI, writer, ISNIResourceDb.class);
        addSearchableList("Books:", books, JHSearchField.BOOK, COLLECTION_URI, writer);
        addSearchableList("Locations:", locs, JHSearchField.PLACE, COLLECTION_URI, writer);

        // Add list of X-refs
        addXRefs(xrefs, writer);
        addInternalRefs(col, annotation, iRefs, writer);

        writer.writeEndElement();
    }
}
