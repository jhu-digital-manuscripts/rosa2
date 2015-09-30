package rosa.website.core.client.widget;

import com.google.gwt.user.cellview.client.CellBrowser;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

public class RosaCellBrowser extends CellBrowser {

    public <T> RosaCellBrowser(Builder<T> builder) {
        super(builder);
    }

    /**
     * Hide the splitter/divider of the last widget that is currently
     * being displayed.
     */
    public void hideLastDivider() {
        if (getWidget() instanceof SplitLayoutPanel) {
            SplitLayoutPanel splitPanel = (SplitLayoutPanel) getWidget();
            splitPanel.setWidgetHidden(
                    splitPanel.getWidget(splitPanel.getWidgetCount() - 1),
                    true
            );
            splitPanel.forceLayout();
        }
    }

    /**
     * Set the width of the first column of this CellBrowser.
     *
     * @param width in pixels
     *              Width is 'double' to support possible resize animations.
     */
    public void setFirstColumnWidth(double width) {
        if (getWidget() instanceof SplitLayoutPanel) {
            SplitLayoutPanel splitPanel = (SplitLayoutPanel) getWidget();
            if (splitPanel.getWidgetCount() > 0) {
                splitPanel.setWidgetSize(splitPanel.getWidget(0), width);
            }
            splitPanel.forceLayout();
        }
    }

}
