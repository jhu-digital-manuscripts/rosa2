package rosa.iiif.image.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageServerProfile;
import rosa.iiif.image.model.ImageServerSupports;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.ServiceReference;
import rosa.iiif.image.model.TileInfo;

/**
 * Serialize responses to IIIF requests.
 */
public class IIIFSerializer {
    private final ComplianceLevel server_compliance_level;
    private final List<ImageServerProfile> server_profiles;
    private final List<TileInfo> server_tiles;

    private JSONArray cached_profiles;
    private JSONArray cached_tiles;

    public IIIFSerializer(ComplianceLevel compliance_level) {
        this.server_compliance_level = compliance_level;
        this.server_profiles = new ArrayList<>();
        this.server_tiles = new ArrayList<>();
    }

    // Cache JSON objects for profiles and tiles

    private void update_cached_json() {
        if (cached_profiles == null) {
            cached_profiles = new JSONArray();

            // First element must be compliance uri
            cached_profiles.put(server_compliance_level.getUri());

            // TODO Profiles should not report capabilities which are part of
            // the compliance level

            for (ImageServerProfile profile : server_profiles) {
                cached_profiles.put(to_json(profile));
            }
        }

        if (cached_tiles == null) {
            cached_tiles = new JSONArray();

            for (TileInfo tile : server_tiles) {
                cached_tiles.put(to_json(tile));
            }
        }
    }

    public void toJsonLd(ImageInfo info, OutputStream os) throws JSONException, IOException {
        update_cached_json();

        JSONObject root = new JSONObject();

        root.put("@context", "http://iiif.io/api/image/2/context.json");
        root.put("@id", info.getImageId());
        root.put("protocol", "http://iiif.io/api/image");
        root.put("width", info.getWidth());
        root.put("height", info.getHeight());

        JSONArray sizes = to_json(info.getSizes());

        if (sizes.length() > 0) {
            root.put("sizes", sizes);
        }

        if (cached_tiles.length() > 0) {
            root.put("tiles", cached_tiles);
        }

        root.put("profile", cached_profiles);

        JSONArray services = to_json(info.getServices());

        if (services.length() > 0) {
            root.put("service", services);
        }

        OutputStreamWriter wos = new OutputStreamWriter(os);
        root.write(wos);
        wos.flush();
    }

    private JSONArray to_json(ServiceReference[] services) {
        JSONArray result = new JSONArray();

        if (services != null) {
            for (ServiceReference ref : services) {
                result.put(to_json(ref));
            }
        }

        return result;
    }

    private JSONObject to_json(ServiceReference ref) {
        JSONObject result = new JSONObject();

        result.put("@context", ref.getContext());
        result.put("@id", ref.getId());
        result.put("profile", ref.getProfile());

        return result;
    }

    private JSONArray to_array(ImageServerSupports[] supports) {
        JSONArray result = new JSONArray();

        if (supports != null) {
            for (ImageServerSupports s : supports) {
                result.put(s.getKeyword());
            }
        }

        return result;
    }

    private JSONArray to_array(Quality[] qualities) {
        JSONArray result = new JSONArray();

        if (qualities != null) {
            for (Quality q : qualities) {
                result.put(q.getKeyword());
            }
        }

        return result;
    }

    private JSONArray to_json(ImageFormat[] formats) {
        JSONArray result = new JSONArray();

        if (formats != null) {
            for (ImageFormat f : formats) {
                result.put(f.getFileExtension());
            }
        }

        return result;
    }

    private JSONObject to_json(TileInfo tile) {
        JSONObject result = new JSONObject();

        result.put("width", tile.getWidth());

        if (tile.getHeight() > 0) {
            result.put("height", tile.getHeight());
        }

        result.put("scaleFactors", tile.getScaleFactors());

        return result;
    }

    private JSONObject to_json(ImageServerProfile profile) {
        JSONObject result = new JSONObject();

        result.put("formats", to_json(profile.getFormats()));
        result.put("qualities", to_array(profile.getQualities()));
        result.put("supports", to_array(profile.getSupports()));

        return result;
    }

    private JSONArray to_json(int[] sizes) {
        JSONArray result = new JSONArray();

        if (sizes == null) {
            return result;
        }

        for (int i = 0; i < sizes.length;) {
            int width = sizes[i++];
            int height = sizes[i++];

            JSONObject o = new JSONObject();

            o.put("width", width);
            o.put("height", height);

            result.put(o);
        }

        return result;
    }

}
