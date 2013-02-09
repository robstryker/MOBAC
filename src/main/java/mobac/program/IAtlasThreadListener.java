package mobac.program;

import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSourceListener;

/**
 * An attempt to unify the atlasThread / AtlasPRogress mingling of logic.
 * Cleaning that up would allow multiple listeners to an AtlasThread 
 */
public interface IAtlasThreadListener extends MapSourceListener {
	/* Initialize from the thread */
	public void initialize(AtlasThread thread);
	
	/**
	 * Handle a critical error
	 * @param t
	 */
	public void criticalError(Throwable t);
	
	/**
	 * The download has been aborted
	 */
	public void downloadAborted();
	
	/**
	 * An event indicationg the download is complete
	 */
	public void atlasCreationFinished();
	
	public void beginMap(MapInterface map);

	
	public void tileDownloaded(int size);
	
	public void tileLoadedFromCache(int size);
	
	public void zoomChanged(int zoom);
	
	public void downloadJobComplete();
	
	public void tilesMissing(int tileCount, int missing) throws InterruptedException;
}
