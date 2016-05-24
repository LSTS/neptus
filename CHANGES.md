Changes
=======

Neptus 4.3.0 (17-05-2016)
------------
 * IMC: Updated IMC (e45bef5) and IMCJava (828667d) (v5.4.8.x).
 * Vehicles: Updated systems parameters to DUNE (dune-2016.05.0-dmsmw, 5a841ca).
 * I18n: Updated POT and translations.
 * |Too many changes to list here, check git log.|

 Prev changes
 
 * MRA/Sidescan Zoom: New ruller for zoomed image.
 * SPOT: Stream ID on the URL is now a configurable parameter.
 * DropMapLayer: Added new interaction for dropping of payload.
 * MRA/Sidescan Zoom: Added support for better sidescan zoom (active if not waterfalling).
 * Console/ROV: Improved ROVInfoLayer and DistancesRadar to be a console layer or a panel (popup dialog).
 * MRA/SDFParser: Added support for Klein sidescan SDF format.
 * MRA/DeltaTParser: Fix reading of soundspeed.
 * MRA/Exporter/DeltaT83PToCorrectedPos83P: Fix to write lat/lon to the right bits in the header.
 * MRA/JsfSidescanParser: Fixed ArrayIndexOutOfBoundsException when Sidescan Analyzer Visualization's timeline ended.


Neptus 4.2.1 (07-01-2015)
------------
 * IMC: No change in IMC and IMCJava (v5.4.3).
 * Vehicles: Updated systems parameters to DUNE 2.6.1 (HEAD,0c854de).
 * I18n: Updated POT and translations.
 * GeneralPreferences: Fix a problem on general preferences creation on the right folder for the first time.
 * MRA/Sidescan: Implementing fix for sidescan data log files still being open after closing MRA window or log close.
 * PlanEditor: Fixed missing transitions after adding a maneuver before the first one.
 * Installers: Added LAUV-Oceaneco 1 and 2 to LE installer includes.
 * MRA/Sidescan: Added null pointer protection to the sidescan recorder when closing streams in SidescanPanel.
 * MRA/JSF Parser: Reading data format from header in order to only read if equals to 0 (1 short per sample, was the format supported but if other was in place the processing was done if equal to 0 format). If not valid format it will return empty data array.
 * MRA/JSF Parser: Fixing reading of number of samples (need to get 4 bits of MSB bytes to be extend the number of samples to 20bit size).


