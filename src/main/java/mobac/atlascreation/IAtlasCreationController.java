package mobac.atlascreation;

import mobac.program.PauseResumeHandler;

/**
 * This interface represents an object which is controlling
 * the atlas creation and can handle commands to 
 * abort, or pause, the process. 
 * 
 * @author rob
 *
 */
public interface IAtlasCreationController {
	public void abortAtlasCreation();

	public void pauseResumeAtlasCreation();

	public boolean isPaused();

	public int countActiveDownloads();
	
	public void setIgnoreErrors(boolean ignore);
	
	public PauseResumeHandler getPauseResumeHandler();
}
