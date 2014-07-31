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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NarrativeScene)) return false;

        NarrativeScene that = (NarrativeScene) o;

        if (criticalEditionEnd != that.criticalEditionEnd) return false;
        if (criticalEditionStart != that.criticalEditionStart) return false;
        if (rel_line_end != that.rel_line_end) return false;
        if (rel_line_start != that.rel_line_start) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + criticalEditionStart;
        result = 31 * result + criticalEditionEnd;
        result = 31 * result + rel_line_start;
        result = 31 * result + rel_line_end;
        return result;
    }

    @Override
    public String toString() {
        return "NarrativeScene{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", criticalEditionStart=" + criticalEditionStart +
                ", criticalEditionEnd=" + criticalEditionEnd +
                ", rel_line_start=" + rel_line_start +
                ", rel_line_end=" + rel_line_end +
                '}';
    }
}