Neptus 4.2.0 (11-11-2014)
------------
 * IMC: No change in IMC and IMCJava (v5.4.3).
 * Vehicles: Updated systems parameters to DUNE 2.6.0 (HEAD,660f19f).
 * I18n: Updated POT and translations.
 * MRA/UAVModePlot: Adding PiePlot of AutopilotMode.
 * Ant/Installers: Adding urready4os.jar and waveglider.jar to excludes for Seacon installer.
 * Ant/Installers: Fix the includes/excludes file names for consistency.
 * MRA: When a log is reindexed, the entire mra/ folder gets deleted.
 * MRA/CorrectedPosition: Added synchronized block for accessing CorrectedPosition data.
 * MRA/CorrectedPosition: Add any non-corrected positions to the list of corrected" positions.
 * MRA/NoptilusMapExporter: Little fix in finding if multibeam data exists.
 * Vehicles: Updated NTNU vehicles IMC IDs.
 * PeriodicUpdatesService: Fixed old clients still being called after calling unregister().
 * MRA/NoptilusMapExporter: Re-enabled plug-in.
 * ConvCao: ConvCao NeptusInteraction: Always send a positive loiter radius if references are underwater; and Stop displaying the references if convcao algorithm is stopped.
 * ConvCao: Changed defaults for Noptilus coordinates.
 * Convcao: Whan acoustic transmission is selected, try to send via wifi as well.
 * ConvCao: Fixed rotation of the square when the map is rotated.
 * MRA/Plots/RealTimePlot: Real-time plot now displays data from multiple vehicles.
 * MissionType: Added time it took to save and caller name to the output.
 * SystemsList/SystemsParameters: Now the systems parameters are updated.
 * Map/StateRenderer2D/Transponders: Removed the TransponderSecurityArea layer (no more drawing the optimal LBL area).
 * MRA/JsfSidescanParser: Fixing processing to handle just one side data, port or starboard.
 * Maneuvers: Adding Dislodge maneuver support.
 * Plan/PlanElement: Fixing a long lived bug that for non located maneuvers, their position on the map was always jumping.
 * LBLRangeDisplay: Protecting against null pointer.
 * GuiUtils: Added missing I18n strings.
 * ImuAlignmentPanel: Changed the icon in the button meaning. If IMU is enabled the icon will be green, if not red, and gray otherwise. Alignment state will be shown in the text area.
 * ImuAlignmentPanel: If IMU is enabled the button text will express that and the text will chenge from "Enable IMU" to "IMU Enabled". If IMU is aligned the text color will be green.
 * PlanControlPanel: Adding some feedback for AcousticOperation sent messages.
 * MantaOperations: Fixing I18n strings.
 * MRA: Changed the visualizations that get opened by default.
 * MRA: Added possibility to activate / deactivate automatic visualizations (from settings Menu).
 * MRA Replay: When replay is hidden, it gets paused and all its popups are closed.
 * MRA: Fixed markers and time of day in MRA Gantt plots.
 * DownloaderPanel: Right-clicking a downloaded log shows the option "Open in MRA" instead of opening directly.
 * MRA/Plots: Fixed timezone conversions (related with marks bug).
 * Checklist: Adding missing classes and jar to account for the GeneralPreferences addition to ChecklistPanel.
 * Workspace/Checklist/GeneralPreferences: I18n addition.
 * Ant: Fix worldmap bundle jar creation dependency.
 * PlanSimulationLayer: Added missing translations.
 * PlanSimulationLayer: Fixed the calculation of total execution time.
 * LblRangeDisplay: Removed unnecessary loop.
 * PlanEditor: Improved Undo/Redo support in plan edition.
 * GraphType: Fixing bug in transition removal.
 * Installers: Removing urready4os.jar from light version.
 * MRA/Importers/JsfParser: Adding end of file protection.
 * PlanEditor: Adding null pointer protections that arose in editing a plan.
 * ImcSystemsHolder: Names are to be updated from Announce (on first creation name is not known). To be revised the ImcSystem creation to be only on announce arrival.
 * Installers: Fix a problem with icon on desktop launcher for Linux (no spaces allowed in the path).
 * LogsDownloader/QueueWorkTickets: Some cleanups on releasing the lock. On stop all force releasing of all loks.
 * MantaOperations: Fix a way to get the systems list from AcousticSystems without the unit postfixed (unit is "list"). Probably change the IMCJava side (to see).
 * LogsDownloaderWorker: Fix a hidden bug because the source of messages for checking for camera activeness was not done.
 * MRA/SidescanConfig: Moved to core src and to package pt.lsts.neptus.console.plugins.PropertiesProviders due to its use in core src LsfReport (making a non compilable for distribution possible).
 * MRA/SidescanPanel: Restoring some measurement features in sidescan while zooming.
 * MRA/MRA2DPlot/SalinityVsDepthPlot: Protecting from a null pointer exception (still to further investigation).
 * Core/NeptusProperties: Added an unserializer for a ColorMap property.
 * MRA/SidescanPanel: Added protection from sometimes concurrent modification exception.
 * MRA/SalinityVsDepthPlot: Protecting from a null pointer exception (still to further investigation).
 * Ant: Changing installers package date to the commit and not the creation date.
 * Vehicles: Added lauv-oceaneco-1 and lauv-oceaneco-2 vehicle definition.
 * Core/About: Update developers list and tweak SCM displaying.
 * LBLRangeDisplay: Get current LblConfig index to plot ranges (using the LblConfig from vehicle and not from the mission tree).
 * Vehicles: Added lauv-dolphin-2 and lauv-dolphin-3 vehicle definition.
 * LogsDownloader: Camera CPU activation (Slave CPU) state display OK. (Using EntityState description code en_US and the current Neptus language.)
 * MRA/Markers: Adding protection in adding marker propagation. (SideScanPanel was getting an error in SalinityVsDepthPlot.addLogMarker(..).)
 * MRA/Importers: Allows opening JSF logs where the last message isn't SONAR_DATA (80).
 * Console: Adding protections in console closing cleanups and changing system out messages to log.
 * Transponders: Deleting SimpleTransponderPanel.
 * Map/Transponders: Added way for the edition and set of transponders ids to be dependent on the configuration file names.
 * MRA/Sidescan: SidescanAnalyzer: Fixed bug of applying zoom twice when zooming over bottom-right corner of the image.
 * MRA/Sidescan/MarksReport: Added support for multiple subsystems in SidescanParser. Scaling and Adjustments in table.
 * MRA/MarksReport: Better structure/doc on createTable() and getSidescanMarkImage() and convertMtoIndex
 * MRA/MarksReport: Merged feature/MarksReport. This allows to view in the PDF report the marks (also the SSS selected image).


Neptus 4.2.0-rc2 (30-09-2014)
----------------
 * IMC: No change in IMC and IMCJava (v5.4.3).
 * Vehicles: Updated systems parameters to DUNE 2.6.0-rc3 (HEAD,ecf0e21).
 * Map/Transponders: Better layout for editing a transponder.
 * Map/Transponders: Merging BeaconsConfigurations into TranspondersUtils.
 * Vehicles/Params: Fix Visibility issues. Now Visibility is layered, at any given level you are supposed to access the lower levels parameters.
 * HFRadarVisualization: Added FolderPropertyEditor as editor for File properties that are folders.
 * PropertiesEditor/RhodamineOilVisualizer: Fix Editing a File as folder in NeptusProperties.
 * Map/TransponderElement: Fixing a null pointer exception that can happen if property file is no found (still need some more investigation).
 * NMEAPlotter/AisContactDb: Fix problem decoding last field of NMEA $A-TLL sentence (heading).
 * IverPlanExporter: Replacing the Iver mission template with an updated one.
 * IverPlanExporter: Iver plans MUST be with Windows line endings (also forcing the template.mis checkout to be with this lie endings).
 * IverPlanExporter: Removed the "PT25" of iverWaypoint(..).
 * IverPlanExporter: Fix to account for different locale to all use dot as decimal separator.
 * Vehicles: Added YoYo maneuver to lupis-1.
 * Console: Added NMEAPlotter to lauv console.
 * Util/PluinUtils: When calling the validate for primitive types properties did not look for validateXXX(..) method with the primitive type as argument (used the Object ones, and so did not found one). Even so only the non array are fixed.
 * Console/RhodamineOilVisualizer: Visualizer first version.
 * FileUtil: Added getFoldersFromDisk(..).
 * FileUtil: Little adjust in name from getFilesToLoadFromDisk(..) to getFilesFromDisk(..).
 * FileUtil: Little adjust to list folders.
 * FileUtil: Moved getFilesToLoadFromDisk(..) from plugin HFRadarVisualization to FileUtil.


