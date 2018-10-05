package rosa.archive.model.aor;

import java.io.Serializable;
import java.util.Objects;

public class TableHeader implements Serializable {
    private static final long serialVersionUID = 1L;

    private String headerLabel;
    private String headerAnchorText;
    private String headerAnchorData;
    private String headerContent;

    public TableHeader() {}

    public TableHeader(String headerLabel, String headerAnchorText, String headerAnchorData, String headerContent) {
        this.headerLabel = headerLabel;
        this.headerAnchorText = headerAnchorText;
        this.headerAnchorData = headerAnchorData;
        this.headerContent = headerContent;
    }

    public String getHeaderLabel() {
        return headerLabel;
    }

    public void setHeaderLabel(String headerLabel) {
        this.headerLabel = headerLabel;
    }

    public String getHeaderAnchorText() {
        return headerAnchorText;
    }

    public void setHeaderAnchorText(String headerAnchorText) {
        this.headerAnchorText = headerAnchorText;
    }

    public String getHeaderAnchorData() {
        return headerAnchorData;
    }

    public void setHeaderAnchorData(String headerAnchorData) {
        this.headerAnchorData = headerAnchorData;
    }

    public String getHeaderContent() {
        return headerContent;
    }

    public void setHeaderContent(String headerContent) {
        this.headerContent = headerContent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableHeader that = (TableHeader) o;
        return Objects.equals(headerLabel, that.headerLabel) &&
                Objects.equals(headerAnchorText, that.headerAnchorText) &&
                Objects.equals(headerAnchorData, that.headerAnchorData) &&
                Objects.equals(headerContent, that.headerContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headerLabel, headerAnchorText, headerAnchorData, headerContent);
    }

    @Override
    public String toString() {
        return "TableHeader{" +
                "headerLabel='" + headerLabel + '\'' +
                ", headerAnchorText='" + headerAnchorText + '\'' +
                ", headerAnchorData='" + headerAnchorData + '\'' +
                ", headerContent='" + headerContent + '\'' +
                '}';
    }
}
