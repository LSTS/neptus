// Initial location
loc 	= param("Start Location", mission.getStartLocation());
length 	= param("Side Length", 40.0);

depth	= param("Maximum Depth", "2");

// set initial location
plan.setLocation(loc);

plan.setDepth(0);	
maneuver("goto");

//square at surface
plan.setDepth(0);	
move(length, 0, 0);
maneuver("goto");
move(0, length, 0);
maneuver("goto");
move(-length, 0, 0);
maneuver("goto");
move(0, -length, 0);
maneuver("goto");

//8 at depth...
plan.setDepth(depth);	
move(0, -length, 0);
maneuver("goto");
move(-length, 0, 0);
maneuver("goto");
move(0, length, 0);
maneuver("goto");
move(2*length, 0, 0);
maneuver("goto");
move(0, length, 0);
maneuver("goto");
move(-length, 0, 0);
maneuver("goto");
move(0, -length, 0);
maneuver("goto");

//final square at surface
plan.setDepth(0);	
move(0, -length, 0);
maneuver("goto");
move(-length, 0, 0);
maneuver("goto");
move(0, length, 0);
maneuver("goto");
move(length, 0, 0);
maneuver("goto");
