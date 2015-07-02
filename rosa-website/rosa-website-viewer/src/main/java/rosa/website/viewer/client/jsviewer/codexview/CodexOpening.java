package rosa.website.viewer.client.jsviewer.codexview;

public interface CodexOpening {
    String label();

    CodexImage recto();

    CodexImage verso();
    
    int position();
}
