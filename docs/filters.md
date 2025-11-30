# Filter

The filter has three different versions which determine how the rest is handled.
- if : Like regular programming. If ok, the steps within the node are executed. Following nodes are unaffected.
- filter : Data needs to pass the checks in order for the following steps to be done.

Simple example.
```xml
 <path delimiter="," id="gps">
        <!-- Global filter, check if data received is valid nmea  -->
        <filter check="nmea"/>
		<!-- If it's valid nmea, check if it's a GPGGA string -->
		<if start="$GPGGA">
            <!-- Do things here if it starts with $GPGGA -->
        </if>
</path>
```

## If structures

Filter allows the use of `if` nodes.
These work as you expect in any other languge, meaning that if the data is altered in one if, the next one starts from that data.
For now, there's no real concept of 'local data' but there are workarounds.

```xml

<path delimiter="," id="gps">
    <if start="$GPGGA">
        <!-- Do things here if it starts with $GPGGA -->
    </if>
    <if start="$GPZDA">
        <!-- Do things here if it starts with $GPZDA -->
    </if>
</path>
```

If this is unwanted behaviour, the `return` node can be used to stop the path directly.
Which essentially allows an switch structure.

```xml

<path delimiter="," id="gps">
    <if start="$GPGGA">
        <!-- Do things here if it starts with $GPGGA -->
        <return/> <!-- The path doesn't go beyond this point -->
    </if>
    <if start="$GPZDA">
        <!-- Do things here if it starts with $GPZDA -->
    </if>
</path>
```
## Rules
- Delimiter selection done in a parent node is used by child nodes unless a local one is provided.
- All rules don't have to end on 's'. If this seems more logical, it can be omitted. For example, both item and items are valid.
- I'd like to use '!' to invert filters, but not allowed in xml.

## Filtering options

### Logic

Does a simple logical check based on the received data.
Do note that the symbols <,> etc aren't allowed in XML...

**Known keywords:**
The shown x and y can be both a reference to an item or a constant.
The items start counting from zero and are abbreviated with i0,i1 and so on.

- x **(not) below** y: eg. i1 below 25 (i0 is lower than 25)
- x **(not) above** y: eg. i0 not above i1 (or i1 is the largest of the two)
- x **at least** y:  eg. i1 at least 10 (i1 is 10 and higher)
-x **equals** y: The two values are identical.
- **from** x **to** y: In the range x to y

**Combining**
- The usual 'and' and 'or' are accepted, and so is 'exor'.
- At the moment only one can be applied.

**Examples**
```xml
<path id="demo" delimiter=",">
    <!-- The data is FF,1250,5 -->
    <!-- splitting on ',' this gives: i0=invalid, i1=1250, i2=5 -->
    <if logic="i1 below 2500 "> <!-- The item on position one needs to be below 2500 -->
        <!--The check is cleared -->
    </if>
    <!-- Or from the opposite end -->
    <if logic="i1 below 2500 and i1 above 10"> <!-- Supports both 'and' and 'or' -->
        <!--The check is cleared -->
    </if>
</path>
````

### Item count
These split the data according to the given delimiter and compare the resulting item count.
* items : Checks if the count is within the given range.
* maxitems : Checks if the count is at most the given value.
* minitems : Checks if the count is at least the given value.

```xml
<path id="demo" delimiter=",">
    <if items="2,4">
        <!-- This is done if the item count after splitting on ',' is 2, 3 or 4 -->
    </if>
    <if delimiter=";" items="3">
        <!-- This is done if the item count after splitting on ';' is 3 -->
    </if>
    <if maxitems="4"> <!-- Check if after splitting on , the result is at most 4 --> 
        <!-- Other steps if check succeeds -->
    </if>
    <filter minitems="3"/> <!-- Don't go beyond this point if there are less than 3 items -->
    <!-- Other steps if the check succeeds -->
</path>
```
### Text matching
These check a certain part of the data for the occurrence of specified text.

```xml
<path id="demo" delimiter=",">
    <!-- The data received must start with the given text. -->
    <if start="$GPGGA">
        <!-- This is done if the data starts with $GPGGA -->
    </if>
    <!-- Or the opposite. -->
    <if nostart="$GP">
        <!-- This is done if the data doesn't start with $GP -->
    </if>
    
    <!-- The data received ends with the given text. -->
    <if ends="!">
        <!-- This is done if the data ends on a '!' -->
    </if>

    <!-- The data received contains the given text. -->
    <if contains="@"> <!-- include is also valid -->
        <!-- This is done if the data contains a '@' -->
    </if>
    <!-- Or the opposite -->
    <if nocontains="@">
        <!-- This is done if the data doesn't contain a '@' -->
    </if>

    <!-- The data must match the given regex -->
    <if regex="[A-Z]*">
        <!-- This is done if the data only contains A to Z -->
    </if>
</path>
```

### Character matching
If a character needs to be at specific position in the data, these can be used.

```xml
<path id="demo" delimiter=",">
    <!-- The data is $GPGGA,12345683212,*AF -->
    <if c_start="2,G"> <!-- At position two (starting from 0), the character needs to be a G -->
        <!--The check is cleared -->
    </if>
    <!-- Or from the opposite end -->
    <if c_end="2,*"> <!-- The third letter starting from the end (start counting at 0), needs to be a '*'-->
        <!--The check is cleared -->
    </if>
</path>
````

### Data Length
Checks if the total length of the data corresponds to the requirement.
```xml
<path id="demo" delimiter=",">
    <!-- The data is $GPGGA,12345683212,*AF -->
    <if minlength="10"> <!-- The data needs to be at least 10 characters long -->
        <!--The check is cleared -->
    </if>
    <!-- Or the opposite -->
    <if maxlength="10"> <!-- The data needs to be at less than 10 characters long-->
        <!--The check is cleared -->
    </if>
</path>
````

### Other

- Check nmea: Check if the data is a valid nmea string (verifies checksum).
```xml
<path id="demo" delimiter=",">
    <filter check="nmea" />
</path>
````