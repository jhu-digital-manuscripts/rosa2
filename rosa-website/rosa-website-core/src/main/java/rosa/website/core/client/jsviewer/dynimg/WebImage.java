package rosa.website.core.client.jsviewer.dynimg;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.user.client.ui.FocusWidget;

/**
 * A widget for displaying an image rendered by an image server. The image isn't
 * loaded and displayed until makeViewable() is called.
 */
public class WebImage extends FocusWidget implements RenderedImage {
	private final MasterImage master;
	private final int width;
	private final int height;
	private final int[] crop;
	private final String url;
	private final ImageElement img;

	/**
	 * 
	 * @param master
	 * @param width
	 * @param height
	 * @param url
	 * @param crop
	 *            (left, top, right, bottom) gives location in master image
	 */
	public WebImage(MasterImage master, int width, int height, String url,
			int[] crop) {
		this.master = master;
		this.width = width;
		this.height = height;
		this.crop = crop;
		this.url = url;
		this.img = Document.get().createImageElement();

		// TODO set background to something when loading...
		
		setElement(img);
		setWidth(width + "px");
		setHeight(height + "px");
		
		setStylePrimaryName("WebImage");
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public MasterImage master() {
		return master;
	}

	public String url() {
		return url;
	}

	public int masterLeft() {
		return crop.length == 0 ? 0 : crop[0];
	}

	public int masterRight() {
		return crop.length == 0 ? master.width() : crop[2];
	}

	public int masterTop() {
		return crop.length == 0 ? 0 : crop[1];
	}

	public int masterBottom() {
		return crop.length == 0 ? master.height() : crop[3];
	}

	public int masterWidth() {
		return crop.length == 0 ? master.width() : crop[2] - crop[0];
	}

	public int masterHeight() {
		return crop.length == 0 ? master.height() : crop[3] - crop[1];
	}

	public void makeViewable() {
		if (img.getSrc() != null) {
			img.setSrc(url);
			addStyleDependentName("Loading");
		}
	}
}
