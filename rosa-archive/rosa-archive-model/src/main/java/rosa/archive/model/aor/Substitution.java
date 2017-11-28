package rosa.archive.model.aor;

import java.io.Serializable;

public class Substitution extends Annotation implements Serializable {
    private static long serialVersionUID = 1L;

    private String type;
    private String method;
    private String amendedText;

    /** Page and scene signature */
    private String signature;

    public Substitution() {}

    public Substitution(String id, String signature, String type, String method, String copyText, String amendedText, String imageId) {
        this(id, signature, type, method, copyText, amendedText, null, null, imageId);
    }

    public Substitution(String id, String signature, String type, String method, String copyText, String amendedText,
                        String language, Location location, String imageId) {
        super(id, copyText, language, imageId, location);
        this.signature = signature;
        this.type = type;
        this.method = method;
        this.amendedText = amendedText;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getCopyText() {
        return getReferencedText();
    }

    public void setCopyText(String copyText) {
        setReferencedText(copyText);
    }

    public String getAmendedText() {
        return amendedText;
    }

    public void setAmendedText(String amendedText) {
        this.amendedText = amendedText;
    }

    @Override
    public String toPrettyString() {
        // TODO perhaps different forms depending on 'type'
        return (signature != null ? signature + ": ": "") +
                "<br><span class=\"sub-label\">Before:</span> " +
                getCopyText() +
                "<br><span class=\"sub-label\">After: " +
                amendedText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Substitution that = (Substitution) o;

        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (amendedText != null ? !amendedText.equals(that.amendedText) : that.amendedText != null) return false;
        return signature != null ? signature.equals(that.signature) : that.signature == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (amendedText != null ? amendedText.hashCode() : 0);
        result = 31 * result + (signature != null ? signature.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Substitution{" +
                "type='" + type + '\'' +
                ", method='" + method + '\'' +
                ", amendedText='" + amendedText + '\'' +
                ", signature='" + signature + '\'' +
                ", " + super.toString() +
                '}';
    }
}
