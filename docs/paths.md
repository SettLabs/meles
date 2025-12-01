# Paths

## What is it?

Paths are the name for the xml scripts used in meles to determine the 'path' data takes from 'raw' to 'processed'.

Paths have four distinct nodes/steps:
- Filter/if/case : These check if the data meets certain 'rules'. If not, no following steps will be done.
- Editor : These are used to alter the data using string operations fe. adding/removing things.
- Math : These alter the data using mathematical formulas, fe. applying calibration coëfficients
- Store : This is most often the final step. Once the data is fully processed, store it in rtvals and potentially trigger database inserts

## Filtering

See Filters

## Editing ascii data
See Editors.


## Customsrc

If you want to check if a forward is working as expected, it's possible to create the data stream locally.

````xml
<path>
    <plainsrc>Hello World!</plainsrc> <!-- Will send Hello World every second -->
    <plainsrc delay="100ms" interval="10s">Hello??</plainsrc> <!-- Will send hello?? every 10s with initial delay of 100ms -->
    <rtvalsrc>{random:6} and {d:rolls_max}</rtvalsrc> <!-- Allows for lines that combine constants and rtvals -->
    <filesrc>todo</filesrc> <!-- will send the data from all the files in the map one line at a time at 1s interval -->
    <cmdsrc>st</cmdsrc> <!-- will send the result of the cmd -->
</path>
````