Neptus 4.2.0-rc1 (23-09-2014)
----------------
 * IMC: Updated IMC and IMCJava to v5.4.3.
 * Vehicles: Updated systems parameters to DUNE 2.6.x (master,f298003).
 * VehiclesParams: For now let all releases access develop parameters.
 * MRA: Fixed Corrected Position Plot missposition of marks
 * Plan: Is now able to send plans through acoustics with more than 1 character. For now is advanced configuration with value 31 (probably will not be configurable in the near future).
 * IMC: Using the more generic Iterable<T> instead of implementation-specific message iterators.
 * Ant: Fixed build.xml to be compatible with Java 8.
 * IMC: Removing dependency on GUI classes from IMCJava.
 * MRA: Fixed ordering problem when showing a message in a table (numeric values were outputted as strings). Closes issue #2566.
 * MapEditor: Added 'Copy Location' option in the menu.
 * MRA: Fixed SalinityVsDepthPlot missposition of marks
 * MRA/KMLExporter: Fix sidescan image file in KML.
 * MRA/KMLExporter: Fixed problem with matching pixels from data into pixels in image from sampling. The symptom was a sidescan image that was cropped on the right side and also the middle was not on the middle (was shifted right), and also the nadir was not properly calculated due to the center shift.
 * MRA/KMLExporter: Fixed problem that when the samplesPerPixel were 1, the values used for the pixel to paint was NaN. The result was a black sidescan images. (Fixed along with previous commit 1a72c5d.)
 * MRA/KMLExporter: Fix to deal with NaN values.
 * MRA/KMLExporter: Added choices for visibility on layers.
 * MRA/KMLExporter: Fix typo in DVL export.
 * MRA: Allowing opening a log in MRA through the command line argument. Usage ">neptus.[sh|bat] mra <log_file>".
 * Console/Exporter: PlanExporter addition of ProgressMonitor to exportFile(..) call.
 * Utils: FileUtil addition of checkFileForExtensions(..).
 * Console/Exporter: Tweak exposed name for IverPlanExporter.
 * Plugins/Utils: Fixed a hardcoded value on getResourceAsStream(..) that made it read the same resource whatever the input.
 * IMC: Adding hashcode to the name of the html message visualization. Closes github #3.
 * MRA: Fixed XYPlot missposition of marks
 * Vehicle: Added 90-ntnu-penguin-001
 * Vehicle: Added 88-ntnu-x8-001, 88-ntnu-x8-002, 88-ntnu-x8-003, ntnu-x8-004
 * Vehicle: Added 86-ntnu-hexa-001, 86-ntnu-hexa-002, 86-ntnu-hexa-003, ntnu-hexa-004
 * Console: Removed unused class ConfigParameter.
 * Console: Removed obsolete EchoSounderPanel class from plugin echosounder.
 * Console: Removed obsolete class ModemRange from plugin acoustic.
 * IMC: Added missing colon in the URI in announcing the IMC version.
 * PlanElement: using next maneuver starting point instead of center position to compute travelled distance.
 * MRA/Sidescan: Allow to zoom continuously; added mouse pointer location box and add some layered features (like measurements). Zoomed image can help with measuring.
 * Consoles/uav-light: Added SimulationActionsPlugin.
 * StreamSpeedPanel: Fixed wrong orientation and gave it a windsock look.
 * MRA/Importer/DeltaTParser: Cleaning up and removing debug output for intensity presence.
 * MRA/DeltaTHeader: Fixing if call for month processing.
 * MRA/BathymetrySwath: Adding protection for DeltaTParser nextSwathNoData(..) call.
 * CoordinateUtil/MRA/DeltaT83PToCorrectedPos83P: Lat/Lon format methods to and from "_dd.mm.xxxxx_N" and "ddd.mm.xxxxx_E" (_ = Space).
 * CoordinateUtil: If maxDecimalHouses not set (< 0) will set the default 3.
 * MRA/Exporters/DeltaT83PToCorrectedPos83P: Added exporter of 83P with corrected position. Outputs to "DataCorrected.83P".
 * MRA/Exporters/MatExporter: Fix for possible null pointer exception.
 * MRA/DeltaTParser: Added "Multibeam" category to MRAProperties that are for multibeam.
 * MRA/DeltaTParser: Added option to apply sound speed correction (false by default).
 * MapEditor: Adding protection to removing toolbar from MapPanel. In some situations a null pointer was thrown.
 * Maneuvers/RowsManeuver: Modified shadow parameters names.
 * MRA/LogBook: Added debug messages to LogBook visualization with different color and icon.
 * IMC: Using recommended constructor for ImcInputStream
 * IMC: Removed NeptusMessageLogger and LsfMessageLogger (now using class in IMCJava)
 * MRA: Fixing some problems that occur when opening logs with a minimal set of messages.
 * Maneuvers/CompassCalibration: fixed counter-clockwise arrow.
 * MyLocation: Added to MyLocation an option to override heading with the hand configured.
 * Maneuvers/PopUp: Making flags "StationKeep" and "WaitAtSurface" not editable and always true (they will become deprecated).
 * Vehicles: Changed Elevator default startZUnits to DEPTH for all AUVs. Closes issue #3096.
 * LogsDownloaderWorker/ImcMsgManager: Temporary fix for using PowerOperation instead of PowerChannelControl to turn on/off camera (for now only for LAUV-Noptilus-3). Also needed to change ImcMsgManager to not set dst on message if its value is different than 0xFFFF.
 * Vehicles/Conf: Let us relax the validation to be able to store vehicle configurations after visual editing them.
 * MRA/Multibeam: DeltaTParser now uses the navigation with applied corrections.
 * MRA/SidescanAnalyzer: Added option for Slant-Range correction in SidescanAnalyzer and Sidescan parameters are now stored to disk.
 * IMC/Console/MRA: MessageHtmlVisualization now also uses the name of its generating entity for distinguising from other messages with same timestamp. Closes #3065.
 * MRA/Exporter/IMC: UpdatedImcExporter now copies the src and src_ent fields to the destination.
 * MRA/KMLExporter: Rework path export to account for different src for the EstimatedState and also the track break for deltas > secondsGapInEstimatedStateForPathBreak (Neptus Property).
 * MRA/KMLExporter: Fix a non relative path for MB legend.
 * MRA/KMLExporter: Account for no altitude measurements (-1).
 * PlanEditor: Dragging of maneuvers (and plan) should now be more responsive in Plan editor.
 * Maneuvers: Fixed empty implementation of Elevator.getWaypoints();
 * Console: Corrections in urready4os plugin to display rhodamine data.
 * Console/RealTimePlot: Made real-time plot more robust to scripting errors.


