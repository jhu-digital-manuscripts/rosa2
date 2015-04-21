package rosa.website.core.client.jsviewer.codexview;

import com.google.gwt.core.client.GWT;
import rosa.website.core.client.jsviewer.util.HttpGet;
import rosa.website.core.client.jsviewer.util.Util;

// TODO handle missing image
// TODO  handle cropping, could crop dynamically???

public class RoseBook {
    private final RoseImage[] images;
    private int opening_end;

    public static void load(final String fsi_collection, String bookid,
            final HttpGet.Callback<RoseBook> topcb) {
        HttpGet.Callback<String> cb = new HttpGet.Callback<String>() {
            public void failure(String error) {
                topcb.failure(error);
            }

            public void success(String result) {
                topcb.success(new RoseBook(fsi_collection, result));
            }
        };

        String url = GWT.getModuleBaseURL() + "data/" + bookid + "/" + bookid
                + ".images.csv";

        HttpGet.request(url, cb);
    }

    public RoseBook(String fsi_collection, String csv) {
        this(fsi_collection, Util.parseCSVTable(csv));
    }

    public RoseBook(String fsi_collection, String[][] table) {
        this.images = new RoseImage[table.length];

        opening_end = -1;

        for (int i = 0; i < table.length; i++) {
            String[] row = table[i];

            if (row[0].startsWith("*")) {
                images[i] = new RoseImage(row[0]);
            } else {
                images[i] = new RoseImage(fsi_collection, row[0], Integer.parseInt(row[1]),
                        Integer.parseInt(row[2]));
            }

            if (row[0].contains("binding.backcover")) {
                opening_end = i + 1;
            }
        }
    }

    private static String getBookIdFromImage(String image) {
        return image.substring(0, image.indexOf('.'));
    }

    private static String getFsiId(String prefix, String image) {
        if (image.startsWith("*")) {
            image = image.substring(1);
        }

        String bookid = getBookIdFromImage(image);

        // TODO
        
        return prefix + "/" + bookid + "/" + image;
    }

    private static String getLabel(String name) {
        int start = name.indexOf('.') + 1;

        // Also strip leading 0
        while (name.charAt(start) == '0') {
            start++;
        }

        int end = name.lastIndexOf('.');

        return name.substring(start, end).replace(".frontmatter", "")
                .replace(".binding", "").replace(".endmatter", "");
    }

    private static class RoseOpening implements CodexOpening {
        private final String label;
        private final CodexImage recto, verso;
        private final int position;

        public RoseOpening(String label, CodexImage recto, CodexImage verso,
                int position) {
            this.label = label;
            this.recto = recto;
            this.verso = verso;
            this.position = position;
        }

        public String label() {
            return label;
        }

        public CodexImage recto() {
            return recto;
        }

        public CodexImage verso() {
            return verso;
        }

        public int position() {
            return position;
        }
    }

    private class RoseImage implements CodexImage {
        private final String id;
        private final String name;
        private final int width;
        private final int height;
        private boolean missing;

        public RoseImage(String fsi_collection, String name, int width, int height) {
            this.name = name;
            this.id = getFsiId(fsi_collection, name);
            this.width = width;
            this.height = height;
            this.missing = false;
        }

        public RoseImage(String name) {
            this.name = name;
            this.id = "rose/" + "missing_image.tif";
            this.missing = true;
            this.width = -1;
            this.height = -1;
        }

        public String id() {
            return id;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean missing() {
            return missing;
        }

        public String label() {
            return getLabel(name);
        }
    }

    public CodexModel model() {
        return new CodexModel() {
            public CodexOpening opening(int opening) {
                int verso = (opening * 2) - 1;
                int recto = verso + 1;

                if (recto == opening_end) {
                    recto = -1;
                }

                String label;
                if (verso == -1) {
                    label = images[recto].label();
                } else if (recto == -1) {
                    label = images[verso].label();
                } else {
                    label = images[verso].label() + "," + images[recto].label();
                }

                return new RoseOpening(label, images[recto], images[verso],
                        opening);
            }

            public CodexImage image(int image) {
                return images[image];
            }

            public int numImages() {
                return images.length;
            }

            public int numOpenings() {
                return ((opening_end + 1) / 2) + 1;
            }

            public int numNonOpeningImages() {
                return images.length - opening_end;
            }

            public CodexImage nonOpeningImage(int index) {
                return images[opening_end + index];
            }

            public int findOpeningImage(String id) {
                for (int i = 0; i < opening_end; i++) {
                    if (images[i].id.equals(id)) {
                        return i;
                    }
                }
                
                return -1;
            }
        };
    }
}
