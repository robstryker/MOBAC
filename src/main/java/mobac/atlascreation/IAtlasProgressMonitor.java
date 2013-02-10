package mobac.atlascreation;

import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSourceListener;

public interface IAtlasProgressMonitor extends MapSourceListener {
	/**
	 * Begin creation of the atlas
	 * @param atlas
	 */
	public void initAtlas(AtlasInterface atlas);
	
	/**
	 * Begin the download of this map
	 * @param map
	 */
	public void initMapDownload(MapInterface map);
	
	/**
	 * Increment by 1 the progress for the map download
	 */
	public void incMapDownloadProgress();
	/**
	 * Begin the creation of this map, which occurs after download
	 * @param maxProgress
	 */
	public void initMapCreation(int maxProgress);
	
	/**
	 * Increment the progress of map creation by 1
	 */
	public void incMapCreationProgress();
	
	/**
	 * Increment the progress of map creation by the value of workDone
	 * @param workDone
	 */
	public void incMapCreationProgress(int workDone);
	
	/**
	 * Set the map creation progress to this value.
	 * This method is not recommended for use as it does
	 * not fit into the workflow properly.
	 * 
	 * @param progress
	 */
	public void setMapCreationProgress(int progress);
	
	/**
	 * Note that a tile has been downloaded
	 * @param size
	 */
	public void tileDownloaded(int size);
	
	/**
	 * Note that a tile has been loaded from cache
	 * @param size
	 */
	public void tileLoadedFromCache(int size);
	
	/**
	 * Creation of the atlas is complete
	 */
	public void atlasCreationFinished();
	
	/**
	 * Is this monitor done yet
	 * @return
	 */
	public boolean isDone();
}