Neptus 4.1.0 (31-07-2014)
------------
 * IMC: Updated IMC and IMCJava to 4667e8e. (no major changes from v5.4.0 except some experimental messages for UAV added).
 * Config: Updated systems parameters to DUNE 2.6.x (master,0724529 for lauvseacon-1 and master, 4f41e09 for all others).
 * I18n: Updated POT and PO files.
 * Console/PlanEditor/PlanElement: Using LocationType to calculate distances.
 * Console/MapEditor: Fixed issues with Undo/Redo and filled property of map elements.
 * MRA/Exporters: Fixing some exception on Sidescan images exporting.
 * Maneuvers/CompassCalibration: Drawing circle according with radius plus clockwise arrow.
 * Vehicles/Defs: Safer CompassCalibration defaults.
 * Maneuvers/Loiter: Fix ClockWise or CounterClockWise drawing.
 * Console/Map: ScatterPointsElement now allows setting 2 colors for creating a gradient.
 * Comms: Fix a long time hidden bug that made disconnection of a system comm. was not properly processed.
 * Console/MyLocation: Possibility to use external systems as the system to follow.
 * Console/Map: Whenever a new point is added to a ScatterPointsElement too far from its center (> 3000), it creates a new center a cleans previous points. 
 * MRA/Exporter: Fixed the order of the coordinates while exporting plans to KML.
 * Console/Plugin Alliance: Added NMEA plotter (AIS ship positions). 
 * MRA/ConcatenateLsfLog: Fix coping of log elements and also for the GZ Data.lsf IS MANDATORY TO USE MultiMemberGZIPInputStream and not GZIPInputStream due to the later does not handle well concatenated GZ files, as DUNE produces.
 * Console/Map: Added LineSegment map element.
 * Console/MainSystemSelectionCombo: different color schemes for most usual vehicle states.
 * MRA/Plots: in Temperature vs Depth, use Depth Sensor if CTD is not available.
 * MRA: Added VideoLegendExporter.
 * MRA: Corrected positions are now cached in mra/ folder.
 * Console/PlanEditor now correctly updates the edited plan statistics whenever the plan is changed by the user.
 * Launchers Shell/MRA/VTK Plugin: Better support for VTK installed on Windows in Batch launchers. If variable "vtk.lib.dir" is set then it will be used ("vtk.lib.dir(x86)" can be set on a x64 Windows to run 32bit java and VTK). If not it is expected to be installed on "%PROGRAMFILES%\VTK\bin" or "%PROGRAMFILES(x86)%\VTK\bin" for 32bit on a x64 Windows.
 * Console/MultiVehiclePlanOverlay: When verbose reports are received, the plan being visualized is not cleaned up.
 * Console/PlanSimulationLayer: Fixing some bugs in PlanSimulationLayer (validation checks were being executed before simulation was finished).
 * Console/PlanControlState: PlanControlStatePanel now requests for plans sent by other neptus consoles (defaults to false).
 * MRA/VTK Plugin: This addition will trigger the point clouds to be meshed otherwise nothing will be shown when user pushes the solid or wired frame based representations.
 * MRA: Fix bug that was not allowing message values to be shown on the table as their type, ordering was not working because of this.
 * Console/PlanControlState/FollowReferenceInteraction: Only process incoming PlanControlState messages if plan_id is not empty. Closes #2925.
 * Util/Colormap: ConvexHull is computed only when necessary by DataDiscretizer.
 * Console/UAV: Some cleanups on UavVirtualHorizonPainter and AutopilotModePanel.
 * LogsDownloaderWorker/FtpDownloader: Some work to remove some bugs. Also now only one download at a time.
 * Console/MyLocationDisplay: Some cleanups and clarifications on its configuration.
 * Console/Plugin Position: Added FindMainSystemLayer for renderer to show where the vehicle is.
 * Console/Europa: Added an Europa planner and viewer (only for linux x64).
 * Loaders: CheckJavaOSArch also generates the os.name property (lowercase in "osName-arch" format).
 * Loaders: LD_LIBRARY_PATH is not overriden in neptus.sh.
 * MRA/Exporters: CTDExporter now also exports GMT time and corrected positions.
 * Console: Added small panel to supervise air | ground and commanded speeds.
 * MRA/VTK: Several cleanups.
 * Libs: Updated Guava from 16.0 to 17.0.
 * MRA/Exporters: MatLab exporter reworked.
 * Vehicles/SystemsList: Added possibility to edit system parameters in the SystemsList.

