package mobac.atlascreation.ui;

import java.util.ArrayList;

import mobac.atlascreation.IAtlasProgressMonitor;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.LayerInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.model.AtlasOutputFormat;

public class AtlasProgressMonitorModel implements IAtlasProgressMonitor {
	AtlasInterface atlas;
	MapInterface currentMap;
	MapProgressInfo mapInfo;
	long numberOfDownloadedBytes = 0;
	long numberOfBytesLoadedFromCache = 0;
	int totalNumberOfTiles = 0;
	int totalNumberOfMaps = 0;
	int totalProgress = 0;
	int totalProgressTenthPercent = -1;
	int currentMapNumber = 0;
	int mapDownloadProgress = 0;
	int mapDownloadNumberOfTiles = 0;
	int mapCreationProgress = 0;
	int mapCreationMax = 0;
	
	int currentMapRetryErrors = 0;
	int currentMapPermanentErrors = 0;
	int currentMapJobsDone = 0;
	int totalRetryErrors = 0;
	int totalPermanentErrors = 0;
	int totalJobsDone = 0;
	
	ArrayList<MapProgressInfo> mapInfos;
	boolean finished = false;

	public void initAtlas(AtlasInterface atlasInterface) {
		atlas = atlasInterface;
		if (atlasInterface.getOutputFormat().equals(AtlasOutputFormat.TILESTORE))
			totalNumberOfTiles = (int) atlasInterface.calculateTilesToDownload();
		else
			totalNumberOfTiles = (int) atlasInterface.calculateTilesToDownload() * 2;
		int mapCount = 0;
		int tileCount = 0;
		mapInfos = new ArrayList<MapProgressInfo>(100);
		for (LayerInterface layer : atlasInterface) {
			mapCount += layer.getMapCount();
			for (MapInterface map : layer) {
				int before = tileCount;
				int mapTiles = (int) map.calculateTilesToDownload();
				tileCount += mapTiles + mapTiles;
				mapInfos.add(new MapProgressInfo(map, before, tileCount));
			}
		}
		mapInfos.trimToSize();
		totalNumberOfMaps = mapCount;
	}

	public void initMapDownload(MapInterface map) {
		int index = mapInfos.indexOf(new MapProgressInfo(map, 0, 0));
		mapInfo = mapInfos.get(index);
		totalProgress = mapInfo.tileCountOnStart;
		mapDownloadNumberOfTiles = (int) map.calculateTilesToDownload();
		mapCreationProgress = 0;
		mapDownloadProgress = 0;
		currentMapNumber = index + 1;
		this.currentMap = map;
	}

	/**
	 * Initialize the GUI progress bars
	 * 
	 * @param maxTilesToProcess
	 */
	public void initMapCreation(int maxTilesToProcess) {
		mapCreationProgress = 0;
		mapCreationMax = maxTilesToProcess;
	}

	public void incMapDownloadProgress() {
		mapDownloadProgress++;
		totalProgress++;
		
	}

	public void incMapCreationProgress() {
		setMapCreationProgress(mapCreationProgress + 1);
	}

	public void incMapCreationProgress(int stepSize) {
		setMapCreationProgress(mapCreationProgress + stepSize);
	}

	public void setMapCreationProgress(int progress) {
		mapCreationProgress = progress;
		totalProgress = mapInfo.tileCountOnStart + mapInfo.mapTiles
				+ (int) (((long) mapInfo.mapTiles) * mapCreationProgress / mapCreationMax);
		totalProgressTenthPercent = (int) (totalProgress * 1000d / (double) totalNumberOfTiles);
	}

	public void tileDownloaded(int size) {
		synchronized (this) {
			numberOfDownloadedBytes += size;
		}
		
	}

	public void tileLoadedFromCache(int size) {
		synchronized (this) {
			numberOfBytesLoadedFromCache += size;
		}
		
	}

	@Override
	public void atlasCreationFinished() {
		finished = true;
	}

	@Override
	public boolean isDone() {
		return finished;
	}

	@Override
	public void incrementRetryErrors() {
		currentMapRetryErrors++;
		totalRetryErrors++;
	}

	@Override
	public void incrementPermanentErrors() {
		currentMapPermanentErrors++;
		totalPermanentErrors++;
	}

	@Override
	public void incrementJobsDone() {
		currentMapJobsDone++;
		totalJobsDone++;
	}
}
