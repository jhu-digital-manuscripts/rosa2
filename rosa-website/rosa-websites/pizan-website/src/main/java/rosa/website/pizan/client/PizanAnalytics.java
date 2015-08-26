package rosa.website.pizan.client;

import rosa.website.core.client.Analytics;

import java.util.List;

public class PizanAnalytics extends Analytics {

    private enum Action {
        VIEW_PIZAN("pizan", "Page", "view", "christine de pizan"),
        VIEW_WORKS("works", "Page", "view", "works"),
        VIEW_PROPER_NAMES("names", "Page", "view", "proper names"),
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

    public static final PizanAnalytics INSTANCE = new PizanAnalytics();

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
