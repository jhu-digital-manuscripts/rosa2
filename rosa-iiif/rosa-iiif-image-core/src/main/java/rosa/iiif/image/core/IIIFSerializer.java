package rosa.iiif.image.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.Quality;
 
// TODO

public class IIIFSerializer {
    public void toJSON(ImageInfo info, OutputStream os) throws JSONException, IOException {
        JSONObject root = new JSONObject();
        
        root.put("identifier", info.getId());
        root.put("width", info.getWidth());
        root.put("height", info.getHeight());
        
        if (info.getTileWidth() > 0 && info.getTileHeight() > 0) {
            root.put("tile_width", info.getTileWidth());
            root.put("tile_height", info.getTileHeight());
        }
        
        int[] scales = info.getScaleFactors();

        if (scales != null && scales.length > 0) {
            JSONArray scale_factors = new JSONArray();
            root.put("scale_factors", scale_factors);

            for (int scale : scales) {
                scale_factors.put(scale);
            }

        }

        ImageFormat[] fmts = info.getFormats();

        if (fmts != null && fmts.length > 0) {
            JSONArray formats = new JSONArray();
            root.put("formats", formats);

            for (ImageFormat fmt: fmts) {
                formats.put(fmt.name().toLowerCase());
            }
        }

        Quality[] quals = info.getQualities();

        if (quals != null && quals.length > 0) {
            JSONArray qualities = new JSONArray();
            root.put("qualities", qualities);

            for (Quality qual: quals) {
                qualities.put(qual.name().toLowerCase());
            }
        }
        
        OutputStreamWriter wos = new OutputStreamWriter(os);
        root.write(wos);
        wos.flush();
    }
}
