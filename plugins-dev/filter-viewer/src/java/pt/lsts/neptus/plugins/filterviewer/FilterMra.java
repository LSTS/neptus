/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: Frédéric Leishman
 * 13 juin 2014
 */
package pt.lsts.neptus.plugins.filterviewer;

import java.awt.Component;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicSliderUI;

import net.miginfocom.swing.MigLayout;
import pt.lsts.colormap.ColorMap;
import pt.lsts.colormap.ColorMapFactory;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.gui.Timeline;
import pt.lsts.neptus.gui.TimelineChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.api.BathymetryParser;
import pt.lsts.neptus.mra.api.BathymetryParserFactory;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.mra.importers.lsf.DVLBathymetryParser;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.vtk.mravisualizer.EventsHandler;
import pt.lsts.neptus.vtk.mravisualizer.EventsHandler.SensorTypeInteraction;
import pt.lsts.neptus.vtk.mravisualizer.InteractorStyleVis3D;
import pt.lsts.neptus.vtk.mravisualizer.Window;
import pt.lsts.neptus.vtk.pointcloud.APointCloud;
import pt.lsts.neptus.vtk.pointcloud.PointCloudHandlerXYZ;
import pt.lsts.neptus.vtk.pointcloud.PointCloudXYZ;
import pt.lsts.neptus.vtk.surface.Delauny2D;
import pt.lsts.neptus.vtk.surface.MeshSmoothingLaplacian;
import pt.lsts.neptus.vtk.surface.PointCloudMesh;
import pt.lsts.neptus.vtk.utils.Utils;
import pt.lsts.neptus.vtk.visualization.Canvas;
import vtk.vtkActor;
import vtk.vtkActorCollection;
import vtk.vtkArrowSource;
import vtk.vtkDataArray;
import vtk.vtkDoubleArray;
import vtk.vtkGenericDataObjectReader;
import vtk.vtkGlyph3D;
import vtk.vtkLineSource;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;
import vtk.vtkTransform;
import vtk.vtkTransformFilter;
import vtk.vtkUnsignedCharArray;


/**
 * @author Frédéric Leishman (www.georgiatech-metz.fr)
 * 
 */
@PluginDescription(author = "fl", name = "3D Filter", icon = "images/menus/view.png")
public class FilterMra extends JPanel implements MRAVisualization, TimelineChangeListener {

    private static final long serialVersionUID = 9026967524309720487L;

    protected MRAPanel mraPanel;
    protected IMraLogGroup source;
    protected double timestep;

    private Boolean componentEnabled = false;
    private Boolean isFirstRender = true;

    // Vtk class extended
    private Canvas canvas;
    private Window winCanvas;
    private InteractorStyleVis3D interactorStyle;
    private EventsHandler events;

    private LinkedHashMap<String, APointCloud<?>> linkedHashMapCloud = new LinkedHashMap<String, APointCloud<?>>();
    private LinkedHashMap<String, PointCloudMesh> linkedHashMapMesh = new LinkedHashMap<String, PointCloudMesh>();

    // Timeline variables
    private Timeline timeline;

    private long firstPingTime;
    private long lastPingTime;
    private long currentTime;

    protected SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    private int kTActual = 0;
    private int kTPreview = 0;

    // Map visualization variables
    class CMaps {
        public APointCloud<?> pointCloud;
        public double latitude, longitude, height;
        public double north, east, down;
        public int num_point;
        public double offset_checked;
    }

    private CMaps map_ref;
    
    // Estimated state variables
    class CEstimatedState {
        public IMraLog parser;
        public int kt = 0;
        public List<vtkLineSource> trajectory;
        public List<double[]> position = new ArrayList<double[]>();
        public List<double[]> orientation = new ArrayList<double[]>();
        public List<Long> timestamp = new ArrayList<Long>();
        public double latitude, longitude, height;
        public double north, east, down;
    }

    private CEstimatedState estimated_state;
    
    // Corrected state
    class CCorrectedState {
        public int enable = 0;
        public int kt = 0;     
        public List<vtkLineSource> trajectory;       
        public List<Long> timestamp = new ArrayList<Long>();
    }

    private CCorrectedState corrected_state;
    
    // Particles
    class CParticles {
        public int enable = 0;
        public int kt = 0;
        public List<Long> timestamp = new ArrayList<Long>();
        long iteration_time = 0;
    } 
    
    private CParticles particles;
    
    // Identifier of actors used
    class C3DListElement {
        public List<Integer> id = new ArrayList<Integer>();
        public List<String> description = new ArrayList<String>();
        public int num_actor = 0;

        public void AddActor(String desc) {
            description.add(desc);
            id.add(num_actor);
            num_actor++;
        }

        public String GetDescription(int index) {
            return description.get(index);
        }
    }

    private C3DListElement idActorList;

    public FilterMra(MRAPanel panel) {
        this.mraPanel = panel;
    }

