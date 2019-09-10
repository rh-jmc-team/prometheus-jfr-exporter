# Prometheus JFR Exporter [![Build Status](https://travis-ci.org/tabjy/prometheus-jfr-exporter.svg?branch=master)](https://travis-ci.org/tabjy/prometheus-jfr-exporter)

a collector that scrapes JFR events from a JVM target at runtime for Prometheus to use

## Obtaining the Exporter

You can obtain a agent jar by downloading a prebuilt artifact or building from source.

### [Option 1] Using a Prebuilt JAR

Download the prebuilt jar from the [release page](https://github.com/tabjy/jfr-prometheus-exporter/releases).
```sh
$ wget https://github.com/tabjy/prometheus-jfr-exporter/releases/latest/download/prometheus-jfr-exporter-1.0-SNAPSHOT-all.jar
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
  $ git clone https://github.com/tabjy/prometheus-jfr-exporter.git
  ```
- Run gradle to build a fat jar.
  ```sh
  $ cd prometheus-jfr-exporter
  $ ./gradlew shadowJar
  ```
- Find the built jar in `build/libs` directory.
  ```sh
  $ ls build/libs/prometheus-jfr-exporter*.jar
  ```

## Running a Exporter

JFR Prometheus Exporter accesses recordings using JMX connections. Before running this program, make sure you have another JVM instance running and listening for JMX connections.

### Usage

```sh
$ java -jar ./prometheus-jfr-exporter-1.0-SNAPSHOT-all.jar -h
Usage of Prometheus JFR exporter:
  program <[jmxHostname][:jmxPort]> [[httpHostname][:httpPort]] [option...]

Options:
  -eventConfiguration <path>  a location where a .jfc configuration can be found
  -destinationFile <path>     a location where data is written on recording stop
  -dumpOnExit [bool]          set this recording to dump to disk when the JVM exits
  -maxAge <time>              how far back data is kept in the disk repository
  -maxSize <size>             how much data is kept in the disk repository
  -name <name>                a human-readable name (for example, "My Recording")
```

### Example
```
$ java -jar ./prometheus-jfr-exporter-1.0-SNAPSHOT-all.jar localhost:9091 0.0.0.0:8080
```

By default, the exporter endpoint will be running on `http://0.0.0.0:8080/metrics` with [`default.jfc`](./src/main/resources/com/redhat/rhjmc/prometheus_jfr_exporter/default.jfc) event configuration.

## Testing

``// TODO``

## License

This project is licensed under the [GNU GENERAL PUBLIC LICENSE (Version 3)](./LICENSE).
