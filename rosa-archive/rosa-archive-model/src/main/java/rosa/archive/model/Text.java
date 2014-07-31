package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 */
public class Text implements IsSerializable {

    private int linesPerColumn;
    private int columnsPerPage;
    private int leavesPerGathering;
    private int NumberOfIllustrations;
    private int numberOfPages;
    private String id;

    public Text() {  }

    public int getLinesPerColumn() {
        return linesPerColumn;
    }

    public void setLinesPerColumn(int linesPerColumn) {
        this.linesPerColumn = linesPerColumn;
    }

    public int getColumnsPerPage() {
        return columnsPerPage;
    }

    public void setColumnsPerPage(int columnsPerPage) {
        this.columnsPerPage = columnsPerPage;
    }

    public int getLeavesPerGathering() {
        return leavesPerGathering;
    }

    public void setLeavesPerGathering(int leavesPerGathering) {
        this.leavesPerGathering = leavesPerGathering;
    }

    public int getNumberOfIllustrations() {
        return NumberOfIllustrations;
    }

    public void setNumberOfIllustrations(int numberOfIllustrations) {
        NumberOfIllustrations = numberOfIllustrations;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Text)) return false;

        Text text = (Text) o;

        if (NumberOfIllustrations != text.NumberOfIllustrations) return false;
        if (columnsPerPage != text.columnsPerPage) return false;
        if (leavesPerGathering != text.leavesPerGathering) return false;
        if (linesPerColumn != text.linesPerColumn) return false;
        if (numberOfPages != text.numberOfPages) return false;
        if (id != null ? !id.equals(text.id) : text.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = linesPerColumn;
        result = 31 * result + columnsPerPage;
        result = 31 * result + leavesPerGathering;
        result = 31 * result + NumberOfIllustrations;
        result = 31 * result + numberOfPages;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Text{" +
                "linesPerColumn=" + linesPerColumn +
                ", columnsPerPage=" + columnsPerPage +
                ", leavesPerGathering=" + leavesPerGathering +
                ", NumberOfIllustrations=" + NumberOfIllustrations +
                ", numberOfPages=" + numberOfPages +
                ", id='" + id + '\'' +
                '}';
    }
}
