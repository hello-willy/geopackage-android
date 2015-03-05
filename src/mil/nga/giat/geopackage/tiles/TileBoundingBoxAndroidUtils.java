package mil.nga.giat.geopackage.tiles;

import java.util.ArrayList;
import java.util.List;

import mil.nga.giat.geopackage.BoundingBox;
import mil.nga.giat.geopackage.geom.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

/**
 * Tile Bounding Box utility methods relying on Android libraries
 * 
 * @author osbornb
 */
public class TileBoundingBoxAndroidUtils {

	/**
	 * Half the world distance in either direction
	 */
	private static double HALF_WORLD_WIDTH = 20037508.34789244;

	/**
	 * Get a rectangle using the tile width, height, bounding box, and the
	 * bounding box section within the outer box to build the rectangle from
	 * 
	 * @param width
	 * @param height
	 * @param boundingBox
	 * @param boundingBoxSection
	 * @return
	 */
	public static Rect getRectangle(long width, long height,
			BoundingBox boundingBox, BoundingBox boundingBoxSection) {

		RectF rectF = getFloatRectangle(width, height, boundingBox,
				boundingBoxSection);

		Rect rect = new Rect(Math.round(rectF.left), Math.round(rectF.top),
				Math.round(rectF.right), Math.round(rectF.bottom));

		return rect;
	}

	/**
	 * Get a rectangle with floating point boundaries using the tile width,
	 * height, bounding box, and the bounding box section within the outer box
	 * to build the rectangle from
	 * 
	 * @param width
	 * @param height
	 * @param boundingBox
	 * @param boundingBoxSection
	 * @return
	 */
	public static RectF getFloatRectangle(long width, long height,
			BoundingBox boundingBox, BoundingBox boundingBoxSection) {

		float left = TileBoundingBoxUtils.getXPixel(width, boundingBox,
				boundingBoxSection.getMinLongitude());
		float right = TileBoundingBoxUtils.getXPixel(width, boundingBox,
				boundingBoxSection.getMaxLongitude());
		float top = TileBoundingBoxUtils.getYPixel(height, boundingBox,
				boundingBoxSection.getMaxLatitude());
		float bottom = TileBoundingBoxUtils.getYPixel(height, boundingBox,
				boundingBoxSection.getMinLatitude());

		RectF rect = new RectF(left, top, right, bottom);

		return rect;
	}

	/**
	 * Get the tile bounding box from the Google Maps API tile coordinates and
	 * zoom level
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @return
	 */
	public static BoundingBox getBoundingBox(int x, int y, int zoom) {

		int tilesPerSide = tilesPerSide(zoom);
		double tileWidthDegrees = tileWidthDegrees(tilesPerSide);
		double tileHeightDegrees = tileHeightDegrees(tilesPerSide);

		double minLon = -180.0 + (x * tileWidthDegrees);
		double maxLon = minLon + tileWidthDegrees;

		double maxLat = 90.0 - (y * tileHeightDegrees);
		double minLat = maxLat - tileHeightDegrees;

		BoundingBox box = new BoundingBox(minLon, maxLon, minLat, maxLat);

		return box;
	}

	/**
	 * Get the Web Mercator tile bounding box from the Google Maps API tile
	 * coordinates and zoom level
	 * 
	 * @param x
	 * @param y
	 * @param zoom
	 * @return
	 */
	public static BoundingBox getWebMercatorBoundingBox(int x, int y, int zoom) {

		int tilesPerSide = tilesPerSide(zoom);
		double tileSize = tileSize(tilesPerSide);

		double minLon = (-1 * HALF_WORLD_WIDTH) + (x * tileSize);
		double maxLon = (-1 * HALF_WORLD_WIDTH) + ((x + 1) * tileSize);
		double minLat = HALF_WORLD_WIDTH - ((y + 1) * tileSize);
		double maxLat = HALF_WORLD_WIDTH - (y * tileSize);

		BoundingBox box = new BoundingBox(minLon, maxLon, minLat, maxLat);

		return box;
	}

	/**
	 * Get the tile grid that includes the entire tile bounding box
	 * 
	 * @param boundingBox
	 * @param zoom
	 * @return
	 */
	public static TileGrid getTileGrid(BoundingBox boundingBox, int zoom) {

		int tilesPerSide = tilesPerSide(zoom);
		double tileSize = tileSize(tilesPerSide);

		int minX = (int) ((boundingBox.getMinLongitude() + HALF_WORLD_WIDTH) / tileSize);
		double tempMaxX = (boundingBox.getMaxLongitude() + HALF_WORLD_WIDTH)
				/ tileSize;
		int maxX = (int) tempMaxX;
		if (tempMaxX % 1 == 0) {
			maxX--;
		}
		maxX = Math.min(maxX, tilesPerSide - 1);

		int minY = (int) (((boundingBox.getMaxLatitude() - HALF_WORLD_WIDTH) * -1) / tileSize);
		double tempMaxY = ((boundingBox.getMinLatitude() - HALF_WORLD_WIDTH) * -1)
				/ tileSize;
		int maxY = (int) tempMaxY;
		if (tempMaxY % 1 == 0) {
			maxY--;
		}
		maxX = Math.min(maxX, tilesPerSide - 1);

		TileGrid grid = new TileGrid(minX, maxX, minY, maxY);

		return grid;
	}

