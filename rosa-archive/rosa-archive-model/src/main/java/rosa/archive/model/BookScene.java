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

    //TODO equals/hashCode
}