    public void createTimeline() {
        estimated_state = new CEstimatedState();
        estimated_state.parser = source.getLog("EstimatedState");

        estimated_state.latitude = (double) estimated_state.parser.firstLogEntry().getDouble("lat");
        estimated_state.longitude = (double) estimated_state.parser.firstLogEntry().getDouble("lon");
        estimated_state.height = (double) estimated_state.parser.firstLogEntry().getDouble("height");

        estimated_state.north = (double) estimated_state.parser.firstLogEntry().getDouble("x");
        estimated_state.east = (double) estimated_state.parser.firstLogEntry().getDouble("y");
        estimated_state.down = (double) estimated_state.parser.firstLogEntry().getDouble("z");

        firstPingTime = estimated_state.parser.firstLogEntry().getTimestampMillis();
        lastPingTime = estimated_state.parser.getLastEntry().getTimestampMillis();

        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        timeline = new Timeline(0, (int) (lastPingTime - firstPingTime), 30, 1000, false);
        timeline.getSlider().setValue(0);
        timeline.addTimelineChangeListener(this);
        timeline.getSlider().setUI(new BasicSliderUI(timeline.getSlider()) {
            @Override
            public void paintTicks(Graphics g) {
                super.paintTicks(g);
            }
        });

        // Timeline location inside the layout
        add(timeline, "w 100%, h 30px, dock south");
    }

    public void createVtkWindow() {
        // Vtk Canvas extended
        canvas = new Canvas();
        canvas.LightFollowCameraOn();
        canvas.setEnabled(true);

        winCanvas = new Window(canvas, interactorStyle, linkedHashMapCloud, linkedHashMapMesh, source) {
            @Override
            public void setUpRenderer() {
                getRenderer().SetGradientBackground(true);
                getRenderer().SetBackground(0.0, 0.0, 0.0);
                getRenderer().SetBackground2(0.5, 0.5, 0.5);
            }
        };

        interactorStyle = winCanvas.getInteracStyle();
        events = interactorStyle.getEventsHandler();

        add(canvas, "w 100%, dock center");
    }

