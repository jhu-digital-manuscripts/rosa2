package rosa.website.search.client.widget;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import rosa.website.search.client.model.SearchMatchModel;

public class SearchMatchCell extends AbstractCell<SearchMatchModel> {

    @Override
    public void render(Context context, SearchMatchModel value, SafeHtmlBuilder sb) {
        if (value == null) {
            return;
        }

        sb.appendHtmlConstant("<div class=\"Result\">");
        if (value.getImageUrl() != null && !value.getImageUrl().isEmpty()) {
            sb.appendHtmlConstant("<img style=\"float:left;margin-right:10px\" src=\"");
            sb.appendEscaped(value.getImageUrl());
            sb.appendHtmlConstant("\">");
        }

        if (value.getTargetUrl() != null && !value.getTargetUrl().isEmpty()) {
            sb.appendHtmlConstant("<a href=\"");
            sb.appendEscaped(value.getTargetUrl());
            sb.appendHtmlConstant("\">");
            sb.appendEscaped(value.getDisplay());
            sb.appendHtmlConstant("</a>");
        } else {
            sb.appendEscaped(value.getDisplay());
        }

        sb.appendHtmlConstant("<table><tr><td>");
        sb.appendHtmlConstant("</td></tr>");

        for (int i = 0, count = 0; i < value.getContext().size() && count < 3; i += 2) {
            String category = value.getContext().get(i);
            if (category == null || category.isEmpty()
                    || category.contains(">collection<")) {
                continue;
            }

            sb.appendHtmlConstant("<tr><td>");

            sb.append(SafeHtmlUtils.fromTrustedString(value.getContext().get(i)));
            if (i + 1 < value.getContext().size()) {
                sb.appendHtmlConstant("</td><td>");
                sb.append(SafeHtmlUtils.fromTrustedString(value.getContext().get(i + 1)));
            }

            sb.appendHtmlConstant("</td></tr>");
            count++;
        }

        sb.appendHtmlConstant("</table></div>");
    }
}