	/**
	 * Convert the bounding box coordinates to a new web mercator bounding box
	 * 
	 * @param boundingBox
	 */
	public static BoundingBox toWebMercator(BoundingBox boundingBox) {

		Point lowerLeftPoint = new Point(false, false,
				boundingBox.getMinLongitude(), boundingBox.getMinLatitude());
		Point upperRightPoint = new Point(false, false,
				boundingBox.getMaxLongitude(), boundingBox.getMaxLatitude());

		toWebMercator(lowerLeftPoint);
		toWebMercator(upperRightPoint);

		BoundingBox mercatorBox = new BoundingBox(lowerLeftPoint.getX(),
				upperRightPoint.getX(), lowerLeftPoint.getY(),
				upperRightPoint.getY());

		return mercatorBox;
	}

	/**
	 * Convert the Point to web mercator
	 * 
	 * @param point
	 */
	private static void toWebMercator(Point point) {
		if ((Math.abs(point.getX()) > 180 || Math.abs(point.getY()) > 90))
			return;

		double num = point.getX() * 0.017453292519943295;
		double x = 6378137.0 * num;
		double a = point.getY() * 0.017453292519943295;

		point.setX(x);
		double y = 3189068.5 * Math.log((1.0 + Math.sin(a))
				/ (1.0 - Math.sin(a)));
		y = Math.max(-HALF_WORLD_WIDTH, y);
		y = Math.min(HALF_WORLD_WIDTH, y);
		point.setY(y);
	}

	/**
	 * Get the tile size in meters
	 * 
	 * @param tilesPerSide
	 * @return
	 */
	public static double tileSize(int tilesPerSide) {
		return (2 * HALF_WORLD_WIDTH) / tilesPerSide;
	}

	/**
	 * Get the tile width in degrees
	 * 
	 * @param tilesPerSide
	 * @return
	 */
	public static double tileWidthDegrees(int tilesPerSide) {
		return 360.0 / tilesPerSide;
	}

	/**
	 * Get the tile height in degrees
	 * 
	 * @param tilesPerSide
	 * @return
	 */
	public static double tileHeightDegrees(int tilesPerSide) {
		return 180.0 / tilesPerSide;
	}

	/**
	 * Get the tiles per side, width and height, at the zoom level
	 * 
	 * @param zoom
	 * @return
	 */
	public static int tilesPerSide(int zoom) {
		return (int) Math.pow(2, zoom);
	}

	/**
	 * Get the longitude distance in the middle latitude
	 * 
	 * @param boundingBox
	 * @return
	 */
	public static double getLongitudeDistance(BoundingBox boundingBox) {
		return getLongitudeDistance(boundingBox.getMinLongitude(),
				boundingBox.getMaxLongitude());
	}

	/**
	 * Get the longitude distance in the middle latitude
	 * 
	 * @param minLongitude
	 * @param maxLongitude
	 * @return
	 */
	public static double getLongitudeDistance(double minLongitude,
			double maxLongitude) {
		LatLng leftMiddle = new LatLng(0, minLongitude);
		LatLng middle = new LatLng(0, maxLongitude - minLongitude);
		LatLng rightMiddle = new LatLng(0, maxLongitude);

		List<LatLng> path = new ArrayList<LatLng>();
		path.add(leftMiddle);
		path.add(middle);
		path.add(rightMiddle);

		double lonDistance = SphericalUtil.computeLength(path);
		return lonDistance;
	}

	/**
	 * Get the latitude distance in the middle longitude
	 * 
	 * @param boundingBox
	 * @return
	 */
	public static double getLatitudeDistance(BoundingBox boundingBox) {
		return getLatitudeDistance(boundingBox.getMinLatitude(),
				boundingBox.getMaxLatitude());
	}

	/**
	 * Get the latitude distance in the middle longitude
	 * 
	 * @param minLatitude
	 * @param maxLatitude
	 * @return
	 */
	public static double getLatitudeDistance(double minLatitude,
			double maxLatitude) {
		LatLng lowerMiddle = new LatLng(minLatitude, 0);
		LatLng upperMiddle = new LatLng(maxLatitude, 0);
		double latDistance = SphericalUtil.computeDistanceBetween(lowerMiddle,
				upperMiddle);
		return latDistance;
	}

}