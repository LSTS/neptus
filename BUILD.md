# Neptus, Command and Control Framework

## Build Instructions

* `./gradlew buildJars` will build main jar and plugin jars (and tasks of type jar)
* `./gradlew classes` will compile the classes
* `./gradlew jar` will create the jars
* `./gradlew clean` will cleanup the build (can be run for subproject)
* `./gradlew run` will run the build inside Gradle (use the `neptus` and `neptus.bat` on the root folder to run on the shell)

## Distribution

* `./gradlew <dist-name>DistZip` and `./gradlew <dist-name>DistTar` where `<dist-name>` is the distribution name (it will not have the JRE)
  * `full` - for full version
  * `seacon` - for seacon version
  * `le` - for light edition
* `./gradlew installer<Dist-name>LinuxDist` with `<Dist-name>` the same as above but capitalized, creates a sh installer for Linux.
* `./gradlew installer<Dist-name>WindowsDist` with `<Dist-name>` the same as above but capitalized, creates an exe installer for Windows (NOT WORKING YET)

### Other related tasks:

* `./gradlew buildBundleJars` task to generate the bundle Jars
* `./gradlew generateI18N` task to generate I18N files

## IDE

### Eclipse

This is not stable yet.

* `./gradlew eclipse` to create the project (not advised yet)
* `./gradlew cleanEclipseBuild` to delete the project build folders
* `./gradlew cleanEclipse` to delete the project

### IntelliJ

Just import the Gradle project to Idea. To run or debug be sure to add the Gradle
task `buildJars` to run also to the default build.

* `./gradlew cleanIdea` to delete the project

## Plugins

The plugins are expected to reside on folders with names `plugins-dev<*>`. On the 'plugins-dev' are the plugins offer by Neptus. The extra `plugins-dev*` folders if exist are read and instrumented for Gradle (should be on separated Git repositories).

Each plugins is instrumented by Gradle in its `jar` task for its output to be placed on the `plugins` folder. The Jar will be a fat jar with all libs in it. The folder structure is expected to be the following:

* `src/java` (Java src files)
* `src/resources` (resource files)
* `lib` (for libs that are not able to be added through Maven, they will be added to dependencies automatically, optional)
* `build.gradle` (to add dependencies, optional)

One very important rule is that a plugin is self-contained, only dependent on the main src and jars and not on any other plugin.
