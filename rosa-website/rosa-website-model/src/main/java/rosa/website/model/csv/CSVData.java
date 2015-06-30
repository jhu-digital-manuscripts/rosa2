package rosa.website.model.csv;

import java.io.Serializable;
import java.util.List;

public interface CSVData <T extends Enum> extends Iterable<CSVEntry>, Serializable {
    T[] columns();
    String getId();
    CSVEntry getRow(int index);
    CSVEntry getRow(String id);
    String getValue(int row, int col);
    String getValue(int row, T col);
    int size();

    List<CSVEntry> asList();
}
