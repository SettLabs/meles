package util.drawio;

import io.hardware.gpio.GpioFab;
import io.hardware.gpio.InputPin;
import io.hardware.gpio.OutputPin;
import io.netty.channel.EventLoopGroup;
import org.tinylog.Logger;
import util.data.vals.FlagVal;
import util.data.vals.OneTimeValUser;
import util.data.vals.Rtvals;
import util.data.vals.ValFab;
import util.tools.TimeTools;
import util.tools.Tools;
import util.xml.XMLdigger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class GpioParser {

    public static HashMap<String, FlagVal> parseDrawIoGpios(Path file, EventLoopGroup eventLoop, Rtvals rtvals) {
        if (!file.getFileName().toString().endsWith(".drawio")) {
            Logger.error("This is not a drawio file: " + file);
            return new HashMap<>();
        }
        var tls = new TaskParser.TaskTools(eventLoop, null, rtvals, new HashMap<>(), new HashMap<>(), new HashMap<>());
        ArrayList<RtvalsParser.ValCell> vals = new ArrayList<>();

        var cells = Drawio.parseFile(file);
        return parseGpios(cells, tls, vals, file);
    }

    public static HashMap<String, FlagVal> parseDrawIoGpios(HashMap<String, Drawio.DrawioCell> cells, EventLoopGroup eventLoop, Rtvals rtvals, Path file) {
        var tls = new TaskParser.TaskTools(eventLoop, null, rtvals, new HashMap<>(), new HashMap<>(), new HashMap<>());
        ArrayList<RtvalsParser.ValCell> vals = new ArrayList<>();
        return parseGpios(cells, tls, vals, file);
    }
    public static HashMap<String, FlagVal> parseXMLGpios( XMLdigger dig, Rtvals rtvals) {

        var pins = new HashMap<String, FlagVal>();

        for (var ele : dig.digOut("*")) {
            var name = ele.attr("name","");

            if( !ele.hasPeek("flag")) {
                Logger.error("No flag defined for "+name);
                continue;
            }

            if (ele.tagName("").equals("inputpin")) {

                var pullPlace = ele.peekAt("pulllogic").value( "none");
                var pull = "none";

                var idle = ele.peekAt("idle").value(false);
                if (pullPlace.startsWith("int")) {
                    pull = idle ? "up" : "down";
                }

                var trigger = ele.peekAt("trigger").value("rising");

                Logger.info("gpio-> Building input at " + name + " ->  " + pull + "&" + trigger);

                var input = GpioFab.buildInput(name, pull, trigger);
                if (input == null) {
                    Logger.error("Failed to build input: " + name);
                    continue;
                }
                var period = ele.peekAt("debounce").value("0s");
                var ms = TimeTools.parsePeriodStringToMillis(period);

                ele.digDown("flag");

                var ip = new InputPin( ele.attr("group", ""), ele.attr("name", ""), ele.attr("unit", ""), input);
                ip.setDebounceMs(ms);

                ValFab.alterFlagVal(ele,ip, rtvals );

                pins.put(ip.id(), ip);
            } else  if (ele.tagName("").equals("outputpin")) {

                var activeHigh = ele.peekAt("activehigh").value(true);
                Logger.info("gpio-> Building output at " + name + " ->  activeHigh?" + activeHigh);

                var output = GpioFab.buildOutput(name, activeHigh,false);
                if (output == null) {
                    Logger.error("Failed to build output: " + name);
                    continue;
                }
                var op = new OutputPin(ele.attr("group", ""), ele.attr("name", ""), ele.attr("unit", ""), output);
                ValFab.alterFlagVal( ele, op, rtvals );
                pins.put(op.id(), op);
            }
        }
        return pins;
    }
    private static HashMap<String, FlagVal> parseGpios(HashMap<String, Drawio.DrawioCell> cells, TaskParser.TaskTools tls, ArrayList<RtvalsParser.ValCell> vals, Path file) {

        var pins = new HashMap<String, FlagVal>();
        // Do output pins first because they migh tbe referenced
        for (var entry : cells.entrySet()) {
            var cell = entry.getValue();
            if (cell.getType().equals("outputpin")) {
                var id = cell.getParam("group", "") + "_" + cell.getParam("name", "");
                if( tls.rtvals().hasFlag(id) ) { // Don't repeat if it already exists.
                    Logger.info("Not adding "+id+" again.");
                    continue;
                }
                var name = cell.getParam("gpio", "");
                var activeHigh = cell.getParam("activehigh", true);
                var initState = cell.getParam("default", false);
                Logger.info("gpio-> Building output at " + name + " ->  activeHigh?" + activeHigh);

                var output = GpioFab.buildOutput(name, activeHigh, initState);
                if (output == null) {
                    Logger.error("Failed to build output: " + name);
                    continue;
                }
                var op = new OutputPin(cell.getParam("group", ""), cell.getParam("name", ""), cell.getParam("unit", ""), output);
                RtvalsParser.alterFlagVal(op, cell, tls); // Add all the flag related stuff
                pins.put(op.id(), op);
            }
        }
        for (var entry : cells.entrySet()) {
            var cell = entry.getValue();

            if (cell.getType().equals("inputpin")) {
                var name = cell.getParam("gpio", "");
                var pullPlace = cell.getParam("pulllogic", "none");
                var pull = "none";

                var idle = Tools.parseBool(cell.getParam("idle", "low"), false);
                if (pullPlace.startsWith("int")) {
                    pull = idle ? "up" : "down";
                }

                var trigger = "";
                // rising might not be pressed, but doesn't matter, just need to check presence of both
                if (cell.getArrowTarget("rising", "pressed") != null && cell.getArrowTarget("falling", "released") != null) {
                    trigger = "both";
                } else {
                    if (cell.hasArrow("rising") || cell.hasArrow(idle ? "released" : "pressed"))
                        trigger = "rising";
                    if (cell.hasArrow("falling") || cell.hasArrow(idle ? "pressed" : "released"))
                        trigger = "falling";
                }
                var flagId = cell.getParam("group", "")+"_"+ cell.getParam("name", "");
                var flag = tls.rtvals().getFlagVal(OneTimeValUser.get(),flagId);
                if( !flag.isDummy() ){
                    if( flag instanceof InputPin ip) {
                        Logger.info("The flag ("+flag.id()+") already exist as inputpin, closing it.");
                        ip.close();
                    }
                }
                Logger.info("gpio-> Building input at " + name + " ->  " + pull + " &" + (trigger.isEmpty()?" No triggers":" trigger") );
                var input = GpioFab.buildInput(name, pull, trigger);
                if (input == null) {
                    Logger.error("Failed to build input: " + name);
                    continue;
                }
                var ip = new InputPin(cell.getParam("group", ""), cell.getParam("name", ""), cell.getParam("unit", ""), input);
                var period = cell.getParam("debounce", "0s");
                var ms = TimeTools.parsePeriodStringToMillis(period);
                ip.setDebounceMs(ms);

                Logger.info("Arrow Labels:"+cell.getArrowLabels());
                for( var label : cell.getArrowLabels().split(",")){
                    if( cell.getArrowTarget(label) !=null)
                        Logger.info(" -> Arrow: "+label+" -> "+ cell.getArrowTarget(label));
                }

                RtvalsParser.alterFlagVal(ip, cell, tls); // Add all the flag related stuff
                pins.put(ip.id(), ip);
            }
        }
        TaskParser.fixControlBlocks(cells, tls);
        DrawioEditor.addIds(tls.idRef(), file);
        return pins;
    }
}
