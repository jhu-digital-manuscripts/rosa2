package rosa.website.search.client.widget;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
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
 * the book, which will be returned to the picker list.
 */
public class BookPickerWidget extends Composite {

    private final SortedListBox bookPicker;
    private final SortedListBox restrictedBooks;
    private final Button clearButton;

    /**
     * Create a new BookPickerWidget
     */
    public BookPickerWidget() {
        FlowPanel root = new FlowPanel();

        this.bookPicker = new SortedListBox(false, true);
        this.restrictedBooks = new SortedListBox(true, false);
        this.clearButton = new Button();

        bookPicker.addItem("Restrict By Book");     // TODO i18n

        restrictedBooks.setVisibleItemCount(5);
        restrictedBooks.setVisible(false);

        clearButton.setVisible(false);

        root.add(bookPicker);
        root.add(restrictedBooks);
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

                restrictedBooks.addItemSorted(bookPicker.getItemText(index), bookPicker.getValue(index));
                bookPicker.removeItem(index);

                if (restrictedBooks.getItemCount() > 0) {
                    restrictedBooks.setVisible(true);
                    clearButton.setVisible(true);
                }
            }
        });

        restrictedBooks.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                int index = restrictedBooks.getSelectedIndex();

                bookPicker.addItemSorted(restrictedBooks.getItemText(index), restrictedBooks.getValue(index));
                restrictedBooks.removeItem(index);

                if (restrictedBooks.getItemCount() == 0) {
                    restrictedBooks.setVisible(false);
                    clearButton.setVisible(false);
                }
            }
        });


        clearButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                while (restrictedBooks.getItemCount() > 0) {
                    bookPicker.addItemSorted(restrictedBooks.getItemText(0), restrictedBooks.getValue(0));
                    restrictedBooks.removeItem(0);
                }

                restrictedBooks.setVisible(false);
                clearButton.setVisible(false);
            }
        });
    }

    /**
     * Add book(s) to the list of books that can be picked for restriction.
     *
     * @param books book info
     */
    public void addBooks(BookInfo... books) {
        if (books == null) {
            return;
        }

        for (BookInfo book : books) {
            bookPicker.addItemSorted(book.title, book.id);
        }
    }

    /**
     * Set a list of books as restricted. If any of the books already exists in
     * the selection list, remove them first.
     *
     * @param books .
     */
    public void setBooksAsRestricted(BookInfo... books) {
        if (books == null) {
            return;
        }

        for (BookInfo book : books) {
            removeFromBookPickerList(book);
            restrictedBooks.addItemSorted(book.title, book.id);
        }

        restrictedBooks.setVisible(true);
        clearButton.setVisible(true);
    }

    /**
     * @return an array of the IDs of all books contained in restriction list
     */
    public String[] getRestrictedBookIds() {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < restrictedBooks.getItemCount(); i++) {
            ids.add(restrictedBooks.getValue(i));
        }

        return ids.toArray(new String[ids.size()]);
    }

    public void setClearButtonText(String text) {
        clearButton.setText(text);
    }

    public void clearLists() {
        restrictedBooks.clear();
        bookPicker.clear();
    }

    private void removeFromBookPickerList(BookInfo book) {
        for (int i = 0; i < bookPicker.getItemCount(); i++) {
            if (bookPicker.getItemText(i).equals(book.title)
                    && bookPicker.getValue(i).equals(book.id)) {
                bookPicker.removeItem(i);
                return;
            }
        }
    }
}
