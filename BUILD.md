# Neptus, Command and Control Framework

## TL;DR

* Run `./gradlew clean && ./gradlew && ./gradlew run`

## Build Instructions

* `./gradlew` Will run with default tasks `buildJars` and `buildBundleJars`
* `./gradlew clean` will cleanup the build (can be run for subproject)
* `./gradlew run` will run the build inside Gradle (use the `neptus` and `neptus.bat` 
  on the root folder to run on the shell)

* `./gradlew buildJars` will build main jar and plugin jars (and tasks of type jar)
* `./gradlew classes` will compile the classes
* `./gradlew jar` will create the jars

## Distribution

* `./gradlew <dist-name>DistZip` and `./gradlew <dist-name>DistTar` where `<dist-name>`
  is the distribution name (it will not have the JRE)
  * `full` - for full version
  * `seacon` - for seacon version
  * `le` - for light edition
* `./gradlew installer<Dist-name>LinuxDist` with `<Dist-name>` the same as above but
  capitalized, creates a sh installer for Linux.
* `./gradlew installer<Dist-name>WindowsDist` with `<Dist-name>` the same as above but
  capitalized, creates an exe installer for Windows (NOT WORKING YET)

### Other related tasks:

* `./gradlew buildBundleJars` task to generate the bundle Jars
* `./gradlew generateI18N` task to generate I18N files (needs gettext installed)

## IDE

### Eclipse

To use in Eclipse don't create the project but import the Gradle project into Eclipse.

* `./gradlew eclipse` to create the project (can be used, but configure the debug runs still WIP)
* `./gradlew cleanEclipseBuild` to delete the project build folders
* `./gradlew cleanEclipse` to delete the project

This is sometimes not stable. If problems with building do:

* If problem with duplicated resource on neptus project do:
    ** `./gradlew cleanEclipseBuild createEclipseBuild`

### IntelliJ

Just import the Gradle project to Idea. To run or debug be sure to use the classpath from
`neptus.main`. At the moment you need to not delegate the Gradle tasks run to Gradle but
to IntelliJ.

* `./gradlew cleanIdeaBuild` to delete the project and build folders

## Plugins

The plugins are expected to reside on folders with names `plugins-dev<*>`. On the 'plugins-dev'
are the plugins offer by Neptus. The extra `plugins-dev*` folders if exist are read and
instrumented for Gradle (should be on separated Git repositories).

Each plugins is instrumented by Gradle in its `jar` task for its output to be placed on the
`plugins` folder. The Jar will be a fat jar with all libs in it. The folder structure is
expected to be the following:

* `src/java` (Java src files)
* `src/resources` (resource files, optional)
* `lib` (for libs that are not able to be added through Maven, they will be added to
  dependencies automatically, optional)
* `build.gradle` (to add dependencies, optional)

One very important rule is that a plugin is self-contained, only dependent on the main src
and jars and not on any other plugin.
