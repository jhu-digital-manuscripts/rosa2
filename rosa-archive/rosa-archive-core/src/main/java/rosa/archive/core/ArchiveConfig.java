package rosa.archive.core;

/**
 *
 */

public class ArchiveConfig {
    private String[] langs;
    private String loc;
    private String encoding;

    public String[] getLanguages() {
        return langs;
    }

    public void setLangs(String[] langs) {
        this.langs = langs;
    }

    public String getLocation() {
        return loc;
    }

    public void setLocation(String loc) {
        this.loc = loc;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
