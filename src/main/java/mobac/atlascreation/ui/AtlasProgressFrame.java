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
package mobac.atlascreation.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import mobac.atlascreation.IAtlasCreationController;
import mobac.atlascreation.IAtlasCreationUIProvider;
import mobac.gui.MainGUI;
import mobac.program.AtlasThread;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.model.Settings;
import mobac.utilities.GBC;
import mobac.utilities.OSUtilities;
import mobac.utilities.Utilities;

import org.apache.log4j.Logger;

/**
 * A window showing the progress while {@link AtlasThread} downloads and processes the map tiles.
 * 
 */
public class AtlasProgressFrame extends JFrame implements ActionListener, IAtlasCreationUIProvider {
	private static String TEXT_MAP_DOWNLOAD = "Collecting tiles for zoom level ";
	private static String TEXT_PERCENT = "%d%% done";
	private static String TEXT_TENTHPERCENT = "%.1f%% done";

	private static final String MSG_TILESMISSING = "Something is wrong with download of atlas tiles.\n"
			+ "The amount of downladed tiles is not as high as it was calculated.\nTherfore tiles "
			+ "will be missing in the created atlas.\n %d tiles are missing.\n\n"
			+ "Are you sure you want to continue " + "and create the atlas anyway?";
	
	private static Logger log = Logger.getLogger(AtlasProgressFrame.class);

	private static final long serialVersionUID = -1L;

	private static final Timer TIMER = new Timer(true);

	private long initialTotalTime;
	private long initialMapDownloadTime;
	int mapRetryErrors = 0;
	int mapPermanentErrors = 0;
	int totalMapsRetryErrors = 0;
	int totalMapsPermanentErrors = 0;


	private boolean aborted = false;
	private boolean finished = false;
	
	private IAtlasCreationController downloadController = null;
	private AtlasProgressMonitorModel model;
	
	private UpdateTask updateTask = null;
	private GUIUpdater guiUpdater = null;


	/* UI Fields below here */

	private Container background;
	private JProgressBar atlasProgressBar;
	private JProgressBar mapDownloadProgressBar;
	private JProgressBar mapCreationProgressBar;

	private JLabel windowTitle;

	private JLabel title;
	private JLabel mapInfoLabel;
	private JLabel mapDownloadTitle;
	private JLabel atlasPercent;
	private JLabel mapDownloadPercent;
	private JLabel atlasMapsDone;
	private JLabel mapDownloadElementsDone;
	private JLabel atlasTimeLeft;
	private JLabel mapDownloadTimeLeft;
	private JLabel mapCreation;
	private JLabel nrOfDownloadedBytes;
	private JLabel nrOfDownloadedBytesValue;
	private JLabel nrOfDownloadedBytesPerSecond;
	private JLabel nrOfDownloadedBytesPerSecondValue;
	private JLabel nrOfCacheBytes;
	private JLabel nrOfCacheBytesValue;
	private JLabel activeDownloads;
	private JLabel activeDownloadsValue;
	private JLabel retryableDownloadErrors;
	private JLabel retryableDownloadErrorsValue;
	private JLabel permanentDownloadErrors;
	private JLabel permanentDownloadErrorsValue;
	private JLabel totalDownloadTime;
	private JLabel totalDownloadTimeValue;

	private JCheckBox ignoreDlErrors;
	private JLabel statusLabel;

	private JButton dismissWindowButton;
	private JButton openProgramFolderButton;
	private JButton abortAtlasCreationButton;
	private JButton pauseResumeDownloadButton;

	/**
	 * Call the constructor for instantiating without a controller
	 * @param model
	 */
	public AtlasProgressFrame(AtlasProgressMonitorModel model) {
		this(null, model);
	}
	
