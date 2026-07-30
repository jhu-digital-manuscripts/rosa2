package rosa.archive.model.aor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An annotation representing a graph on a page, consisting of nodes, links,
 * and associated text elements.
 */
public final class Graph extends AbstractAnnotation implements MultiPart {

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

    /**
     * Creates a graph annotation with the specified fields.
     *
     * @param id          the annotation identifier
     * @param type        the graph type
     * @param orientation the book orientation (degrees)
     * @param location    the physical location on the page
     * @param method      the method used to create the graph
     */
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

    /**
     * Returns the graph type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the graph type.
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the book orientation in degrees.
     *
     * @return the orientation
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * Sets the book orientation.
     *
     * @param orientation the orientation in degrees
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    /**
     * Returns the method used to create this graph.
     *
     * @return the method, or null
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method.
     *
     * @param method the method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the list of graph nodes.
     *
     * @return the nodes (never null)
     */
    public List<GraphNode> getNodes() {
        return nodes;
    }

    /**
     * Sets the list of nodes.
     *
     * @param nodes the nodes
     */
    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * Adds a node to this graph.
     *
     * @param node the node to add
     */
    public void addNode(GraphNode node) {
        nodes.add(node);
    }

    /**
     * Returns the list of links between nodes.
     *
     * @return the links (never null)
     */
    public List<AnnotationLink> getLinks() {
        return links;
    }

    /**
     * Sets the list of links.
     *
     * @param links the links
     */
    public void setLinks(List<AnnotationLink> links) {
        this.links = links;
    }

    /**
     * Adds a link to this graph.
     *
     * @param link the link to add
     */
    public void addLink(AnnotationLink link) {
        links.add(link);
    }

    /**
     * Returns the graph text elements.
     *
     * @return the graph texts (never null)
     */
    public List<GraphText> getGraphTexts() {
        return graphTexts;
    }

    /**
     * Sets the graph text elements.
     *
     * @param graphTexts the graph texts
     */
    public void setGraphTexts(List<GraphText> graphTexts) {
        this.graphTexts = graphTexts;
    }

    /**
     * Adds a graph text element.
     *
     * @param graphText the graph text to add
     */
    public void addGraphText(GraphText graphText) {
        graphTexts.add(graphText);
    }

    /**
     * Returns the internal references.
     *
     * @return the internal references (never null)
     */
    public List<InternalReference> getInternalRefs() {
        return internalRefs;
    }

    /**
     * Sets the internal references.
     *
     * @param internalRefs the internal references
     */
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
        return "Graph{type=" + type + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Graph graph)) return false;
        if (!super.equals(o)) return false;
        return orientation == graph.orientation &&
                Objects.equals(type, graph.type) &&
                Objects.equals(method, graph.method) &&
                Objects.equals(continuesTo, graph.continuesTo) &&
                Objects.equals(continuesFrom, graph.continuesFrom) &&
                Objects.equals(toTranscription, graph.toTranscription) &&
                Objects.equals(fromTranscription, graph.fromTranscription) &&
                Objects.equals(nodes, graph.nodes) &&
                Objects.equals(links, graph.links) &&
                Objects.equals(graphTexts, graph.graphTexts) &&
                Objects.equals(internalRefs, graph.internalRefs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, orientation, method,
                continuesTo, continuesFrom, toTranscription, fromTranscription,
                nodes, links, graphTexts, internalRefs);
    }

    @Override
    public String toString() {
        return "Graph{" +
                "type='" + type + '\'' +
                ", orientation=" + orientation +
                ", method='" + method + '\'' +
                ", nodes=" + nodes.size() +
                ", links=" + links.size() +
                ", graphTexts=" + graphTexts.size() +
                '}';
    }
}
