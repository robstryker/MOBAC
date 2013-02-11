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
package mobac.gui.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import mobac.exceptions.InvalidNameException;
import mobac.gui.MainGUI;
import mobac.gui.atlastree.JAtlasTree;
import mobac.program.Logging;
import mobac.program.interfaces.AtlasInterface;
import mobac.program.interfaces.MapSource;
import mobac.program.model.Layer;
import mobac.program.model.MapSelection;
import mobac.program.model.SelectedZoomLevels;
import mobac.program.model.Settings;
import mobac.program.model.TileImageParameters;

public class AddRectangleMapAutocut implements ActionListener {

	public void actionPerformed(ActionEvent event) {
		addSelectionWithNewLayer(null, event);
	}
	
	public void addSelectionWithNewLayer(String suggestedLayerName, ActionEvent event) {
		if( suggestedLayerName == null )
			suggestedLayerName = MainGUI.getMainGUI().getName();
		
		MainGUI mg = MainGUI.getMainGUI();
		JAtlasTree jAtlasTree = mg.jAtlasTree;
		AtlasInterface atlasInterface = jAtlasTree.getAtlas();
		MapSource mapSource = mg.getSelectedMapSource();
		SelectedZoomLevels sZL = mg.getSelectedZoomLevels();
		MapSelection ms = mg.getMapSelectionCoordinates();
		if (ms == null) {
			JOptionPane.showMessageDialog(mg, "Please select an area");
			return;
		}
		int[] zoomLevels = sZL.getZoomLevels();
		if (zoomLevels.length == 0) {
			JOptionPane.showMessageDialog(mg, "Please select at least one zoom level");
			return;
		}
		TileImageParameters customTileParameters = mg.getSelectedTileImageParameters();
		Layer l = addSelectionWithNewLayer(atlasInterface, mapSource, 
				suggestedLayerName, ms, zoomLevels, customTileParameters);
		jAtlasTree.getTreeModel().notifyNodeInsert(l);

	}
	
	public Layer  addSelectionWithNewLayer(AtlasInterface atlas, MapSource source, String suggestedLayerName, 
			MapSelection ms, int[] zoomLevels, TileImageParameters customTileParameters) {
		Settings settings = Settings.getInstance();
		final String mapNameFmt = "%s %02d";
		String layerName = suggestedLayerName;
		Layer layer = null;
		int c = 1;
		boolean success = false;
		do {
			try {
				layer = new Layer(atlas, layerName);
				success = true;
			} catch (InvalidNameException e) {
				layerName = suggestedLayerName + "_" + Integer.toString(c++);
			}
		} while (!success);
		for (int zoom : zoomLevels) {
			Point tl = ms.getTopLeftPixelCoordinate(zoom);
			Point br = ms.getBottomRightPixelCoordinate(zoom);
			try {
				String mapName = String.format(mapNameFmt, new Object[] { layerName, zoom });
				layer.addMapsAutocut(mapName, source, tl, br, zoom, customTileParameters, settings.maxMapSize, settings.mapOverlapTiles);
			} catch (InvalidNameException e) {
				Logging.LOG.error("", e);
			}
		}
		atlas.addLayer(layer);
		return layer;
	}

}
