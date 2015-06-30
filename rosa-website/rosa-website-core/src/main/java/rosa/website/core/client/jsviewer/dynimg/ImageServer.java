package rosa.website.core.client.jsviewer.dynimg;


// TODO use doubles to help rounding issues?

public abstract class ImageServer {
    /**
     * 
     * @param img
     * @param width
     *            of rendered image
     * @param height
     *            of rendered image
     * @param crop
     *            left, top, right, bottom (0,0 is top left)
     * @return
     */

    public WebImage render(MasterImage image, int width, int height,
            int... crop) {
        return new WebImage(image, width, height, renderToUrl(image, width,
                height, crop), crop);
    }

    public abstract String renderToUrl(MasterImage image, int width,
            int height, int... crop);

    /**
     * Render image into the specified square, preserving the aspect ratio. The
     * actual image may be smaller than the square.
     * 
     * @param image
     * @param width
     * @param height
     * @param crop
     * @return
     */

    public WebImage renderToSquare(MasterImage image, int square_size,
            int... crop) {
        int width, height;

        if (image.width() > image.height()) {
            width = square_size;
            height = (square_size * image.height()) / image.width();

            if (height > square_size) {
                height = square_size;
                width = (height * image.width()) / image.height();
            }
        } else {
            height = square_size;
            width = (square_size * image.width()) / image.height();

            if (width > square_size) {
                width = square_size;
                height = (square_size * image.height()) / image.width();
            }
        }

        return new WebImage(image, width, height, renderToUrl(image, width,
                height, crop), crop);
    }
    
    /**
     * Render image into the specified rectangle, preserving the aspect ratio. The
     * actual image may be smaller than the square.
     * 
     * @param image
     * @param width
     * @param height
     * @param crop
     * @return
     */

    // TODO wrong
    public WebImage renderToRectangle(MasterImage image, int rect_width, int rect_height,
            int... crop) {
        int width, height;

        if (image.width() > image.height()) {
            width = rect_width;
            height = (rect_width * image.height()) / image.width();

            if (height > rect_height) {
                height = rect_height;
                width = (rect_height * image.width()) / image.height();
            }
        } else {
            height = rect_height;
            width = (rect_height * image.width()) / image.height();

            if (width > rect_width) {
                width = rect_width;
                height = (rect_width * image.height()) / image.width();
            }
        }

        return new WebImage(image, width, height, renderToUrl(image, width,
                height, crop), crop);
    }

    /**
     * Render image into the specified width, preserving the aspect ratio.
     * 
     * @param image
     * @param width
     * @param height
     * @param crop
     * @return
     */

    public WebImage renderToWidth(MasterImage image, int render_width,
            int... crop) {
        int height = (render_width * image.height()) / image.width();

        return new WebImage(image, render_width, height, renderToUrl(image,
                render_width, height, crop), crop);
    }

    /**
     * All tiles except perhaps the right most have the same width. All tiles
     * except perhaps the bottom most have the same height.
     * 
     * @param master
     * @param tiled_width
     * @param tiled_height
     * @return
     */
    public WebImage[][] tile(MasterImage master, int tiled_width,
            int tiled_height) {
        int rows = tiled_height / tileSize();

        if ((tiled_height % tileSize()) != 0) {
            rows++;
        }

        int cols = tiled_width / tileSize();

        if ((tiled_width % tileSize()) != 0) {
            cols++;
        }

        WebImage[][] tiles = new WebImage[rows][cols];

        int master_tile_size = (master.width() * tileSize()) / tiled_width;
//        Window.alert("master tile size " + master_tile_size);
//        Window.alert("master dim " + master.width() + "," + master.height());

        for (int row = 0, master_tile_top = 0; row < rows; row++, master_tile_top += master_tile_size) {
            for (int col = 0, master_tile_left = 0; col < cols; col++, master_tile_left += master_tile_size) {
                int tile_width, master_tile_right;

                if (col == cols - 1) {
                    tile_width = tiled_width - (col * tileSize());
                    master_tile_right = master.width();
                } else {
                    tile_width = tileSize();
                    master_tile_right = master_tile_left + master_tile_size;
                }

                int tile_height, master_tile_bottom;

                if (row == rows - 1) {
                    tile_height = tiled_height - (row * tileSize());
                    master_tile_bottom = master.height();
                } else {
                    tile_height = tileSize();
                    master_tile_bottom = master_tile_top + master_tile_size;
                }

//                Window.alert("tile dimensions " + tile_width + "x"
//                    + tile_height + " "  + " master left,top,right,bottom "
//                    + master_tile_left + "," + master_tile_top + ","
//                    + master_tile_right + "," + master_tile_bottom);

                
//                if (master_tile_right - master_tile_left < 0
//                        || master_tile_bottom - master_tile_top < 0) {
//                    Window.alert("error " + master.id()
//                            + " master left,top,right,bottom "
//                            + master_tile_left + "," + master_tile_top + ","
//                            + master_tile_right + "," + master_tile_bottom);
//                }

                WebImage tile = render(master, tile_width, tile_height,
                        master_tile_left, master_tile_top, master_tile_right,
                        master_tile_bottom);

                tiles[row][col] = tile;
            }
        }

        return tiles;
    }

    public abstract int maxRenderSize();

    public abstract int tileSize();
}
