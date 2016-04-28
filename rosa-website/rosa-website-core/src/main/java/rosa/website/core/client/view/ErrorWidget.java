package rosa.website.core.client.view;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * Widget with an ability to display errors.
 */
public interface ErrorWidget extends IsWidget {
    /**
     * Add one or more error messages to display to the user.
     *
     * @param msg string message(s)
     */
    void addErrorMessage(String... msg);

    /**
     * Clear all error messages.
     */
    void clearErrors();
}
