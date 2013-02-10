package mobac.atlascreation;

import mobac.gui.AtlasProgress;
import mobac.program.PauseResumeHandler;

/**
 * This is a migration interface to help migrate to the new AtlasCreationControllerThread
 * It centralizes all the apis others expected to get from an AtlasThread.
 * 
 * Care must be taken during migration to ensure no binary incompatibilities with 
 * older map source types like google maps
 */
public interface IAtlasThread {
	public AtlasProgress getAtlasProgress();
	public PauseResumeHandler getPauseResumeHandler();
}
