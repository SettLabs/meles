## Changelog
Note: Version numbering: x.y.z 
  -> x goes up with major breaking api changes
  -> Y goes up with major addition/changes
  -> z goes up for minor additions and bugfixes

### To do/fix
- back up path for sqlite db etc? for when sd is missing/full...

## 1.1.0 (wip)

### XML
- Started on xsd validation of the xml files, nodes done:
  - databases 
  - telnet
  - streams
  - i2c
  - taskmanager
  - email
  - mqtt (partial)

### MQTT
- Fixed: Data stream can now be stopped in telnet.
- Moved code to MQTTv5
- Enabled the use of properties:
  - 'Datatype' is added by default and based on the used rtval.
  - A timestamp can be added.
  - The unit of the rtval can be added.
  - Broker level config in xml to disable the use of properties.
- Added option for subscriptions node with topic subnodes instead of subscribe in root.
- Requestable stream didn't include topic info, current format <topic>:<value>
- General cleanup of the code, should fix more bugs than it introduces.
- Other modules can now request the writable. Allows directly publishing a topic.

### Drawio (Still experimental)
- Global changes/additions
  - If no label is given to an arrow connecting two shapes, `next` is given as default.
  - The method that updates the melesid's now first checks to see if the node already has that id. No write is done if
nothing is changed.
  - Added support for the property `meleslabel` on arrows to allow visual styles without visible label.
- Added flagval
    - Active: Flagblock that allows set/clear/toggle of a flagval
    - Reactive: Flagval that allows arrows that depend on the effect the update had (h->h,l->l,l->h,h->l)
        - Can connect a conditionblock to a flagval using 'update', this adds a reference to the flagval in that
          conditionblock that triggers the update with the result of the condition.
        - Made it so a conditionblock that is connected to a flagval encapsulates it and takes over its 'next'
- Added GPIO, pretending to be flagval. 
  - Input can trigger on rising and falling edge, just like regular flag.
  - Because these are flagvals under the hood, everything that can be done with those is also possible.
  - This involved adding extra classes, xml version still uses 'old' ones.
- Controlblock 
  - Can now be used for 'any' taskblock instead of only origin block and allows `start` as alternative to
  `trigger`.
  - Fixed, if its trigger target isn't created by a task, meaning it's just an alternative path, the id was
    taken from the next target. Now it get [T]|0 and so on appended to its id. 
- Conditionblock received a new arrow with label 'update', when connected to a flagval, it will update the state
  according to the result.
- Reader/Writer block
  - Allow the target/source to be split in two properties target+targettype and source+sourcetype.
  This is done to allow only showing id on the block.
  - Reader block can now read emails
  - Reader block now has selectable check instead of only equals ignorecase.
    - Equals,equalsignorecase,startswith,contains and regex.
  - Reader now supports a third outgoing arrow 'error' or 'failure' to indicate a issue with the source.
- Intervalblock and delay block
  - Now use the alternative route when they get cancelled/stopped.
  - Now use ms as timeunit instead of second
  - Added `retrigger` property to `delayblock`, options are restart,cancel,ignore.
- Added referring to numericalvals (real,integer,boolean) in log blocks {group_name}
- Emailblock now is an actual block instead of wrapped command block and the 'next' is triggered only if the email is send.
- MQTT block
  - Triggering this causes a publish to be done. Either a rtval or a fixed value. 
  - Has a parameter 'expiretime' to define how long attempts of sending it can be done
before giving up.
- Rtvalsblocks
  - No longer require a conditionblock to start a sequence after an rtval update (code inserts a dummy).
  - A post update sequence is now actually after an update was done (it's part of the update sequence in code
just the user could put it afterwards but then calls to the val being updated, returned the old value)
  - Consecutive conditionblocks after a rtvalblock now all can use new/old as long as no other block besides
logblock is inbetween.

### Database Manager
- Added option to refer to a flag to determine if inserts are allow in a table or not.
  If the flagval isn't defined anywhere yet, it will created and added to the global pool.
```xml
      <table allowinsert="sensor_doinserts" name="data">
        <real>temp</real>
        <real>other</real>
      </table>
```
- Auto generated timestamps now properly work via rtvals.

### Emailworker
- Added subscribing to email content using `email:read` command.
- Added passing a writable to 'callback' the result of a send attempt.
- Changed it so the `from` node (in outbox) can use a ref instead of only full emailaddress.
- Default `from` if none is given is now the admin ref, used to be meles@email.com.
- Fixed the xml reading and allow the email node to be under the root instead of inside the settings node.

