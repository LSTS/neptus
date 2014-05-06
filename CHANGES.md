Changes
=======

Neptus v3.1.0 (not officially released)
-------------
 * Updated vehicles parameters (DUNE v2.5.0-rc1, master-77b8b7e).
 * Added UAV vehicles X8-02 and X8-03.
 * Updated libimc.jar to the latest version (IMC v5.4.x cycle, b26419eaf9a715c8dbb24e5e2accc0e204e99a2f).

Neptus v3.0.2
-------------
 * PluginUtils: Fix a small bug introduced at 8afb5b3c making all 'int' NeptusProperties not to be loaded properly.
 * PlanDB: Fix a problem in removing the plans in the local DB for the vehicle (a synchronized image of the vehicle's plan DB). Closes #2907.
 * Console.WorldMap: Fix a small annoyance in the layer tiles that when loaded from disk, transparency was not applied (but layers already have transparency, so we must reapply transparency to the opaque parts). (All world map images have a transparency factor.)
 * Console.MapPanel: Fix vehicle tail points with offset error if too far from home ref. Closes #2905. Also if the map was rotated it was not correctly painted.
 * Scripts: Little tweak on gather-day.logs.sh. Don't re-compress gz files, delete uncompressed IMC.xml, avoid using (if configured) external diff on Git.
 * Fix OutputMonitor to grab the system outs before logger (output-xxx.html files now have log output also).
 * Console.SystemConfiguration: Fix a problem when loading systems' parameters. Allowed maximum and minimum limits were ready for real values if only the two were defined.
 * Console.LogsDownloaderWorker: Adding a protection against a null pointer when LogsDownloaderWorker queries for PowerChannelState (camera CPU state related).
 * Console.MissionTree: Now when sending the acoustic beacons to the vehicle, is also send the message to ask for the current vehicle's beacons configurations to allow updated information in the console.
 * Console.PlanControlPanel & MissionTree: Fix a problem with adding acoustics beacons to mission. In some situations the beacon was added to a mission map that was not added to the mission, and therefore in some situations was absent when collecting the beacons to sent to a vehicle (commit 3617f01).
 * Console.MissionTree: You can now edit the selected plan just retrieved from the vehicle.
 * Console.PlanEditor: Putting paste close to copy maneuver in the pop-up menu and fix a bug that prevented the pasted maneuver to get placed on the mouse click location. Closes #2838.
 * MRA.BathymetryReplay: Distance message has to exist for the bathymetry reply layer to work. Closes #2831.
 * Console.SystemConfiguration: Fix some problems in configuring parameters where real, in computers where the decimal separator is the comma, got invalid for editing. Closes #2830.
 * Added LAUV-Xplore-1 vehicle.

Neptus v3.0.1
-------------
 * Updated vehicles parameters.
 * LAUV-Seacon-4 was renamed to LAUV-Seacon-1.
 * Minor translations POT files updates.
 * For vehicles with camera payload, the IP is now calculated by adding 3 to the last byte of the IP of the vehicle.
 * Updated IMC to 5.3.0.
 * MRA: Fix Echo Sounder MRA visualization showing up even when CTD entity doesn't exist. closes #2766.
 * Console.Simulation: Added support for YoYoManeuvers and fixed StationKeeping in the simulation preview.
 * Console.LBL: Fixed bug LBL ranges painted on the console are switched. Closes #2756.
 * Console.Planning: Missing properties update when editing payload settings for all maneuvers.
 * Console.Planning: Now when editing payload settings for all maneuvers, the values for all maneuvers are checked, and non default values take precedence for showing. The operator is informed if he is seeing common values for all maneuvers or not. Closes #2760.
 * WorldMaps: Fixing WorldMaps static initializers.
 * MRA: Added a new TableModel that prevents missing messages when displaying messages table. Also some minor bugs were fixed.
 * Console.MissionTree: Added function to remove all beacons from vehicle.
 * Console.EntityStatePanel: Fix EntityStatePanel sum state color.
 * Console.ImuAlignmentPanel: Now warns the user when IMU becomes aligned / not aligned. Closes #2809
 * MRA: Fix calculating depth extrema statistics in MRA.
 * MRA.RevisionOverlays: Fixed log markers not showing up in the console.
 * Console.MapEditor: Fixed several issues related with map edition using object IDs different of object names (using only ID for now).
 * Console.Planning: Fixed cloning of Rows and Formation Maneuvers (missing elements).
 * LocationType now stores latitudes and longitudes as radians.
 * Updating S57 to use v2.1. Also all extracted and cached data goes to base folder ".cache/s57".
 * Console.MapEditor: Fix for not being able to drag some map elements in the map.
 * Console.MapEditor: Fix a bug that made the first map change did not trigger the undo button.

