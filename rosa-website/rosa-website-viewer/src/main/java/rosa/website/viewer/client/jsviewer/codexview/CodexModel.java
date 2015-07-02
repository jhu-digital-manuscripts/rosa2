package rosa.website.viewer.client.jsviewer.codexview;


/**
 * The model contains sequences of opening images, nonopening images, and openings.
 */
public interface CodexModel {
    int numImages();
    CodexImage image(int index);
    
    int numOpenings();
    CodexOpening opening(int index);
    
    int numNonOpeningImages();
    CodexImage nonOpeningImage(int index);
    
    int findOpeningImage(String id);
}
