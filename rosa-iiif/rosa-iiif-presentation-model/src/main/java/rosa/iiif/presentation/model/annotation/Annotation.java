package rosa.iiif.presentation.model.annotation;

import java.util.ArrayList;
import java.util.List;

import rosa.iiif.presentation.model.IIIFNames;
import rosa.iiif.presentation.model.PresentationBase;

public class Annotation extends PresentationBase {
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + height;
        result = prime * result + ((motivation == null) ? 0 : motivation.hashCode());
        result = prime * result + ((sources == null) ? 0 : sources.hashCode());
        result = prime * result + ((targets == null) ? 0 : targets.hashCode());
        result = prime * result + width;
        return result;
    }

    @Override
    protected boolean canEqual(Object obj) {
        return obj instanceof Annotation;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Annotation))
            return false;
        Annotation other = (Annotation) obj;
        
        if (!other.canEqual(this)) {
            return false;
        }
        
        if (height != other.height)
            return false;
        if (motivation == null) {
            if (other.motivation != null)
                return false;
        } else if (!motivation.equals(other.motivation))
            return false;
        if (sources == null) {
            if (other.sources != null)
                return false;
        } else if (!sources.equals(other.sources))
            return false;
        if (targets == null) {
            if (other.targets != null)
                return false;
        } else if (!targets.equals(other.targets))
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Annotation [motivation=" + motivation + ", width=" + width + ", height=" + height + ", sources="
                + sources + ", targets=" + targets + "]";
    }
}
