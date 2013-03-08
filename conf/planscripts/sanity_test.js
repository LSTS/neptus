// Initial location
loc 	= param("Start Location", mission.getStartLocation());
length 	= param("Side Length", 40.0);

// depths are comma-separated values
d	= param("Depths", "0,2");

depths = d.split(",");

// set initial location
plan.setLocation(loc);

//initial goto
plan.setDepth(depths[0]);	
maneuver("goto");

for (depth in depths) {
	plan.setDepth(depth);	
	move(length, 0, 0);
	maneuver("goto");
	move(0, length, 0);
	maneuver("goto");
	move(-length, 0, 0);
	maneuver("goto");
	move(0, -length, 0);
	maneuver("goto");
}
