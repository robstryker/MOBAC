/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.atlascreation;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JOptionPane;

import mobac.exceptions.AtlasTestException;
import mobac.exceptions.MapDownloadSkippedException;
import mobac.gui.AtlasProgress;
import mobac.gui.AtlasProgress.AtlasCreationController;
import mobac.program.DirectoryManager;
import mobac.program.JobDispatcher;
import mobac.program.PauseResumeHandler;
import mobac.program.atlascreators.AtlasCreator;
import mobac.program.atlascreators.tileprovider.DownloadedTileProvider;
import mobac.program.atlascreators.tileprovider.FilteredMapSourceProvider;
import mobac.program.atlascreators.tileprovider.TileProvider;
import mobac.program.download.DownloadJobProducerThread;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.DownloadJobListener;
import mobac.program.interfaces.DownloadableElement;
import mobac.program.interfaces.FileBasedMapSource;
import mobac.program.interfaces.LayerInterface;
import mobac.program.interfaces.MapInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.interfaces.MapSource.LoadMethod;
import mobac.program.model.AtlasOutputFormat;
import mobac.program.model.Settings;
import mobac.program.tilestore.TileStore;
import mobac.utilities.GUIExceptionHandler;
import mobac.utilities.SystemPropertyUtils;
import mobac.utilities.tar.TarIndex;
import mobac.utilities.tar.TarIndexedArchive;

import org.apache.log4j.Logger;

public class AtlasCreationControllerThread extends Thread implements DownloadJobListener, AtlasCreationController, IAtlasThread, IAtlasCreationController {
	private static final String MSG_DOWNLOADERRORS = "<html>Multiple tile downloads have failed. "
			+ "Something may be wrong with your connection to the download server or your selected area. "
			+ "<br>Do you want to:<br><br>"
			+ "<u>Continue</u> map download and ignore the errors? (results in blank/missing tiles)<br>"
			+ "<u>Retry</u> to download this map, by starting over?<br>"
			+ "<u>Skip</u> the current map and continue to process other maps in the atlas?<br>"
			+ "<u>Abort</u> the current map and atlas creation process?<br></html>";

	private static final Logger log = Logger.getLogger(AtlasCreationControllerThread.class);
	private static int threadNum = 0;

	private File customAtlasDir = null;

	private DownloadJobProducerThread djp = null;
	private JobDispatcher downloadJobDispatcher;

	private AtlasInterface atlas;
	private AtlasCreator atlasCreator = null;
	private PauseResumeHandler pauseResumeHandler;

	private int activeDownloads = 0;
	private int maxDownloadRetries = 1;

	private boolean ignoreErrors = Settings.getInstance().ignoreDlErrors;
	private IAtlasProgressMonitor monitor;
	private IAtlasCreationUIProvider ui;
	
	
	private int currentMapRetryErrors;
	
	public AtlasCreationControllerThread(AtlasInterface atlas, 
			IAtlasProgressMonitor monitor, IAtlasCreationUIProvider ui) throws AtlasTestException {
		this(atlas, atlas.getOutputFormat().createAtlasCreatorInstance(),
				monitor, ui);
	}

	public AtlasCreationControllerThread(AtlasInterface atlas, AtlasCreator atlasCreator,
			IAtlasProgressMonitor monitor, IAtlasCreationUIProvider ui) throws AtlasTestException {
		super("AtlasThread " + getNextThreadNum());
		this.atlas = atlas;
		this.atlasCreator = atlasCreator;
		this.monitor = monitor;
		this.ui = ui;
		testAtlas();
		TileStore.getInstance().closeAll();
		maxDownloadRetries = Settings.getInstance().downloadRetryCount;
		pauseResumeHandler = new PauseResumeHandler();
	}
	
	public AtlasInterface getAtlas() {
		return atlas;
	}
	
	private void testAtlas() throws AtlasTestException {
		try {
			for (LayerInterface layer : atlas) {
				for (MapInterface map : layer) {
					MapSource mapSource = map.getMapSource();
					if (!atlasCreator.testMapSource(mapSource))
						throw new AtlasTestException("The selected atlas output format \"" + atlas.getOutputFormat()
								+ "\" does not support the map source \"" + map.getMapSource() + "\"");
				}
			}
		} catch (AtlasTestException e) {
			throw e;
		} catch (Exception e) {
			throw new AtlasTestException(e);
		}
	}

	private static synchronized int getNextThreadNum() {
		threadNum++;
		return threadNum;
	}

	public void run() {
		GUIExceptionHandler.registerForCurrentThread();
		log.info("Starting altas creation");
		try {
			createAtlas();
			monitor.atlasCreationFinished();
			log.info("Altas creation finished");
		} catch (OutOfMemoryError e) {
			System.gc();
			ui.criticalError(e);
			log.error("Out of memory: ", e);
		} catch (InterruptedException e) {
			ui.downloadAborted();
			log.info("Altas creation was interrupted by user");
		} catch (Exception e) {
			ui.downloadAborted();
			log.error("Altas creation aborted because of an error: ", e);
			GUIExceptionHandler.showExceptionDialog(e);
		}
		System.gc();
	}

