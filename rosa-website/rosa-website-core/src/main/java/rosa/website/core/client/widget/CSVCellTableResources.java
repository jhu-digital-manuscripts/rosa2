package rosa.website.core.client.widget;

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Style;

public interface CSVCellTableResources extends CellTable.Resources {
    @Override
    @Source("CSVCellTable.css")
    Style cellTableStyle();
}
