package mobac;

import mobac.atlascreation.AtlasCreationControllerThread;
import mobac.atlascreation.IAtlasCreationUIProvider;
import mobac.atlascreation.ui.AtlasProgressFrame;
import mobac.atlascreation.ui.AtlasProgressMonitorModel;
import mobac.exceptions.AtlasTestException;
import mobac.gui.MainGUI;
import mobac.gui.actions.AddRectangleMapAutocut;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.model.EastNorthCoordinate;
import mobac.program.model.MapSelection;
import mobac.program.model.Settings;
import mobac.utilities.GridSelectionUtility;
import mobac.utilities.SystemPropertyUtils;

public class ScriptMain {

	private static void initUI(String[] args) {
		StartMOBAC.main(args);
		
		// Wait for the UI to be fully loaded
		while(MainGUI.getMainGUI() == null ) {
			try {
				System.out.println("Still waiting for UI to load...");
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
	}
	
	public static void main(String[] args) {
		// Start via the other start class
		System.out.println("Beginning ScriptMain");
		initUI(args);
		System.out.println("UI Initialized");
		
		if( args.length > 0 ) {
			generateCommandList();
			return;
		}
		
		
		Settings.getInstance().maxMapSize = 1048575;
		// Ensure our grid is selected
		String selArg = System.getProperty(SystemPropertyUtils.MAP_INITIAL_SELECTION);
		MapSource source = MainGUI.getMainGUI().previewMap.getMapSource();
		MapSelection ms = GridSelectionUtility.createMapSelectionFromCSV(source, selArg);
		MainGUI.getMainGUI().previewMap.setSelectionByMapSelection(ms, true);
		
		// Add the selection to a layer
		String layerName = System.getProperty(SystemPropertyUtils.NEW_ATLAS_NAME_SYSPROP);
		new AddRectangleMapAutocut().addSelectionWithNewLayer(layerName, null);
		
		
		// Launch the thread
		// We have to work on a deep clone otherwise the user would be
		// able to modify settings of maps, layers and the atlas itself
		// while the AtlasThread works on that atlas reference
		Settings.getInstance().ignoreDlErrors=true;
		AtlasInterface atlasToCreate = MainGUI.getMainGUI().jAtlasTree.getAtlas().deepClone();
		
		try {
			AtlasProgressMonitorModel monitor = new AtlasProgressMonitorModel() {
				public void atlasCreationFinished() {
					super.atlasCreationFinished();
					new Thread() {
						public void run() {
							try {
								Thread.sleep(2000);
							} catch(InterruptedException ie) {
							}
							System.exit(0);
						}
					}.start();
				}
			};
			IAtlasCreationUIProvider ui = new AtlasProgressFrame(monitor);
			AtlasCreationControllerThread atlasThread = new AtlasCreationControllerThread(
					atlasToCreate, monitor, ui);
			ui.setDownloadControlerListener(atlasThread);
			atlasThread.start();
		} catch(AtlasTestException ate) {
		}
		
	}

	/*
	      To run this, run the following command
	 */
	private static void generateCommandList() {
		String initialPos = System.getProperty(SystemPropertyUtils.MAP_INITIAL_POSITION);
		String[] asArr = initialPos.split(",");
		Double lat = Double.parseDouble(asArr[0]);
		Double lon = Double.parseDouble(asArr[1]);
		EastNorthCoordinate NECorner = new EastNorthCoordinate(lat.doubleValue(), lon.doubleValue());
		char initialLetter = 'G';
		int initialRow = 8;
		
		int columnsTotal=7;
		int rowsTotal=9;

		char currentLetter;
		System.out.println("___ ScriptMain command output");
		for( int i = 0; i < columnsTotal; i++ ) {
			for( int j = 0; j < rowsTotal; j++ ) {
				MapSource source = MainGUI.getMainGUI().previewMap.getMapSource();
				int zoom = MainGUI.getMainGUI().previewMap.getZoom();
				EastNorthCoordinate point = moveGrid(source, zoom, NECorner, i, j);
				currentLetter = (char)(initialLetter + i);
				String squareId = ("" + currentLetter) + (initialRow+j);
				outputCommand(squareId, point);
			}
		}
		System.out.println("___ End ScriptMain command output");
		System.exit(0);

	}
	
	private static void outputCommand(String squareId, EastNorthCoordinate point) {
		StringBuffer sb = new StringBuffer();
		sb.append("java -Xms64m -Xmx1024M ");
		sb.append("-Dmobac.mapsources.dir=/home/rob/apps/MOBAC/app/mapsources ");
		sb.append("-Dmobac.forceNewAtlas=true ");
		sb.append("-Dmobac.newAtlasOutputFormat=OruxMapsSqlite ");
		sb.append("-Dmobac.mapSourceOverride=\"Google Maps China\" ");
		sb.append("-Dmobac.mapGridZoomOverride=7 ");
		sb.append("-Dmobac.mapZoomOverride=4 ");
		sb.append("-Dmobac.initialZoomlist=16,14,12,10,9,8,7,6,5 ");
		sb.append("-Dmobac.newAtlasName=" + squareId + " ");
		sb.append("-Dmobac.mapInitialPosition=\"" + point.lat + "," + point.lon + "\" ");
		sb.append("-Dmobac.mapInitialSelection=\"" + point.lat + "," + point.lon + "\" ");
		sb.append("-cp Mobile_Atlas_Creator.jar:lib/* mobac.ScriptMain");
		
		StringBuffer sb2 = new StringBuffer();
		sb2.append("id=");
		sb2.append(squareId);
		sb2.append(", loc=");
		sb2.append(point.lat);
		sb2.append(",");
		sb2.append(point.lon);
		System.out.println(sb.toString());
	}
	
	private static EastNorthCoordinate moveGrid(MapSource source, int zoom, EastNorthCoordinate origin, int x, int y) {
		EastNorthCoordinate next = GridSelectionUtility.getDistanceGridPoint(source, source.getMapSpace(), MainGUI.getMainGUI().previewMap.getZoom(), origin, GridSelectionUtility.WEST, x);
		return GridSelectionUtility.getDistanceGridPoint(source, source.getMapSpace(), MainGUI.getMainGUI().previewMap.getZoom(), next, GridSelectionUtility.SOUTH, y);
	}
}
