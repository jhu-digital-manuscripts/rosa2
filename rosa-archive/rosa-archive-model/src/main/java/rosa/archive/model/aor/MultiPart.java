package rosa.archive.model.aor;

public interface MultiPart {
    String getContinuesTo();
    String getContinuesFrom();
    String getToTranscription();
    String getFromTranscription();

    void setContinuesTo(String continuesTo);
    void setContinuesFrom(String continuesFrom);
    void setToTranscription(String toTranscription);
    void setFromTranscription(String fromTranscription);
}
