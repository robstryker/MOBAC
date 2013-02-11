package mobac.utilities;

import java.awt.Point;
import java.util.ArrayList;

import mobac.gui.MainGUI;
import mobac.gui.mapview.PreviewMap;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSpace;
import mobac.program.model.EastNorthCoordinate;
import mobac.program.model.MapSelection;
import mobac.program.model.MercatorPixelCoordinate;

/**
 * Utility methods for dealing with grids, 
 * accessing or setting selections, etc. 
 * 
 */
public class GridSelectionUtility {

	
	public static Point[] cleanPointsForSelection(MapSource mapSource, int cZoom, Point pStart, Point pEnd) {
		Point pNewStart = new Point();
		Point pNewEnd = new Point();
		int mapMaxCoordinate = mapSource.getMapSpace().getMaxPixels(cZoom) - 1;
		// Sort x/y coordinate of points so that pNewStart < pnewEnd and limit selection to map size
		pNewStart.x = Math.max(0, Math.min(mapMaxCoordinate, Math.min(pStart.x, pEnd.x)));
		pNewStart.y = Math.max(0, Math.min(mapMaxCoordinate, Math.min(pStart.y, pEnd.y)));
		pNewEnd.x = Math.max(0, Math.min(mapMaxCoordinate, Math.max(pStart.x, pEnd.x)));
		pNewEnd.y = Math.max(0, Math.min(mapMaxCoordinate, Math.max(pStart.y, pEnd.y)));

		int zoomDiff = PreviewMap.MAX_ZOOM - cZoom;

		pNewEnd.x <<= zoomDiff;
		pNewEnd.y <<= zoomDiff;
		pNewStart.x <<= zoomDiff;
		pNewStart.y <<= zoomDiff;

		return new Point[] { pNewStart, pNewEnd};
	}
	/**
	 * Returns an array of two points, one marking the beginning, one marking the end
	 * These points are points on a map image, NOT latitude / longitude
	 * 
	 * @param mapSource
	 * @param iSelectionMin
	 * @param iSelectionMax
	 * @param gridZoom
	 * @param maxZoom
	 * @return
	 */
	public static Point[] getSelectionAsGridSelection(MapSource mapSource, Point iSelectionMin, Point iSelectionMax, int gridZoom) {
		if (gridZoom < 0) {
			return new Point[]{iSelectionMin, iSelectionMax};
		}

		if (iSelectionMin == null || iSelectionMax == null)
			return null;

		int gridZoomDiff = PreviewMap.MAX_ZOOM - gridZoom;
		int gridFactor = mapSource.getMapSpace().getTileSize() << gridZoomDiff;

		Point pNewStart = new Point(iSelectionMin);
		Point pNewEnd = new Point(iSelectionMax);

		// Snap to the current grid

		pNewStart.x = MyMath.roundDownToNearest(pNewStart.x, gridFactor);
		pNewStart.y = MyMath.roundDownToNearest(pNewStart.y, gridFactor);
		pNewEnd.x = MyMath.roundUpToNearest(pNewEnd.x, gridFactor) - 1;
		pNewEnd.y = MyMath.roundUpToNearest(pNewEnd.y, gridFactor) - 1;

		return new Point[]{pNewStart, pNewEnd};
	}

	/**
	 * Provided points on the map, convert them to pixel coordinates
	 * @param mapSource
	 * @param iSelectionMin
	 * @param iSelectionMax
	 * @return
	 */
	public static MercatorPixelCoordinate[] asPixelCoordinates(MapSource mapSource, Point iSelectionMin, Point iSelectionMax) {
		if( iSelectionMin == null || iSelectionMax == null )
			return null;
		int x_min, y_min, x_max, y_max;
		x_min = iSelectionMin.x;
		y_min = iSelectionMin.y;
		x_max = iSelectionMax.x;
		y_max = iSelectionMax.y;
		MercatorPixelCoordinate min = new MercatorPixelCoordinate(mapSource.getMapSpace(), x_min, y_min, PreviewMap.MAX_ZOOM);
		MercatorPixelCoordinate max = new MercatorPixelCoordinate(mapSource.getMapSpace(), x_max, y_max, PreviewMap.MAX_ZOOM);
		return new MercatorPixelCoordinate[] { min, max};
	}
	
