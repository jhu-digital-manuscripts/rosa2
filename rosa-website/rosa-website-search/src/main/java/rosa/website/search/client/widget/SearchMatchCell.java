package rosa.website.search.client.widget;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import rosa.website.search.client.model.SearchMatchModel;

public class SearchMatchCell extends AbstractCell<SearchMatchModel> {

    @Override
    public void render(Context context, SearchMatchModel value, SafeHtmlBuilder sb) {

        sb.appendHtmlConstant("<div class=\"Result\">");
        if (value.getImageUrl() != null && !value.getImageUrl().isEmpty()) {
            sb.appendHtmlConstant("<img style=\"float:left\" src=\"");
            sb.appendEscaped(value.getImageUrl());
            sb.appendHtmlConstant("\">");
        }

        sb.appendHtmlConstant("<table><tr><td>");

        if (value.getTargetUrl() != null && !value.getTargetUrl().isEmpty()) {
            sb.appendHtmlConstant("<a href=\"");
            sb.appendEscaped(value.getTargetUrl());
            sb.appendHtmlConstant("\">");
            sb.appendEscaped(value.getDisplay());
            sb.appendHtmlConstant("</a>");
        } else {
            sb.appendEscaped(value.getDisplay());
        }

        sb.appendHtmlConstant("</td></tr>");

        for (String searchContext : value.getContext()) {
            sb.appendHtmlConstant("<tr><td>");
            sb.append(SafeHtmlUtils.fromTrustedString(searchContext));
            sb.appendHtmlConstant("</td></tr>");
        }

        sb.appendHtmlConstant("</table></div>");
    }
}
