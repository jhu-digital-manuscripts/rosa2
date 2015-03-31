package rosa.website.core.client.widget;

import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import rosa.website.model.select.BookInfo;
import rosa.website.model.select.BookSelectData;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.BookSelection;
import rosa.website.model.select.SelectCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TreeViewModel implementation for the CellBrowser used when selecting which book to read.
 */
public class BookSelectionTreeViewModel implements TreeViewModel {

    /** The actual data backing this model */
    private final BookSelectList data;
    private final SelectCategory category;

    private final ListDataProvider<BookSelection> dataProvider;
    private final SelectionModel<BookInfo> selectionModel;
    private final DefaultSelectionEventManager<BookInfo> selectionManager =
            DefaultSelectionEventManager.createCheckboxManager();

    public BookSelectionTreeViewModel(BookSelectList data, SelectCategory category,
                                      SelectionModel<BookInfo> selectionModel) {
        this.data = data;
        this.category = category;
        this.selectionModel = selectionModel;



        // Set list for the data provider to wrap
        this.dataProvider = new ListDataProvider<>();
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
        if (value == null) {
            return new DefaultNodeInfo<>(dataProvider, new BookSelectionCell());
        } else if (value instanceof BookSelection) {
            BookSelection selection = (BookSelection) value;

            // From the selection, create the list of BookInfo
        } else if (value instanceof BookInfo) {

        }


        return null;
    }

    @Override
    public boolean isLeaf(Object value) {
        return false;
    }

    private List<BookSelection> adaptBookSelectListToBookSelections(BookSelectList data) {
        Map<String, BookSelection> selections = new HashMap<>();
        for (BookSelectData entry : data) {
            String name = getName(entry);

            if (name == null || name.isEmpty()) {
                continue;
            }

            BookSelection sel = selections.get(name);
            if (sel == null) {
                sel = new BookSelection(category, name);
                selections.put(name, sel);
            } else {
                sel.increment();
            }
        }


        return null;
    }

    private String getName(BookSelectData data) {
        switch (category) {
            case ID:
                return data.id();
            case REPOSITORY:
                return data.repository();
            case COMMON_NAME:
                return data.commonName();
            case LOCATION:
                return data.currentLocation();
            case DATE:
                return data.date();
            case ORIGIN:
                return data.origin();
            case TYPE:
                return data.type();
            case NUM_ILLUSTRATIONS:
                return data.numberOfIllustrations();
            case NUM_FOLIOS:
                return data.numberOfFolios();
            default:
                return null;
        }
    }
}
