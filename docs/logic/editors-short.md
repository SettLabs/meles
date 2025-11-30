# Editors

Examples start with 'Hello World'
Do note:
- The delimiter can also be set in the path node instead.
- Index can be shortened to just 'i' and start at 0.

## Adding
```xml
<append>?</append>                  <!-- Appends to the end [Hello World?] -->
<prepend>Say </prepend>             <!-- Prepends to the front [Say Hello World] -->

<insert delimiter=" " index="6">round </insert>    <!-- Inserts at the given index (0 is start) [Hello round World] -->
```

## Cutting
```xml
<cutstart>5</cutstart>  <!-- Cut characters from the start [ World] -->
<cutend>3</cutend>      <!-- Cut characters from the end [Hello Wor]-->
<remove>l</remove>      <!-- Remove all occurrences [Heo Word] -->
<trim/>                 <!-- Trim spaces from start and end -->

<removeindex delimiter=" ">1</removeindex>  <!-- Remove value at given index after split [Hello ] -->
<regexremove>[a-z]</rexremove>              <!-- Remove based on the regex  [H W] -->
```

## Replacing
```xml
<replace find="orld">hat</replace>                        <!-- Replaces all occurrences of given find [Hello What] -->
<replaceindex delimiter=" " index="1">Moon</replaceindex> <!-- Replace the element at the index [Hello Moon] -->
<regexreplace find="[l]">L</regexreplace>                 <!-- Replaces regex matches [HeLLo WorLd] -->
```
## Time and Date
```xml
<reformatdate index="0" from="yyyy-MM-dd">ddMMyyyy</reformatdate>
<reformattime i="0" from="HH:mm:ss">HHmmss</reformattime>
<millisdate i="0">yyyy-MM-dd HH:mm:ss.SSS</millisdate> <!-- Parse epoch millis at given position -->

```
## Advanced

These are a bit to complicated for a one line explanation.

### Resplit
```xml
<resplit delimiter=" " leftover="append or remove">elements</resplit>
```