Neptus 4.0.0 (30-05-2014)
------------
 * IMC: Updated IMC and IMCJava to v5.4.0.
 * Config: Updated systems parameters to DUNE 2.5.0 (release/dune-2.5.x,b8da6dd).
 * I18n: Updated POT and PO files.
 * Vehicles/Defs: Modified default CompassCalibration duration to 20 minutes.
 * Vehicles/Defs: Clean vehicles conf files (normalization and cleanup).
 * MRA/Plots: Fixed Corrected Position plot.
 * MRA/Exporters: Removed plugin description (not loaded as plugin) from exporters not yet ready.
 * Installers/Linux: Added creation of GNOME launchers for Linux SFX installers.
 * MRA: Protecting from error decompressing Data.lsf.gz.
 * ChronometerPanel: Replaced Timer with a Thread. (Panels using this panel sould stop the chronometer for proper thread cleanup.)
 * Joystick/ControllerPanel: Adding a proper cleanup.
 * Installers: Adjusts on NeptusSE installer files to include.
 * LogsDownloaderWorker/FtpDownloader: Set a 15s data timeout.
 * Ant/build.xml: Some corrections in order to compile main source without plugin dependencies, also each plugin doesn't compile with others plugins dependency.
 * General/NeptusProperty: Fix a problem that was not properly fix with editable property setting (rename from hidden was not completely finished).
 * MRA: Sort columns values is now working properly added single table cell selection. Closes #3012.
 * MRA/MRALogReplay: Added temporary fix for MRALogReplayTimeline interval collection of messages to replay. (We need to rethink the ordering of messages in the index.)
 * Log/Config: Little update on log4j configuration. Also added more clear example for extended debug configuration.
 * Consoles/lauv.ncon: Re-added marks interaction on right Map mouse click (contact marker).
 * LogsDownloaderWorker: Some cleanups.
 * Console: Fix some problems with hidden Abort button and MissionTree scrollbar in console layout lauv.ncon.
 * MRA/VTK/Vis3D: Some bug fixeds.
 * MRA/Exporters: Added Video Overlay MRA exporter (EstimatedState needed).
 * Console/HFRadarVisualization: Fixes reading and displaying data. 

