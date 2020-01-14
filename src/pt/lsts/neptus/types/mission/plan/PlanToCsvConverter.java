/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.types.mission.plan;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.MissionFileChooser;
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.GuiUtils;

public class PlanToCsvConverter implements IPlanFileExporter {

	public static void main(String[] args) {
		File mfile = MissionFileChooser.showOpenMissionDialog(new String[] {"nmis", "nmisz"});
		if (mfile == null)
			System.exit(0);
		
		try {
			final MissionType mt = new MissionType(mfile.getAbsolutePath());
			Vector<String> plans = new Vector<String>();
			for (PlanType plan : mt.getIndividualPlansList().values()) {
				plans.add(plan.getId());
			}
			Object res = JOptionPane.showInputDialog(null, "please choose plan to export", "Select plan", JOptionPane.QUESTION_MESSAGE, null, plans.toArray(new String[0]), plans.firstElement());
			
			JFileChooser chooser = new JFileChooser();			
			if (chooser.showSaveDialog(null) != JFileChooser.CANCEL_OPTION) {
				NeptusLog.pub().info("<###>Save "+res+" to "+chooser.getSelectedFile().getAbsolutePath());
				saveAsCsv(mt.getIndividualPlansList().get(res.toString()), chooser.getSelectedFile());
			}
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void exportToFile(PlanType plan, File out, ProgressMonitor monitor) throws Exception {
	    saveAsCsv(plan, out);
	}
	
	@Override
	public String getExporterName() {
	    return "CSV";
	}
	
	@Override
	public String[] validExtensions() {
	    return new String[] {"csv"};
	}

	public static void saveAsCsv(PlanType plan, File out) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));

		bw.write("Waypoint Id\tLatitude\tLongitude\tLatDegrees\tLonDegrees\n");

		LocationType lastLocation = null;

		for (Maneuver m : plan.getGraph().getManeuversSequence()) {
			if (m instanceof LocatedManeuver) {
				lastLocation = new LocationType(((LocatedManeuver)m).getManeuverLocation());			
			}
			if (lastLocation != null) {
				bw.write(m.getId()+"\t"+lastLocation.getLatitudeAsPrettyString()+"\t"+lastLocation.getLongitudeAsPrettyString()+"\t"+GuiUtils.getNeptusDecimalFormat(7).format(lastLocation.getAbsoluteLatLonDepth()[0])+"\t"+GuiUtils.getNeptusDecimalFormat(7).format(lastLocation.getAbsoluteLatLonDepth()[1])+"\n");
			}
		}
		bw.close();
	}
}
