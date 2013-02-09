package mobac;

import mobac.exceptions.AtlasTestException;
import mobac.gui.AtlasProgress;
import mobac.gui.MainGUI;
import mobac.gui.actions.AddRectangleMapAutocut;
import mobac.program.AbstractAtlasThreadListener;
import mobac.program.AtlasThread;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.model.EastNorthCoordinate;
import mobac.program.model.MapSelection;
import mobac.program.model.Settings;
import mobac.utilities.SystemPropertyUtils;

public class ScriptMain {

	public static void main(String[] args) {
		// Start via the other start class
		StartMOBAC.main(args);
		
		// Wait for the UI to be fully loaded
		while(MainGUI.getMainGUI() == null ) {
			try {
				System.out.println("Still null");
				Thread.sleep(500);
			} catch(InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		
		// Wait just a little longer for the previews to load
		try {
			Thread.sleep(3000);
		} catch(InterruptedException ie) {
			ie.printStackTrace();
		}
		System.out.println("Done");
		
//		Settings.getInstance().maxMapSize = 1048575;
//		// Select the proper location TODO change to coords
//		EastNorthCoordinate enc = new EastNorthCoordinate(43,105);
//		MapSelection ms = new MapSelection(MainGUI.getMainGUI().previewMap.getMapSource(), enc, enc);
//		MainGUI.getMainGUI().previewMap.setSelectionByMapSelection(ms, true);
//		
//		// Add the selection to a layer
//		String layerName = System.getProperty(SystemPropertyUtils.NEW_ATLAS_NAME_SYSPROP);
//		new AddRectangleMapAutocut().addSelectionWithNewLayer(layerName, null);
//		
//		
//		// Launch the thread
//		// We have to work on a deep clone otherwise the user would be
//		// able to modify settings of maps, layers and the atlas itself
//		// while the AtlasThread works on that atlas reference
//		Settings.getInstance().ignoreDlErrors=true;
//		AtlasInterface atlasToCreate = MainGUI.getMainGUI().jAtlasTree.getAtlas().deepClone();
//		
//		try {
//			AtlasThread atlasThread = new AtlasThread(atlasToCreate);
//			AtlasProgress ap =  new AtlasProgress(atlasThread);
//			ap.setDownloadControlerListener(atlasThread);
//			atlasThread.addAtlasThreadListener(ap);
//			final String layerName2 = layerName;
//			atlasThread.addAtlasThreadListener(new AbstractAtlasThreadListener() {
//				public void tilesMissing(int tileCount, int missing)
//						throws InterruptedException {
//					System.err.println("Log missing tiles for " + layerName2 + ": " + missing);
//				}
//				public void criticalError(Throwable t) {
//					System.err.println("Download Incomplete, aborted");
//					t.printStackTrace();
//				}
//				public void downloadAborted() {
//					System.err.println("Download Incomplete, aborted");
//				}
//				public void atlasCreationFinished() {
//					// TODO Should write a report if possible
//					System.out.println("Download complete");
//					System.exit(0);
//				}
//			});
//			atlasThread.start();
//		} catch(AtlasTestException ate) {
//		}
	}

	
}
