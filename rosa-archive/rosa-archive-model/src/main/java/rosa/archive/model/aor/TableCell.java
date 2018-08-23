package rosa.archive.model.aor;

import java.io.Serializable;

public class TableCell implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int row;
    public final int col;
    public final String anchorText;
    public final String anchorData;
    public final String content;

    public TableCell(int row, int col, String anchorText, String anchorData, String content) {
        this.row = row;
        this.col = col;
        this.anchorText = anchorText;
        this.anchorData = anchorData;
        this.content = content;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public String getAnchorText() {
        return anchorText;
    }

    public String getAnchorData() {
        return anchorData;
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableCell tableCell = (TableCell) o;

        if (row != tableCell.row) return false;
        if (col != tableCell.col) return false;
        if (anchorText != null ? !anchorText.equals(tableCell.anchorText) : tableCell.anchorText != null) return false;
        if (anchorData != null ? !anchorData.equals(tableCell.anchorData) : tableCell.anchorData != null) return false;
        return content != null ? content.equals(tableCell.content) : tableCell.content == null;
    }

    @Override
    public int hashCode() {
        int result = row;
        result = 31 * result + col;
        result = 31 * result + (anchorText != null ? anchorText.hashCode() : 0);
        result = 31 * result + (anchorData != null ? anchorData.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TableCell{" +
                "row=" + row +
                ", col=" + col +
                ", anchorText='" + anchorText + '\'' +
                ", anchorData='" + anchorData + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
