Changes
=======

Neptus 4.1.0 dev (??-??-2014)
------------
 * MRA/Exporters: MatLab exporter reworked.

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