### Math parser
- Can now process `a+1<b` allows more advanced expressions in conditionblock.

### Streams
Started a long overdue refactor of netty related code
- Using built in reader idle again (not sure why this got changed years ago).
- Moved writing raw data to logging framework into the pipeline.
- StreamManager changes, to 'demote' it to a pool instead.
  - Moved acting on events (idle etc) into the stream class
  - Moved reconnect triggering into the stream class, and added backoff interval instead of fixed
  - The connect logic was done in a worker thread, didn't make sense actual connecting is done by eventloopgroup...
  - 
- Changed timestamp (in BaseStream) to atomic because it's read by another thread
- Cleaned up BaseStream:
  - Removed unused code
  - Moved things minority of extending classes use to them
- Changed parent of UDPstream to TCPstream (was BaseStream)
- Removed re-use of bootstraps... this was just wrong.
- Moved the framing of modbus packets as a separate step in the pipeline of modbus tcp.
- 

### Fixes
- Datagram always added a : even without arguments, changed that. This was reason for exit no longer working in telnet.
- Taskmanagerpool watcher 
  - gave an error on linux because it wasn't using an absolute path.
  - tried to monitor even without a valid folder

## 1.0.0 (15/05/2025) - renamed project and reset version to 1

- Added EXPERIMENTAL support for reading draw.io files for configuration as alternative to xml.
- Rewritten TaskManager and PathForward to move to a factory making small functional parts that link together.
- Rewritten RealtimeValues, to make them leaner by splitting of 'extras' and implement the new logic parser.
- Added rawworker for bulk processing of data using multithreading and staged processing.
- Math and logic parsing was all over the codebase, centralized in evalcore and streamlined the layout. Logic got a
  major rewrite to allow for 'lazy evaluation ' (aka shortcircuit'ing).
- A lot of QoL fixes/changes and the cleanup based on Codacy feedback.
- Result is a diff of "211 files changed, 20413 insertions(+), 20338 deletions(-)"
- Major version bump because a lot has changed under the hood and major bump was overdue anyway.

### General

- Fixed 210 issues found by [Codacy](https://app.codacy.com/gh/michieltjampens/meles/dashboard). (none actually critical)
- Refactored **a lot**
- Cleaned up the full code base.

### Draw io integration

- Parser converts shapes (object in xml) to Java Objects containing properties and links.
- Parser traverses tabs but doesn't retain info on where it came from.
- Added file watcher so it's possible to have meles auto reload (disabled for now).
- Started making it possible to configure certain parts using drawio.
  - Task manager blocks have a drawio equivalent, added some 'virtual' ones to simplify common
    actions. Can create a taskmanager using a drawio,
    - Origin blocks have two properties that determine auto start, both default to no.
      - `autostart` starts the task on bootup or reload (yes/no,true/false)
      - `shutdownhook` starts the task when the system is shutting down (useful for cleanup) (yes/no,true/false)
  - Rtvals is work in progress, realval is mostly done. Can represent and generate. Not all
    possible iterations tested (it's rather flexible).
- Initial steps on annotating the drawio file. After parsing, the shapes get their meles id
  added as a property. This makes annotating easier as it no longer requires a lookup table. This does
  create infinite loop with autoreload so turned it off for now.
  - Proof of concept involved having the origin block show the amount of runs, updated every 30s.

### Task Manager

- Complete rewrite
- Modular instead of the earlier monolith, that's the last legacy monolith torn down.
- Most functionality is retained.
- Added retry/while nodes to make those more flexible and potentially easier to understand.
  Difference is that retry is done till condition is met (up to retries) and while repeats till condition is no longer
  met (up to maxruns).
  Both have a short and long form.
```xml

<retry retries="5" onfail="stop"> <!-- onfail can be 'continue', then the rest of the task is done -->
  <stream to="Board">C2;C4</stream>
  <delay>20s</delay>
  <req>{pump_warmup} equals 1</req> <!-- if this fails, the block is retried -->
</retry>

<while interval="5s" maxruns="6">{gas_temp} above 20</while>
```

### Stream manager
- If a stream is disconnected messages are no longer logged every attempt
- Removed 'prefixorigin' option, the `>>prefixid` in telnet is probably the only use case and better.
- `Sx:data` or `streamid:data` cmd for sending data can now mix binary and ascii, so `S1:\h(0x24)=dollarsign` is now
  valid.
- `ss:buffers` command gave an error when no buffers were in use, replaced with reply that no buffers are in use.
- The triggered actions (write,cmd) can now be in the <triggered> node instead of directly in <stream> node. Purely
  a visual change. This is now the default when adding through commands.
- `ss:id1,tunnel,id2` This command is now persistent.
- `ss:id,link,source` changed to `ss:id;request,requestcmd` to make the function clearer, now also persistent.
- Fixed, adding tcpserver, wasn't added to list because of nullpointer.Also added it to the help.
- Command help now lists how to add the servers.
- Fixed, adding a udp server, wasn't added to xml nor the manager, just started.
- Changed status message of the udp client to show it's send only.
- TCP server port was in an address node instead of port
- Fixed, The time since last data received wasn't determined for serial with empty eol
- Fixed `ss:id,eol,` for clearing eol.
- Now gives an error when trying to connect to an ip:port or serial port already in use.

### Telnet

- It's now possible to use `>>es` and `>>ts` or `>>ds` at the same time.
- Spaces are added as padding to line up `>>es` result because it's variable length (up to 8).
- Changed color of `>>es` to cyan so the output doesn't look like ts/ds (which is in orange).
- `>>?` àdded for a list and info on the available commands.
- Added `>>prefixid` does the same as `>>prefix` does/did but made more sense to show it prefixed the id.
- Added `telnet:writable,id` allows a telnet session to request the writable of another session. Instead
  of the id, * is valid for the first session that isn't the current one.
- Added ` >alt` suffix for commands issued through telnet, this will use the writable of the other
  session in the datagram so the result is shown there instead.

### Store
- Breaking, Changed the order of the add command so that group is no longer optional.
- Calculations should be slightly more performant because of decreased parsing.
- Added command to alter idlereset of a store.
- Fixed, Rtvals that are calculated are now also reset if idlereset is true.
- Fixed, Store that use map can now also use calculated values.

### RealtimeValues

- Rewrote it to with the intention to make it leaner and more flexible.  
  I thought that the various val's had become to 'bloated', especially RealVal and IntegerVal. They all had
  the code to do a lot of things that weren't used in 99% of the cases. So I set out to make
  them 'simpler', not sure if I failed or not... On the surface they probably are
  (as in, the code is a lot shorter), but they now are much more a reactive component
  within meles and thus have outgrown their rule of 'short term memory'.

  * RealVal, IntegerVal (aka real and integer container)
    * Optional pipeline in update method (where new value replaces old)
      * Precheck the incoming data
        * Allow both simple logic or using references to other ones
        * Set `cmds` to execute on pass or fail, those can reference both new and old value
        * Propagate the result or always allow the next step
      * Math expression(s) applied
        * Alter received data before it's applied
        * Receives both old and new value
        * Refer to other vals if needed both is input and output (as in directly trigger update of another)
        * Not needed to actually use incoming data
        * Allowed to overwrite either memory slot, new value slot will be applied.
      * Post Check of the calculated data
        * Same as pre check but now it can additionally respond to calculated values. This means math could calculate
          offset between new and old and this can be checked.
    * No need to use the full pipeline, can just 'activate' the bits needed.
    * **Aggregator** variant of both classes that acts as a collection, 'update' insert in the collection and requesting
      the
      value returns the results of a builtin reducer function. (think average, variance and so on)
    * **Symbiote** variant to hide the complexity of derived values, this 'takes over' a realval or integerval and
      propagates
      any update that would be done on it to a collection of vals that are derived from it. Think min,max and so on.
  ```xml
   <group id="test">
      <flag def="true" name="flag"/>
      <real def="0" scale="3" name="pressure" unit="Bar">
        <derived def="0" reducer="variance" suffix="variance" window="50">
          <!-- i0 refers to the received value and i1 would be the 'old' one -->
          <derived def="0" math="i0/(50-1)" suffix="sample"/>
          <!-- final name: pressure_variance_sample -->
          <derived def="0" mainsuffix="population_variance" math="i0/50"/>
          <!-- final name: pressure_population_variance -->
          <derived builtin="sqrt" def="0" mainsuffix="stdev"/>
          <!-- final name: pressure_stdev -->
        </derived>
        <derived builtin="max" def="0" suffix="max"/>
        <!-- final name: pressure_max -->
        <derived builtin="min" def="0"/>
        <!-- final name: pressure_min because no suffix or name defined -->
      </real>
  </group>
  ```

* FlagVal (aka boolean container)
  * Can have both `cmd` as response as triggering update of other val.
  * Four distinct trigger options:
    * Raising edge (false->true)
    * Falling edge (true->false)
    * Stay high (true->true)
    * Stay low (false->false)
  * Meaning these can trigger an update of an IntegerVal that is set to 'counter' mode. (that just means it ignores the
    input and adds one to the current value);
  * No logic(besides discerning triggers) or math involved in this because didn't seem useful.
* TextVal (ake String container )
  * Nothing really changed to this, remains a simple container.


- Fixed, `ss:id,reloadstore` didn't properly reload if a map was used.
- Fixed, `*v:id,update,value` wasn't looking at * but to id instead

### GIS
- No longer possible to use duplicate id's for waypoints or geoquads.

### Database Manager
- Fixed, tables weren't read from sqlite db.
- MariaDB, can add a node <timeprecision> to set the amount of digits in the seconds default 0.
- Server connect is now threaded, 5 consecutive fails trigger a query dump to csv. Logging is also rated.
- Rollover now uses the same format as everywhere else.
- Rewrote how reloading of the database works, now the instance is retained instead of a 'clean slate'

### Paths

- Rewrote PathForward to be similar in logic as taskmanager.
- Targets are stored between reloads, so updates are 'live'.
- Editor, indexreplace used an inmutable list so didn't work...
- Editor, added some alternives to the types to maybe make it more straightforward rex -> regex and so on.
- Updated `help:editor` to use node tags instead of type attribute.
- Fixed, response to `p:id,new...` is how it should be again.
- Fixed, src was cleared when reading from xml and the path is in the settings.xml
- Added `pf:pathid,switchsrc,newsrc` to switch the src for a path at runtime, can be used to implement redundancy.
  This won't alter the xml.
- Reloading a path now triggers a check if the db tables that have missing rtvals now can find them.
- Filter now allows the use of 'or' and 'and' and ,like the editor, it's now possible to
  use the tag name instead of the type attr.

```xml

<filter>
  <rule type="contains">1000</rule>
  <or/> <!-- or and, if none is mentioned, defaults to 'and' -->
  <contains>-6</contains>
  <contains>1000 OR -6</contains> <!-- Does the same as the three lines above -->
</filter>
```

```xml
<if contains="1001 OR -4"> <!-- Is also possible now -->
</if>
```

- Added the `return` node, mainly useful in combination with the `if`
  - For now only one behavior will add others in the future
  - Turns two consecutive if's into an if/else if

```xml

<path>
  <if contains="1001">
    <!-- do stuff -->
    <return/> <!-- After arriving here, leave the path early -->
  </if>
  <if contains="-4"> <!-- Is also possible now -->
    <!-- Do other stuff -->
  </if>
</path>
``` 

### Evalcore (package containing Logic and Math parser)

- Moved all code related to both logic and math parser to their own package.
- Rewrote the layout of both parsers, now the code is no longer split over modules according to requirements
  but one single central one.
- Each has a 'fab', LogicFab and MathFab that fabricate evaluators MathEvaluator and LogicEvaluator.
- Now the chaining of Math expressions is part of the class instead of needing a separate one. But it's not like an
  Evaluator contain multiple expressions (for now).
- Logic parser was rewritten to allow more complicated expressions (brackets and negation) and the parsing
  now attempts 'lazy evaluation' or 'shortcircuit' when before it just evaluated everything. No nesting of brackets
  (yet) though. That lazy bit is well lazy, it splits the expression on and/or (accounting for brackets) and evaluates
  the one that might lead to a solution first. So e.g. (a<b && c>d)||e<=f ,it will do e<=f first.

### Raw worker

- Added worker to reprocess raw log file if it doesn't require realtime data  (except in store)
- How it works:
  - Reads data from a single day
  - Each line gets a counter prefix for sorting it later
  - Starts stage 1 processing (these are things that can be multithreaded)
  - Data is sorted to match the original sequence
  - Stage two is done with the sorted data (this is the store to DB step)
  - Read the next day

````xml

<rawworker filepath="todo">
  <stage1>
    <!-- Path forwards except store -->
  </stage1>
  <stage2>
    <!-- Path forwards including store -->
  </stage2>
</rawworker>
````