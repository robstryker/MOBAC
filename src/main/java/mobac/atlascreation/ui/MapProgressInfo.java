package mobac.atlascreation.ui;

import mobac.program.interfaces.MapInterface;

public class MapProgressInfo {
	final MapInterface map;
	final int tileCountOnStart;
	final int tileCountOnEnd;
	final int mapTiles;

	public MapProgressInfo(MapInterface map, int tileCountOnStart, int tileCountOnEnd) {
		super();
		this.map = map;
		this.tileCountOnStart = tileCountOnStart;
		this.tileCountOnEnd = tileCountOnEnd;
		this.mapTiles = (int) map.calculateTilesToDownload();
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MapProgressInfo))
			return false;
		return map.equals(((MapProgressInfo) obj).map);
	}
}
