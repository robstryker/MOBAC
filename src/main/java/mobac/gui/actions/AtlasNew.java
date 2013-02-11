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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import mobac.gui.MainGUI;
import mobac.program.model.AtlasOutputFormat;
import mobac.utilities.SystemPropertyUtils;

public class AtlasNew implements ActionListener {

	private interface INewAtlasProvider {
		public String getName();
		public AtlasOutputFormat getFormat();
	}
	
	public void actionPerformed(ActionEvent event) {
		MainGUI mg = MainGUI.getMainGUI();

		INewAtlasProvider provider = new UINewAtlasProvider();
		String cmd = System.getProperty(SystemPropertyUtils.FORCE_NEW_ATLAS_SYSPROP);
		System.out.println("Checking force new atlas: has value " + cmd);
		if( cmd != null && cmd.equals("true")) {
			provider = new CmdLineNewAtlasProvider();
		}
		if( provider.getName() == null || provider.getFormat() == null ) 
			provider = new UINewAtlasProvider();
		mg.jAtlasTree.newAtlas(provider.getName(), provider.getFormat());
		mg.getParametersPanel().atlasFormatChanged(provider.getFormat());
	}
	
	/*
	 * Return values from the system properties
	 */
	public class CmdLineNewAtlasProvider implements INewAtlasProvider {
		public String getName() {
			return System.getProperty(SystemPropertyUtils.NEW_ATLAS_NAME_SYSPROP);
		}
		public AtlasOutputFormat getFormat() {
			String outputFormat = System.getProperty(SystemPropertyUtils.NEW_ATLAS_FORMAT_SYSPROP);
			return AtlasOutputFormat.getFormatByName(outputFormat);
		}
	}
	
	/*
	 * A provider which polls the user via the UI
	 */
	public class UINewAtlasProvider implements INewAtlasProvider {
		private String name = null;
		private AtlasOutputFormat format = null;
		private boolean finished = false;
		
		private void showDialog() {
			MainGUI mg = MainGUI.getMainGUI();
			Vector<AtlasOutputFormat> allFormats = AtlasOutputFormat.getFormatsAsVector();
			
			JPanel panel = new JPanel();
			BorderLayout layout = new BorderLayout();
			layout.setVgap(4);
			panel.setLayout(layout);
	
			JPanel formatPanel = new JPanel(new BorderLayout());
	
			formatPanel.add(new JLabel("<html><b>Please select the desired atlas format</b></html>"), BorderLayout.NORTH);
			JList atlasFormatList = new JList(allFormats);
			atlasFormatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane scroller = new JScrollPane(atlasFormatList);
			scroller.setPreferredSize(new Dimension(100, 200));
			formatPanel.add(scroller, BorderLayout.CENTER);
	
			JPanel namePanel = new JPanel(new BorderLayout());
			namePanel.add(new JLabel("<html><b>Name of the new atlas:<b></html>"), BorderLayout.NORTH);
			JTextField atlasName = new JTextField("Unnamed atlas");
			namePanel.add(atlasName, BorderLayout.SOUTH);
	
			panel.add(namePanel, BorderLayout.NORTH);
			panel.add(formatPanel, BorderLayout.CENTER);
			AtlasOutputFormat currentAOF = null;
			try {
				currentAOF = mg.getAtlas().getOutputFormat();
			} catch (Exception e) {
			}
			if (currentAOF != null)
				atlasFormatList.setSelectedValue(currentAOF, true);
			else
				atlasFormatList.setSelectedIndex(1);
			int result = JOptionPane.showConfirmDialog(MainGUI.getMainGUI(), panel, "Settings for new Atlas",
					JOptionPane.OK_CANCEL_OPTION);
			if (result != JOptionPane.OK_OPTION)
				return;
	
			format = (AtlasOutputFormat) atlasFormatList.getSelectedValue();
			name = atlasName.getText();
			finished = true;
		}
		public String getName() {
			if( !finished ) 
				showDialog();
			return name;
		}
		public AtlasOutputFormat getFormat() {
			if( !finished ) 
				showDialog();
			return format;
		}
	}
}
