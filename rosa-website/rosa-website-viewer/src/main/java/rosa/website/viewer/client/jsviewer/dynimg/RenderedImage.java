package rosa.website.viewer.client.jsviewer.dynimg;

/**
 * Image rendered by an image server.
 */
 
public interface RenderedImage {
	public int width();

	public int height();

	public MasterImage master();

	/**
	 * @return URL to image data.
	 */
	public String url();

	public int masterLeft();

	public int masterRight();

	public int masterTop();

	public int masterBottom();

	public int masterWidth();

	public int masterHeight();
}
