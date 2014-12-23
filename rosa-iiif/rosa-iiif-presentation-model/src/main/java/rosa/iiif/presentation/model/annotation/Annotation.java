package rosa.iiif.presentation.model.annotation;

import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationBase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Annotation extends PresentationBase implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String motivation;

    protected int width;
    protected int height;

    protected List<AnnotationSource> sources;
    protected List<AnnotationTarget> targets;

    public Annotation() {
        super();

        sources = new ArrayList<>();
        targets = new ArrayList<>();

        width = -1;
        height = -1;

        setType(IIIFNames.OA_ANNOTATION);
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean hasSourceChoice() {
        return sources != null && sources.size() > 1;
    }

    public boolean hasTargetChoice() {
        return targets != null && targets.size() > 1;
    }

    public List<AnnotationSource> getSources() {
        return sources;
    }

    public AnnotationSource getDefaultSource() {
        return sources != null && sources.size() > 0 ? sources.get(0) : null;
    }

    public void setDefaultSource(AnnotationSource source) {
        sources.add(0, source);
    }

    public void addSourceChoice(AnnotationSource source) {
        sources.add(source);
    }

    public void setSources(List<AnnotationSource> sources) {
        this.sources = sources;
    }

    public List<AnnotationTarget> getTargets() {
        return targets;
    }

    public AnnotationTarget getDefaultTarget() {
        return targets != null && targets.size() > 0 ? targets.get(0) : null;
    }

    public void setDefaultTarget(AnnotationTarget target) {
        targets.add(0, target);
    }

    public void addTargetChoice(AnnotationTarget target) {
        targets.add(target);
    }

    public void setTargets(List<AnnotationTarget> targets) {
        this.targets = targets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Annotation that = (Annotation) o;

        if (height != that.height) return false;
        if (width != that.width) return false;
        if (motivation != null ? !motivation.equals(that.motivation) : that.motivation != null) return false;
        if (sources != null ? !sources.equals(that.sources) : that.sources != null) return false;
        if (targets != null ? !targets.equals(that.targets) : that.targets != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (motivation != null ? motivation.hashCode() : 0);
        result = 31 * result + width;
        result = 31 * result + height;
        result = 31 * result + (sources != null ? sources.hashCode() : 0);
        result = 31 * result + (targets != null ? targets.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Annotation{" +
                super.toString() +
                "motivation='" + motivation + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", sources=" + sources +
                ", targets=" + targets +
                '}';
    }
}
