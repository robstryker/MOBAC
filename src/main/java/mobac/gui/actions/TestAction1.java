package mobac.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import mobac.gui.MainGUI;
import mobac.program.model.EastNorthCoordinate;
import mobac.program.model.MapSelection;
import mobac.utilities.SystemPropertyUtils;

public class TestAction1 implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
		System.out.println("In test action 1");
		EastNorthCoordinate enc = new EastNorthCoordinate(43,105);
		MapSelection ms = new MapSelection(MainGUI.getMainGUI().previewMap.getMapSource(), enc, enc);
		MainGUI.getMainGUI().previewMap.setSelectionByMapSelection(ms, true);
		
		String layerName = System.getProperty(SystemPropertyUtils.NEW_ATLAS_NAME_SYSPROP);
		new AddRectangleMapAutocut().addSelectionWithNewLayer(layerName, null);
	}

}
