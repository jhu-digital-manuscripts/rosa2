package rosa.website.rose.client;

import rosa.website.core.client.Analytics;

import java.util.List;

public class RoseAnalytics extends Analytics {

    private enum Action {
        VIEW_NARRATIVE_SECTIONS("sections", "Page", "view", "narrative sections"),
        VIEW_ROSE_HISTORY("rose", "Page", "view", "rose history"),
        VIEW_PROJECT_HISTORY("project", "Page", "view", "project history"),
        VIEW_CORPUS("corpus", "Page", "view", "corpus"),
        VIEW_DONATION("donation", "Page", "view", "donation"),
        VIEW_COLLECTION_DATA("data", "Page", "view", "collection data"),
        VIEW_CHARACTER_NAMES("chars", "Page", "view", "char names"),
        VIEW_ILLUSTRATION_TITLES("illustrations", "Page", "view", "illus titles"),
        VIEW_BOOK_BIB("bib", "Page", "view", "bib"),
        NONE("", "", "", "");

        final String prefix;
        final String category;
        final String actionName;
        final String label;

        Action(String prefix, String category, String actionName, String label) {
            this.prefix = prefix;
            this.category = category;
            this.actionName = actionName;
            this.label = label;
        }

        public static Action getAction(String action) {
            for (Action a : Action.values()) {
                if (a.prefix.equals(action)) {
                    return a;
                }
            }

            return NONE;
        }
    }

    public static final RoseAnalytics INSTANCE = new RoseAnalytics();

    @Override
    protected void trackOther(String action, String bookid, List<String> args) {
        Action a = Action.getAction(action);

        switch (a) {
            default:
                doTrack(a.category, a.actionName, a.label, -1);
                break;
            case NONE:
                break;
        }
    }
}
