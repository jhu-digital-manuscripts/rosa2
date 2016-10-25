package rosa.website.viewer.client.jsviewer.dynimg;

import com.google.gwt.http.client.URL;

public final class FSIImageServer extends ImageServer {
	private final String baseurl;

	public FSIImageServer(String baseurl) {
	    this.baseurl = (baseurl.endsWith("/") ? baseurl : baseurl + "/") + "server";
	}

	public int maxRenderSize() {
		return 1000;
	}

	public int tileSize() {
		return 500;
	}

	public String renderToUrl(MasterImage image, int width, int height,
			int... crop) {
		return baseurl + "?type=image&source="
				+ URL.encodeQueryString(image.id()) + "&width="
				+ width
				+ "&height="
				+ height
				// rect is top left x,y and then width, height, all as dimension
				// percentages
				+ (crop.length == 4 ? "&rect="
						+ ((double) crop[0] / image.width()) + ","
						+ ((double) crop[1] / image.height()) + ","
						+ (((double) crop[2] - crop[0]) / image.width()) + ","
						+ (((double) crop[3] - crop[1]) / image.height()) : "");
	}
}
