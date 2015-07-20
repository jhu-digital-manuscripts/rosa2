package rosa.website.core.client;

import com.google.gwt.http.client.URL;

public class FSIUtil {
    /**
     * Get an FSI URL for the desired image.
     *
     * @param share name of fsi share
     * @param image image ID
     * @param width desired width in pixels
     * @param height desired height in pixels
     * @param imageServer base URL of image server
     * @return URL for the image on the FSI image server
     */
    public static String getFSIImageUrl(String share, String book, String image,
                                        int width, int height, String imageServer) {
        if (isEmpty(share) || isEmpty(book) || isEmpty(image) || isEmpty(imageServer)) {
            return null;
        }

        return imageServer
                + "?type=image&"
                + "source="
                + URL.encodeQueryString(share + "/" + book + "/" + image)
                + "&width="
                + width
                + "&height="
                + height;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