Neptus 4.0.0rc1 (07-05-2014)
------------
 * IMC: Updated IMC and IMCJava to v5.4.0-rc1. (same as v3.1.0)
 * Config: Updated systems parameters to DUNE 2.5.0-rc1 (HEAD,6db5dd1). (same as v3.1.0)
 * I18n: Updated POT and PO files.
 * MRA/SidescanPanel: Little tweak on the info overlay.
 Console/MapPanel/FeatureFocuser: Added center map on main vehicle/system. Closes #2981.
 * MRA/Exporter: Added "Updated IMC" MRA exporter (translates the messages that are compatable to the current IMC version).
 * Console/ControllerPanel: Teleoperation is working again.
 * Console/SettingsWindow: Map layers and interactions now appear in the SettingsWindow (needs only to be a PropertiesProviders). Also private NeptusProperties are shown.
 * LocationType: Fix a bug introduced in 94d4c1b2 where the toString() if height is 0 does not output it. This cause a problem in the related method valueOf(..) that was expecting it.
 * build.xml: Fixing 'clean' target to clean all generated jars.
 * Console/MRA/LogsDownloaderWorker: Adding stopping trigger when processing log folders list.
 * Console: Little change on saving or not into console's XML of the layers and interactions added by already console XML saved panels. When adding ConsoleLayers and ConsoleInteractions the default is to store in the Console XML.
 * Comms/AnnounceWorker: Adding extra logging in the proccess of choosing IP for the system from Announce message.
 * Comms/AnnounceWorker: Adding extra delivery info for heartbeat.
 * Console/SystemsList: Better color for painting course in renderer. Closes issue #2938.
 * Console/UAV: Some uav plugin cleanup of unused code and components.
 * Console/AirCamDisplay: Reworked video panel.
 * Console/Comms: When messages are sent using Iridium, provide feedback on success or failure.
 * Console: Fixed Argos location timestamps (now using GMT timezone). Closes #2927.
 * Interpolation/GeoLocation: Fix vectorial option in GeoCollection.
 * Console/PathControlLayer: Can now (optionally) display the start of the commanded path instead of selecting the current vehicle position.
 * Mission: More robust (and comprehensive error) during plan loading.
 * Comms/Hub: Added logging to Hub iridium messenger.
 * Console/TrexMapLayer: Added possibility to send T-REX goals directly to the vehicle via Iridium.
 * Console/Comms: Added possibility to send any (IMC) plan via Iridium.
 * Console: Fixed (major) bug that was preventing some ConsolePanels to correctly receive the main vehicle.
 * MRA/Replay: Added TemperatureReplay.
 * MRA/Replay: Added SalinityReplay.
 * MRA/VTK: Possiblility to take snapshots.
 * MRA/Replay: Changed MultibeamReplay from the plugin to the mrareplay package.
 * WorldMaps/S57: Fix a bug where was needed to open the S57 options dialog for proper loading of the charts. Closes #2845.
 * MRA: Adding missing statistics visualization on mra.
 * Vehicle files definitions cleanups (speeds to m/s by default and cleanup supported maneuvers).
 * Console/TrexMapLayer: For UAVs removing automatic enable when sending goals. Adding visualization of sent Spotter goals. Adjust option enable/disable to Trex state of activation. Add option to clean goals on Neptus.
 * Console: Added possibility to set height for simulated GpsFix messages.
 * Console: Some refactorings in CommandPlanner and MissionTreePanel to solve the issue of commanded plans not being synchronized.
 * Console: VehicleStateMonitor now makes a better job at distinguishing tele-operation and regular maneuvering modes.
 * Console: Added support for fetching argos data from web service (sunfish).
 * Console/MissionTree/Beacons: Adding option to get beacon configuration from vehicle when not synchronized.
 * MRA: Now using multiple threads to load the different MRA replay layers.
 * Console: FollowReferenceInteraction now checks for whether the vehicles are owned by this console or not before sending references.
 * Console: MantaOperations should now listen for acoustic systems announced by Manta (adding them to user-defined ones).
 * Console/FollowReference: Added interactions helper to popup menu. Closes #2862.
 * Workspace: Removed all code related to the SshShell components (library jsch).
 * Console: Added plug-in for Sunfish situational awareness and decision support.
 * Console/HFRadarVisualization: Added and tested adding HF-Radar NetCDF from Spanish source. So now loads both TUV and NetCDF HF-Radar files.
 * Comms/ImcMsgManager: Fixed a null pointer introduced in ImcMsgManager.sendMessageToSystem() at commit aeea299d.
 * MRA: Added code for exporting CTD data in batch.
 * WorldMap: Added TiliShipTrafficDensity world map tile layer source (maximum amount of detail to 10).
 * MRA: Added CTD3D visualizer.
 * Updated mail.jar to javax.mail-1.5.1.jar.
 * Comms: Added method in ImcMsgManager for posting a message locally.
 * Comms: Added methods for fetching messages from GMail (will be used to poll Iridium messages).
 * ByteUtil: Added method for encoding a byte array as an hexadecimal string.
 * Comms: Added method for waiting for result of reliable message sending.
 * Mission: Map elements now have only IDs.
 * Console: Added Plugin for exporting S57 depth soundings (to CSV).
 * Console/SpotOverlay: Update of Spot API.
 * MRA: CTD to CSV exporter now also exports the vehicle medium together with C, T and D.
 * Mission: Some fixes in serialization / deserialization of plans.
 * Added plugin for sending of text messages to systems (IMC, SMS, and Iridium or RockBlockIridiumMessenger, or Acoustics).
 * Added GSM and Iridium parameters to NP1, NP2 NP3, XT2, SC2, and SC3.
 * Added support for GSM and Iridium protocol parameters in VehicleType.
 * Console/PlanEditor: Added yoyo survey plan template.
 * MRA: Added ruler for Echosounder panel.
 * MRA: Choose Bathymetry parser by sensor type.
 * Console: Update plugin manager to the new plugin system with layers and interactions.
 * Console: Added console layer plugin for computing beacon visibility in the map (BeaconsVisibilityLayer).
 * MRA: Added CPU Usage plot.
 * MRA: Added plug-in for forwarding replay messages to the network (NetworkReplay).
 * MRA: Corrected timezone for raw messages table and Log replay.
 * MRA: Ability to view any message by double-clicking it in "All messages" visualization.
 * MRA: Added Raw Messages visualization back to MRA.
 * MRA: LogReplay reworked.
 * MRA: Fix exception when MRA recently opened files was not existent.
 * Console: Removed references to the deprecated PlanStateGenerator (replaced by PlanSimulationEngine).
 * Checklist: For now remove "Variable Test" for checklists.
 * Console: PlanSimulationLayer can also check for collisions with obstacles.
 * Console/MapEditor: Improved click intersection detection in some map elements, and can now be marked as obstacles. 
 * Updated Apache httpclient from 4.2.2 to 4.3.2 and httpcore from 4.2.2 to 4.3.1.
 * Updated Guava from 15.0 to 16.0.
 * Updated MiGLayout from 4.0 to 4.2 (separated now into two jars, swing and core).
 * Updated JGoodies commons from 1.4.0 to 1.7.0 and looks from 2.5.2 to 2.5.3.
 * Updated Apache commons-net from 1.4.0 to 3.3.
 * Updated Apache commons-logging from 1.1.1 to 1.1.3.
 * Updated Apache commons-lang3 from 3.1 to 3.2.1.
 * Updated Apache commons-email from 1.2 to 1.3.2.
 * Updated Apache commons-compress from 1.3 to 1.7.
 * WorldMap: Setup of the worldmap to display is now done by right-click popup menu on the map.
 * Updated Apache commons-codec from 1.8 to 1.9.
 * MRA: Added CTD colormap (side view).
 * MRA/Exporters: VTK point-cloud and generated mesh exporter added. Allows exporting to VTK, OBJ, PLY, STL, VRML, and X3D.
 * Console: Plan Simulation now can warn operator for AUV or UAV distances from planned positions (configurable).
 * MRA/Exporters: Exporters menu is now being loaded on tools menu.
 * Console: Fixed rotation in plan simulation overlay.
 * Console: To be able to have all default interactions (rotation and measurement) in all interactions InteractionAdapter must be called.
 * Console/PlanEditor: Plan templates are now part of PlanEditor.
 * MRA: PDF reports are now being saved on log path. The report file also has the log name not only the system millis.
 * MRA: added CTDExporter to CSV.
 * MRA: Added MRAFilesHandler with extractors, open/close log files (extrated from NeptusMRA), and PDF generation.
 * Console: Face-lifting for SystemsInfoPainter.
 * Console/AbstractConsolePlugin: Added AbstractConsolePlugin to be base for ConsoleLayer and  ConsoleInteraction.
 * Console/ConsoleInteraction: Created class ConsoleInteraction and interface IConsoleInteraction for adding MapPanel interaction extensions (in the console).
 * Console.ConsoleLayer: Created class ConsoleLayer and interface IConsoleLayer for adding MapPanel layer extensions (in the console).
 * MRA: MRA menuBar on separate class (MRAMenuBar) and some coding revision.
 * MRA: MRA properties are on a separate class. closes #2718.
 * Console: New annotation @Periodic that can be used to annotate methods that should be called periodically in ConsolePanel's.
 * Console: Single way of listening for vehicle change events (using EventBus). Use @Subscribe on a method with ConsoleEventMainSystemChange as argument.
 * Removed several static initializers that were fetching images from disk.
 * Console/ConsolePanel: SimpleSubPanel and SubPanel became one and only and then renamed as ConsolePanel. Also removed need for SubPanelProvider. 
 * Single way of starting Workspace, MRA and Consoles (through NeptusMain).
 * Console/PlanControl: Changed PlanControlStatePanel message listening to EventBus.
 * MRA: Changed MraExporter interface by adding two parameters: source (the log to be exported), and pmonitor (a progress monitor popup that can inform the user of the operation progress).
 * MRA: Added Sidescan Images exporter.
 * Console: Plan Simulation (PlanExecutionPreview) now supports multiple vehicles.
 * MRA/VTK: Loading DVL data to point cloud. ...
 * MRA: Added DVLBathymetryParser for generating bathymetry data from DVL beams.
 * MRA: Saving markers even if MRA is closed abruptly.
 * MRA: LogMarkers are now showing in LogMarkersReplay layer. Closes #2723.
 * MRA: Improvements to the KMLExporter MRA plug-in.
 * MRA: Fixing opening of compressed files in MRA.
 * MRA: NeptusMRA is now able to open LSF files from command line (using ./neptus.sh -f <filename\>).
 * Console: Merged caoAgent plugin to noptilus project plugin.
 * MRA: Moved plugins to core src code to pt.lsts.neptus.mra.plots package (mra-jzy3d).
 * Console: Moved plugins to core src code to pt.lsts.neptus.console.plugins package (configWindow, planning).
 * Console: Remove core code dependency of plugin code (web, oplimits, map, noptilus).
 * Console: Containers is not a plug-in anymore (moved to core src to pt.lsts.neptus.console.plugins.containers).
 * MRA: Moved LBLRangesReplay to acoustic plugin to remove core code dependency of plugin code.
 * I18n: Added pt.lsts.neptus.i18n.Translate annotation to signal to PluginsPotGenerator to add the enum field to POT for translation.
 * All MRA visualizations and replay layers, and exporters are now plug-ins. 
 * Mission/PlanCompability/PlanSimulationLayer: Added classes for testing vehicle-plan compatibility (used for plan simulation).
 * IMC: Current IMC definition is not stored anymore in conf/messages/IMC.xml file, now uses the one embedded in the libimc.jar.
 * MRA/VTK: Some optimizations on Multibeam and DVL visualizations.
 * MissionTree: Optimizations on operations with beacons and multiple selected plans.

