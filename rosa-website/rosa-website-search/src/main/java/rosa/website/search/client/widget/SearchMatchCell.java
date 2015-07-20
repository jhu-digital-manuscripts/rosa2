package rosa.website.search.client.widget;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import rosa.website.search.client.model.SearchMatchModel;

public class SearchMatchCell extends AbstractCell<SearchMatchModel> {

    @Override
    public void render(Context context, SearchMatchModel value, SafeHtmlBuilder sb) {
        String url = GWT.getHostPageBaseURL() + "#read;" + value.getId();

        sb.appendHtmlConstant("<div class=\"Result\">");
        if (value.getImageUrl() != null && !value.getImageUrl().isEmpty()) {
            sb.appendHtmlConstant("<img style=\"float:left\" src=\"");
            sb.appendEscaped(value.getImageUrl());
            sb.appendHtmlConstant("\">");
        }

        sb.appendHtmlConstant("<table>");

        sb.appendHtmlConstant("<tr><td><a href=\"");
        sb.appendEscaped(url);
        sb.appendHtmlConstant("\">");
        sb.appendEscaped(value.getId());
        sb.appendHtmlConstant("</a></td></tr>");

        for (String searchContext : value.getContext()) {
            sb.appendHtmlConstant("<tr><td>");
            sb.append(SafeHtmlUtils.fromTrustedString(searchContext));
            sb.appendHtmlConstant("</td></tr>");
        }

        sb.appendHtmlConstant("</table></div>");
    }
}
