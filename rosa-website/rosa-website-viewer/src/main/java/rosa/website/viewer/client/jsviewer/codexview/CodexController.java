package rosa.website.viewer.client.jsviewer.codexview;

import java.util.List;

public interface CodexController {
    interface ChangeHandler {
        void openingChanged(CodexOpening opening);
        void viewChanged(List<CodexImage> view);
    }

    void addChangeHandler(ChangeHandler handler);

    void removeChangeHandler(ChangeHandler handler);
    
    List<CodexImage> view();

    CodexOpening getOpening();

    void gotoOpening(CodexOpening opening);

    /**
     * Go to the next opening if possible.
     */
    void gotoNextOpening();

    /**
     * Go to the previous opening if possible.
     */
    void gotoPreviousOpening();

    void setView(CodexImage... img);
}