Neptus v3.1.0 (07-05-2014)
-------------
 * IMC: Updated IMC and IMCJava to v5.4.0-rc1.
 * Config: Updated systems parameters to DUNE 2.5.0-rc1 (HEAD,6db5dd1).
 * Added AUV LAUV-Lupis-1.
 * Added UAV vehicles X8-02, X8-03, and X8-04.

Neptus v3.0.2 (07-05-2014)
-------------
 * Console/RealTimePlot: Roll was actually plotting psi. All angles are display now (phi, theta, and psi).
 * PluginUtils: Fix a small bug introduced at 8afb5b3c making all 'int' NeptusProperties not to be loaded properly.
 * PlanDB: Fix a problem in removing the plans in the local DB for the vehicle (a synchronized image of the vehicle's plan DB). Closes #2907.
 * WorldMap: Fix a small annoyance in the layer tiles that when loaded from disk, transparency was not applied (but layers already have transparency, so we must reapply transparency to the opaque parts). (All world map images have a transparency factor.)
 * Console/MapPanel: Fix vehicle tail points with offset error if too far from home ref. Closes #2905. Also if the map was rotated it was not correctly painted.
 * Scripts: Little tweak on gather-day.logs.sh. Don't re-compress gz files, delete uncompressed IMC.xml, avoid using (if configured) external diff on Git.
 * Fix OutputMonitor to grab the system outs before logger (output-xxx.html files now have log output also).
 * Console/SystemConfiguration: Fix a problem when loading systems' parameters. Allowed maximum and minimum limits were ready for real values if only the two were defined.
 * Console/LogsDownloaderWorker: Adding a protection against a null pointer when LogsDownloaderWorker queries for PowerChannelState (camera CPU state related).
 * Console/MissionTree: Now when sending the acoustic beacons to the vehicle, is also send the message to ask for the current vehicle's beacons configurations to allow updated information in the console.
 * Console/PlanControlPanel & MissionTree: Fix a problem with adding acoustics beacons to mission. In some situations the beacon was added to a mission map that was not added to the mission, and therefore in some situations was absent when collecting the beacons to sent to a vehicle (commit 3617f01).
 * Console/MissionTree: You can now edit the selected plan just retrieved from the vehicle.
 * Console/PlanEditor: Putting paste close to copy maneuver in the pop-up menu and fix a bug that prevented the pasted maneuver to get placed on the mouse click location. Closes #2838.
 * MRA/BathymetryReplay: Distance message has to exist for the bathymetry reply layer to work. Closes #2831.
 * Console/SystemConfiguration: Fix some problems in configuring parameters where real, in computers where the decimal separator is the comma, got invalid for editing. Closes #2830.
 * Added LAUV-Xplore-1 vehicle.

Neptus v3.0.1 (28-02-2014)
-------------
 * Updated vehicles parameters.
 * LAUV-Seacon-4 was renamed to LAUV-Seacon-1.
 * Minor translations POT files updates.
 * For vehicles with camera payload, the IP is now calculated by adding 3 to the last byte of the IP of the vehicle.
 * Updated IMC to 5.3.0.
 * MRA: Fix Echo Sounder MRA visualization showing up even when CTD entity doesn't exist. closes #2766.
 * Console/Simulation: Added support for YoYoManeuvers and fixed StationKeeping in the simulation preview.
 * Console/LBL: Fixed bug LBL ranges painted on the console are switched. Closes #2756.
 * Console/Planning: Missing properties update when editing payload settings for all maneuvers.
 * Console/Planning: Now when editing payload settings for all maneuvers, the values for all maneuvers are checked, and non default values take precedence for showing. The operator is informed if he is seeing common values for all maneuvers or not. Closes #2760.
 * WorldMaps: Fixing WorldMaps static initializers.
 * MRA: Added a new TableModel that prevents missing messages when displaying messages table. Also some minor bugs were fixed.
 * Console/MissionTree: Added function to remove all beacons from vehicle.
 * Console/EntityStatePanel: Fix EntityStatePanel sum state color.
 * Console/ImuAlignmentPanel: Now warns the user when IMU becomes aligned / not aligned. Closes #2809
 * MRA: Fix calculating depth extrema statistics in MRA.
 * MRA.RevisionOverlays: Fixed log markers not showing up in the console.
 * Console/MapEditor: Fixed several issues related with map edition using object IDs different of object names (using only ID for now).
 * Console/Planning: Fixed cloning of Rows and Formation Maneuvers (missing elements).
 * LocationType now stores latitudes and longitudes as radians.
 * Updating S57 to use v2.1. Also all extracted and cached data goes to base folder ".cache/s57".
 * Console/MapEditor: Fix for not being able to drag some map elements in the map.
 * Console/MapEditor: Fix a bug that made the first map change did not trigger the undo button.

