package rosa.website.model.csv;

public interface CSVData <T> extends Iterable<CSVEntry> {
    T[] columns();
    String getId();
    CSVEntry getRow(int index);
    CSVEntry getRow(String id);
    String getValue(int row, int col);
    String getValue(int row, T col);
    int size();
}
