# FORGET ALL BELOW: JUST USE MAVEN.

1. Install Maven: https://maven.apache.org/index.html
2. Run [mvn package] inside vermouth-master root (where pom.xml resides).
3. Sit back and Wait.
4. You will get the jar files in the target directory.

# Dependencies

~~- Apache Log4J~~
	~~Webiste: http://logging.apache.org/log4j/~~
	~~Download: http://logging.apache.org/log4j/2.x/download.html~~
	~~Which JAR to use: https://logging.apache.org/log4j/2.0/faq.html#which_jars~~
~~- Apache Commons~~
	~~Website: http://commons.apache.org/~~
	~~Download: http://commons.apache.org/proper/commons-cli/download_cli.cgi~~
~~- opencsv~~
	~~Website: http://opencsv.sourceforge.net/~~
	~~Download: https://sourceforge.net/projects/opencsv/~~

~~Download the above and put them under vermouth-master/src/main/java/dep (this is a new folder; assumed to be used in the instruction below). Also create two directories named bin and config under vermouth-master/src/main/java. Create a text file named Manifest.mf inside the config directory and put the content like below:~~

~~Manifest-Version: 1.0~~
~~Class-Path: ./dep/apache-log4j-2.13.3-bin/log4j-core-2.13.3.jar ./dep/apache-log4j-2.13.3-bin/log4j-api-2.13.3.jar ./dep/commons-cli-1.4/commons-cli-1.4.jar ./dep/opencsv-5.2.jar~~
~~Main-Class: eaai.ginrummy.Main~~

~~The Class-Path info of course needs to be adjusted accordingly if you want to give another name for the new directories. The final directory structure looks like:~~

~~vermouth-master -> src -> main -> java -> eaai, ginrummy, dep, bin, config~~

~~with the dependencies inside dep, and Manifest.mf inside config, and bin is empty.~~

# Source code change needed for Windows

For the instruction below, on Linux systems, use (:) for path delimiter. On Windows systems, use semicolon (;) instead, as shown below.

For Windows, due to filename and path convention, following lines need to be changed.

At line 159 of eaai.ginrummy.Main.java, change
```java
String identifier = cmd.getOptionValue("id", (new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss:SSS")).format(new Date()));
```
into
```java
String osdepstr = "yyyy-MM-dd-HH:mm:ss:SSS";
if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
	osdepstr = "yyyy-MM-dd_HH-mm-ss-SSS";
}
String identifier = cmd.getOptionValue("id", (new SimpleDateFormat(osdepstr)).format(new Date()));
```
At line 48 of eaai.ginrummy.util.FileMap.java, change
```java
Path path = Paths.get(String.format("./%s.zip", identifier)).toAbsolutePath().normalize();
URI uri = URI.create("jar:file:" + path);
```
into
```java
URI uri;
if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
	uri = URI.create("jar:" + Paths.get(String.format("./%s.zip", identifier)).toUri());
} else {
	Path path = Paths.get(String.format("./%s.zip", identifier)).toAbsolutePath().normalize();
	uri = URI.create("jar:file:" + path);
}
```
# Compile

~~Clone vermount repo.~~

~~cd src/main/java~~

~~javac -d ./bin ginrummy/GinRummyGame.java~~

~~javac eaai/ginrummy/Main.java -d ./bin -cp ./;./bin;./dep/apache-log4j-2.13.3-bin/log4j-core-2.13.3.jar;./dep/apache-log4j-2.13.3-bin/log4j-api-2.13.3.jar;./dep/commons-cli-1.4/commons-cli-1.4.jar;./dep/opencsv-5.2.jar~~

~~cd bin~~

~~jar cvfm vermouth.jar ../conf/Manifest.mf .~~

~~Copy the created vermouth.jar to the vermouth-master/src/main/java so that the classpath defined inside Manifest.mf works.~~

# Run

Use import instead of package. Package may be used if we are creating a jar -- may need testing. See the top of SimplePlayer.java for example.

```java
import ginrummy.*;
```

To run (Linux): Linux can use relative path.

```bash
java -jar vermouth.jar --oneall --games 100 --agents 'file:./SimpleGinRummyPlayer' 'file:./SimpleGinRummyPlayer'
```

To run (Windows): Windows must use full absolute path.

```bash
java -jar vermouth.jar --oneall --games 100 --agents "file://D:/eaai2021/vermouth-master/src/main/java/SimplePlayer" "file://D:/eaai2021/vermouth-master/src/main/java/SimplePlayer"
```
