package rosa.archive.model.aor;

/**
 * Interface for annotations that can span multiple pages, providing continuation
 * references to preceding and following pages.
 */
public interface MultiPart {

    /**
     * Returns the page that this annotation continues to.
     *
     * @return the continuation target page identifier, or null
     */
    String getContinuesTo();

    /**
     * Returns the page that this annotation continues from.
     *
     * @return the continuation source page identifier, or null
     */
    String getContinuesFrom();

    /**
     * Returns the transcription page this annotation continues to.
     *
     * @return the transcription target identifier, or null
     */
    String getToTranscription();

    /**
     * Returns the transcription page this annotation continues from.
     *
     * @return the transcription source identifier, or null
     */
    String getFromTranscription();

    /**
     * Sets the page that this annotation continues to.
     *
     * @param continuesTo the continuation target page identifier
     */
    void setContinuesTo(String continuesTo);

    /**
     * Sets the page that this annotation continues from.
     *
     * @param continuesFrom the continuation source page identifier
     */
    void setContinuesFrom(String continuesFrom);

    /**
     * Sets the transcription page this annotation continues to.
     *
     * @param toTranscription the transcription target identifier
     */
    void setToTranscription(String toTranscription);

    /**
     * Sets the transcription page this annotation continues from.
     *
     * @param fromTranscription the transcription source identifier
     */
    void setFromTranscription(String fromTranscription);
}
