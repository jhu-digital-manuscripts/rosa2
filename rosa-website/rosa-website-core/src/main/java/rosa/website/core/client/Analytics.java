package rosa.website.core.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import java.util.List;
import java.util.logging.Logger;

/**
 * Global Javascript var pageTracker must be set.
 */
public abstract class Analytics {

    public enum SharedAction {
        HOME("home", "Page", "view"),
        SEARCH("search", "Search", "search"),
        BROWSE_BOOK("browse", "Book", "browse-images"),
        SELECT_BOOK("select", "Browse", "select"),
        READ_BOOK("read", "Book", "turn-pages"),
        VIEW_BOOK("book", "Book", "view"),
        VIEW_PARTNERS("partners", "Page", "view"),
        VIEW_CONTACT("contact", "Page", "view"),
        VIEW_TERMS("terms", "Page", "view"),
        NONE("", "", "");

        final String prefix;
        final String category;
        final String actionName;

        SharedAction(String prefix, String category, String actionName) {
            this.prefix = prefix;
            this.category = category;
            this.actionName = actionName;
        }

        public static SharedAction getAction(String action) {
            for (SharedAction sa : SharedAction.values()) {
                if (sa.prefix.equals(action)) {
                    return sa;
                }
            }

            return NONE;
        }
    }

    private static final Logger logger = Logger.getLogger(Analytics.class.toString());

    public native void trackEvent(String category, String action, String label) /*-{
	  if ($wnd.pageTracker) {
	    $wnd.pageTracker._trackEvent(category, action, label);
	  }
	}-*/;

    public native void trackEvent(String category, String action, String label, int value) /*-{
      if ($wnd.pageTracker) {
	    $wnd.pageTracker._trackEvent(category, action, label, value);
	  }
	}-*/;

    public void track(String action, String bookId, List<String> args) {
        SharedAction sharedAction = SharedAction.getAction(action);

        if (sharedAction == null || sharedAction == SharedAction.NONE) {
            trackOther(action, bookId, args);
        } else {
            String label = null;
            int value = -1;

            switch (sharedAction) {
                case BROWSE_BOOK:
                case READ_BOOK:
                case VIEW_BOOK:
                    label = bookId;
                    break;
                case SELECT_BOOK:
                    args.get(0);
                    break;
                case SEARCH:
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < args.size() - 1;) {
                        sb.append(args.get(i++));
                        sb.append(": ");
                        sb.append(args.get(i++));
                    }

                    label = sb.toString();
                    try {
                        value = Integer.parseInt(args.get(args.size() - 1));
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid search token. [" + label + "]");
                    }
                    break;
                default:
                    return;
            }

            doTrack(
                    sharedAction.category,
                    sharedAction.actionName,
                    label != null ? label : sharedAction.prefix,
                    value
            );
        }
    }

    protected void doTrack(final String category, final String actionname, final String label,
                         final int value) {
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                if (value == -1) {
                    trackEvent(category, actionname, label);
                } else {
                    trackEvent(category, actionname, label, value);
                }
            }
        });
    }

    protected abstract void trackOther(String action, String bookid, List<String> args);
}
