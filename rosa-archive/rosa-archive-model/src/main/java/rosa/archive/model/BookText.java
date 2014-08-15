package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 */
public class BookText implements IsSerializable {

    private int linesPerColumn;
    private int columnsPerPage;
    private int leavesPerGathering;
    private int NumberOfIllustrations;
    private int numberOfPages;
    private String id;
    private String title;
    private String firstPage;
    private String lastPage;

    public BookText() {  }

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstPage() {
        return firstPage;
    }

    public void setFirstPage(String firstPage) {
        this.firstPage = firstPage;
    }

    public String getLastPage() {
        return lastPage;
    }

    public void setLastPage(String lastPage) {
        this.lastPage = lastPage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookText)) return false;

        BookText bookText = (BookText) o;

        if (NumberOfIllustrations != bookText.NumberOfIllustrations) return false;
        if (columnsPerPage != bookText.columnsPerPage) return false;
        if (leavesPerGathering != bookText.leavesPerGathering) return false;
        if (linesPerColumn != bookText.linesPerColumn) return false;
        if (numberOfPages != bookText.numberOfPages) return false;
        if (firstPage != null ? !firstPage.equals(bookText.firstPage) : bookText.firstPage != null) return false;
        if (id != null ? !id.equals(bookText.id) : bookText.id != null) return false;
        if (lastPage != null ? !lastPage.equals(bookText.lastPage) : bookText.lastPage != null) return false;
        if (title != null ? !title.equals(bookText.title) : bookText.title != null) return false;

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
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (firstPage != null ? firstPage.hashCode() : 0);
        result = 31 * result + (lastPage != null ? lastPage.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookText{" +
                "linesPerColumn=" + linesPerColumn +
                ", columnsPerPage=" + columnsPerPage +
                ", leavesPerGathering=" + leavesPerGathering +
                ", NumberOfIllustrations=" + NumberOfIllustrations +
                ", numberOfPages=" + numberOfPages +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", firstPage='" + firstPage + '\'' +
                ", lastPage='" + lastPage + '\'' +
                '}';
    }
}
