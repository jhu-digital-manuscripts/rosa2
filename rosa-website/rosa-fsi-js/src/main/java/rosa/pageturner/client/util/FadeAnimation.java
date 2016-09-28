package rosa.pageturner.client.util;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.Element;

import java.math.BigDecimal;

public class FadeAnimation extends Animation {
    private Element element;
    private double opacityIncrement;
    private double targetOpacity;
    private double baseOpacity;

    public FadeAnimation(Element element) {
        this.element = element;
    }

    @Override
    protected void onUpdate(double progress) {
        element.getStyle().setOpacity(baseOpacity + progress * opacityIncrement);
    }

    @Override
    protected void onComplete() {
        super.onComplete();
        element.getStyle().setOpacity(targetOpacity);
    }

    public void fadeIn(int duration) {
        fade(duration, 1.0d);
    }

    public void fadeOut(int duration) {
        fade(duration, 0.0d);
    }

    private void fade(int duration, double targetOpacity) {
        this.targetOpacity = targetOpacity;
        String opacityStr = element.getStyle().getOpacity();
        try {
            baseOpacity = new BigDecimal(opacityStr).doubleValue();
            opacityIncrement = targetOpacity - baseOpacity;
            run(duration);
        } catch(NumberFormatException e) {
            // set opacity directly
            onComplete();
        }
    }
}
