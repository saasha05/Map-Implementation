package huskymaps.server.logic;

import huskymaps.params.RasterRequest;
import huskymaps.params.RasterResult;

import java.util.Objects;

import static huskymaps.utils.Constants.*;


/** Application logic for the RasterAPIHandler. */
public class Rasterer {

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param request RasterRequest
     * @return RasterResult
     */
    public static RasterResult rasterizeMap(RasterRequest request) {
        // find range of x
        int[] xResult = new int[2];
        int startX = (int) ((request.ullon - ROOT_ULLON) / LON_PER_TILE[request.depth]);
        xResult[0] = startX;
        double rightLon = ROOT_ULLON + (LON_PER_TILE[request.depth] * xResult[0]);
        while (rightLon < request.lrlon) {
            startX++;
            rightLon = ROOT_ULLON + (LON_PER_TILE[request.depth] * startX);
        }
        xResult[1] = startX - 1;
        //System.out.println(startX);
        // find range of y
        int[] yResult = new int[2];
        int startY = (int) ((ROOT_ULLAT - request.ullat) / LAT_PER_TILE[request.depth]);
        yResult[0] = startY;
        double bottomLat = ROOT_ULLAT - (LAT_PER_TILE[request.depth] * yResult[0]);
        while (bottomLat > request.lrlat) {
            startY++;
            bottomLat = ROOT_ULLAT - (LAT_PER_TILE[request.depth] * startY);
        }
        //System.out.println(startY);
        yResult[1] = startY - 1;

        // create grid with x and y range values
        Tile[][] grid =  new Tile[yResult[1] - yResult[0] + 1][xResult[1] - xResult[0] + 1];
        //System.out.println(xResult[0] + ", " + xResult[1] + "\n" + yResult[0] + ", " + yResult[1]);
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = new Tile(request.depth, j + xResult[0], i + yResult[0]);
                //System.out.println(grid[i][j]);
            }
        }
        return new RasterResult(grid);
    }

    public static class Tile {
        public final int depth;
        public final int x;
        public final int y;

        public Tile(int depth, int x, int y) {
            this.depth = depth;
            this.x = x;
            this.y = y;
        }

        public Tile offset() {
            return new Tile(depth, x + 1, y + 1);
        }

        /**
         * Return the latitude of the upper-left corner of the given slippy map tile.
         * @return latitude of the upper-left corner
         * @source https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
         */
        public double lat() {
            double n = Math.pow(2.0, MIN_ZOOM_LEVEL + depth);
            int slippyY = MIN_Y_TILE_AT_DEPTH[depth] + y;
            double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * slippyY / n)));
            return Math.toDegrees(latRad);
        }

        /**
         * Return the longitude of the upper-left corner of the given slippy map tile.
         * @return longitude of the upper-left corner
         * @source https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
         */
        public double lon() {
            double n = Math.pow(2.0, MIN_ZOOM_LEVEL + depth);
            int slippyX = MIN_X_TILE_AT_DEPTH[depth] + x;
            return slippyX / n * 360.0 - 180.0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Tile tile = (Tile) o;
            return depth == tile.depth &&
                    x == tile.x &&
                    y == tile.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(depth, x, y);
        }

        @Override
        public String toString() {
            return "d" + depth + "_x" + x + "_y" + y + ".jpg";
        }
    }
}
