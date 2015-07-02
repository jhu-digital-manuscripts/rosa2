package rosa.website.viewer.client.jsviewer.codexview;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Widget;

// TODO refactor into a page turner animation
// new verso must move and slowly expand from right to left as new recto expands in place
// Put images into abosolutepanel which changes to size to do expands

public class ScrollAnimation extends Animation {
    private final Element[] els;
    private final int start_x[];
    private final int start_y[];
    
    private int dx;
    private int dy;
    
    public ScrollAnimation(Element... els) {
        this.els = els;
        this.start_x = new int[els.length];
        this.start_y = new int[els.length];
    }

    public void scroll(int dx, int dy, int milliseconds, Widget... toremove) {
        this.dx = dx;
        this.dy = dy;
        
        for (int i = 0; i < els.length; i++) {
            start_x[i] = els[i].getOffsetLeft();
            start_y[i] = els[i].getOffsetTop();
        }

        run(milliseconds);
    }

    protected void onUpdate(double progress) {
        for (int i = 0; i < els.length; i++) {
            double x = start_x[i] + (progress * dx);
            double y = start_y[i] + (progress * dy);

            Style style = els[i].getStyle();
            style.setLeft(x, Unit.PX);
            style.setTop(y, Unit.PX);
        }
    }

    protected void onComplete() {
        super.onComplete();

        for (int i = 0; i < els.length; i++) {
            double x = start_x[i] + dx;
            double y = start_y[i] + dy;
            
            Style style = els[i].getStyle();
            style.setLeft(x, Unit.PX);
            style.setTop(y, Unit.PX);
        }
    }
}
