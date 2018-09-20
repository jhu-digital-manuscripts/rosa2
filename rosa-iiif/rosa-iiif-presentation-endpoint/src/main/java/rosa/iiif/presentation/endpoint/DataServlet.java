package rosa.iiif.presentation.endpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Serve out archive files.
 */
@Singleton
public class DataServlet extends AbstractStaticResourceServlet {
    private static final long serialVersionUID = 1L;
    private Path data;

    @Inject
    public DataServlet() {
        data = Util.getArchivePath(); 
    }
    
    @Override
    protected StaticResource getStaticResource(HttpServletRequest request) throws IllegalArgumentException{
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            throw new IllegalArgumentException();
        }

        String name;
        
        try {
            name = URLDecoder.decode(pathInfo.substring(1), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        
        Path resource = data.resolve(name).toAbsolutePath();
        
        // Prevent access to resource outside of the data path
        if (!resource.startsWith(data)) {
            return null;
        }

        if (!Files.exists(resource)) {
            return null;
        }
        
        File file = resource.toFile();
        
        return new StaticResource() {
            @Override
            public long getLastModified() {
                return file.lastModified();
            }
            
            @Override
            public InputStream getInputStream() throws IOException {
                return Files.newInputStream(resource);
            }
            
            @Override
            public String getFileName() {
                return file.getName();
            }
            
            @Override
            public long getContentLength() {
                return file.length();
            }
        };
    }
}
