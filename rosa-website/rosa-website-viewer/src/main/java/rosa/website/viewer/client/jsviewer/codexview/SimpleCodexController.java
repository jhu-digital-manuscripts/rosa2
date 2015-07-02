package rosa.website.viewer.client.jsviewer.codexview;

import java.util.ArrayList;
import java.util.List;

public class SimpleCodexController implements CodexController {
    private final CodexModel codex;
    private final List<ChangeHandler> change_handlers;
    private final List<CodexImage> view;
    private int opening_position;

    public SimpleCodexController(CodexModel codex) {
        this.codex = codex;
        this.change_handlers = new ArrayList<ChangeHandler>(1);
        this.view = new ArrayList<CodexImage>(2);
    }

    public void addChangeHandler(ChangeHandler handler) {
        change_handlers.add(handler);
    }

    public void removeChangeHandler(ChangeHandler handler) {
        change_handlers.remove(handler);
    }

    public void gotoOpeningPosition(int opening) {
        opening_position = opening;
        openingChanged();
    }

    public void gotoNextOpening() {
        if (opening_position < codex.numOpenings() - 1) {
            opening_position++;

            openingChanged();
        }
    }

    public void gotoPreviousOpening() {
        if (opening_position > 0) {
            opening_position--;

            openingChanged();
        }
    }

    private void openingChanged() {
        for (ChangeHandler handler : change_handlers) {
            handler.openingChanged(codex.opening(opening_position));
        }
    }

    private void viewChanged() {
        for (ChangeHandler handler : change_handlers) {
            handler.viewChanged(view);
        }
    }

    public List<CodexImage> view() {
        return view;
    }

    public void gotoOpening(int opening) {
        if (opening >= 0 && opening < codex.numOpenings()) {

        }
    }

    public void gotoOpening(CodexOpening opening) {
        opening_position = opening.position();
        openingChanged();
    }

    public CodexOpening getOpening() {
        return codex.opening(opening_position);
    }

    @Override
    public void setView(CodexImage... images) {
        view.clear();

        for (CodexImage img : images) {
            view.add(img);
        }

        viewChanged();
    }
}
