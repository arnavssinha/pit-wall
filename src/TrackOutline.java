import acm.graphics.GPolygon;

/**
 * Builds a small outline drawing of a track that fits inside a box, for menus and cards.
 */
public final class TrackOutline {

    /**
     * Not instantiated; all methods are static.
     */
    private TrackOutline() {
    }

    /**
     * Builds an unfilled polygon of the centerline, scaled and centered to fit a box, north up.
     *
     * @param track the track
     * @param width box width in pixels
     * @param height box height in pixels
     * @return the polygon, positioned with its box's top-left corner at (0, 0)
     */
    public static GPolygon fit(Track track, double width, double height) {
        double spanX = track.getMaxX() - track.getMinX();
        double spanY = track.getMaxY() - track.getMinY();
        double scale = Math.min(width / spanX, height / spanY);
        double offsetX = (width - spanX * scale) / 2 - track.getMinX() * scale;
        double offsetY = (height - spanY * scale) / 2 + track.getMaxY() * scale;
        GPolygon outline = new GPolygon();
        for (int i = 0; i < track.getSampleCount(); i += 3) {
            TrackSample s = track.getSample(i);
            outline.addVertex(offsetX + s.getX() * scale, offsetY - s.getY() * scale);
        }
        outline.setFilled(false);
        outline.setColor(Palette.MINIMAP_TRACK);
        return outline;
    }
}
