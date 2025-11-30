# Filters - Quick Reference

## Structure
```xml
<if ...>      <!-- Conditional block - doesn't affect following steps -->
<filter ... />  <!-- Gate - blocks following steps if fails -->
```

## Logic
After splitting on delimiter, items are numbered starting from 0.  
e.g: i0,i1 and so on

```xml
<if logic="i0 below 10 and i1 above 5">
<if logic="i2 from 10 to 20 or i3 equals 5">
```

## Item Count
```xml
<if items="3">              <!-- Exactly 3 items -->
<if items="1,3">            <!-- 1 to 3 items, so 1,2 or 3 -->
<if minitems="2">           <!-- At least 2 -->
<if maxitems="5">           <!-- At most 5 -->
```

## Text Matching
Hello world passes these tests.
```xml
<if start="Hello">          <!-- Starts with -->
<if nostart="llo">          <!-- Doesn't start with -->
<if ends="world">           <!-- Ends with -->
<if contains=" wor">        <!-- Contains -->
<if nocontains="bye">       <!-- Doesn't contain -->
<if regex="[a-Z]*">         <!-- Regex match -->
```
**Characters**
```xml
<if c_start="2,e">          <!-- Second letter is an e -->
<if c_end="2,l">            <!-- Second letter from the end is an r -->
```

## Other
```xml
<if check="nmea">           <!-- Valid NMEA checksum -->
<if maxlength="10">         <!-- Max data length -->
<if minlength="10">         <!-- Min data length -->
```