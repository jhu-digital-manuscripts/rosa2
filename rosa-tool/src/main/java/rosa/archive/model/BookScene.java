package rosa.archive.model;

import java.util.Objects;

/**
 * A narrative scene mapped onto specific pages in a book, defining
 * start and end positions by page, column, and line offset.
 */
public class BookScene {

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

    /**
     * Creates an empty BookScene with default values (-1 for offsets).
     */
    public BookScene() {
        startLineOffset = -1;
        endLineOffset = -1;
        startCriticalEdition = -1;
    }

    /**
     * Returns the scene identifier.
     *
     * @return the scene id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the scene identifier.
     *
     * @param id the scene id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the start page identifier.
     *
     * @return the start page
     */
    public String getStartPage() {
        return startPage;
    }

    /**
     * Sets the start page identifier.
     *
     * @param startPage the start page
     */
    public void setStartPage(String startPage) {
        this.startPage = startPage;
    }

    /**
     * Returns the end page identifier.
     *
     * @return the end page
     */
    public String getEndPage() {
        return endPage;
    }

    /**
     * Sets the end page identifier.
     *
     * @param endPage the end page
     */
    public void setEndPage(String endPage) {
        this.endPage = endPage;
    }

    /**
     * Returns the start page column.
     *
     * @return the start page column
     */
    public String getStartPageCol() {
        return startPageCol;
    }

    /**
     * Sets the start page column.
     *
     * @param startPageCol the start page column
     */
    public void setStartPageCol(String startPageCol) {
        this.startPageCol = startPageCol;
    }

    /**
     * Returns the end page column.
     *
     * @return the end page column
     */
    public String getEndPageCol() {
        return endPageCol;
    }

    /**
     * Sets the end page column.
     *
     * @param endPageCol the end page column
     */
    public void setEndPageCol(String endPageCol) {
        this.endPageCol = endPageCol;
    }

    /**
     * Returns the start line offset within the page.
     *
     * @return the start line offset, or -1 if unknown
     */
    public int getStartLineOffset() {
        return startLineOffset;
    }

    /**
     * Sets the start line offset.
     *
     * @param startLineOffset the start line offset
     */
    public void setStartLineOffset(int startLineOffset) {
        this.startLineOffset = startLineOffset;
    }

    /**
     * Returns the end line offset within the page.
     *
     * @return the end line offset, or -1 if unknown
     */
    public int getEndLineOffset() {
        return endLineOffset;
    }

    /**
     * Sets the end line offset.
     *
     * @param endLineOffset the end line offset
     */
    public void setEndLineOffset(int endLineOffset) {
        this.endLineOffset = endLineOffset;
    }

    /**
     * Returns the start position in the critical edition.
     *
     * @return the critical edition start, or -1 if unknown
     */
    public int getStartCriticalEdition() {
        return startCriticalEdition;
    }

    /**
     * Sets the start position in the critical edition.
     *
     * @param startCriticalEdition the critical edition start
     */
    public void setStartCriticalEdition(int startCriticalEdition) {
        this.startCriticalEdition = startCriticalEdition;
    }

    /**
     * Returns the start transcription reference.
     *
     * @return the start transcription
     */
    public String getStartTranscription() {
        return startTranscription;
    }

    /**
     * Sets the start transcription reference.
     *
     * @param startTranscription the start transcription
     */
    public void setStartTranscription(String startTranscription) {
        this.startTranscription = startTranscription;
    }

    /**
     * Returns whether this scene mapping is marked as correct.
     *
     * @return {@code true} if correct
     */
    public boolean isCorrect() {
        return correct;
    }

    /**
     * Sets whether this scene mapping is correct.
     *
     * @param correct whether correct
     */
    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BookScene that)) return false;
        return startLineOffset == that.startLineOffset &&
                endLineOffset == that.endLineOffset &&
                startCriticalEdition == that.startCriticalEdition &&
                correct == that.correct &&
                Objects.equals(id, that.id) &&
                Objects.equals(startPage, that.startPage) &&
                Objects.equals(endPage, that.endPage) &&
                Objects.equals(startPageCol, that.startPageCol) &&
                Objects.equals(endPageCol, that.endPageCol) &&
                Objects.equals(startTranscription, that.startTranscription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, startPage, endPage, startPageCol, endPageCol,
                startLineOffset, endLineOffset, startCriticalEdition,
                startTranscription, correct);
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