	/**
	 * This class requires a controller to which the ui can pass the abort
	 * commands, or other such commands. It will not run without one.
	 * 
	 * However, instantiating without a controller should be fine, so long as 
	 * a controller is set before any updates.
	 * 
	 * @param controller
	 * @param model
	 */
	public AtlasProgressFrame(IAtlasCreationController controller, AtlasProgressMonitorModel model) {
		super("Atlas creation in progress");
		this.model = model;
		this.downloadController = controller;
		ToolTipManager.sharedInstance().setDismissDelay(12000);
		if (MainGUI.getMainGUI() == null) // Atlas creation started via command-line, no MainGUi available
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		else
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		setIconImages(MainGUI.MOBAC_ICONS);
		setLayout(new GridBagLayout());
		updateTask = new UpdateTask();
		guiUpdater = new GUIUpdater(model);

		createComponents();
		// Initialize the layout in respect to the layout (font size ...)
		pack();

		guiUpdater.run();

		// The layout is now initialized - we disable it because we don't want
		// want to the labels to jump around if the content changes.
		background.setLayout(null);
		setResizable(false);

		Dimension dScreen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension dContent = getSize();
		setLocation((dScreen.width - dContent.width) / 2, (dScreen.height - dContent.height) / 2);

		initialTotalTime = System.currentTimeMillis();
		initialMapDownloadTime = System.currentTimeMillis();

		addWindowListener(new CloseListener());
	}

