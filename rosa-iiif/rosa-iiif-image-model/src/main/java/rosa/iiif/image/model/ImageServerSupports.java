package rosa.iiif.image.model;

public enum ImageServerSupports {
    BASE_URI_REDIRECT("baseUriRedirect"), CANONICAL_LINK_HEADER("canonicalLinkHeader"), CORS("cors"), JSONLD_MEDIA_TYPEType(
            "jsonldMediaType"), MIRRORING("mirroring"), PROFILE_LINK_HEADER("profileLinkHeader"), REGION_BY_PCT(
            "regionByPct"), REGION_BY_PX("regionByPx"), ROTATION_ARBITRARY("rotationArbitrary"), ROTATION_BY_90S(
            "rotationBy90s"), SIZE_ABOVE_FULL("sizeAboveFull"), SIZE_BY_WH_LISTED("sizeByWhListed"), SIZE_BY_FORCED_WH(
            "sizeByForcedWh"), SIZE_BY_H("sizeByH"), SIZE_BY_PCT("sizeByPct"), SIZE_BY_W("sizeByW"), SIZE_BY_WH(
            "sizeByWh");

    private final String keyword;

    private ImageServerSupports(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }
}
