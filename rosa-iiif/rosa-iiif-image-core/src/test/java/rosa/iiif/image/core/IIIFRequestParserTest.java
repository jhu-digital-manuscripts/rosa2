package rosa.iiif.image.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rosa.iiif.image.model.ImageFormat;
import rosa.iiif.image.model.ImageRequest;
import rosa.iiif.image.model.InfoFormat;
import rosa.iiif.image.model.InfoRequest;
import rosa.iiif.image.model.Quality;
import rosa.iiif.image.model.Region;
import rosa.iiif.image.model.RegionType;
import rosa.iiif.image.model.RequestType;
import rosa.iiif.image.model.Rotation;
import rosa.iiif.image.model.Size;
import rosa.iiif.image.model.SizeType;

public class IIIFRequestParserTest {
    private IIIFRequestParser parser = new IIIFRequestParser();

    @Test
    public void testDetermineType() {
        assertEquals(RequestType.INFO, parser.determineRequestType("/abcd1234/info.json"));
        assertEquals(RequestType.IMAGE, parser.determineRequestType("/abcd1234"));
        assertEquals(RequestType.OPERATION, parser.determineRequestType("/abcd1234/full/full/0/native.jpg"));

        // No exceptions on garbage data
        parser.determineRequestType("\\aA'");
        parser.determineRequestType("");
        parser.determineRequestType("/");
        parser.determineRequestType("/iiif");
        parser.determineRequestType("/");
    }

    @Test
    public void testParseInfoRequest() throws Exception {
        InfoRequest info, test;

        info = new InfoRequest();
        info.setImageId("abc/123");
        info.setFormat(InfoFormat.JSON);

        test = parser.parseImageInfoRequest("/abc%2F123/info.json");
        assertEquals(info, test);
    }

    @Test
    public void testParseImageRequest() throws Exception {
        {
            ImageRequest img = new ImageRequest();
            img.setImageId("moo");
            img.setFormat(ImageFormat.PNG);
            img.setQuality(Quality.DEFAULT);
            img.setRegion(new Region(RegionType.FULL));
            img.setSize(new Size(SizeType.FULL));
            img.setRotation(new Rotation(0));

            ImageRequest test = parser.parseImageRequest("/moo/full/full/0/default.png");
            assertEquals(img, test);
        }

        {
            ImageRequest img = new ImageRequest();
            img.setImageId("m o o");
            img.setFormat(ImageFormat.JPG);
            img.setQuality(Quality.COLOR);
            img.setRotation(new Rotation(90));
            img.setRegion(new Region(RegionType.FULL));
            img.setSize(new Size(SizeType.FULL));

            ImageRequest test = parser.parseImageRequest("/m%20o%20o/full/full/90/color.jpg");
            assertEquals(img, test);
        }

        {
            ImageRequest img = new ImageRequest();

            img.setImageId("id1");
            img.setFormat(ImageFormat.PNG);
            img.setQuality(Quality.DEFAULT);
            img.setRotation(new Rotation(90));

            Region reg = new Region(RegionType.ABSOLUTE);
            reg.setX(0);
            reg.setY(10);
            reg.setWidth(100);
            reg.setHeight(200);
            img.setRegion(reg);

            Size size = new Size(SizeType.PERCENTAGE);
            size.setPercentage(50);
            img.setSize(size);

            ImageRequest test = parser.parseImageRequest("/id1/0,10,100,200/pct:50/90/default.png");
            assertEquals(img, test);
        }

        {
            ImageRequest img = new ImageRequest();

            img.setImageId("id1");
            img.setFormat(ImageFormat.PDF);
            img.setQuality(Quality.COLOR);
            img.setRotation(new Rotation(22.5));

            Region reg = new Region(RegionType.PERCENTAGE);
            reg.setPercentageX(10);
            reg.setPercentageY(10);
            reg.setPercentageWidth(80);
            reg.setPercentageHeight(90);
            img.setRegion(reg);

            Size size = new Size(SizeType.EXACT_WIDTH);
            size.setWidth(50);
            img.setSize(size);

            ImageRequest test = parser.parseImageRequest("/id1/pct:10,10,80,90/50,/22.5/color.pdf");
            assertEquals(img, test);
        }
    }

    @Test(expected = IIIFException.class)
    public void testInvalidRequest() throws IIIFException {
        parser.parseImageRequest("/[frob]/full/full/0/default.jpg");
    }

