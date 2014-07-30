package rosa.archive.model;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Used in NarrativeSection
 */
public class NarrativeScene implements IsSerializable {

    private String id;
    private String description;
    private int criticalEditionStart;
    private int criticalEditionEnd;
    private int rel_line_start;
    private int rel_line_end;

    private NarrativeScene() {  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCriticalEditionStart() {
        return criticalEditionStart;
    }

    public void setCriticalEditionStart(int criticalEditionStart) {
        this.criticalEditionStart = criticalEditionStart;
    }

    public int getCriticalEditionEnd() {
        return criticalEditionEnd;
    }

    public void setCriticalEditionEnd(int criticalEditionEnd) {
        this.criticalEditionEnd = criticalEditionEnd;
    }

    public int getRel_line_start() {
        return rel_line_start;
    }

    public void setRel_line_start(int rel_line_start) {
        this.rel_line_start = rel_line_start;
    }

    public int getRel_line_end() {
        return rel_line_end;
    }

    public void setRel_line_end(int rel_line_end) {
        this.rel_line_end = rel_line_end;
    }

    // TODO equals/hashCode
}
