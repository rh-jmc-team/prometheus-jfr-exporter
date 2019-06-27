# JFR Prometheus Exporter

JFR to Prometheus Exporter: a collector that scrapes JFR events from a JVM target at runtime for Prometheus to use.

## Obtaining the Exporter

JFR Prometheus Exporter is designed to be a Java Agent loaded in the target JVM. You can obtain a agent jar by downloading a prebuilt artifact or building from source.

### [Option 1] Using a Prebuilt JAR

Download the prebuilt jar from the [realse page](https://github.com/tabjy/jfr-prometheus-exporter/releases).
```sh
$ wget https://github.com/tabjy/jfr-prometheus-exporter/releases/download/v0.1-alpha/jfr-prometheus-exporter-1.0-SNAPSHOT.jar
```

### [Option 2] Building from Source

#### Requirements

This project references libraries from JDK Misson Control project, which is not published on Maven Central. In order to build from source, gradle expects to find JMC artifacts in Maven Local. Follow [offical instructions](http://hg.openjdk.java.net/jmc/jmc/file/5e0a199762b6/README.md#l177) to build JMC.

After packaging JMC, run the following command in JMC root directory to install artifacts to Maven Local.
```sh
$ mvn install -DskipTests -Dspotbugs.skip=true
```

#### Building Instructions

- Clone this repository to local.
  ```sh
  $ git clone https://github.com/tabjy/jfr-prometheus-exporter.git
  ```
- Run gradle to build a fat jar.
  ```sh
  $ cd jfr-prometheus-exporter
  $ ./gradlew build
  ```
- Find the built jar in `build/libs` directory.
  ```sh
  $ ls build/libs/jfr-prometheus-exporter*.jar
  ```

## Running a Exporter

The exporter agent can either be loaded statically or dynamically. 

### [Option 1] Loading statically

Start target application with:
```sh
$ java -javaagent:<path-to-exporter-jar>=[config-arguments] <rest-of-your-java-arguments>
```

### [Option 2] Loading dynamically

Start target application normally, then run the exporter jar as a stand-alone application:
```sh
$ java -jar <path-to-exporter-jar> [config-arguments]
```

Then follow the wizard to attach the agent to a locally running JVM.

## Exporter Configuration Arguments

You can optionally pass in arguments to affects how the exporter behaves. A config arugment string follows the format below:
```
[port=<int>][,hostname=<string>][,config=<path_to_config.xml|builtin_config_name|<event-name>#<setting-name>=<value>[:...]>][,authorization=<username>:<password>]
```

### Example
```
$ java -javaagent:./build/libs/jfr-prometheus-exporter-1.0-SNAPSHOT.jar=port=8081,hostname=localhost,config=jdk.ActiveRecording#enabled=true:jdk.CPULoad#enabled=true,authorization=user:password myapp.jar
```

By default, the exporter endpoint will be running on `http://0.0.0.0:8080/metrics` with `default` JFR configuration.

## Testing

``// TODO``

## License

This project is licensed under the [GNU GENERAL PUBLIC LICENSE (Version 3)](LICENSE).
