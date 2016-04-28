package rosa.website.core.client.view;

public interface HTMLView extends ErrorWidget {
    void setHTML(String html);

    /**
     * Clear contents of view.
     */
    void clear();
}
