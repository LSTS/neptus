Neptus, Command and Control Framework
=====================================

Build Instructions
------------------

  * `./gradlew buildJars` will build main jar and plugin jars
  * `./gradlew clean` will cleanup the build
  * `./gradlew <dist-name>DistZip` and `./gradlew <dist-name>DistTar` where `<dist-name>` is the distribution name (it will not have the JRE)
    * `full` - for full version
    * `seacon` - for seacon version
    * `le` - for light edition