    public void loadMapToPointCloud() {
        
        // Read the map file (list NED format)
        try {
            String mapFile = FileUtil.getResourceAsFileKeepName(FileUtil.getPackageAsPath(FilterMra.this) + "/Maps/carte_mbp.txt");
            InputStream ips = new FileInputStream(mapFile);

            InputStreamReader ipsr = new InputStreamReader(ips);
            BufferedReader br = new BufferedReader(ipsr);
            String ligne;

            map_ref = new CMaps();
            map_ref.pointCloud = new PointCloudXYZ();

            map_ref.offset_checked = 0;
            
            // Get vertical offset if exist
            for (IMCMessage m : source.getLsfIndex().getIterator("CorrectedState", 0, 1000)) {
                map_ref.offset_checked = (float) m.getValue("alt");
                if(map_ref.offset_checked!=0)
                {
                    break;
                }
            }
            
            // Read header
            if ((ligne = br.readLine()) != null) { // LLH Offset
                String[] str_point = ligne.split("[,:]");
                map_ref.latitude = Double.parseDouble(str_point[1]);
                map_ref.longitude = Double.parseDouble(str_point[2]);
                map_ref.height = Double.parseDouble(str_point[3]);
            }

            if ((ligne = br.readLine()) != null) { // NED Offset
                String[] str_point = ligne.split("[,:]");
                map_ref.north = Double.parseDouble(str_point[1]);
                map_ref.east = Double.parseDouble(str_point[2]);
                map_ref.down = Double.parseDouble(str_point[3]);
            }

            if ((ligne = br.readLine()) != null) { // Number of Point
                String[] str_point = ligne.split("[,:]");
                map_ref.num_point = (int) Double.parseDouble(str_point[1]);
            }

            vtkPoints pts = new vtkPoints();

            // Difference between map LLH and current Log LLH
            double[] offsetLatLon = CoordinateUtil.latLonDiff(Math.toDegrees(map_ref.latitude),
                    Math.toDegrees(map_ref.longitude), Math.toDegrees(estimated_state.latitude),
                    Math.toDegrees(estimated_state.longitude));

            // Difference between the reference height of the map with the reference height of the log
            double offsetHeight = (map_ref.height - map_ref.down) - (estimated_state.height - estimated_state.down);

            // Read map pointcloud
            while ((ligne = br.readLine()) != null) {
                String[] str_point = ligne.split("[,]");
                double pos_x = Double.parseDouble(str_point[0]) - offsetLatLon[0];
                double pos_y = Double.parseDouble(str_point[1]) - offsetLatLon[1];
                double pos_z = Double.parseDouble(str_point[2]) - offsetHeight + map_ref.offset_checked;
                pts.InsertNextPoint(pos_x, pos_y, pos_z);
            }

            br.close();

            // Conditioning the points XYZ in VTK pointcloud
            map_ref.pointCloud.setNumberOfPoints(map_ref.num_point);
            map_ref.pointCloud.setXYZPoints(pts);
            map_ref.pointCloud.createActorFromPoints();
            map_ref.pointCloud.generateHandler();
            map_ref.pointCloud.getPolyData().GetPointData()
                    .SetScalars(((PointCloudHandlerXYZ) (map_ref.pointCloud.getColorHandler())).getColorsZ());

            // Surface map representation (is not indispensable but it's class^^)
            Delauny2D delauny = new Delauny2D();
            delauny.performDelauny(map_ref.pointCloud);

            PointCloudMesh mesh = new PointCloudMesh();
            mesh.generateLODActorFromPolyData(delauny.getPolyData());

            // Smoothing mesh generation (so class^^)
            MeshSmoothingLaplacian smoothing = new MeshSmoothingLaplacian();
            smoothing.performProcessing(mesh);

            mesh.setPolyData(new vtkPolyData());
            mesh.generateLODActorFromPolyData(smoothing.getPolyData());

            // Define the type of interaction
            events.setSensorTypeInteraction(SensorTypeInteraction.ALL);

            // Add the map to the scene
            if (map_ref.pointCloud.getNumberOfPoints() != 0) {
                // Add the map inside the scene
                canvas.GetRenderer().AddActor(mesh.getMeshCloudLODActor());

                // Add the map to the list
                idActorList.AddActor("Map");

                // Add and display the color scalar bar (winCanvas is used (UI canvas))
                interactorStyle.getScalarBar().setUpScalarBarLookupTable(
                        ((PointCloudHandlerXYZ) map_ref.pointCloud.getColorHandler()).getLutZ());
                canvas.GetRenderer().AddActor(winCanvas.getInteracStyle().getScalarBar().getScalarBarActor());
            }
        }
        catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public void loadDVLPointCloud() {
        
        vtkPoints pts = new vtkPoints();
        int num_dvl_point = 0;
        
        BathymetryParser parser = BathymetryParserFactory.build(this.source, "dvl");
        IMraLog estimated_state_parser = source.getLog("EstimatedState");     

        if (parser instanceof DVLBathymetryParser) {
            parser.rewind();
            BathymetrySwath bs;

            LocationType initLoc = null;

            while ((bs = parser.nextSwath()) != null) {
                LocationType loc = bs.getPose().getPosition();
                if (initLoc == null) {
                    initLoc = new LocationType(loc);
                    
                    // Offset Correction, we get the first location correspond to the DVL acquisition beginning
                    double init_offset_north = (double) estimated_state_parser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("x");
                    double init_offset_east  = (double) estimated_state_parser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("y");
                    double init_offset_down  = (double) estimated_state_parser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("z");
                    
                    initLoc.setOffsetNorth(-init_offset_north);
                    initLoc.setOffsetEast(-init_offset_east);                   
                    initLoc.setOffsetDown(init_offset_down);    
                }

                for (int c = 0; c < bs.getNumBeams(); ++c) {
                    BathymetryPoint p = bs.getData()[c];
                    if (p == null)
                        continue;

                    LocationType tempLoc = new LocationType(loc);
                    tempLoc.translatePosition(p.north, p.east, 0);

                    double data[] = tempLoc.getOffsetFrom(initLoc);
                    pts.InsertNextPoint(data[0] + initLoc.getOffsetNorth(), data[1] + initLoc.getOffsetEast(),
                            p.depth);

                    ++num_dvl_point;
                }
            }
        }

        // Conditioning the particle list XYZ in VTK PointCloud table
        APointCloud<?> pointcloud_dvl = new PointCloudXYZ();
        pointcloud_dvl.setNumberOfPoints(num_dvl_point);
        pointcloud_dvl.setXYZPoints(pts);
        pointcloud_dvl.createActorFromPoints();
        pointcloud_dvl.generateHandler();

        // Add the particles blocs to the scene
        pointcloud_dvl.getCloudLODActor().VisibilityOn();
        pointcloud_dvl.getCloudLODActor().GetProperty().SetColor(0.0, 0.2, 0.7);
        pointcloud_dvl.getCloudLODActor().GetProperty().SetPointSize(2);
        canvas.GetRenderer().AddActor(pointcloud_dvl.getCloudLODActor());

        // Add the particles blocs to the list
        idActorList.AddActor("DVL_Cloud");
    }
    
    private void loadMBSPointCloud() {
        vtkPoints pts = new vtkPoints();
        int num_mbs_point = 0;
        
        BathymetryParser parser = BathymetryParserFactory.build(this.source, "multibeam");
        IMraLog estimated_state_parser = source.getLog("EstimatedState");        
        
        if (parser instanceof DeltaTParser) {
            
            parser.rewind();
            BathymetrySwath bs;

            LocationType initLoc = null;

            while ((bs = parser.nextSwath()) != null) {
                LocationType loc = bs.getPose().getPosition();
                if (initLoc == null) {
                    initLoc = new LocationType(loc);
                    
                    // Offset Correction, we get the first location correspond to the MBS acquisition beginning
                    double init_offset_north = (double) estimated_state_parser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("x");
                    double init_offset_east  = (double) estimated_state_parser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("y");
                    double init_offset_down  = (double) estimated_state_parser.getEntryAtOrAfter(bs.getTimestamp()).getDouble("z");
                    
                    initLoc.setOffsetNorth(-init_offset_north);
                    initLoc.setOffsetEast(-init_offset_east);                   
                    initLoc.setOffsetDown(init_offset_down);  
                }

                for (int c = 0; c < bs.getNumBeams(); ++c) {
                    BathymetryPoint p = bs.getData()[c];
                    if (p == null)
                        continue;

                    LocationType tempLoc = new LocationType(loc);
                    tempLoc.translatePosition(p.north, p.east, 0);

                    double data[] = tempLoc.getOffsetFrom(initLoc);
                    pts.InsertNextPoint(data[0], data[1], p.depth);

                    ++num_mbs_point;
                }
            }
        }

        // Conditioning the particle list XYZ in VTK PointCloud table
        APointCloud<?> pointcloud_mbs = new PointCloudXYZ();
        pointcloud_mbs.setNumberOfPoints(num_mbs_point);
        pointcloud_mbs.setXYZPoints(pts);
        pointcloud_mbs.createActorFromPoints();
        pointcloud_mbs.generateHandler();

        // Add the particles blocs to the scene
        pointcloud_mbs.getCloudLODActor().VisibilityOn();
        pointcloud_mbs.getCloudLODActor().GetProperty().SetColor(0.0, 0.7, 0.2);
        pointcloud_mbs.getCloudLODActor().GetProperty().SetPointSize(2);
        canvas.GetRenderer().AddActor(pointcloud_mbs.getCloudLODActor());

        // Add the particles blocs to the list
        idActorList.AddActor("MBS_Cloud");
    }
    
    private void loadLauvModel() {
        // Load the LAUV 3D model (vtk file)
        vtkGenericDataObjectReader vtk_generic_reader = new vtkGenericDataObjectReader();
        String nopModelFile = FileUtil.getResourceAsFileKeepName(FileUtil.getPackageAsPath(FilterMra.this) + "/Models/noptilus.vtk");
        vtk_generic_reader.SetFileName(nopModelFile);
        vtk_generic_reader.ReadAllNormalsOn();
        vtk_generic_reader.ReadAllScalarsOn();
        vtk_generic_reader.ReadAllVectorsOn();
        vtk_generic_reader.ReadAllColorScalarsOn();
        vtk_generic_reader.ReadAllFieldsOn();
        vtk_generic_reader.Update();

        vtkPolyData polydata = vtk_generic_reader.GetPolyDataOutput();

        vtkUnsignedCharArray color = (vtkUnsignedCharArray) polydata.GetPointData().GetArray("RGB");
        vtkDataArray normal = polydata.GetPointData().GetArray("Normals");

        polydata.GetPointData().SetNormals(normal);
        polydata.GetPointData().SetScalars(color);

        // Transform the source in function to the Z exaggeration
        vtkTransform transform = new vtkTransform();
        transform.Scale(1.0, 1.0, 1.0 / MRAProperties.zExaggeration);

        vtkTransformFilter transformfilter = new vtkTransformFilter();
        transformfilter.SetTransform(transform);
        transformfilter.SetInputConnection(polydata.GetProducerPort());

        vtkPolyDataMapper mapper = new vtkPolyDataMapper();
        mapper.SetInputConnection(transformfilter.GetOutputPort());
        vtkActor actor = new vtkActor();
        actor.SetMapper(mapper);
        actor.SetPosition(estimated_state.position.get(0));
        actor.SetOrientation(estimated_state.orientation.get(0));

        // Add each line to the scene
        canvas.GetRenderer().AddActor(actor);
        idActorList.AddActor("NoptilusModel");
    }
    
    private void loadEstimatedState() {
        
        vtkPoints pts_trajectory = new vtkPoints();
        estimated_state.trajectory = new ArrayList<vtkLineSource>();
       
        int k = 0;
        double x = 0;
        double y = 0;
        double depth = 0;
        double phi = 0;
        double theta = 0;
        double psi = 0;

        // Get the estimated state list
        for (IMCMessage m : source.getLsfIndex().getIterator("EstimatedState", 0, 1000)) {
            x = (float) m.getValue("x");
            y = (float) m.getValue("y");
            depth = (float) m.getValue("depth");

            pts_trajectory.InsertNextPoint(x, y, depth);

            phi = (float) m.getValue("phi");
            theta = (float) m.getValue("theta");
            psi = (float) m.getValue("psi");

            double position[] = { x, y, depth * MRAProperties.zExaggeration };
            estimated_state.position.add(position);

            double orientation[] = { Math.toDegrees(phi), Math.toDegrees(theta), Math.toDegrees(psi) };
            estimated_state.orientation.add(orientation);
            
            Long timestamp = m.getTimestampMillis();
            estimated_state.timestamp.add(timestamp);

            k++;
        }
        estimated_state.kt = k;

        // Create and convert vtkLine in actor inside the scene
        for (int i = 1; i < estimated_state.kt; i++) {
            // Create each line of the trajectory
            vtkLineSource line_temp = new vtkLineSource();
            line_temp.SetPoint1(pts_trajectory.GetPoint(i - 1));
            line_temp.SetPoint2(pts_trajectory.GetPoint(i));
            estimated_state.trajectory.add(line_temp);

            // Actor mapping
            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(line_temp.GetOutputPort());
            vtkActor actor = new vtkActor();
            actor.SetMapper(mapper);
            actor.GetProperty().SetLineWidth(1);
            actor.GetProperty().SetColor(1.0, 0.0, 0.0);
            // actor.VisibilityOff();

            // Add each line to the scene
            canvas.GetRenderer().AddActor(actor);

            // Add the estimated trajectory to the list
            idActorList.AddActor("PathEstimated");
        }
    }
    
    private void loadCorrectedState() {

        IMraLog estimated_state_parser = source.getLog("EstimatedState");
        long last_timestamp = estimated_state_parser.getLastEntry().getTimestampMillis();
        
        corrected_state = new CCorrectedState();
        
        // Corrected State Structure Initialization
        int k = 0;
        vtkPoints pts_trajectory = new vtkPoints();
        
        for (IMCMessage m : source.getLsfIndex().getIterator("CorrectedState", 0, 1000)) {

            // Point defining the corrected trajectory
            double x = (float) m.getValue("x");
            double y = (float) m.getValue("y");
            double depth = (float) m.getValue("depth");
            
            // Point is added only if it's valid
            if(x!=0 && y!=0)
            {
                if(m.getTimestampMillis() < last_timestamp) {
                    // Coord of valid point
                    pts_trajectory.InsertNextPoint(x, y, depth);
                    
                    // Get the timestamp for each valid point 
                    Long timestamp = m.getTimestampMillis();
                    corrected_state.timestamp.add(timestamp); 
                    
                    k++;
                }
            }
        }
        
        // No corrected state
        if(k==0){
            corrected_state.enable = 0;
            return;
        }
        
        // Number of corrected state valid
        corrected_state.kt = k;
        corrected_state.trajectory = new ArrayList<vtkLineSource>();

        for (int i = 1; i < corrected_state.kt; i++) {
            // Create each line of the trajectory
            vtkLineSource line_temp = new vtkLineSource();
            line_temp.SetPoint1(pts_trajectory.GetPoint(i - 1));
            line_temp.SetPoint2(pts_trajectory.GetPoint(i));
            corrected_state.trajectory.add(line_temp);

            // Actor mapping
            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(line_temp.GetOutputPort());
            vtkActor actor = new vtkActor();
            actor.SetMapper(mapper);
            actor.GetProperty().SetLineWidth(1);
            actor.GetProperty().SetColor(0.0, 0.0, 1.0);
            // actor.VisibilityOff();

            // Add each line to the scene
            canvas.GetRenderer().AddActor(actor);

            // Add the corrected trajectory to the list
            idActorList.AddActor("PathCorrected");
        }
    }
    
    private void loadParticles() {
        IMraLog estimated_state_parser = source.getLog("EstimatedState");
        long last_timestamp = estimated_state_parser.getLastEntry().getTimestampMillis();
        
        particles = new CParticles();
        
        int k = 0;
        for (IMCMessage m : source.getLsfIndex().getIterator("DataParticle", 0, 1000)) {
                       
            int num_particle = (int) m.getValue("num");
            byte[] tab_data;
            tab_data = (byte[]) m.getValue("data");
    
            double x, y, d, w;           
    
            String str_data = "";
    
            for (int i = 0; i < tab_data.length; i++) {
                str_data += (char) tab_data[i];
            }
    
            String[] list_str = str_data.split(" ");
    
            int i = 0;
    
            vtkPoints pts = new vtkPoints();
    
            double wmax = -10;
            double wmin = 10;
    
            for (int n = 0; n < num_particle; n++) {
                x = Double.parseDouble(list_str[i]);
                y = Double.parseDouble(list_str[i + 1]);
                d = Double.parseDouble(list_str[i + 2]);
                w = Double.parseDouble(list_str[i + 3]);
    
                // Min/Max Weight for normalization
                if (wmax < w) {
                    wmax = w;
                }
                if (wmin > w) {
                    wmin = w;
                }
    
                i = i + 4;
    
                // Particle position
                pts.InsertNextPoint(x, y, d);
            }
    
            // Colors attribution
            vtkUnsignedCharArray colors = new vtkUnsignedCharArray();
            colors.SetNumberOfComponents(3);
            colors.SetName("Colors");
    
            // Normals attribution
            vtkDoubleArray normal = new vtkDoubleArray();
            normal.SetNumberOfComponents(3);
            normal.SetName("Normals");
    
            // Color Intensity format
            ColorMap cm = ColorMapFactory.createWhiteColorMap();
            
            // Get Vehicle Orientation
            if(m.getTimestampMillis() < last_timestamp)
            {
                Long timestamp = m.getTimestampMillis();
                particles.timestamp.add(timestamp);
                
                double direction = (double) estimated_state_parser.getEntryAtOrAfter(m.getTimestampMillis()).getDouble("psi");
    
                i = 0;
                for (int n = 0; n < num_particle; n++) {
                    w = Double.parseDouble(list_str[i + 3]);
                    i = i + 4;
        
                    // Weight normalization (0->wmax : 0->1)
                    w = w / wmax;
        
                    // Color Intensity function of particle weight
                    colors.InsertNextTuple3(cm.getColor(w).getRed(), cm.getColor(w).getGreen(), cm.getColor(w).getBlue());
        
                    // Normals direction
                    normal.InsertNextTuple3(Math.cos(direction),
                            Math.sin(direction), 0.0);
                }
        
                // Define the polydata
                vtkPolyData polydata = new vtkPolyData();
                polydata.SetPoints(pts);
                polydata.GetPointData().SetScalars(colors);
                polydata.GetPointData().SetNormals(normal);
        
                // Define the source associates to the glyph
                vtkArrowSource arrow = new vtkArrowSource();
                arrow.SetTipResolution(3);
                arrow.SetShaftResolution(4);
        
                // Transform the source in function to the Z exaggeration
                vtkTransform transform = new vtkTransform();
                transform.Scale(1.0, 1.0, 1.0 / MRAProperties.zExaggeration);
        
                vtkTransformFilter transformfilter = new vtkTransformFilter();
                transformfilter.SetTransform(transform);
                transformfilter.SetInputConnection(arrow.GetOutputPort());
        
                // Create and define the glyph
                vtkGlyph3D glyph = new vtkGlyph3D();
        
                glyph.SetColorModeToColorByScalar();
        
                glyph.OrientOn();
                glyph.SetVectorModeToUseNormal();
        
                glyph.ScalingOn();
                glyph.SetScaleFactor(0.002);
        
                glyph.SetSourceConnection(transformfilter.GetOutputPort());
                glyph.SetInput(polydata);
        
                glyph.Update();
        
                // Actor mapping
                vtkPolyDataMapper mapper = new vtkPolyDataMapper();
                mapper.SetInputConnection(glyph.GetOutputPort());
                vtkActor actor = new vtkActor();
                actor.SetMapper(mapper);
                // actor.VisibilityOff();
        
                // Add each line to the scene
                canvas.GetRenderer().AddActor(actor);
        
                // Add the particles blocs to the list
                idActorList.AddActor("Particle");
                k++;
            }
        }
        
        // No particle
        if(k==0){
            particles.enable = 0;
            return;
        }
        
        if(k>=1){
            particles.iteration_time = (particles.timestamp.get(2)-particles.timestamp.get(1));
        }
        particles.kt = k;
    }
    
    
    private void loadReferenceTrajectory() {
        IMraLog estimated_state_parser = source.getLog("EstimatedState");
        long last_timestamp = estimated_state_parser.getLastEntry().getTimestampMillis();
        
        // Corrected State Structure Initialization
        int k = 0;
        vtkPoints pts_trajectory = new vtkPoints();
        
        for (IMCMessage m : source.getLsfIndex().getIterator("SensoriMotorState", 0, 1000)) {

            // Point defining the corrected trajectory
            double x = (float) m.getMessage("EstimatedState").getValue("x");
            double y = (float) m.getMessage("EstimatedState").getValue("y");
            double depth = (float) m.getMessage("EstimatedState").getValue("depth");
            
            // Point is added only if it's valid
            if(x!=0 && y!=0)
            {
                if(m.getTimestampMillis() < last_timestamp) {
                    // Coord of valid point
                    pts_trajectory.InsertNextPoint(x, y, depth);
                    k++;
                }
            }
        }
        
        // No reference trajectory
        if(k<=1){
            return;
        }

        for (int i = 1; i < k; i++) {
            // Create each line of the trajectory
            vtkLineSource line_temp = new vtkLineSource();
            line_temp.SetPoint1(pts_trajectory.GetPoint(i - 1));
            line_temp.SetPoint2(pts_trajectory.GetPoint(i));

            // Actor mapping
            vtkPolyDataMapper mapper = new vtkPolyDataMapper();
            mapper.SetInputConnection(line_temp.GetOutputPort());
            vtkActor actor = new vtkActor();
            actor.SetMapper(mapper);
            actor.GetProperty().SetLineWidth(3);
            actor.GetProperty().SetColor(0.0, 1.0, 0.0);
            // actor.VisibilityOff();

            // Add each line to the scene
            canvas.GetRenderer().AddActor(actor);

            // Add the corrected trajectory to the list
            idActorList.AddActor("Reference_trajectory");
        }
    }
    
    private void sceneScaleAxesExageration(double Sx, double Sy, double Sz) {
        vtkActorCollection actor_list = canvas.GetRenderer().GetActors();
        actor_list.InitTraversal();

        for (int i = 0; i < actor_list.GetNumberOfItems(); i++) {
            actor_list.GetNextItem().SetScale(Sx, Sy, Sz);
        }
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {

        if (!componentEnabled) {
            componentEnabled = true;

            this.source = source;
            this.timestep = timestep;

            // Main layout
            setLayout(new MigLayout());

            // 3D Scene Actor identifier
            idActorList = new C3DListElement();

            // Create and configure the MRA panel (timeline and vtk window)
            createTimeline();
            createVtkWindow();

            // Load all 3D object in the vtk 3d buffer
            loadMapToPointCloud();           
            loadDVLPointCloud();
            loadMBSPointCloud();
            loadEstimatedState();
            loadLauvModel();    
            loadCorrectedState();
            loadParticles();
            
            loadReferenceTrajectory();

            // Control adequacy between actor present inside the scene and our list of actor
            vtkActorCollection vtk_actor_list = canvas.GetRenderer().GetActors();
            if (idActorList.num_actor != vtk_actor_list.GetNumberOfItems()) {
                return null;
            }

            // For all object of the 3D scene (warning all must be load)
            sceneScaleAxesExageration(1.0, 1.0, MRAProperties.zExaggeration);
        }
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        if (!Utils.hasTryedToLoadVtkLib) {
            Utils.loadVTKLibraries();
        }

        return NeptusMRA.vtkEnabled;
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(this.getClass()), 16, 16);
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public String getName() {
        return I18n.text(PluginUtils.getPluginName(this.getClass()));
    }

    @Override
    public void onHide() {
        timeline.pause();
    }

    @Override
    public void onShow() {
        if (isFirstRender) {
            canvas.GetRenderer().GetActiveCamera().SetPosition(0.0, 0.0, -30.0);
            canvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, 1.0, -1.0);

            canvas.GetRenderWindow().SetCurrentCursor(9);
            canvas.RenderSecured();
            canvas.GetRenderer().ResetCamera();

            isFirstRender = false;
        }
    }

    @Override
    public void onCleanup() {
        mraPanel = null;
        if (canvas != null)
            canvas.Delete();
        
        removeAll();
    }

    @SuppressWarnings("unused")
    private void computeCameraView(int kt, double height, double distance) {
        // Delta position (distance in the robot axes and height)
        double dx = distance * Math.cos(Math.toRadians(estimated_state.orientation.get(kt)[2]));
        double dy = distance * Math.sin(Math.toRadians(estimated_state.orientation.get(kt)[2]));
        double dz = height;

        // Position of the camera
        double x0 = estimated_state.position.get(kt)[0] - dx;
        double y0 = estimated_state.position.get(kt)[1] - dy;
        double z0 = estimated_state.position.get(kt)[2] - dz;

        // Point observed by the camera
        double x1 = estimated_state.position.get(kt)[0];
        double y1 = estimated_state.position.get(kt)[1];
        double z1 = estimated_state.position.get(kt)[2];

        // Norm of vector between both previous point
        double norme_p0p1 = Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1) + (z0 - z1) * (z0 - z1));

