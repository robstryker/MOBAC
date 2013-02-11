package mobac.utilities;

/**
 * A utility class meant both to help discover the state
 * of system properties, but also to make a central location
 * where such properties can be discovered by users or other
 * developers looking to change the behavior of the app. 
 */
public class SystemPropertyUtils {

	/**
	 * This is specifically for designating what directory your 
	 * map sources are stored in!
	 */
	public static final String MAPSOURCES_SYS_PROP = "mobac.mapsources.dir";

	/*
	 * Three system properties meant to force the application to create a new
	 * atlas on startup. 
	 */
	public static final String FORCE_NEW_ATLAS_SYSPROP = "mobac.forceNewAtlas";
	public static final String NEW_ATLAS_NAME_SYSPROP = "mobac.newAtlasName";
	public static final String NEW_ATLAS_FORMAT_SYSPROP = "mobac.newAtlasOutputFormat";

	/* 
	 * A system property for setting the map source to be used. 
	 * This will OVERRIDE the settings. Settings will be ignored if this value is valid
	 * This is for which map source to use, NOT WHERE YOUR MAP SOURCES ARE LOCATED
	 */
	public static final String MAP_SOURCE = "mobac.mapSourceOverride";
	
	/*
	 * Set the zoom after startup
	 */
	public static final String MAP_ZOOM = "mobac.mapZoomOverride";


	/*
	 * Set the zoom after startup
	 */
	public static final String MAP_GRID_ZOOM = "mobac.mapGridZoomOverride";
	
	/*
	 * Set the initial position after startup in lat,lon
	 */
	public static final String MAP_INITIAL_POSITION = "mobac.mapInitialPosition";

	/*
	 * Set the initial selection after startup in lat-min, lat-max, lon-min, lon-max
	 */
	public static final String MAP_INITIAL_SELECTION = "mobac.mapInitialSelection";

	
	/*
	 * Set the initial list of zoom level checkboxes in comma-separated form
	 */
	public static final String MAP_INITIAL_ZOOM_LIST = "mobac.initialZoomlist";

}
