package rosa.website.search.client.widget;

import com.google.gwt.user.client.ui.ListBox;

public class SortedListBox extends ListBox {

    private final boolean hasLabel;

    public SortedListBox(boolean isMultipleSelect, boolean hasLabel) {
        super();
        this.hasLabel = hasLabel;
        
        setMultipleSelect(isMultipleSelect);
    }

    public void addItemSorted(String item, String value) {
        int insert_index = hasLabel ? 1 : 0;
        for (int i = hasLabel ? 1 : 0; i < getItemCount(); i++, insert_index++) {
            if (getItemText(i).compareToIgnoreCase(item) > 0) {
                break;
            }
        }

        insertItem(item, value, insert_index);
    }

}
