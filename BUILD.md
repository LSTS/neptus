Neptus, Command and Control Framework
=====================================

Build Instructions
------------------

  * `./gradlew buildJars` will build main jar and plugin jars
  * `./gradlew clean` will cleanup the build

Distribution
------------

  * `./gradlew <dist-name>DistZip` and `./gradlew <dist-name>DistTar` where `<dist-name>` is the distribution name (it will not have the JRE)
    * `full` - for full version
    * `seacon` - for seacon version
    * `le` - for light edition
  * `./gradlew installer<Dist-name>LinuxDist` with `<Dist-name>` the same as above but capitalized, creates a sh installer for Linux.
  * `./gradlew installer<Dist-name>WindowsDist` with `<Dist-name>` the same as above but capitalized, creates an exe installer for Windows (NOT WORKING YET).

Eclipse
-------

  * `./gradlew eclipse` to create the project
  * `./gradlew cleanEclipseBuild` to delete the project build folders
  * `./gradlew cleanEclipse` to delete the project
