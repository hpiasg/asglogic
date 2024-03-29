ASGlogic
--------

ASGlogic is a logic synthesiser, which generates a Verilog netlist from an STG specification.

### Installation ###

Download and unpack the appropriate package for your operating system (on UNIX with 64-bit architecture, 32-bit support is required). All external tools needed for operation are included in the package. You don't have to install anything or make changes to environment variables. To run it you will need a Java runtime environment (JRE) v1.7 (or later).

### Configuration ###

You should generate the configuration files with [ASGconfigGen](https://github.com/hpiasg/asgconfiggen).

#### Main configuration file ####

The default main configuration file is `ASGlogic_DIR/config/logic_config.xml`. You can specify another config file with the `-cfg <file>` option of ASGlogic.

The `<workdir>` tag specifies a path where ASGlogic stores all temporary files during operation. The default value is empty (and therefore a default operating system directory is used). You can override these settings with `-w <dir>` option of ASGlogic.

The `<defaulttech>` tag specifies the path to the default technology used for implementation (The file must be in [Genlib format](https://www.ece.cmu.edu/~ee760/760docs/genlib.pdf)).

#### External tools configuration file ####

The external tools configuration file specifies how third party (and ASGtools) are called from within ASGlogic. For details see [ASGconfigGen](https://github.com/hpiasg/asgconfiggen).

ASGlogic uses the following external tools:
* DesiJ (v3.2.1 is included)
* Espresso (v2.3 is included)
* MPSAT (v6.31beta is included)
* Petrify (v5.2 is included)
* PUNF (v9.11beta (parallel) is included)


### Usage ###

For the following example commands it is assumed that your current working directory is the ASGlogic main directory. If you want run ASGlogic from another directory you have to add the path to the ASGlogic main directory in front of the following commands (or you could add the `bin/` directory to your `PATH` variable).

#### Runner ####

To run a graphical tool featuring input masks for all important command line arguments execute

    bin/ASGlogic_run

#### List of supported arguments ####

To see a list of supported command line arguments execute

    bin/ASGlogic

#### Synthesis ####

To synthesise a circuit with default values execute

    bin/ASGlogic -lib tech/tech_gen.lib stg.g

The `-lib` option expects a technology library file in the [Genlib format](https://www.ece.cmu.edu/~ee760/760docs/genlib.pdf) (which is also used by [Petrify](http://www.lsi.upc.edu/~jordicf/petrify/)).

The command will create the files `logic.v`, `logic.log` and `logic.zip`. `logic.v` contains the Verilog implementation of the STG. `logic.log` is the log file of the operation. `logic.zip` contains all temporary files created during operation. You can change these default filenames with the following parameters:
* `-out` specifies the filename of the Verilog implementation
* `-log` specifies the filename of the log file
* `-zip` specifies the filename of the zipped temporary files

#### External CSC solving ####

By default ASGlogic just checks an STG for CSC and aborts if it's not satisfied. You can instruct it to use external programs to solve CSC (if needed) with the `-csc` option. To solve CSC with Petrify use `-csc P` and `-csc M` to solve CSC with [PUNF/MPSAT](http://homepages.cs.ncl.ac.uk/victor.khomenko/tools/tools.html).

#### Architecture ####

You can specify which standard architecture ASGlogic should use to implement the circuit. With `-arch sC` it will use a standard C-Element architecture (and will use actual C-Elements - if there are none in the library it will abort). With `-arch gC` it will implement the circuit with a generalised C-Element architecture (using set/reset dominant C-Element/RS-Latch functions).

#### Reset insertion ####

With the `-rst` option you can specify whether ASGlogic will insert reset logic for all signals (`-rst full`) or just for signals which are not self-reseting (i.e. that the levels of input signals at reset time will result in the wrong signal level) with `-rst ondemand`. There might be more options for reset insertion in the future.

### Build instructions ###

To build ASGlogic, Apache Maven v3.1.1 (or later) and the Java Development Kit (JDK) v1.8 (or later) are required.

1. Build [DesiJ](https://github.com/hpiasg/desij)
2. Build [ASGcommon](https://github.com/hpiasg/asgcommon)
3. Build [ASGwrapper-asgtools](https://github.com/hpiasg/asgwrapper-asgtools)
4. Build [ASGwrapper-asynctools](https://github.com/hpiasg/asgwrapper-asynctools)
5. Install libraries, that can't be obtained from a central Maven repository

    ```
    mvn install:install-file -Dfile=./src/main/resources/lib/JavaBDD/javabdd_src_1.0b2.jar -DpomFile=./src/main/resources/lib/JavaBDD/pom.xml
    ```

6. Execute `mvn clean install -DskipTests`