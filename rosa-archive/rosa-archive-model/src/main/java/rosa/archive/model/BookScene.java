package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 *
 */
public class BookScene implements IsSerializable {

    private String id;
    private String startPage;
    private String endPage;
    private String startPageCol;
    private String endPageCol;
    private int startLineOffset;
    private int endLineOffset;
    private int startCriticalEdition;
    private String startTranscription;

    public BookScene() {  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStartPage() {
        return startPage;
    }

    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    public String getEndPage() {
        return endPage;
    }

    public void setEndPage(String endPage) {
        this.endPage = endPage;
    }

    public String getStartPageCol() {
        return startPageCol;
    }

    public void setStartPageCol(String startPageCol) {
        this.startPageCol = startPageCol;
    }

    public String getEndPageCol() {
        return endPageCol;
    }

    public void setEndPageCol(String endPageCol) {
        this.endPageCol = endPageCol;
    }

    public int getStartLineOffset() {
        return startLineOffset;
    }

    public void setStartLineOffset(int startLineOffset) {
        this.startLineOffset = startLineOffset;
    }

    public int getEndLineOffset() {
        return endLineOffset;
    }

    public void setEndLineOffset(int endLineOffset) {
        this.endLineOffset = endLineOffset;
    }

    public int getStartCriticalEdition() {
        return startCriticalEdition;
    }

    public void setStartCriticalEdition(int startCriticalEdition) {
        this.startCriticalEdition = startCriticalEdition;
    }

    public String getStartTranscription() {
        return startTranscription;
    }

    public void setStartTranscription(String startTranscription) {
        this.startTranscription = startTranscription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookScene)) return false;

        BookScene bookScene = (BookScene) o;

        if (endLineOffset != bookScene.endLineOffset) return false;
        if (startCriticalEdition != bookScene.startCriticalEdition) return false;
        if (startLineOffset != bookScene.startLineOffset) return false;
        if (endPage != null ? !endPage.equals(bookScene.endPage) : bookScene.endPage != null) return false;
        if (endPageCol != null ? !endPageCol.equals(bookScene.endPageCol) : bookScene.endPageCol != null) return false;
        if (id != null ? !id.equals(bookScene.id) : bookScene.id != null) return false;
        if (startPage != null ? !startPage.equals(bookScene.startPage) : bookScene.startPage != null) return false;
        if (startPageCol != null ? !startPageCol.equals(bookScene.startPageCol) : bookScene.startPageCol != null)
            return false;
        if (startTranscription != null ? !startTranscription.equals(bookScene.startTranscription) : bookScene.startTranscription != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (startPage != null ? startPage.hashCode() : 0);
        result = 31 * result + (endPage != null ? endPage.hashCode() : 0);
        result = 31 * result + (startPageCol != null ? startPageCol.hashCode() : 0);
        result = 31 * result + (endPageCol != null ? endPageCol.hashCode() : 0);
        result = 31 * result + startLineOffset;
        result = 31 * result + endLineOffset;
        result = 31 * result + startCriticalEdition;
        result = 31 * result + (startTranscription != null ? startTranscription.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BookScene{" +
                "id='" + id + '\'' +
                ", startPage='" + startPage + '\'' +
                ", endPage='" + endPage + '\'' +
                ", startPageCol='" + startPageCol + '\'' +
                ", endPageCol='" + endPageCol + '\'' +
                ", startLineOffset=" + startLineOffset +
                ", endLineOffset=" + endLineOffset +
                ", startCriticalEdition=" + startCriticalEdition +
                ", startTranscription='" + startTranscription + '\'' +
                '}';
    }
}
