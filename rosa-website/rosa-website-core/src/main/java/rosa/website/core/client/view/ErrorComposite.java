package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ErrorComposite extends Composite implements ErrorWidget {
    protected Panel errorPanel;

    protected ErrorComposite() {
        errorPanel = new VerticalPanel();
        errorPanel.addStyleName("error");
    }

    @Override
    public void addErrorMessage(String... msg) {
        if (msg == null) {
            return;
        }

        for (String str : msg) {
            errorPanel.add(new Label(str));
        }
    }

    @Override
    public void clearErrors() {
        errorPanel.clear();
    }
}