        double n_x = (x0 - x1) / norme_p0p1;
        double n_y = (y0 - y1) / norme_p0p1;
        double n_z = (z0 - z1) / norme_p0p1;

        double norme_n = Math.sqrt(n_x * n_x + n_y * n_y + n_z * n_z);

        // Vector normalized (focus axe)
        double w_x = n_x / norme_n;
        double w_y = n_y / norme_n;
        double w_z = n_z / norme_n;

        // Vector Up for the scene
        double up_x = 0;
        double up_y = 0;
        double up_z = -1.0;

        // Cross product -> Vector resulting (right axe)
        double up_n_x = up_y * n_z - up_z * n_y;
        double up_n_y = up_z * n_x - up_x * n_z;
        double up_n_z = up_x * n_y - up_y * n_z;

        double norme_up_n = Math.sqrt(up_n_x * up_n_x + up_n_y * up_n_y + up_n_z * up_n_z);

        // Vector right normalized
        double u_x = up_n_x / norme_up_n;
        double u_y = up_n_y / norme_up_n;
        double u_z = up_n_z / norme_up_n;

        // Cross product forward-right -> Vector up resulting
        double v_x = w_y * u_z - w_z * u_y;
        double v_y = w_z * u_x - w_x * u_z;
        double v_z = w_x * u_y - w_y * u_x;

