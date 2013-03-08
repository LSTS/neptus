settings = new java.util.LinkedHashMap();
settings.put("duration", 30);


// Initial location
loc       = param("Start Location", mission.getStartLocation());
time      = param("Execution time (minutes)", 60);
usePopUps = param("Use PopUps", true);
timepopup = param("Time between pop-up's (minutes)", 30);
length    = param("Side length", 100.0);
depth     = param("Depth", "2");
speed     = param("Speed (m/s)", 1);
clockwise = param("Clockwise (true) or Counter-Clockwise (false)", true);

timetoend = time * 60;
timetopopup = timepopup * 60;
offsets = [[length,0],[0,length],[-length,0],[0,-length]];

settingsGoto = new java.util.LinkedHashMap();
settingsGoto.put("speed", speed);
settingsGoto.put("units", "m/s");

settings.put("speed", speed);
settings.put("units", "m/s");

// set initial location
plan.setLocation(loc);
plan.setDepth(depth);
//plan.setSpeed(speed);

// add a maneuver at start
maneuver("goto", settingsGoto);

while (timetoend > 0) {
    // add a square
    for (i = 0; i < 4; i++) {
      if (clockwise == true) {
        ind = i;
      } else {
        ind = 3 - i;
      }
      move(offsets[ind][0], offsets[ind][1], 0);
      maneuver("goto", settingsGoto);
      timetoend -= length / speed;
      timetopopup -= length / speed;
    }

    // if needed add a popup
    if (usePopUps == true) {
      if (timetopopup < 0) {
        maneuver("popup", settings);
        timetopopup = timepopup * 60;
      }
    }
}