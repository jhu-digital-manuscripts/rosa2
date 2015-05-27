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

import java.util.ArrayList;
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
            DefaultSelectionEventManager.createDefaultManager();

    public BookSelectionTreeViewModel(BookSelectList data, SelectCategory category,
                                      SelectionModel<BookInfo> selectionModel) {
        this.data = data;
        this.category = category;
        this.selectionModel = selectionModel;

        // Set list for the data provider to wrap
        this.dataProvider = new ListDataProvider<>(adaptBookSelectListToBookSelections(data));
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {

        if (value == null) {
            return new DefaultNodeInfo<>(dataProvider, new BookSelectionCell());
        } else if (value instanceof BookSelection) {
            return new DefaultNodeInfo<>(
                    new ListDataProvider<>(getListFromBookSelection((BookSelection) value)),
                    new BookInfoCell(),
                    selectionModel,
                    selectionManager,
                    null
            );
        }

        // Unhandled type.
        throw new IllegalArgumentException("Unsupported object type: " + value.getClass().getName());
    }

    @Override
    public boolean isLeaf(Object value) {
        return value instanceof BookInfo;
    }

    /**
     * @param data CSV-like book data
     * @return a list of sub-categories from which books can be selected
     */
    private List<BookSelection> adaptBookSelectListToBookSelections(BookSelectList data) {
        if (data == null) {
            throw new NullPointerException("BookSelectionTreeViewModel cannot wrap a NULL list.");
        }

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

        List<BookSelection> selectionList = new ArrayList<>();
        for (Map.Entry<String, BookSelection> entry : selections.entrySet()) {
            selectionList.add(entry.getValue());
        }
        return selectionList;
    }

    /**
     * @param selection current selection
     * @return a list of books in this selection
     */
    private List<BookInfo> getListFromBookSelection(BookSelection selection) {
        List<BookInfo> infos = new ArrayList<>();

        int count = 0;
        for (BookSelectData bs : data) {
            if (count++ > selection.getCount() || !selection.name.equals(getName(bs))) {
                continue;
            }

            infos.add(new BookInfo(bs.repository() + ", " + bs.shelfmark(), bs.id()));
        }

        return infos;
    }

    /**
     * @param data CSV-like book data
     * @return the correct book 'name' for this category
     */
    private String getName(BookSelectData data) {
        if (data == null) {
            return null;
        }

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
            case TRANSCRIPTION:
                return String.valueOf(data.hasTranscription());
            default:
                return null;
        }
    }
}
