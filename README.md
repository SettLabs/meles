Meles
=========
## Your Data Ingestion Workhorse

*Like its namesake, Meles works relentlessly in the background, not just digging and storing, 
but actively routing, transforming, and automating your data flows so Grafana can shine.*

1. **"My sensor speaks Serial, but I need MQTT"** → Meles converts protocols
2. **"I need to calculate derived values in real-time"** → Meles does windowed analytics
3. **"Writing ingestion scripts is taking forever"** → Meles offers visual design (Draw.io)
4. **"My data needs cleaning before storage"** → Meles filters and transforms

Want to get started right away? Check the [Basics Guide](https://github.com/michieltjampens/meles/blob/main/docs/Basics.md).

## Basics

The fundamentals of any datalogger are in place:
- **Data Sources:** TCP (client&server), UDP(client&server), serial, MQTT
- **Data Targets:** SQL databases (MySQL/MariaDB, Postgres, MSSQL, SQLite) and files (custom formats)
- All received data is logged and timestamped by default.

The following sections detail the more advanced features of meles.

## Draw.io based of automation tasks

Starting with version 1.0.0, meles supports reading Draw.io diagrams as a basic form of visual programming.

* **Multitab support** The parser can handle multiple tabs.
* **Minimal visual restrictions** The configuration is defined by shape properties and arrow label/value, allowing users to customize visuals freely.
* **To get started** Some examples of visuals can be found in
  the [drawio folder](https://github.com/michieltjampens/meles/tree/main/drawio), it also contains libraries.
* **Auto-reload on change** For now this does a full reset, hotplugging is planned.
* **Task Blocks** TaskManager blocks have shape
  alternatives, [an md about this](https://github.com/michieltjampens/meles/blob/main/docs/drawio/taskblocks.md)
  * Adds delays and interval or timer based triggers.
  * Reading from/writing to serial,tcp,udp.
  * `Condition block` adds branching logic based on realtime data, allows to jump forward or back in the sequence.
  * Combine all of the above with counters to mimic retry/while/for loops.
  * Command block can issue any command the user could input in telnet. This enables altering realtime data, redirecting
    data flows, trigger database inserts and more, before I make a 'block' for it.
* **GPIO blocks**
  * Enable input pin triggering based on edge.
  * Adds setting an output pin state.
* **Rtvals blocks**
  * Enables reactive logic based on real/integer/boolean variables.
  * An rtval block representing a variable can be joined by two condition blocks in its update path that can check the
    current and new value.
    This can be used to block an update or start a sequence of other task blocks in response.
  * Add a math expression between those condition blocks to do 'last minute' changes or just 'demote' the variable to a
    counter.
* **I2C blocks**
  * Started working on adding i2c comms.

They say a picture is worth a thousand words... (but need to update this).

<img src="drawio/example.png" width="500" height="300">

## Data Collection

meles supports data collection from a variety of sources:

* **Local storage:** Line delimited files, SQLite
* **Local instrumentation:** Serial/TTY, I2C, SPI and files (can be used as pass through from other logging systems).
* **Network-based (LAN):** TCP/UDP (both server and client), MODBUS(ascii), MQTT (client).
* **Internet (WAN):** Email and [Matrix](https://matrix.org/).

## Data in-place processing & Filtering

Configure the [path](docs/paths.md) of the collected data through various modules or steps:

* **Line filtering:** Exclude unwanted data or lines based on specific criteria.
* **String operations:** Modify or format data using regex or custom string manipulation.
* **Mathematical operations:** Perform complex calculations on numeric data.

## Data Forwarding

Flexible routing of data, including:

* **Return to origin:** Send (altered) data back to its original source.
* **Protocol conversion:** Or over any other link, such as serial to TCP.
* **Multi-source support:** Merge multiple data sources
* **Multi-destination support:** Why limit to a single destination?

## Data Storage

Store (processed) data in various formats:

* **Memory:** Data is initially parsed and stored [in memory](docs/rtvals.md).
* **Log files:** Data can be saved in timestamped .log files, providing a simple and accessible history of raw or
  altered data.
* **SQLite:** Stores data locally in a SQLite database, with automatic database and table creation.
* **Server-based databases:** Supports MariaDB, MySQL, PostgreSQL, and MSSQL, automatically creating and reading the
  table structure (though only as an output, reading from these databases is not supported yet).
* **MQTT:** Data can be sent back to an MQTT broker, enabling real-time data forwarding and integration with other
  systems.
* **Email:** An email inbox is technically a storage...

## Scheduling

Handled by the [Task Manager](docs/taskmanager.md), provides flexibility through tasks consisting of:

* **Trigger:** Based on delay, interval or time of day (on a specified weekday).
* **Action:** Send data to a source, send an email, or execute a user command.
* **Additional conditions:** Requirements based on real-time data or repeating until preconfigured conditions are met.

## Triggering

Automate everything a user could do or the tasks mentioned earlier:

* **Directly:** Trigger actions through Telnet, email, or Matrix commands.
* **On predetermined real-time data conditions:** Trigger based on logical operations on real-time data, such as exceeding thresholds or meeting
  conditions.
* **Hardware events:** Respond to events such as a source disconnecting, being idle, (re)connecting, or even GPIO
  events.
* **Geofencing:** Trigger actions on entering or leaving a specified area, such as a four-corner or circular
  zone, with an optional bearing check (only for the circular zone option).

These triggers allow for automation of complex tasks, enabling meles to respond to a wide range of conditions and
events, both from the software and the hardware side.

## Configuration via XML: Simple (ok... that's an opinion) and powerful

At the heart of meles is its command functionality, made possible by configuring everything seen so far through
flexible XML files. Although it might seem complex at first, this approach offers powerful control and easy automation,
making it adaptable to a wide range of use cases.

## Use Cases

### As a Tool

* **Device Control and Monitoring:** Schedule tasks to interact with devices or add hardware to control pumps,
  solenoids or other equipment based on time, sensor data, or geofencing events.
* **Flexible Data Forwarding:** Put a serial device on the network or sniff its traffic to reverse engineer
  communication protocols, enabling seamless integration with other systems or remote monitoring.

### As a logging platform

* **At Home:** Start small: run everything on a Raspberry Pi, logging data from MQTT-connected sensors, all stored in a
  lightweight database. Perfect for local, simple setups.
* **In the Field:** Still on a Raspberry Pi (or similar small device), collect environmental data during
  trips or fieldwork, uploading it to a central server for analysis without the need for a full-scale server setup.
* **On a Research Vessel:** Transition to something bigger on a server: handle more complex data streams from a
  range of sensors. The system tracks and analyzes real-time data, all while supporting remote access and continuous
  logging.
* **On a Buoy:** Back to low power: now you’ve got a system running on a buoy, autonomously collecting and transmitting
  data without the need for large servers, operating efficiently on minimal power in remote environments.
* **In Deep Space:** Over vast distances: a "tiny" nuclear cell (RTG)  might be necessary... but it is possible!

## Installation
* Make sure you have _at least_ java17 installed. If not, [download and install java 17](https://adoptium.net/)
* Either 
  * Download the most recent (pre)release [here](https://github.com/michieltjampens/meles/releases) and unpack to a
    working folder
  * Or clone the repo and build it with Maven (`mvn install`) directly or by using an IDE. Then copy the resulting
    `meles*.jar` and `/lib` folder to a working directory

## Running it
### Windows

* If you have java17+ installed properly, just doubleclick the `meles*.jar`
  * You'll see extra folders and a settings.xml appear in your working folder, confirming a succesful startup.
* If java 17+ isn't installed properly, check the installation step above
   
### Linux
* In a terminal:
  * Go to the folder containing the .jar
  * Run `sudo java -jar meles-*.jar`  (sudo is required to be able to open the Telnet port)
  * To make this survive closing the terminal, use [tmux](https://linuxize.com/post/getting-started-with-tmux/) to start it or run it as a service (see below)
* As a service:
  * If going the repo route, first copy-paste the `install_as_service.sh` file to the same folder as the meles*.jar
  * Run `chmod +x install_as_service.sh file`
  * `./install_as_service.sh`
    * Restarting the service: `sudo systemctl restart meles`
    * Get the status: `sudo systemctl status meles`
    * Read the full log: `sudo journalctl -u meles.service`
    * Follow the console: `sudo journalctl -u meles.service -f`
   * Optionally add bash alias's for easier usage (apply with `. ~/.bashrc)`
     * ```echo "alias meles_restart='sudo systemctl restart meles'" >> ~/.bashrc```
     * ```echo "alias meles_start='sudo systemctl start meles'" >> ~/.bashrc```
     * ```echo "alias meles_stop='sudo systemctl stop meles'" >> ~/.bashrc```
     * ```echo "alias meles_log='sudo journalctl -u meles.service'" >> ~/.bashrc```
     * ```echo "alias meles_track='sudo journalctl -u meles.service -f'" >> ~/.bashrc```
     * ```echo "alias meles='telnet localhost 2323'" >> ~/.bashrc```
  
## First steps

It is recommended to follow [this](https://github.com/michieltjampens/meles/blob/main/docs/Basics.md) guide if it's your
first time using it.

Once running and after opening a Telnet connection to it (default: port 2323), you'll be greeted with the following screen:

<img src="https://user-images.githubusercontent.com/60646590/112713982-65630380-8ed8-11eb-8987-109a2a066b66.png" width="500" height="300">

In the background, a fresh `settings.xml` was generated in your working directory:
````xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<meles>
    <settings>
        <!-- Settings related to the telnet server -->
        <telnet port="2323" title="meles">
            <textcolor>lightgray</textcolor>
        </telnet>
    </settings>
    <streams>
        <!-- Defining the various streams that need to be read -->
    </streams>
</meles>
````
Back in the telnet client, add a data source:
* `ss:addserial,serialsensor,COM1:19200`  --> adds a serial connection to a sensor called "serialsensor" that runs at 19200 Baud
* `ss:addtcp,tcpsensor,localhost:4000`  --> adds a tcp connection to a sensor called "tcpsensor" with a locally hosted tcp server

Assuming the data has the default eol (end-of-line) sequence of `<CR><LF>`, you'll receive the data in the open terminal by typing:
* `raw:serialsensor` --> for the serial sensor
* `raw:tcpsensor` --> for the tcp sensor

Meanwhile, in the background, the `settings.xml` was updated as follows:
````xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<meles>
  <settings>
    <!-- Settings related to the telnet server -->
      <telnet port="2323" title="meles">
          <textcolor>lightgray</textcolor>
      </telnet>
  </settings>
  <streams>
    <!-- Defining the various streams that need to be read -->
    <stream id="serialsensor" type="serial">
      <eol>crlf</eol>
      <serialsettings>19200,8,1,none</serialsettings>
      <port>COM1</port>
    </stream>
    <stream id="tcpsensor" type="tcp">
      <eol>crlf</eol>
      <address>localhost:4000</address>
    </stream>
  </streams>
</meles>
````

Sending `help` in the Telnet interface will provide a list of available commands and guidance on
the next recommended steps. For more in-depth and extensive information, check:
* The docs folder in the repo.
* [The tutorial](docs/README.md)

Oh, and the command `sd` shuts it down.
