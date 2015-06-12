package rosa.website.core.client.widget;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import rosa.website.model.select.BookInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A UI widget that allows the user to pick books that they wish to
 * go into a list. A list of books is provided to the user, who can
 * pick among the list to build a list of books.
 *
 * As books are chosen from the picker list, they are added to the
 * displayed list of books that have been chosen by the user. The
 * book is also removed from the picker list, so that it cannot be
 * picked more than once.
 *
 * A book can be removed from the user selected list by clicking on
 * the book, which will be returned to the picker list. TODO list should be sorted always
 */
public class BookPickerWidget extends Composite {

    private final ListBox bookPicker;
    private final ListBox display;
    private final Button clearButton;

    /**
     * Create a new BookPickerWidget
     */
    public BookPickerWidget() {
        FlowPanel root = new FlowPanel();

        this.bookPicker = new ListBox(false);
        this.display = new ListBox(false);
        this.clearButton = new Button();

        bookPicker.addItem("Restrict by book");

        display.addItem("");
        display.setVisibleItemCount(5);
        display.setVisible(false);

        clearButton.setVisible(false);

        root.add(bookPicker);
        root.add(display);
        root.add(clearButton);

        root.setStylePrimaryName("BookList");

        bind();

        initWidget(root);
    }

    private void bind() {
        bookPicker.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int index = bookPicker.getSelectedIndex();

                display.addItem(bookPicker.getItemText(index), bookPicker.getValue(index));
                bookPicker.removeItem(index);

                if (display.getItemCount() == 2) {
                    display.setVisible(true);
                    clearButton.setVisible(true);
                }
            }
        });

        display.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int index = display.getSelectedIndex();

                bookPicker.addItem(display.getItemText(index), display.getValue(index));
                display.removeItem(index);

                if (display.getItemCount() == 1) {
                    display.setVisible(false);
                    clearButton.setVisible(false);
                }
            }
        });


        clearButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                while (display.getItemCount() > 1) {
                    bookPicker.addItem(display.getItemText(1), display.getValue(1));
                    display.removeItem(1);
                }

                display.setVisible(false);
                clearButton.setVisible(false);
            }
        });
    }

    /**
     * Add book(s) to the list of books that can be picked for restriction.
     *
     * @param books book info
     */
    public void addBooks(BookInfo ... books) {
        if (books == null) {
            return;
        }

        for (BookInfo book : books) {
            bookPicker.addItem(book.title, book.id);
        }
    }

    /**
     * @return an array of the IDs of all books contained in restriction list
     */
    public String[] getRestrictedBookIds() {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < display.getItemCount(); i++) {
            ids.add(display.getValue(i));
        }

        return ids.toArray(new String[ids.size()]);
    }

    public void setClearButtonText(String text) {
        clearButton.setText(text);
    }
}
