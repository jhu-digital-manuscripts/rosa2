package rosa.archive.model.aor;

import java.io.Serializable;

public class TableRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rowNum;
    private String headerLabel;
    private String headerAnchorText;
    private String headerAnchorData;
    private String headerContent;

    public TableRow(int rowNum) {
        this.rowNum = rowNum;
    }

    public TableRow(int rowNum, String headerLabel, String headerAnchorText, String headerAnchorData, String headerContent) {
        this.rowNum = rowNum;
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

        TableRow tableRow = (TableRow) o;

        if (rowNum != tableRow.rowNum) return false;
        if (headerLabel != null ? !headerLabel.equals(tableRow.headerLabel) : tableRow.headerLabel != null)
            return false;
        if (headerAnchorText != null ? !headerAnchorText.equals(tableRow.headerAnchorText) : tableRow.headerAnchorText != null)
            return false;
        if (headerAnchorData != null ? !headerAnchorData.equals(tableRow.headerAnchorData) : tableRow.headerAnchorData != null)
            return false;
        return headerContent != null ? headerContent.equals(tableRow.headerContent) : tableRow.headerContent == null;
    }

    @Override
    public int hashCode() {
        int result = rowNum;
        result = 31 * result + (headerLabel != null ? headerLabel.hashCode() : 0);
        result = 31 * result + (headerAnchorText != null ? headerAnchorText.hashCode() : 0);
        result = 31 * result + (headerAnchorData != null ? headerAnchorData.hashCode() : 0);
        result = 31 * result + (headerContent != null ? headerContent.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TableRow{" +
                "rowNum=" + rowNum +
                ", headerLabel='" + headerLabel + '\'' +
                ", headerAnchorText='" + headerAnchorText + '\'' +
                ", headerAnchorData='" + headerAnchorData + '\'' +
                ", headerContent='" + headerContent + '\'' +
                '}';
    }
}