	/**
	 * Create atlas: For each map download the tiles and perform atlas/map creation
	 */
	protected void createAtlas() throws InterruptedException, IOException {

		long totalNrOfOnlineTiles = atlas.calculateTilesToDownload();

		for (LayerInterface l : atlas) {
			for (MapInterface m : l) {
				// Offline map sources are not relevant for the maximum tile limit.
				if (m.getMapSource() instanceof FileBasedMapSource)
					totalNrOfOnlineTiles -= m.calculateTilesToDownload();
			}
		}

		if (totalNrOfOnlineTiles > 500000) {
			NumberFormat f = DecimalFormat.getInstance();
			JOptionPane.showMessageDialog(null, "Mobile Atlas Creator has detected that you are trying to\n"
					+ "download an extra ordinary large atlas " + "with a very high number of tiles.\n"
					+ "Please reduce the selected areas on high zoom levels and try again.\n"
					+ "The maximum allowed amount of tiles per atlas is " + f.format(500000)
					+ "\nThe number of tile in the currently selected atlas is: " + f.format(totalNrOfOnlineTiles),
					"Atlas download prohibited", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			atlasCreator.startAtlasCreation(atlas, customAtlasDir);
		} catch (AtlasTestException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Atlas format restriction violated",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		monitor.initAtlas(atlas);
		ui.begin(atlas);

		String threadCount = System.getProperty(SystemPropertyUtils.DOWNLOAD_JOB_THREAD_COUNT);
		int tCount = -1;
		try {
			tCount = Integer.parseInt(threadCount);
		} catch(NumberFormatException nfe) {}
		if( tCount == -1 )
			tCount = Settings.getInstance().downloadThreadCount;
		
		downloadJobDispatcher = new JobDispatcher(tCount, pauseResumeHandler, monitor);
		try {
			for (LayerInterface layer : atlas) {
				atlasCreator.initLayerCreation(layer);
				for (MapInterface map : layer) {
					try {
						while (!createMap(map))
							;
					} catch (InterruptedException e) {
						throw e; // User has aborted
					} catch (MapDownloadSkippedException e) {
						// Do nothing and continue with next map
					} catch (Exception e) {
						log.error("", e);
						String[] options = { "Continue", "Abort", "Show error report" };
						int a = JOptionPane.showOptionDialog(null, "An error occured: " + e.getMessage() + "\n["
								+ e.getClass().getSimpleName() + "]\n\n", "Error", 0, JOptionPane.ERROR_MESSAGE, null,
								options, options[0]);
						switch (a) {
						case 2:
							GUIExceptionHandler.processException(e);
						case 1:
							throw new InterruptedException();
						}
					}
				}
				atlasCreator.finishLayerCreation();
			}
		} catch (InterruptedException e) {
			atlasCreator.abortAtlasCreation();
			throw e;
		} catch (Error e) {
			atlasCreator.abortAtlasCreation();
			throw e;
		} finally {
			// In case of an abort: Stop create new download jobs
			if (djp != null)
				djp.cancel();
			downloadJobDispatcher.terminateAllWorkerThreads();
			if (!atlasCreator.isAborted())
				atlasCreator.finishAtlasCreation();
			monitor.atlasCreationFinished();
			ui.finished();
		}

	}
	
	private int currentZoom = 0;
	public int getCurrentZoom() {
		return currentZoom;
	}
	
	/**
	 * 
	 * @param map
	 * @return true if map creation process was finished and false if something went wrong and the user decided to retry
	 *         map download
	 * @throws Exception
	 */
	public boolean createMap(MapInterface map) throws Exception {
		TarIndex tileIndex = null;
		TarIndexedArchive tileArchive = null;

		monitor.initMapDownload(map);

		if (currentThread().isInterrupted())
			throw new InterruptedException();

		currentMapRetryErrors = 0;
		
		// Prepare the tile store directory
		// ts.prepareTileStore(map.getMapSource());

		/***
		 * In this section of code below, tiles for Atlas is being downloaded and saved in the temporary layer tar file
		 * in the system temp directory.
		 **/
		currentZoom = map.getZoom();
		
		final int tileCount = (int) map.calculateTilesToDownload();
		try {
			tileArchive = null;
			TileProvider mapTileProvider;
			if (!(map.getMapSource() instanceof FileBasedMapSource)) {
				// For online maps we download the tiles first and then start creating the map if
				// we are sure we got all tiles
				if (!AtlasOutputFormat.TILESTORE.equals(atlas.getOutputFormat())) {
					String tempSuffix = "MOBAC_" + atlas.getName() + "_" + map.getZoom() + "_";
					File tileArchiveFile = File.createTempFile(tempSuffix, ".tar", DirectoryManager.tempDir);
					// If something goes wrong the temp file only persists until the VM exits
					tileArchiveFile.deleteOnExit();
					log.debug("Writing downloaded tiles to " + tileArchiveFile.getPath());
					tileArchive = new TarIndexedArchive(tileArchiveFile, tileCount);
				} else
					log.debug("Downloading to tile store only");

				djp = new DownloadJobProducerThread(this, downloadJobDispatcher, tileArchive, (DownloadableElement) map);

				boolean failedMessageAnswered = false;

				while (djp.isAlive() || (downloadJobDispatcher.getWaitingJobCount() > 0)
						|| downloadJobDispatcher.isAtLeastOneWorkerActive()) {
					Thread.sleep(500);
					if (!failedMessageAnswered && (currentMapRetryErrors > 50) && !ignoreErrors) {
						pauseResumeHandler.pause();
						
						// TODO move this to the UI ***
						String[] answers = new String[] { "Continue", "Retry", "Skip", "Abort" };
						int answer = JOptionPane.showOptionDialog(null, MSG_DOWNLOADERRORS,
								"Multiple download errors - how to proceed?", 0, JOptionPane.QUESTION_MESSAGE, null,
								answers, answers[0]);
						failedMessageAnswered = true;
						switch (answer) {
						case 0: // Continue
							pauseResumeHandler.resume();
							break;
						case 1: // Retry
							djp.cancel();
							djp = null;
							downloadJobDispatcher.cancelOutstandingJobs();
							return false;
						case 2: // Skip
							downloadJobDispatcher.cancelOutstandingJobs();
							throw new MapDownloadSkippedException();
						default: // Abort or close dialog
							downloadJobDispatcher.cancelOutstandingJobs();
							downloadJobDispatcher.terminateAllWorkerThreads();
							throw new InterruptedException();
						}
					}
				}
				djp = null;
				log.debug("All download jobs has been completed!");
				if (tileArchive != null) {
					tileArchive.writeEndofArchive();
					tileArchive.close();
					tileIndex = tileArchive.getTarIndex();
					if (tileIndex.size() < tileCount && !ignoreErrors) {
						ui.tilesMissing(tileCount, tileCount-tileIndex.size());
					}
				}
				downloadJobDispatcher.cancelOutstandingJobs();
				log.debug("Starting to create atlas from downloaded tiles");
				mapTileProvider = new DownloadedTileProvider(tileIndex, map);
			} else {
				// We don't need to download anything. Everything is already stored locally therefore we can just use it
				mapTileProvider = new FilteredMapSourceProvider(map, LoadMethod.DEFAULT);
			}
			atlasCreator.initializeMap(map, mapTileProvider);
			atlasCreator.createMap();
		} catch (Error e) {
			log.error("Error in createMap: " + e.getMessage(), e);
			throw e;
		} finally {
			if (tileIndex != null)
				tileIndex.closeAndDelete();
			else if (tileArchive != null)
				tileArchive.delete();
		}
		return true;
	}

	public void pauseResumeAtlasCreation() {
		if (pauseResumeHandler.isPaused()) {
			log.debug("Atlas creation resumed");
			pauseResumeHandler.resume();
		} else {
			log.debug("Atlas creation paused");
			pauseResumeHandler.pause();
		}
	}

	public boolean isPaused() {
		return pauseResumeHandler.isPaused();
	}

	public PauseResumeHandler getPauseResumeHandler() {
		return pauseResumeHandler;
	}

	/**
	 * Stop listener from {@link AtlasProgress}
	 */
	public void abortAtlasCreation() {
		try {
			DownloadJobProducerThread djp_ = djp;
			if (djp_ != null)
				djp_.cancel();
			if (downloadJobDispatcher != null)
				downloadJobDispatcher.terminateAllWorkerThreads();
			pauseResumeHandler.resume();
			this.interrupt();
		} catch (Exception e) {
			log.error("Exception thrown in stopDownload()" + e.getMessage());
		}
	}

	public synchronized void jobStarted() {
		activeDownloads++;
	}

	public void jobFinishedSuccessfully(int bytesDownloaded) {
		synchronized (this) {
			activeDownloads--;
			currentMapRetryErrors++;
			monitor.incrementJobsDone();
			monitor.incMapDownloadProgress();
		}
	}

	public void jobFinishedWithError(boolean retry) {
		synchronized (this) {
			activeDownloads--;
			if (retry)
				monitor.incrementRetryErrors();
			else {
				monitor.incrementPermanentErrors();
				monitor.incMapDownloadProgress();
			}
		}
		if (!ignoreErrors)
			Toolkit.getDefaultToolkit().beep();
	}
	
	public int getMaxDownloadRetries() {
		return maxDownloadRetries;
	}

	/* 
	 * This is a migration task to prevent having to rewrite
	 * all other output formats
	 */
	public AtlasProgress getAtlasProgress() {
		return new AtlasProgress(this.monitor);
	}

	public File getCustomAtlasDir() {
		return customAtlasDir;
	}

	public void setCustomAtlasDir(File customAtlasDir) {
		this.customAtlasDir = customAtlasDir;
	}
	
	public void setIgnoreErrors(boolean b) {
		this.ignoreErrors = b;
	}

	@Override
	public int countActiveDownloads() {
		return activeDownloads;
	}
}