	private void createComponents() {
		background = new JPanel(new GridBagLayout());

		windowTitle = new JLabel("<html><h3>ATLAS CREATION IN PROGRESS...</h3></html>");

		title = new JLabel("Processing maps of atlas:");

		mapInfoLabel = new JLabel("Processing map ABCDEFGHIJKLMNOPQRSTUVWXYZ-nn "
				+ "of layer ABCDEFGHIJKLMNOPQRSTUVWXYZ from map source ABCDEFGHIJKLMNOPQRSTUVWXYZ");

		atlasMapsDone = new JLabel("000 of 000 done");
		atlasPercent = new JLabel(String.format(TEXT_TENTHPERCENT, 100.0));
		atlasTimeLeft = new JLabel("Time remaining: 00000 minutes 00 seconds", JLabel.RIGHT);
		atlasProgressBar = new JProgressBar();

		mapDownloadTitle = new JLabel(TEXT_MAP_DOWNLOAD + "000");
		mapDownloadElementsDone = new JLabel("1000000 of 1000000 tiles done");
		mapDownloadPercent = new JLabel(String.format(TEXT_PERCENT, 100));
		mapDownloadTimeLeft = new JLabel("Time remaining: 00000 minutes 00 seconds", JLabel.RIGHT);
		mapDownloadProgressBar = new JProgressBar();

		mapCreation = new JLabel("Map Creation");
		mapCreationProgressBar = new JProgressBar();

		nrOfDownloadedBytesPerSecond = new JLabel("Average download speed");
		nrOfDownloadedBytesPerSecondValue = new JLabel();
		nrOfDownloadedBytes = new JLabel("Downloaded");
		nrOfDownloadedBytesValue = new JLabel();
		nrOfCacheBytes = new JLabel("Loaded from tile store");
		nrOfCacheBytesValue = new JLabel();

		activeDownloads = new JLabel("Active tile fetcher threads");
		activeDownloadsValue = new JLabel();
		retryableDownloadErrors = new JLabel("Transient download errors");
		retryableDownloadErrors
				.setToolTipText("<html><h4>Download errors for the current map and for the total atlas (transient/unrecoverable)</h4>"
						+ "<p>Mobile Atlas Creator retries failed tile downloads up to two times. <br>"
						+ "If the tile downloads fails the second time the tile will be counted as <br>"
						+ "<b>unrecoverable</b> error and not tried again during the current map creation run.<br></p></html>");
		retryableDownloadErrorsValue = new JLabel();
		retryableDownloadErrorsValue.setToolTipText(retryableDownloadErrors.getToolTipText());
		permanentDownloadErrors = new JLabel("Unrecoverable download errors");
		permanentDownloadErrors.setToolTipText(retryableDownloadErrors.getToolTipText());
		permanentDownloadErrorsValue = new JLabel();
		permanentDownloadErrorsValue.setToolTipText(permanentDownloadErrors.getToolTipText());
		totalDownloadTime = new JLabel("Total creation time");
		totalDownloadTimeValue = new JLabel();

		ignoreDlErrors = new JCheckBox("Ignore download errors and continue automatically",
				Settings.getInstance().ignoreDlErrors);
		ignoreDlErrors.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				downloadController.setIgnoreErrors(ignoreDlErrors.isSelected());
			} });
		statusLabel = new JLabel("Status:");
		Font f = statusLabel.getFont();
		statusLabel.setFont(f.deriveFont(Font.BOLD));
		abortAtlasCreationButton = new JButton("Abort creation");
		abortAtlasCreationButton.setToolTipText("Abort current Atlas download");
		dismissWindowButton = new JButton("Close Window");
		dismissWindowButton.setToolTipText("Atlas creation in progress...");
		dismissWindowButton.setVisible(false);
		openProgramFolderButton = new JButton("Open Atlas Folder");
		openProgramFolderButton.setToolTipText("Atlas creation in progress...");
		openProgramFolderButton.setEnabled(false);
		pauseResumeDownloadButton = new JButton("Pause/Resume");

		GBC gbcRIF = GBC.std().insets(0, 0, 20, 0).fill(GBC.HORIZONTAL);
		GBC gbcEol = GBC.eol();
		GBC gbcEolFill = GBC.eol().fill(GBC.HORIZONTAL);
		GBC gbcEolFillI = GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 0);

		// background.add(windowTitle, gbcEolFill);
		// background.add(Box.createVerticalStrut(10), gbcEol);

		background.add(mapInfoLabel, gbcEolFill);
		background.add(Box.createVerticalStrut(20), gbcEol);

		background.add(title, gbcRIF);
		background.add(atlasMapsDone, gbcRIF);
		background.add(atlasPercent, gbcRIF);
		background.add(atlasTimeLeft, gbcEolFill);
		background.add(atlasProgressBar, gbcEolFillI);
		background.add(Box.createVerticalStrut(20), gbcEol);

		background.add(mapDownloadTitle, gbcRIF);
		background.add(mapDownloadElementsDone, gbcRIF);
		background.add(mapDownloadPercent, gbcRIF);
		background.add(mapDownloadTimeLeft, gbcEolFill);
		background.add(mapDownloadProgressBar, gbcEolFillI);
		background.add(Box.createVerticalStrut(20), gbcEol);

		background.add(mapCreation, gbcEol);
		background.add(mapCreationProgressBar, gbcEolFillI);
		background.add(Box.createVerticalStrut(10), gbcEol);

		JPanel infoPanel = new JPanel(new GridBagLayout());
		GBC gbci = GBC.std().insets(0, 3, 3, 3);
		infoPanel.add(nrOfDownloadedBytes, gbci);
		infoPanel.add(nrOfDownloadedBytesValue, gbci.toggleEol());
		infoPanel.add(nrOfCacheBytes, gbci.toggleEol());
		infoPanel.add(nrOfCacheBytesValue, gbci.toggleEol());
		infoPanel.add(nrOfDownloadedBytesPerSecond, gbci.toggleEol());
		infoPanel.add(nrOfDownloadedBytesPerSecondValue, gbci.toggleEol());
		infoPanel.add(activeDownloads, gbci.toggleEol());
		infoPanel.add(activeDownloadsValue, gbci.toggleEol());
		infoPanel.add(retryableDownloadErrors, gbci.toggleEol());
		infoPanel.add(retryableDownloadErrorsValue, gbci.toggleEol());
		infoPanel.add(permanentDownloadErrors, gbci.toggleEol());
		infoPanel.add(permanentDownloadErrorsValue, gbci.toggleEol());
		infoPanel.add(totalDownloadTime, gbci.toggleEol());
		infoPanel.add(totalDownloadTimeValue, gbci.toggleEol());

		JPanel bottomPanel = new JPanel(new GridBagLayout());
		bottomPanel.add(infoPanel, GBC.std().gridheight(2).fillH());
		bottomPanel.add(ignoreDlErrors, GBC.eol().anchor(GBC.EAST));

		bottomPanel.add(statusLabel, GBC.eol().anchor(GBC.CENTER));

		GBC gbcRight = GBC.std().anchor(GBC.SOUTHEAST).insets(5, 0, 0, 0);
		bottomPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
		bottomPanel.add(abortAtlasCreationButton, gbcRight);
		bottomPanel.add(dismissWindowButton, gbcRight);
		bottomPanel.add(pauseResumeDownloadButton, gbcRight);
		bottomPanel.add(openProgramFolderButton, gbcRight);

		background.add(bottomPanel, gbcEolFillI);

		JPanel borderPanel = new JPanel(new GridBagLayout());
		borderPanel.add(background, GBC.std().insets(10, 10, 10, 10).fill());

		add(borderPanel, GBC.std().fill());

		abortAtlasCreationButton.addActionListener(this);
		dismissWindowButton.addActionListener(this);
		openProgramFolderButton.addActionListener(this);
		pauseResumeDownloadButton.addActionListener(this);
	}

	public void begin(AtlasInterface atlasInterface) {
		initialTotalTime = System.currentTimeMillis();
		initialMapDownloadTime = -1;
		updateGUI();
		setVisible(true);
		TIMER.schedule(updateTask, 0, 500);
	}
	

	public void finished() {
		finished = true;
		stopUpdateTask();
		forceUpdateGUI();
		downloadController = null;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				abortAtlasCreationButton.setEnabled(false);

				if (aborted) {
					windowTitle.setText("<html><h2>ATLAS CREATION HAS BEEN " + "ABORTED BY USER</h2></html>");
					setTitle("Atlas creation aborted");
				} else {
					windowTitle.setText("<html><h2>ATLAS CREATION FINISHED " + "SUCCESSFULLY</h2></html>");
					setTitle("Atlas creation finished successfully");
				}
				atlasMapsDone.setText(model.currentMapNumber + " of " + model.totalNumberOfMaps + " done");

				abortAtlasCreationButton.setVisible(false);

				dismissWindowButton.setToolTipText("Close atlas creation progress window");
				dismissWindowButton.setVisible(true);

				if (!aborted) {
					openProgramFolderButton.setToolTipText("Open folder where atlas output folder");
					openProgramFolderButton.setEnabled(true);
				}
			}
		});
	}

	private String formatTime(long longSeconds) {
		String timeString = "";

		if (longSeconds < 0) {
			timeString = "unknown";
		} else {
			int minutes = (int) (longSeconds / 60);
			int seconds = (int) (longSeconds % 60);
			if (minutes > 0)
				timeString += Integer.toString(minutes) + " " + (minutes == 1 ? "minute" : "minutes") + " ";
			timeString += Integer.toString(seconds) + " " + (seconds == 1 ? "second" : "seconds");
		}
		return timeString;
	}
