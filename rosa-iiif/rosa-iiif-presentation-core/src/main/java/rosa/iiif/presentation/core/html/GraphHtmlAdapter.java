package rosa.iiif.presentation.core.html;

import com.google.inject.Inject;
import rosa.archive.model.Book;
import rosa.archive.model.BookCollection;
import rosa.archive.model.BookImage;
import rosa.archive.model.aor.Graph;
import rosa.archive.model.aor.GraphNode;
import rosa.archive.model.aor.GraphText;
import rosa.archive.model.aor.Location;
import rosa.iiif.presentation.core.PresentationUris;
import rosa.iiif.presentation.core.jhsearch.JHSearchField;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

public class GraphHtmlAdapter extends AnnotationBaseHtmlAdapter<Graph> {

    @Inject
    public GraphHtmlAdapter(PresentationUris pres_uris) {
        super(pres_uris);
    }

    @Override
    Class<Graph> getAnnotationType() {
        return Graph.class;
    }

    @Override
    void annotationAsHtml(BookCollection col, Book book, BookImage page, Graph annotation) throws XMLStreamException{
        List<String> people = new ArrayList<>();
        List<String> books = new ArrayList<>();
        List<String> locs = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        StringBuilder notes = new StringBuilder();      // Notes that target the graph, not an individual node
        StringBuilder notesTr = new StringBuilder();

        for (GraphText gt : annotation.getGraphTexts()) {
            people.addAll(gt.getPeople());
            books.addAll(gt.getBooks());
            locs.addAll(gt.getLocations());
            symbols.addAll(gt.getSymbols());

            gt.getNotes().forEach(note -> {
                notes.append(note.content);
                if (note.internalLink == null || note.internalLink.isEmpty()) {
                    notes.append(note.content).append(", ");
                }
            });
            add(notesTr, gt.getTranslations(), ", ");
        }

        writer.writeStartElement(ANNOTATION_ELEMENT);

        assembleLocationIcon(orientation(annotation.getOrientation()), new Location[] { annotation.getLocation() }, writer);

        writer.writeStartElement(TRANSCRIPTION_ELEMENT);      // Nodes
        addSimpleElement(writer, GRAPH_NODE_ELEMENT, NODES_LABEL, CSS_CLASS, CSS_CLASS_EMPHASIZE);
        for (int i = 0; i < annotation.getNodes().size(); i++) {
            if (i > 0) {
                writer.writeEmptyElement("br");
            }
            GraphNode node = annotation.getNodes().get(i);

            writer.writeCharacters(node.getContent());
            if (isNotEmpty(node.getPerson())) {
                writer.writeCharacters(" (" + node.getPerson() + ")");
            }

            String note = getNoteForGraphNode(node.getId(), annotation);
            if (isNotEmpty(note)) {
                writer.writeCharacters(" Note: " + note);
            }
        }
        writer.writeEndElement();

        addListOfValues(NOTES_LABEL, notes.toString(), writer);
        addListOfValues(NOTES_TRANS_LABEL, notesTr.toString(), writer);
        addSearchableList(PEOPLE_LABEL, people, JHSearchField.PEOPLE, pres_uris.getCollectionURI(col.getId()), writer);
        addListOfValues(BOOKS_LABEL, books, writer);
        addListOfValues(LOCATIONS_LABEL, locs, writer);
        addListOfValues(SYMBOLS_LABEL, symbols, writer);

        addInternalRefs(col, annotation, annotation.getInternalRefs(), writer);

        writer.writeEndElement();
    }

    private String getNoteForGraphNode(String nodeId, Graph graph) {
        if (nodeId == null || nodeId.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        // Each graph text, find any a note with an 'internal_link' matching 'nodeId'.
        // Add each of those to the final result.
        graph.getGraphTexts().stream()
                .map(GraphText::getNotes)
                .forEach(notes ->
                        result.append(notes.stream().filter(note -> nodeId.equals(note.internalLink))
                                .map(moo -> moo.content)
                                .findFirst()
                                .orElse("")).append(' ')
                );
        return result.toString();
    }
}
