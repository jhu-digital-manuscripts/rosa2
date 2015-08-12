package rosa.website.core.client.widget;

import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

public class ViewerControlsWidget extends Composite {

    // Controls: (page controls) first, last, next, previous, *goto.
    // *Show: transcriptions, transcriptions (lecoy), illustrations descriptions
    private ListBox showExtraListBox;
    private TextBox goToTextBox;

    public ViewerControlsWidget() {
        FlowPanel root = new FlowPanel();

        showExtraListBox = new ListBox(false);
        goToTextBox = new TextBox();

        showExtraListBox.setVisibleItemCount(1);
        goToTextBox.setStylePrimaryName("GoTextBox");

        root.add(goToTextBox);
        root.add(showExtraListBox);

        initWidget(root);
    }

    public void clear() {
        showExtraListBox.clear();
        goToTextBox.setText("");
    }

    public String getSelected() {
        return showExtraListBox.getValue(showExtraListBox.getSelectedIndex());
    }

    public void setShowExtraVisible(boolean visible) {
        showExtraListBox.setVisible(visible);
    }

    public void setGoToVisible(boolean visible) {
        goToTextBox.setVisible(visible);
    }

    public HandlerRegistration addGotoKeyDownHandler(KeyDownHandler handler) {
        return goToTextBox.addKeyDownHandler(handler);
    }

    public void setGotoLabel(String label) {
        goToTextBox.setText(label);
    }

    public String getGotoText() {
        return goToTextBox.getText();
    }

    public void setShowExtraLabels(String... data) {
        showExtraListBox.clear();

        if (data == null) {
            return;
        }
        for (String str : data) {
            showExtraListBox.addItem(str);
        }
    }

    public void setSelectedShowExtra(String selected) {
        for (int i = 0; i < showExtraListBox.getItemCount(); i++) {
            if (showExtraListBox.getItemText(i).equals(selected)
                    || showExtraListBox.getValue(i).equals(selected)) {
                showExtraListBox.setSelectedIndex(i);
                break;
            }
        }
    }

    public HandlerRegistration addShowExtraChangeHandler(ChangeHandler handler) {
        return showExtraListBox.addChangeHandler(handler);
    }

}