//
//	public void setZoomLevel(int theZoomLevel) {
//		mapDownloadTitle.setText(TEXT_MAP_DOWNLOAD + Integer.toString(theZoomLevel));
//	}


	private synchronized void stopUpdateTask() {
		try {
			updateTask.cancel();
			updateTask = null;
		} catch (Exception e) {
		}
	}

	public void closeWindow() {
		try {
			stopUpdateTask();
			downloadController = null;
			setVisible(false);
		} finally {
			dispose();
		}
	}
	
	public void setDownloadControlerListener(IAtlasCreationController threadControlListener) {
		this.downloadController = threadControlListener;
	}

	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		File atlasFolder = Settings.getInstance().getAtlasOutputDirectory();
		if (openProgramFolderButton.equals(source)) {
			try {
				OSUtilities.openFolderBrowser(atlasFolder);
			} catch (Exception e) {
				log.error("", e);
			}
		} else if (dismissWindowButton.equals(source)) {
			downloadController = null;
			closeWindow();
		} else if (abortAtlasCreationButton.equals(source)) {
			aborted = true;
			stopUpdateTask();
			if (downloadController != null)
				downloadController.abortAtlasCreation();
			else
				closeWindow();
		} else if (pauseResumeDownloadButton.equals(source)) {
			if (downloadController != null)
				downloadController.pauseResumeAtlasCreation();
		}
	}

	public void updateGUI() {
		guiUpdater.updateAsynchronously();
	}

	public void forceUpdateGUI() {
		SwingUtilities.invokeLater(guiUpdater);
	}

	private class GUIUpdater implements Runnable {
		private AtlasProgressMonitorModel data;
		GUIUpdater(AtlasProgressMonitorModel model) {
			this.data = model;
		}
		int scheduledCounter = 0;

		public void updateAsynchronously() {
			// If there is still at least one scheduled update request to be
			// executed we don't have add another one as this can result in an
			// to overloaded swing invocation queue.
			synchronized (this) {
				if (scheduledCounter > 0)
					return;
				scheduledCounter++;
			}
			SwingUtilities.invokeLater(this);
		}

		public void run() {
			synchronized (this) {
				scheduledCounter--;
			}

			if (data.currentMap != null) {
				String text = "<html>Processing map <b>" + data.currentMap.getName() + "</b> " + "of layer <b>"
						+ data.currentMap.getLayer().getName() + "</b> " + "from map source <b>" + data.currentMap.getMapSource()
						+ "</b></html>";
				mapInfoLabel.setText(text);
			}

			// atlas progress
			atlasProgressBar.setMaximum(data.totalNumberOfTiles);
			atlasProgressBar.setValue(data.totalProgress);

			try {
				String statusText = "RUNNING";
				if (aborted)
					statusText = "ABORTED";
				else if (finished)
					statusText = "FINISHED";
				else if( downloadController == null ) {
					statusText = "UNKNOWN";
				} else {
					boolean pauseState = downloadController.isPaused();
					if (pauseState)
						statusText = "PAUSED";
					else
						statusText = "RUNNING";
				}
				statusLabel.setText("Status: " + statusText);

				atlasPercent.setText(String.format(TEXT_TENTHPERCENT, data.totalProgressTenthPercent / 10.0));
				if (data.atlas != null) {
					String text = String.format(TEXT_PERCENT + " - processing atlas \"%s\" of type %s",
							data.totalProgressTenthPercent / 10, data.atlas.getName(),
							data.atlas.getOutputFormat());
					if (downloadController != null && downloadController.isPaused())
						text += " [PAUSED]";
					AtlasProgressFrame.this.setTitle(text);
				}
			} catch (NullPointerException e) {
			}

			long seconds = -1;
			int totalProgress = data.totalProgress;
			if (totalProgress != 0) {
				// Avoid for a possible division by zero
				int totalTilesRemaining = data.totalNumberOfTiles - totalProgress;
				long totalElapsedTime = System.currentTimeMillis() - initialTotalTime;
				seconds = (totalElapsedTime * totalTilesRemaining / (1000L * totalProgress));
			}
			atlasTimeLeft.setText("Time remaining: " + formatTime(seconds));

			// layer progress
			mapDownloadProgressBar.setMaximum(data.mapDownloadNumberOfTiles);
			mapDownloadProgressBar.setValue(data.mapDownloadProgress);

			mapDownloadPercent.setText(String.format(TEXT_PERCENT,
					(int) (mapDownloadProgressBar.getPercentComplete() * 100)));

			mapDownloadElementsDone.setText(Integer.toString(data.mapDownloadProgress) + " of "
					+ data.mapDownloadNumberOfTiles + " tiles done");

			seconds = -1;
			int mapDlProgress = data.mapDownloadProgress;
			if (mapDlProgress != 0 && initialMapDownloadTime > 0)
				seconds = ((System.currentTimeMillis() - initialMapDownloadTime)
						* (data.mapDownloadNumberOfTiles - mapDlProgress) / (1000L * mapDlProgress));
			mapDownloadTimeLeft.setText("Time remaining: " + formatTime(seconds));

			// map progress
			mapCreation.setText("Map creation");
			System.out.println("map creation: " + data.mapCreationProgress + "/" + data.mapCreationMax);
			mapCreationProgressBar.setValue(data.mapCreationProgress);
			mapCreationProgressBar.setMaximum(data.mapCreationMax);
			atlasMapsDone.setText((data.currentMapNumber - 1) + " of " + data.totalNumberOfMaps + " done");

			// bytes per second
			long rate = data.numberOfDownloadedBytes * 1000;
			long time = System.currentTimeMillis() - initialMapDownloadTime;
			if (data.mapCreationProgress == 0 && initialMapDownloadTime > 0) {
				if (time == 0) {
					nrOfDownloadedBytesPerSecondValue.setText(": ?? KiByte / second");
				} else {
					rate = rate / time;
					nrOfDownloadedBytesPerSecondValue.setText(": " + Utilities.formatBytes(rate) + " / second");
				}
			}

			// downloaded bytes
			nrOfDownloadedBytesValue.setText(": " + Utilities.formatBytes(data.numberOfDownloadedBytes));
			nrOfCacheBytesValue.setText(": " + Utilities.formatBytes(data.numberOfBytesLoadedFromCache));

			// total creation time
			long totalSeconds = (System.currentTimeMillis() - initialTotalTime) / 1000;
			totalDownloadTimeValue.setText(": " + formatTime(totalSeconds));
			totalDownloadTimeValue.repaint();

			// active downloads
			int activeDownloads = (downloadController == null) ? 0 : downloadController.countActiveDownloads();
			activeDownloadsValue.setText(": " + activeDownloads);
			activeDownloadsValue.repaint();

			// TODO fix this
			retryableDownloadErrorsValue.setText(": current map: " + data.currentMapRetryErrors + ", total: "
					+ data.totalRetryErrors);
			permanentDownloadErrorsValue.setText(": current map: " + data.currentMapPermanentErrors + ", total: "
					+ data.totalPermanentErrors);
			retryableDownloadErrorsValue.repaint();
			permanentDownloadErrorsValue.repaint();
		}
	}

	private class UpdateTask extends TimerTask {

		@Override
		public void run() {
			updateGUI();
		}
	}

	private class CloseListener extends WindowAdapter {

		@Override
		public void windowClosing(WindowEvent e) {
			log.debug("Closing event detected for atlas progress window");
			IAtlasCreationController listener = AtlasProgressFrame.this.downloadController;
			if (listener != null)
				listener.abortAtlasCreation();
		}

	}
	
	/**
	 * The following 3 methods are methods the controller calls
	 * on the UI to inform it whats going on.
	 */
	
	public void criticalError(Throwable t) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				String message = "Mobile Atlas Creator has run out of memory.";
				int maxMem = Utilities.getJavaMaxHeapMB();
				if (maxMem > 0)
					message += "\nCurrent maximum memory associated to MOBAC: " + maxMem + " MiB";
				JOptionPane.showMessageDialog(null, message, "Out of memory", JOptionPane.ERROR_MESSAGE);
				closeWindow();
			}
		});
	}

	public void downloadAborted() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JOptionPane.showMessageDialog(null, "Atlas download aborted", "Information",
						JOptionPane.INFORMATION_MESSAGE);
				closeWindow();
			}
		});
	}
	
	public void tilesMissing(int tileCount, int missing) throws InterruptedException {
		log.debug("Expected tile count: " + tileCount + " downloaded tile count: " + (tileCount-missing)
				+ " missing: " + missing);
		int answer = JOptionPane.showConfirmDialog(this, String.format(MSG_TILESMISSING, missing),
				"Error - tiles are missing - do you want to continue anyway?",
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
		if (answer != JOptionPane.YES_OPTION)
			throw new InterruptedException();
	}
}
