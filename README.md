ASGlogic
--------

ASGlogic is a logic synthesiser, which generates a Verilog netlist from an STG specification.

### Installation ###

Download and unpack the appropriate package for your operating system. All external tools needed for operation are included in the package. You don't have to install anything or make changes to environment variables. To run it you will need a Java runtime environment (JRE) v1.7 (or later).


### Configuration ###

The default configuration file is `ASGlogic_DIR/config/logicconfig.xml`. You can specify another config file with the `-cfg <file>` option of ASGlogic.

The `<workdir>` tag specifies a path where ASGlogic stores all temporary files during operation. The default value is empty (and therefore a default operating system directory is used). You can override these settings with `-w <dir>` option of ASGlogic.

With the `<tools>` tag (and subtags) you can specify the command line to call external tools. Defaults are the included versions of the tools.

### Usage ###

For the following example commands it is assumed that your current working directory is the ASGlogic main directory. If you want run ASGlogic from another directory you have to add the path to the ASGlogic main directory in front of the following commands (or you could add the `bin/` directory to your `PATH` variable).

##### List of supported arguments #####

To see a list of supported command line arguments execute

    bin/ASGlogic

##### Synthesis #####

To synthesise a circuit with default values execute

    bin/ASGlogic -lib library.lib stg.g

The `-lib` option expects a technology library file in the [genlib format](https://www.ece.cmu.edu/~ee760/760docs/genlib.pdf) (which is also used by Petrify).

The command will create the files `logic.v`, `logic.log` and `logic.zip`. `logic.v` contains the Verilog implementation of the STG. `logic.log` is the log file of the operation. `logic.zip` contains all temporary files created during operation. You can change these default filenames with the following parameters:
* `-out` specifies the filename of the Verilog implementation
* `-log` specifies the filename of the log file
* `-zip` specifies the filename of the zipped temporary files

##### External CSC solving #####

By default ASGlogic just checks an STG for CSC and aborts if it's not satisfied. You can instruct it to use external programs to solve CSC (if needed) with the `-csc` option. To solve CSC with Petrify use `-csc P` and `-csc M` to solve CSC with PUNF/MPSAT.

##### Architecture #####

You can specify which standard architecture ASGlogic should use to implement the circuit. With `-arch sC` it will use a standard C-Element architecture (and will use actual C-Elements - if there are none in the library it will abort). With `-arch gC` it will implement the circuit with a generalised C-Element architecture (using set/reset dominant C-Element/RS-Latch functions).

##### Reset insertion #####

With the `-rst` option you can specify whether ASGlogic will insert reset logic for all signals (`-rst full`) or just for signals which are not self-reseting (i.e. that the levels of input signals at reset time will result in the wrong signal level) with `-rst ondemand`. There might be more options for reset insertion in the future.

### Build instructions ###

To build ASGlogic, Apache Maven v3 (or later) and the Java Development Kit (JDK) v1.7 (or later) are required.

1. Build [ASGcommon](https://github.com/hpiasg/asgcommon)
2. Install libraries, that can't be obtained from a central Maven repository

    ```
    mvn install:install-file -Dfile=./src/main/resources/lib/JavaBDD/javabdd_src_1.0b2.jar -DpomFile=./src/main/resources/lib/JavaBDD/pom.xml
    ```

3. Execute `mvn clean install -DskipTests`