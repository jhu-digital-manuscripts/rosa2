package rosa.iiif.image.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.json.JSONException;
import org.json.JSONWriter;

import rosa.iiif.image.model.ComplianceLevel;
import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageInfo;
import rosa.iiif.image.model.ImageServerProfile;
import rosa.iiif.image.model.ImageServerSupports;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.ServiceReference;
import rosa.iiif.image.model.TileInfo;

/**
 * Serialize responses to IIIF image info requests.
 */
public class IIIFResponseSerializer {

    /**
     * Create a new IIIFResponseSerializer
     */
    public IIIFResponseSerializer() {
    }

    private void writeJsonLd(ImageInfo info, JSONWriter out) throws JSONException, IOException {
        out.object();

        out.key("@context").value("http://iiif.io/api/image/2/context.json");
        out.key("@id").value(info.getImageUri());

        out.key("protocol").value("http://iiif.io/api/image");
        out.key("width").value(info.getWidth());
        out.key("height").value(info.getHeight());

        write_sizes_json_ld(info.getSizes(), out);
        write_json_ld(info.getTiles(), out);
        write_json_ld(info.getCompliance(), info.getProfiles(), out);
        write_json_ld(info.getServices(), out);

        out.endObject();
    }

    /**
     *
     * @param info the image info
     * @param os output stream to write the response
     * @throws JSONException
     *          if the JSON writer is not able to write JSON as requested
     * @throws IOException
     *          if the output stream not available
     */
    public void writeJsonLd(ImageInfo info, OutputStream os) throws JSONException, IOException {
        Writer writer = new OutputStreamWriter(os, "UTF-8");
        JSONWriter out = new JSONWriter(writer);
        writeJsonLd(info, out);
        writer.flush();
    }

    private void write_json_ld(ServiceReference[] services, JSONWriter out) {
        if (services == null || services.length == 0) {
            return;
        }

        out.key("services");
        out.array();

        for (ServiceReference ref : services) {
            write_json_ld(ref, out);
        }

        out.endArray();
    }

    private void write_json_ld(ComplianceLevel compliance, ImageServerProfile[] profiles, JSONWriter out) {
        out.key("profile");
        out.array();

        out.value(compliance.getUri());

        if (profiles != null) {
            for (ImageServerProfile profile : profiles) {
                write_json_ld(profile, out);
            }
        }

        out.endArray();
    }

    private void write_json_ld(ServiceReference ref, JSONWriter out) {
        out.object();

        out.key("@context").value(ref.getContext());
        out.key("@id").value(ref.getId());
        out.key("profile").value(ref.getProfile());

        out.endObject();
    }

    private void write_json_ld(ImageServerSupports[] supports, JSONWriter out) {
        out.key("supports");
        out.array();

        if (supports != null) {
            for (ImageServerSupports s : supports) {
                out.value(s.getKeyword());
            }
        }

        out.endArray();
    }

    private void write_json_ld(Quality[] qualities, JSONWriter out) {
        out.key("qualities");
        out.array();

        if (qualities != null) {
            for (Quality q : qualities) {
                out.value(q.getKeyword());
            }
        }

        out.endArray();
    }

    private void write_json_ld(ImageFormat[] formats, JSONWriter out) {
        out.key("formats");
        out.array();

        if (formats != null) {
            for (ImageFormat f : formats) {
                out.value(f.getFileExtension());
            }
        }

        out.endArray();
    }

    private void write_json_ld(TileInfo tile, JSONWriter out) {
        out.object();

        out.key("width").value(tile.getWidth());

        if (tile.getHeight() > 0) {
            out.key("height").value(tile.getHeight());
        }

        out.key("scaleFactors");

        out.array();
        for (int factor : tile.getScaleFactors()) {
            out.value(factor);
        }
        out.endArray();

        out.endObject();
    }

    private void write_json_ld(TileInfo[] tiles, JSONWriter out) {
        if (tiles == null || tiles.length == 0) {
            return;
        }

        out.key("tiles");
        out.array();

        if (tiles != null) {
            for (TileInfo tile : tiles) {
                write_json_ld(tile, out);
            }
        }

        out.endArray();
    }

    private void write_json_ld(ImageServerProfile profile, JSONWriter out) {
        out.object();

        write_json_ld(profile.getFormats(), out);
        write_json_ld(profile.getQualities(), out);
        write_json_ld(profile.getSupports(), out);

        out.endObject();
    }

    private void write_sizes_json_ld(int[] sizes, JSONWriter out) {
        if (sizes == null || sizes.length == 0) {
            return;
        }

        out.key("sizes");
        out.array();

        for (int i = 0; i < sizes.length;) {
            int width = sizes[i++];
            int height = sizes[i++];

            out.object();
            out.key("width").value(width);
            out.key("height").value(height);
            out.endObject();
        }

        out.endArray();
    }
}
