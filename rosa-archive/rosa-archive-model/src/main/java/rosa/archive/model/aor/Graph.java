package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * Element representing a graph annotation.
 *
 * &lt;graph type book_orientation place method id internal_ref graph_continues_to graph_continues_from
 *      graph_to_transcription graph_from_transcription&gt;
 *   &lt;node&gt;
 *   &lt;link&gt;
 *   &lt;graph_text&gt;
 *   &lt;internal_ref&gt;
 * &lt;/graph&gt;
 *
 * Attributes:
 * <ul>
 *   <li>type (required)</li>
 *   <li>book_orientation (required:number)</li>
 *   <li>place (required)</li>
 *   <li>method (required)</li>
 *   <li>id (optional)</li>
 *   <li>internal_ref (optional)</li>
 *   <li>graph_continues_to (optional)</li>
 *   <li>graph_continues_from (optional)</li>
 *   <li>graph_to_transcription (optional)</li>
 *   <li>graph_from_transcription (optional)</li>
 * </ul>
 *
 * Contains elements:
 * <ul>
 *   <li>node (zero or more) : {@link GraphNode}</li>
 *   <li>link (zero or more) : {@link AnnotationLink}</li>
 *   <li>graph_text (zero or more)</li>
 *   <li>internal_ref (zero or more) : {@link InternalReference}</li>
 * </ul>
 */
public class Graph extends Annotation implements Serializable, MultiPart {
    private static final long serialVersionUID = 1L;

    private String type;
    private int orientation;
    private String method;

    private String continuesTo;
    private String continuesFrom;
    private String toTranscription;
    private String fromTranscription;

    private List<GraphNode> nodes;
    private List<AnnotationLink> links;
    private List<GraphText> graphTexts;
    private List<InternalReference> internalRefs;

    public Graph(String id, String type, int orientation, Location location, String method) {
        super(id, null, null, location);
        this.type = type;
        this.orientation = orientation;
        this.method = method;

        this.nodes = new ArrayList<>();
        this.links = new ArrayList<>();
        this.graphTexts = new ArrayList<>();
        this.internalRefs = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(GraphNode node) {
        nodes.add(node);
    }

    public List<AnnotationLink> getLinks() {
        return links;
    }

    public void setLinks(List<AnnotationLink> links) {
        this.links = links;
    }

    public void addLink(AnnotationLink link) {
        links.add(link);
    }

    public List<GraphText> getGraphTexts() {
        return graphTexts;
    }

    public void setGraphTexts(List<GraphText> graphTexts) {
        this.graphTexts = graphTexts;
    }

    public void addGraphText(GraphText graphText) {
        graphTexts.add(graphText);
    }

    public List<InternalReference> getInternalRefs() {
        return internalRefs;
    }

    public void setInternalRefs(List<InternalReference> internalRefs) {
        this.internalRefs = internalRefs;
    }

    @Override
    public String getContinuesTo() {
        return continuesTo;
    }

    @Override
    public String getContinuesFrom() {
        return continuesFrom;
    }

    @Override
    public String getToTranscription() {
        return toTranscription;
    }

    @Override
    public String getFromTranscription() {
        return fromTranscription;
    }

    @Override
    public void setContinuesTo(String continuesTo) {
    this.continuesTo = continuesTo;
    }

    @Override
    public void setContinuesFrom(String continuesFrom) {
        this.continuesFrom = continuesFrom;
    }

    @Override
    public void setToTranscription(String toTranscription) {
        this.toTranscription = toTranscription;
    }

    @Override
    public void setFromTranscription(String fromTranscription) {
        this.fromTranscription = fromTranscription;
    }

    @Override
    public String toPrettyString() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Graph graph = (Graph) o;

        if (orientation != graph.orientation) return false;
        if (type != null ? !type.equals(graph.type) : graph.type != null) return false;
        if (method != null ? !method.equals(graph.method) : graph.method != null) return false;
        if (continuesTo != null ? !continuesTo.equals(graph.continuesTo) : graph.continuesTo != null) return false;
        if (continuesFrom != null ? !continuesFrom.equals(graph.continuesFrom) : graph.continuesFrom != null)
            return false;
        if (toTranscription != null ? !toTranscription.equals(graph.toTranscription) : graph.toTranscription != null)
            return false;
        if (fromTranscription != null ? !fromTranscription.equals(graph.fromTranscription) : graph.fromTranscription != null)
            return false;
        if (nodes != null ? !nodes.equals(graph.nodes) : graph.nodes != null) return false;
        if (links != null ? !links.equals(graph.links) : graph.links != null) return false;
        if (graphTexts != null ? !graphTexts.equals(graph.graphTexts) : graph.graphTexts != null) return false;
        return internalRefs != null ? internalRefs.equals(graph.internalRefs) : graph.internalRefs == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + orientation;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (continuesTo != null ? continuesTo.hashCode() : 0);
        result = 31 * result + (continuesFrom != null ? continuesFrom.hashCode() : 0);
        result = 31 * result + (toTranscription != null ? toTranscription.hashCode() : 0);
        result = 31 * result + (fromTranscription != null ? fromTranscription.hashCode() : 0);
        result = 31 * result + (nodes != null ? nodes.hashCode() : 0);
        result = 31 * result + (links != null ? links.hashCode() : 0);
        result = 31 * result + (graphTexts != null ? graphTexts.hashCode() : 0);
        result = 31 * result + (internalRefs != null ? internalRefs.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Graph{" +
                "type='" + type + '\'' +
                ", orientation=" + orientation +
                ", method='" + method + '\'' +
                ", continuesTo='" + continuesTo + '\'' +
                ", continuesFrom='" + continuesFrom + '\'' +
                ", toTranscription='" + toTranscription + '\'' +
                ", fromTranscription='" + fromTranscription + '\'' +
                ", nodes=" + nodes +
                ", links=" + links +
                ", graphTexts=" + graphTexts +
                ", internalRefs=" + internalRefs +
                '}';
    }
}
