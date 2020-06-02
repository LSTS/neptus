Neptus, Command and Control Framework
=====================================

Build Instructions
------------------

* `./gradlew buildJars` will build main jar and plugin jars
* `./gradlew compileJava` will compile main jar and plugin jars
* `./gradlew clean` will cleanup the build
* `./gradlew run` will run the build inside Gradle (use the `neptus` and `neptus.bat` on the root folder)

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

This is not stable yet.

* `./gradlew eclipse` to create the project (not advised yet)
* `./gradlew cleanEclipseBuild` to delete the project build folders
* `./gradlew cleanEclipse` to delete the project

IntelliJ
--------

Just import the Gradle project to Idea. To run or debug be sure to add the Gradle
task `buildJars` to run also to the default build.

* `./gradlew cleanIdea` to delete the project