        canvas.GetRenderer().GetActiveCamera().SetPosition(x0, y0, z0);

        canvas.GetRenderer().GetActiveCamera().SetFocalPoint(x1, y1, z1);

        canvas.GetRenderer().GetActiveCamera().SetViewUp(v_x, v_y, v_z);
    }

    @Override
    public void timelineChanged(int value) {

        try {

            currentTime = value;

            // Slider's end
            if (currentTime + firstPingTime >= lastPingTime) {
                timeline.pause();
            }

            // Update timeline
            timeline.setTime(firstPingTime + currentTime);

            // Id Iteration
            kTActual = (int) ((double) (estimated_state.kt) * (double) (currentTime) / (double) (lastPingTime - firstPingTime));
            
            if(kTActual > estimated_state.kt - 1) {
                kTActual =estimated_state.kt - 1; 
            }
            
            // Update the dynamic 3D mesh state
            vtkActorCollection vtkActorList = canvas.GetRenderer().GetActors();
            vtkActorList.InitTraversal();

            for (int n = 0; n < vtkActorList.GetNumberOfItems(); n++) {
                vtkActor actor = vtkActorList.GetNextItem();
                if (idActorList.GetDescription(n) == "NoptilusModel") {
                    actor.SetPosition(estimated_state.position.get(kTActual));
                    actor.SetOrientation(estimated_state.orientation.get(kTActual));
                }
            }
            
            // The camera tracking is operational only for the normal reading
            //if((kT_actual-kT_preview)==1) { ComputeCameraView(kT_actual, 40.0, 70.0); }
            
            // Update the static 3D mesh preprocessed
            if ((kTActual - kTPreview) >= 1) {

                // Visible only until time position
                vtkActorList = canvas.GetRenderer().GetActors();
                vtkActorList.InitTraversal();

                int k_particle = 0;
                int k_corrected = 0;
                int k_estimated = 0;

                for (int n = 0; n < vtkActorList.GetNumberOfItems(); n++) {
                    vtkActor actor = vtkActorList.GetNextItem();
                    
                    if (idActorList.GetDescription(n) == "Particle") {
                        if ( (particles.timestamp.get(k_particle)<=estimated_state.timestamp.get(kTActual))
                                &&(estimated_state.timestamp.get(kTActual)-particles.timestamp.get(k_particle)<particles.iteration_time)){
                            actor.VisibilityOn();
                        }
                        else {
                            actor.VisibilityOff();
                        }
                        k_particle++;
                    }

                    if (idActorList.GetDescription(n) == "PathCorrected") {
                        if (corrected_state.timestamp.get(k_corrected) < estimated_state.timestamp.get(kTActual)) {
                            actor.VisibilityOn();
                        }
                        else {
                            actor.VisibilityOff();
                        }
                        k_corrected++;
                    }

                    if (idActorList.GetDescription(n) == "PathEstimated") {
                        if (k_estimated < kTActual) {
                            actor.VisibilityOn();
                        }
                        else {
                            actor.VisibilityOff();
                        }
                        k_estimated++;
                    }
                }
            }
            else {
                if ((kTActual - kTPreview) < 0) {
                    // Visible only until time position
                    vtkActorList = canvas.GetRenderer().GetActors();
                    vtkActorList.InitTraversal();

                    int k_particle = 0;
                    int k_corrected = 0;
                    int k_estimated = 0;
                    
                    for (int n = 0; n < vtkActorList.GetNumberOfItems(); n++) {
                        vtkActor actor = vtkActorList.GetNextItem();

                        // Display only the current cloud particle
                        if (idActorList.GetDescription(n) == "Particle") {

                                if (   (particles.timestamp.get(k_particle)<=estimated_state.timestamp.get(kTActual))
                                     &&(estimated_state.timestamp.get(kTActual)-particles.timestamp.get(k_particle)<particles.iteration_time) ) {
                                    actor.VisibilityOn();
                                }
                                else {
                                    actor.VisibilityOff();
                                }
                                k_particle++;
                        }

                        // Display all corrected trajectories before kT actual
                        if (idActorList.GetDescription(n) == "PathCorrected") {
                            if (corrected_state.timestamp.get(k_corrected) < estimated_state.timestamp.get(kTActual)) {
                                actor.VisibilityOn();
                            }
                            else {
                                actor.VisibilityOff();
                            }
                            k_corrected++;
                        }

                        // Display all estimated trajectories before kT actual
                        if (idActorList.GetDescription(n) == "PathEstimated") {
                            if (k_estimated < kTActual) {
                                actor.VisibilityOn();
                            }
                            else {
                                actor.VisibilityOff();
                            }
                            k_estimated++;
                        }
                    }
                }
            }

            kTPreview = kTActual;

            // Frame Refresh
            canvas.RenderSecured();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
