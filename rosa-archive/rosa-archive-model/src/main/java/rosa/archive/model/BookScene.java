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
    private boolean correct;

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

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookScene)) return false;

        BookScene scene = (BookScene) o;

        if (correct != scene.correct) return false;
        if (endLineOffset != scene.endLineOffset) return false;
        if (startCriticalEdition != scene.startCriticalEdition) return false;
        if (startLineOffset != scene.startLineOffset) return false;
        if (endPage != null ? !endPage.equals(scene.endPage) : scene.endPage != null) return false;
        if (endPageCol != null ? !endPageCol.equals(scene.endPageCol) : scene.endPageCol != null) return false;
        if (id != null ? !id.equals(scene.id) : scene.id != null) return false;
        if (startPage != null ? !startPage.equals(scene.startPage) : scene.startPage != null) return false;
        if (startPageCol != null ? !startPageCol.equals(scene.startPageCol) : scene.startPageCol != null) return false;
        if (startTranscription != null ? !startTranscription.equals(scene.startTranscription) : scene.startTranscription != null)
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
        result = 31 * result + (correct ? 1 : 0);
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
                ", correct=" + correct +
                '}';
    }
}
