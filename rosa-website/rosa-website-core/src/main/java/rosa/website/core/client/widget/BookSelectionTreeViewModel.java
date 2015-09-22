package rosa.website.core.client.widget;

import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import rosa.website.core.client.Labels;
import rosa.website.model.select.BookInfo;
import rosa.website.model.select.BookSelectData;
import rosa.website.model.select.BookSelectList;
import rosa.website.model.select.BookSelection;
import rosa.website.model.select.DataStatus;
import rosa.website.model.select.SelectCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    /**
     * @param data .
     * @param category .
     * @param selectionModel .
     */
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
        Collections.sort(selectionList, new Comparator<BookSelection>() {
            @Override
            public int compare(BookSelection o1, BookSelection o2) {
                if (o1.category != o2.category) {
                    return o1.category.compareTo(o2.category);
                } else if (isNumeric(o1.name) && isNumeric(o2.name)) {
                    return compareAsNumbers(o1.name, o2.name);
                } else {
                    return o1.name.compareTo(o2.name);
                }
            }
        });

        return selectionList;
    }

    private native int compareAsNumbers(String s1, String s2) /*-{
        return s1 - s2;
    }-*/;

    private native boolean isNumeric(String str) /*-{
        return !isNaN(str);
    }-*/;

    /**
     * Based on selection criteria in the BookSelection input, look through
     * the data list to find all matching books.
     *
     * @param selection current selection
     * @return a list of books in this selection
     */
    private List<BookInfo> getListFromBookSelection(BookSelection selection) {
        List<BookInfo> infos = new ArrayList<>();

        int count = 0;
        for (BookSelectData bs : data) {
            if (selection.name.equals(getName(bs))) {
                infos.add(new BookInfo(bs.repository() + ", " + bs.shelfmark(), bs.id()));
                count++;
            }

            if (count >= selection.getCount()) {
                break;
            }
        }
        Collections.sort(infos);

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
                return getTranscriptionStatus(data.transcriptionStatus());
            default:
                return null;
        }
    }

    private String getTranscriptionStatus(DataStatus status) {
        if (status == null) {
            return Labels.INSTANCE.none();
        }
        String display;
        switch (status) {
            case NONE:
            default:
                display = Labels.INSTANCE.none();
                break;
            case PARTIAL:
                display = Labels.INSTANCE.partial();
                break;
            case FULL:
                display = Labels.INSTANCE.complete();
        }

        return display;
    }
}
