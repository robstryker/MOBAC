package mobac.atlascreation;

import mobac.program.interfaces.AtlasInterface;

/**
 * This interface represents a class which is capable 
 * of handling questions or feedback to the user 
 * if an IAtlasCreationController has questions or 
 * details to show the user 
 */
public interface IAtlasCreationUIProvider {
	/**
	 * Begin running
	 */
	public void begin(AtlasInterface atlas);
	
	/**
	 * The process has completed
	 */
	public void finished();
	
	/**
	 * Alert the user that some tiles are missing. 
	 * This method should alert a user as to the issue, 
	 * and throw an InterruptedException if the user wishes to cancel.
	 * 
	 * @param tileCount
	 * @param missing
	 * @throws InterruptedException
	 */
	public void tilesMissing(int tileCount, int missing) throws InterruptedException;
	
	/**
	 * Alert the user as to a critical error
	 * @param t
	 */
	public void criticalError(Throwable t);
	
	/**
	 * Alert the user that the download was aborted
	 */
	public void downloadAborted();
	
	/**
	 * Set the controller, to which we should pass abort commands etc
	 */
	public void setDownloadControlerListener(IAtlasCreationController threadControlListener);
}