	public static MapSelection asMapSelection(MapSource mapSource, MercatorPixelCoordinate[] coords) {
		return new MapSelection(mapSource, coords[0], coords[1]);
	}
	
	
	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int WEST = 2;
	public static final int EAST = 3;
	
	
	/** 
	 * Get a point guaranteed to be in a grid section north, south, east, or west, from an origin
	 * 
	 * @param source
	 * @param space
	 * @param zoom
	 * @param origin
	 * @param direction
	 * @return
	 */
	public static EastNorthCoordinate getAdjacentGridPoint(MapSource source, MapSpace space, int zoom, EastNorthCoordinate origin, int direction) {
		MapSelection mapSel = new MapSelection(source, origin, origin);
		Point pStart = mapSel.getTopLeftPixelCoordinate(zoom);
		Point pEnd = mapSel.getBottomRightPixelCoordinate(zoom);

		Point[] cleaned = GridSelectionUtility.cleanPointsForSelection(source, zoom, pStart, pEnd);
		Point iSelectionMin = cleaned[0];
		Point iSelectionMax = cleaned[1];
		Point[] results = GridSelectionUtility.getSelectionAsGridSelection(source, iSelectionMin, iSelectionMax, 7);
		MercatorPixelCoordinate[] coords = GridSelectionUtility.asPixelCoordinates(source, results[0], results[1]);
		double latDif = Math.abs(coords[0].getEastNorthCoordinate().lat - coords[1].getEastNorthCoordinate().lat);
		double lonDif = Math.abs(coords[0].getEastNorthCoordinate().lon - coords[1].getEastNorthCoordinate().lon);
		double centerLat = Math.min(coords[0].getEastNorthCoordinate().lat, coords[1].getEastNorthCoordinate().lat) + (0.5*latDif);
		double centerLon = Math.min(coords[0].getEastNorthCoordinate().lon, coords[1].getEastNorthCoordinate().lon) + (0.5*lonDif);
		
		if( direction == NORTH)
			return new EastNorthCoordinate(centerLat + latDif, centerLon);
		if( direction == SOUTH)
			return new EastNorthCoordinate(centerLat - latDif, centerLon);
		if( direction == WEST)
			return new EastNorthCoordinate(centerLat, centerLon - lonDif);
		if( direction == EAST)
			return new EastNorthCoordinate(centerLat, centerLon + lonDif);
		
		return null;
	}
	
	public static EastNorthCoordinate getDistanceGridPoint(MapSource source, MapSpace space, int zoom, EastNorthCoordinate origin, int direction, int distance) {
		EastNorthCoordinate working = origin;
		for( int i = 0; i < distance; i++ ) {
			working = getAdjacentGridPoint(source, space, zoom, working, direction);
		}
		return working;
	}
	
	/**
	 * Create a series of east-north coordinates based on a comma-separated list
	 * @param commaSeparated
	 * @return
	 */
	public static EastNorthCoordinate[] createEastNorthCoordinatesFromCSV(String commaSeparated) {
		if( commaSeparated == null )
			return new EastNorthCoordinate[0];
		
		String[] split = commaSeparated.split(",");
		if( split.length % 2 == 1 )
			throw new IllegalArgumentException();
		
		try {
			ArrayList<EastNorthCoordinate> coords = new ArrayList<EastNorthCoordinate>();
			for( int i = 0; i < split.length / 2; i++ ) {
				Double d1 = Double.parseDouble(split[(2*i)]);
				Double d2 = Double.parseDouble(split[(2*i)+1]);
				EastNorthCoordinate coord = new EastNorthCoordinate(d1.doubleValue(), d2.doubleValue());
				coords.add(coord);
			}
			return (EastNorthCoordinate[]) coords.toArray(new EastNorthCoordinate[coords.size()]);
		} catch(NumberFormatException nfe) {
			return new EastNorthCoordinate[0];
		}
	}
	public static MapSelection createMapSelectionFromCSV(MapSource source, String commaSeparated) {
		System.out.println("INSIDE createMapSelectionFromCSV");
		System.out.println("CSV is " + commaSeparated);
		EastNorthCoordinate[] coords = GridSelectionUtility.createEastNorthCoordinatesFromCSV(commaSeparated);
		if( coords == null || coords.length == 0 )
			return null;
		EastNorthCoordinate min, max;
		if( coords.length == 1) {
			min = max = coords[0];
		} else {
			min = coords[0];
			max = coords[1];
		}
		MapSelection ms = new MapSelection(source, min, max);
		return ms;
	}
}
