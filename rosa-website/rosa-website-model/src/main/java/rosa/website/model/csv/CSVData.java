package rosa.website.model.csv;

import java.io.Serializable;
import java.util.List;

public interface CSVData <T extends Enum> extends Iterable<CSVRow>, Serializable {
    T[] columns();
    String getId();
    CSVRow getRow(int index);
    CSVRow getRow(String id);
    String getValue(int row, int col);
    String getValue(int row, T col);
    int size();

    List<CSVRow> asList();
    String stringify();
}
