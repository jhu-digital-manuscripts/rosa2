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
        
        if (value.getTargetUrl() != null && !value.getTargetUrl().isEmpty()) {
            sb.appendHtmlConstant("<a href=\"");
            sb.appendEscaped(value.getTargetUrl());
            sb.appendHtmlConstant("\">");
            sb.appendEscaped(value.getDisplay());
            sb.appendHtmlConstant("</a>");
        } else {
            sb.appendEscaped(value.getDisplay());
        }

        // Display 1x2 table with image and context
        sb.appendHtmlConstant("<table><tr><td>");
        
        if (value.getImageUrl() != null && !value.getImageUrl().isEmpty()) {
            sb.appendHtmlConstant("<a href=\"");
            sb.appendEscaped(value.getTargetUrl());
            sb.appendHtmlConstant("\">");
            sb.appendHtmlConstant("<img style=\"float:left;margin-right:10px\" src=\"");
            sb.appendEscaped(value.getImageUrl());
            sb.appendHtmlConstant("\">");
            sb.appendHtmlConstant("</a>");
        }

        sb.appendHtmlConstant("</td><td>");
        
        for (int i = 0; i < value.getContext().size();) {
            String category = value.getContext().get(i++);
            String html = value.getContext().get(i++);
            
            sb.appendHtmlConstant("<b>");
            sb.append(SafeHtmlUtils.fromString(category + ": "));
            sb.appendHtmlConstant("</b>");
            sb.append(SafeHtmlUtils.fromTrustedString(html + " "));
        }

        sb.appendHtmlConstant("</td></tr></table></div>");
    }
}
