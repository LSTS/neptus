Changes
=======

Neptus 3.1.0
------------
 * All MRA visualizations and replay layers are now plug-ins.
 * 
 
Neptus 3.0.1
------------
 * Updated vehicles parameters.
 * LAUV-Seacon-4 was renamed to LAUV-Seacon-1.
 * Minor translations POT files updates.
 * Now for vehicles with camera payload the IP is found by adding 3 to the last byte of the IP of the vehicle.
 * Updated IMC to 5.3.0.
 * MRA: Fix Echo Sounder MRA appearing even if the entity doesn't exist. closes #2766.
 * Console.Simulation: Added support for YoYoManeuvers and fixed StationKeeping in the simulation preview.
 * Console.LBL: Fixed bug LBL ranges painted on the console are switched. Closes #2756.
 * Console.Planning: Missing properties update when editing payload settings for all maneuvers.
 * Console.Planning: Now when editing payload settings for all maneuvers, the values for all maneuvers are checked, and non default values take precedence for showing. The operator is informed if he is seeing common values for all maneuvers or not. Closes #2760.
 * WorldMaps: Fixing WorldMaps static initializers.
 * MRA: Added a new TableModel that prevents missig messages when displaying messages table. Also some minor bugs were fixed.
 * Console.MissionTree: Added function to remove all beacons from vehicle.
 * Console.EntityStatePanel: Fix EntityStatePanel sum state color.
 * Console.ImuAlignmentPanel: Now warns the user when IMU becomes aligned / not
    aligned. Closes #2809
 * MRA: Fix calculating depth for statistics in MRA.
 * MRA.RevisionOverlays: Fixed log markers not showing up in the console.
 * Console.MapEditor: Fixed several issues related with map edition using object IDs different
    of object names (now there is only ID).
 * Console.Planning: Fixed cloning of Rows and Formation Maneuvers (missing elements).
 * LocationType now stores latitudes and longitudes as radians. This slight
    decrease in precision actually solves a lot of issues regarding
    translation to/from IMC.
 * Updating S57 to use v2.1. Also all extrated and cached data goes to base folder ".cache/s57".
 * Console.MapEditor: Fix for not being able to drag some map elements in the map.
 * Console.MapEditor: Fix a bug that made the first map change did not trigger the undo button.
