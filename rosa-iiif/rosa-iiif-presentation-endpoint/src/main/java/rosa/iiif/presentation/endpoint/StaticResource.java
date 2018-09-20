package rosa.iiif.presentation.endpoint;

import java.io.IOException;
import java.io.InputStream;

interface StaticResource {

    /**
     * Returns the file name of the resource. This must be unique across all static resources. If any, the file
     * extension will be used to determine the content type being set. If the container doesn't recognize the
     * extension, then you can always register it as <code>&lt;mime-type&gt;</code> in <code>web.xml</code>.
     * @return The file name of the resource.
     */
    public String getFileName();

    /**
     * Returns the last modified timestamp of the resource in milliseconds.
     * @return The last modified timestamp of the resource in milliseconds.
     */
    public long getLastModified();

    /**
     * Returns the content length of the resource. This returns <code>-1</code> if the content length is unknown.
     * In that case, the container will automatically switch to chunked encoding if the response is already
     * committed after streaming. The file download progress may be unknown.
     * @return The content length of the resource.
     */
    public long getContentLength();

    /**
     * Returns the input stream with the content of the resource. This method will be called only once by the
     * servlet, and only when the resource actually needs to be streamed, so lazy loading is not necessary.
     * @return The input stream with the content of the resource.
     * @throws IOException When something fails at I/O level.
     */
    public InputStream getInputStream() throws IOException;

}