    @Test
    public void testParseScale() throws IIIFException {
        ImageRequest img, test;
        Size scale;

        scale = new Size(SizeType.PERCENTAGE);
        img = new ImageRequest();
        img.setImageId("moo");
        img.setFormat(ImageFormat.TIF);
        img.setQuality(Quality.COLOR);
        img.setRotation(new Rotation(90.0));
        img.setRegion(new Region(RegionType.FULL));

        scale.setPercentage(50.0);
        img.setSize(scale);

        test = parser.parseImageRequest("/moo/full/pct:50.0/90.0/color.tif");
        assertEquals(img, test);

        scale = new Size(SizeType.EXACT_WIDTH);
        scale.setWidth(100);
        img.setSize(scale);

        test = parser.parseImageRequest("/moo/full/100,/90.0/color.tif");
        assertEquals(img, test);

        scale = new Size(SizeType.EXACT_HEIGHT);
        scale.setHeight(100);
        img.setSize(scale);

        test = parser.parseImageRequest("/moo/full/,100/90.0/color.tif");
        assertEquals(img, test);

        scale = new Size(SizeType.EXACT);
        scale.setWidth(100);
        scale.setHeight(200);
        img.setSize(scale);

        test = parser.parseImageRequest("/moo/full/100,200/90.0/color.tif");
        assertEquals(img, test);

        scale = new Size(SizeType.BEST_FIT);
        scale.setWidth(100);
        scale.setHeight(200);
        img.setSize(scale);

        test = parser.parseImageRequest("/moo/full/!100,200/90.0/color.tif");
        assertEquals(img, test);

        scale = new Size(SizeType.PERCENTAGE);
        scale.setPercentage(20);
        img.setSize(scale);

        test = parser.parseImageRequest("/moo/full/pct:20/90.0/color.tif");
        assertEquals(img, test);
    }

    @Test
    public void testParseRegion() throws IIIFException {
        ImageRequest img, test;
        Region region;

        img = new ImageRequest();
        img.setImageId("moo");
        img.setFormat(ImageFormat.JP2);
        img.setQuality(Quality.GRAY);
        img.setRotation(new Rotation(0));
        img.setSize(new Size(SizeType.FULL));

        region = new Region(RegionType.FULL);
        img.setRegion(region);

        test = parser.parseImageRequest("/moo/full/full/0/gray.jp2");
        assertEquals(img, test);

        region = new Region(RegionType.ABSOLUTE);
        region.setX(10);
        region.setY(20);
        region.setWidth(100);
        region.setHeight(200);
        img.setRegion(region);

        test = parser.parseImageRequest("/moo/10,20,100,200/full/0/gray.jp2");
        assertEquals(img, test);

        region = new Region(RegionType.PERCENTAGE);
        region.setPercentageX(10.0);
        region.setPercentageY(20.0);
        region.setPercentageWidth(50.0);
        region.setPercentageHeight(60.0);
        img.setRegion(region);

        test = parser.parseImageRequest("/moo/pct:10.0,20.0,50.0,60.0/full/0/gray.jp2");
        assertEquals(img, test);
    }

    @Test
    public void testParseRotation() throws IIIFException {
        ImageRequest img, test;

        img = new ImageRequest();
        img.setImageId("moo");
        img.setFormat(ImageFormat.JPG);
        img.setQuality(Quality.DEFAULT);
        img.setRotation(new Rotation(32));
        img.setSize(new Size(SizeType.FULL));
        img.setRegion(new Region(RegionType.FULL));

        test = parser.parseImageRequest("/moo/full/full/32/default.jpg");
        assertEquals(img, test);

        img = new ImageRequest();
        img.setImageId("moo:cow.png");
        img.setFormat(ImageFormat.PNG);
        img.setQuality(Quality.DEFAULT);
        img.setRotation(new Rotation(300, true));
        img.setSize(new Size(SizeType.FULL));
        img.setRegion(new Region(RegionType.FULL));

        test = parser.parseImageRequest("/moo:cow.png/full/full/!300/default.png");
        assertEquals(img, test);

    }

    @Test
    public void testImageIdDecoding() throws IIIFException {
        ImageRequest test;

        test = parser.parseImageRequest("/moo%2Fcow/full/full/0/gray.jpg");
        assertEquals("moo/cow", test.getImageId());

        test = parser
                .parseImageRequest("/f23dc590%252D8736%252D11e2%252Da400%252D0050569b3c3f/full/full/0/gray.png");

        assertEquals("f23dc590%2D8736%2D11e2%2Da400%2D0050569b3c3f", test.getImageId());

        test = parser
                .parseImageRequest("/urn:sici:1046-8188(199501)13:1%253C69:FTTHBI%253E2.0.TX;2-4/full/full/0/default.jpg");
        assertEquals("urn:sici:1046-8188(199501)13:1%3C69:FTTHBI%3E2.0.TX;2-4", test.getImageId());

        test = parser.parseImageRequest("/http:%2F%2Fexample.com%2F%3F54%23a/full/full/0/default.jpg");
        assertEquals("http://example.com/?54#a", test.getImageId());

        test = parser.parseImageRequest("/urn:foo:a123,456/full/full/0/default.jpg");
        assertEquals("urn:foo:a123,456", test.getImageId());

        test = parser.parseImageRequest("/ark:%2F12025%2F654xz321/full/full/0/default.jpg");
        assertEquals("ark:/12025/654xz321", test.getImageId());
    }
}
