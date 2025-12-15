package util.data.vals;

import meles.Commandable;
import meles.Core;
import meles.Paths;
import io.Writable;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.telnet.TelnetCodes;
import org.tinylog.Logger;
import util.LookAndFeel;
import util.data.ValTools;
import util.data.procs.ValPrinter;
import util.data.vals.symbiote.IntegerValSymbiote;
import util.data.vals.symbiote.RealValSymbiote;
import util.data.vals.symbiote.SymbioteTools;
import util.tools.TimeTools;
import util.xml.XMLdigger;
import worker.Datagram;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Rtvals implements Commandable,ValUser {
    /* Data stores */
    private final ConcurrentHashMap<String, RealVal> realVals = new ConcurrentHashMap<>();         // doubles
    private final ConcurrentHashMap<String, IntegerVal> integerVals = new ConcurrentHashMap<>(); // integers
    private final ConcurrentHashMap<String, TextVal> textVals = new ConcurrentHashMap<>();         // strings
    private final ConcurrentHashMap<String, FlagVal> flagVals = new ConcurrentHashMap<>();         // booleans
    private final HashMap<String, DynamicUnit> units = new HashMap<>();

    private final HashSet<ValUser> needUpdates = new HashSet<>();
    private final HashSet<ValUser> needCreations = new HashSet<>();

    EventLoopGroup eventLoop;

    public Rtvals() {
        readFromXML(XMLdigger.goIn(Paths.settings(), "meles", "rtvals"));
    }

    /**
     * Read an rtvals node
     */
    public void readFromXML(XMLdigger dig) {

        if (dig.isInvalid()) {
            Logger.info("No rtvals in settings.xml");
            return;
        }
        Logger.info("Reading rtvals");
        if (dig.hasPeek("group")) {
            dig.digOut("group").forEach(d -> {
                var groupName = d.attr("id", "");
                ValFab.digRealVals(d, groupName, realVals, this);
                ValFab.digIntegerVals(d, groupName, integerVals, this);

                flagVals.putAll( ValFab.digFlagVals(d, groupName, this ) );
                textVals.putAll( ValFab.digTextVals(d, groupName) );
            });
            dig.goUp();
        }
        Logger.info("Reading Dynamic Units");
        dig.digOut("unit").forEach(node -> {
            var pair = DynamicUnit.processUnitElement(node);
            units.put(pair.getKey(), pair.getValue());
        });
    }
    public EventLoopGroup getEventLoop(){
        if( eventLoop==null)
            eventLoop = new DefaultEventLoopGroup(1, new DefaultThreadFactory("Rtvals-group"));
        return eventLoop;
    }
    /**
     * Adds the AbstractVal to the appropriate collection if not in it yet and returns the val at the key
     *
     * @param val The val to add if new
     * @return The final val at the key
     */
    public BaseVal AddIfNewAndRetrieve(ValUser user, BaseVal val) {
        if (val instanceof RealVal rv)
            return addRealVal(user,rv);
        if (val instanceof IntegerVal iv)
            return addIntegerVal(user,iv);
        if (val instanceof FlagVal fv)
            return addFlagVal(user,fv,false);
        if (val instanceof TextVal tv)
            return addTextVal(user,tv);
        return null;
    }
    /* ************************************ R E A L V A L ***************************************************** */

    /**
     * Add a RealVal to the collection if it doesn't exist yet
     *
     * @param rv The RealVal to add
     */
    public RealVal addRealVal(ValUser user, RealVal rv) {
        if (rv == null) {
            Logger.error("Invalid RealVal received, won't try adding it");
            return null;
        }
        if( rv instanceof RealValSymbiote rvs){
            var old = realVals.get(rv.id());
            applyUser(rvs,true);
            if( ! (old instanceof RealValSymbiote) && old !=null ){
                SymbioteTools.upgradeToRealSymbiote(rv);
            }
        }
        if( integerVals.putIfAbsent(rv.id(), rv) == null ) {
            broadCastCreation(rv); //because new was created or may
            if( rv instanceof RealValSymbiote ) // Because it might be a symbiote
                broadcastReplacement(rv); // Because existing was replaced
        }else{
            broadcastReplacement(rv); // Because existing was replaced
        }

        applyUser(user,true);
        if( rv instanceof RealValSymbiote){
            var old = realVals.get(rv.id());
            if( ! (old instanceof RealValSymbiote) && old !=null )
                realVals.remove(rv.id());
        }
        if( realVals.putIfAbsent(rv.id(), rv) == null ) {
            broadCastCreation(rv); //because new was created or may
        }else{
            broadcastReplacement(rv); // Because existing was replaced
        }
        applyUser(user,true);
        return realVals.get(rv.id());
    }
    /**
     * Retrieve a RealVal from the hashmap based on the id
     *
     * @param id The reference with which the object was stored
     * @return The requested RealVal or dummy if not found
     */
    public RealVal getRealVal(ValUser user, String id) {
        var opt = Optional.ofNullable(realVals.get(id));
        applyUser(user,opt.isPresent());
        var split = id.split("_",2);
        return opt.orElseGet(() -> RealVal.createDummy(split[0], split[1]));
    }

    public boolean hasReal(String id) {
        if (id.isEmpty()) {
            Logger.error("RealVal -> Empty id given");
            return false;
        }
        return realVals.containsKey(id);
    }
    /**
     * Sets the value of a real (in the hashmap)
     *
     * @param id    The parameter name
     * @param value The value of the parameter
     */
    public void updateReal(String id, double value) {
        var rv = getRealVal(OneTimeValUser.get(),id);
        if( rv.isDummy()) {
            Logger.error("Tried to update a dummy real: "+id );
        }else{
            rv.update(value);
        }
    }

    /* ************************************ I N T E G E R V A L ***************************************************** */

    /**
     * Adds an integerval if it doesn't exist yet
     *
     * @param iv The IntegerVal to add
     */
    public IntegerVal addIntegerVal(ValUser user, IntegerVal iv) {
        if (iv == null) {
            Logger.error("Invalid IntegerVal received, won't try adding it");
            return null;
        }
        if( iv instanceof IntegerValSymbiote ivs){
            var old = integerVals.get(iv.id());
            applyUser(ivs,true);
            if( ! (old instanceof IntegerValSymbiote) && old !=null ){
                upgradeToIntegerSymbiote(iv);
            }
        }
        if( integerVals.putIfAbsent(iv.id(), iv) == null ) {
            broadCastCreation(iv); //because new was created or may
            if( iv instanceof IntegerValSymbiote ) // Because it might be a symbiote
                broadcastReplacement(iv); // Because existing was replaced
        }else{
            broadcastReplacement(iv); // Because existing was replaced
        }

        applyUser(user,true);
        return integerVals.get(iv.id());
    }
    public IntegerValSymbiote upgradeToIntegerSymbiote( IntegerVal iv ){
        var reg = integerVals.get(iv.id());
        IntegerValSymbiote ivs;
        if( iv instanceof IntegerValSymbiote sym ){
            ivs = sym;
        }else{
            ivs = new IntegerValSymbiote(0, iv );
        }
        integerVals.put(ivs.id(),ivs);

        if( reg.isDummy() ){// No such variable exist yet, so create it
            broadCastCreation(ivs);
        }else if( !(reg instanceof IntegerValSymbiote)){ // Exist, so encapsulate
            broadcastReplacement(ivs);
        }else{
            Logger.info("Already a symbiote, not touching it");
            ivs=(IntegerValSymbiote) reg;
        }
        applyUser(ivs,true);
        return ivs;
    }
    public boolean hasInteger(String id) {
        return integerVals.containsKey(id);
    }

    /**
     * Retrieve a IntegerVal from the hashmap based on the id
     *
     * @param id The reference with which the object was stored
     * @return The requested IntegerVal or empty optional if not found
     */
    public IntegerVal getIntegerVal(ValUser user, String id) {
        var opt = Optional.ofNullable(integerVals.get(id));
        applyUser(user,opt.isPresent());
        var split = id.split("_",2);
        return opt.orElseGet(() -> IntegerVal.createDummy(split[0], split[1]));
    }

    /* *********************************** T E X T S  ************************************************************* */
    public TextVal addTextVal(ValUser user, TextVal tv) {
        if (tv == null) {
            Logger.error("Invalid TextVal received, won't try adding it");
            return null;
        }
        applyUser(user,true);
        textVals.putIfAbsent(tv.id(), tv);
        return textVals.get(tv.id());
    }
    /**
     * Retrieve a TextVal from the hashmap based on the id
     *
     * @param id The reference with which the object was stored
     * @return The requested TextVal or empty optional if not found
     */
    public TextVal getTextVal(ValUser user, String id) {
        var opt = Optional.ofNullable(textVals.get(id));
        applyUser(user,opt.isPresent());
        var split = id.split("_",2);
        return opt.orElseGet(() -> TextVal.createDummy(split[0], split[1]));
    }

     /* ************************************** F L A G S ************************************************************* */
    public FlagVal addFlagVal(ValUser user, FlagVal fv) {
        return addFlagVal(user,fv,false);
    }
    public FlagVal addFlagVal(ValUser user, FlagVal fv, boolean overwrite) {
        if (fv == null) {
            Logger.error("Invalid FlagVal received, won't try adding it");
            return null;
        }
        if( overwrite ) {
            Logger.info("Overwriting the flagval: "+fv.id());
            return flagVals.put(fv.id(), fv);
        }
        var old = flagVals.putIfAbsent(fv.id(), fv);
        applyUser(user,true);
        return old==null?fv:old;
    }
    public boolean hasFlag(String flag) {
        return flagVals.containsKey(flag);
    }

    public FlagVal getFlagVal(ValUser user,String id) {
        var opt = Optional.ofNullable(flagVals.get(id));
        applyUser(user,opt.isPresent());
        var split = id.split("_",2);
        return opt.orElseGet(() -> FlagVal.createDummy(split[0], split[1]));
    }
    /* ******************************************************************************************************/
    public BaseVal addBaseVal( ValUser user, BaseVal val ){
        if( val instanceof RealVal rv )
            return addRealVal(user,rv);
        if( val instanceof IntegerVal iv )
            return addIntegerVal(user,iv);
        if( val instanceof FlagVal fv )
            return addFlagVal(user,fv);
        if( val instanceof TextVal tv )
            return addTextVal(user,tv);
        return val;
    }
    /* ******************************************************************************************************/
    public void applyUser( ValUser user, boolean valOk ){
        if( OneTimeValUser.isOneTime(user))
            return;
        if( valOk) {
            if( !needCreations.contains(user)) // Make sure this isn't done if it's already in other one
                needUpdates.add(user);
        }else{
            needCreations.add(user);
            needUpdates.remove(user); // needCreations has priority
        }
    }
    public void registerCreationUser( ValUser user ){
        needCreations.add(user);
    }
    public void registerUpdateUser( ValUser user ){
        needUpdates.add(user);
    }
    public void broadCastCreation( BaseVal val ){
        if( needCreations.isEmpty() || val==null )
            return;
        for( var valUser : needCreations ){
            if( valUser.provideVal(val) ){ // True means all dummies resolved
                needUpdates.add(valUser); // So move to updates
            }
        }
        // Remove the ones that were added to the other collection
        needCreations.removeIf(needUpdates::contains);
    }
    public void broadcastReplacement( BaseVal val ){
        if( needUpdates.isEmpty() || val==null)
            return;
        needUpdates.forEach(valUser ->valUser.provideVal(val) );
    }
    public boolean checkValUsers(){
        var ok = true;
        if( needCreations.isEmpty() ){
            Logger.info("rtvals -> Need creation collection is empty");
        }else{
            Logger.error("rtvals -> Still missing "+needCreations.stream().map(ValUser::getValIssues).collect(Collectors.joining("; ")));
            ok=false;
        }
        Logger.info("NeedUpdates contains "+needUpdates.size()+" elements.");
        return ok;
    }
    /**
     * Look through all the vals for one that matches the id
     *
     * @param id The id to find
     * @return An optional of the val, empty if not found
     */
    public BaseVal getBaseVal(ValUser user,String id) {

        var r = getRealVal(OneTimeValUser.get(),id);
        if( !r.isDummy()){
            applyUser(user,true);
            return r;
        }
        var i = getIntegerVal(OneTimeValUser.get(),id);
        if( !i.isDummy()){
            applyUser(user,true);
            return i;
        }
        var f = getFlagVal(OneTimeValUser.get(),id);
        if( !f.isDummy()){
            applyUser(user,true);
            return f;
        }
        var t = getTextVal(OneTimeValUser.get(),id);
        if( !t.isDummy()){
            applyUser(user,true);
            return t;
        }
        applyUser(user,false);
        return AnyDummy.createDummy(id);
    }
    public NumericVal getNumericalVal(ValUser user,String id) {
        var r = getRealVal(OneTimeValUser.get(),id);
        if( !r.isDummy()){
            applyUser(user,true);
            return r;
        }
        var i = getIntegerVal(OneTimeValUser.get(),id);
        if( !i.isDummy()){
            applyUser(user,true);
            return i;
        }
        var f = getFlagVal(OneTimeValUser.get(),id);
        if( !f.isDummy()){
            applyUser(user,true);
            return f;
        }
        applyUser(user,false);
        return AnyDummy.createDummy(id);
    }
    /**
     * Look through all the vals for one that matches the id
     *
     * @param id The id to find
     * @return An optional of the val, empty if not found
     */
    public boolean hasBaseVal(String id) {
        return Stream.of(realVals, integerVals, flagVals, textVals)
                .map(map -> (BaseVal) map.get(id))
                .anyMatch(Objects::nonNull);
    }

    /* *************************** WRITABLE *********************************************************************** */
    public int addRequest(Writable writable, String type, String req) {
        var av = switch (type) {
            case "double", "real" -> realVals.get(req);
            case "int", "integer" -> integerVals.get(req);
            case "text" -> textVals.get(req);
            case "flag" -> flagVals.get(req);
            default -> {
                Logger.warn("rtvals -> Requested unknown type: " + type);
                yield null;
            }
        };
        if (av == null)
            return 0;
        if( av instanceof RealVal rv ){
            if( !(rv instanceof RealValSymbiote sy) ){
                var sym = new RealValSymbiote(0,rv,new ValPrinter(av,writable));
                addRealVal(OneTimeValUser.get(),sym);
            }else{
                sy.addUnderling(new ValPrinter(av,writable));
            }
        } else if ( av instanceof IntegerVal iv) {
            if( !(iv instanceof IntegerValSymbiote sy) ){
                var sym = upgradeToIntegerSymbiote(iv);
                applyUser(sym,true);
                sym.addUnderling(new ValPrinter(av,writable));
            }else{
                sy.addUnderling(new ValPrinter(av,writable));
            }
        }
        return 1;
    }

    public boolean addRequest(Writable writable, String rtval) {
        var val = getBaseVal(OneTimeValUser.get(), rtval);
        // TODO
        return false;
    }

    /* ************************************************************************************************************ */
    public String getNameVals(String regex) {
        return Stream.of(realVals, integerVals, flagVals, textVals)
                .flatMap(map -> map.values().stream()) // Flatten all the values from the maps
                .filter(val -> val.name().matches(regex)
                        && (!val.group().equals("meles") || !(val instanceof TextVal))) // Filter by group
                .map(val -> val.id() + " : " + val.asString())
                .collect(Collectors.joining("\r\n"));
    }

    public ArrayList<BaseVal> getGroupVals(String group) {
        return Stream.of(realVals, integerVals, flagVals, textVals)
                .flatMap(map -> map.values().stream()) // Flatten all the values from the maps
                .filter(val -> val.group().equalsIgnoreCase(group)) // Filter by group
                .collect(Collectors.toCollection(ArrayList::new)); // Collect the results into a List
    }

    public String getRTValsGroupList(String group, boolean html, boolean crop) {

        var tempList = Stream.of(realVals, integerVals, flagVals, textVals)
                .flatMap(bv -> bv.values().stream())
                .filter(bv -> bv.group().equalsIgnoreCase(group))
                .sorted(Comparator.comparing(BaseVal::name))
                .collect(Collectors.toCollection(ArrayList::new));

        var syms = Stream.of(realVals)
                .flatMap(bv -> bv.values().stream())
                .filter(bv -> bv.group().equalsIgnoreCase(group))
                .filter(bv -> bv instanceof RealValSymbiote)
                .map(rv -> (RealValSymbiote) rv)
                .toList();
        var symInt = Stream.of(integerVals)
                .flatMap(bv -> bv.values().stream())
                .filter(bv -> bv.group().equalsIgnoreCase(group))
                .filter(bv -> bv instanceof IntegerValSymbiote)
                .map(rv -> (IntegerValSymbiote) rv)
                .toList();

        for (var sym : syms) {
            for (var derived : sym.getDerived())
                tempList.remove(derived);
        }
        for (var sym : symInt) {
            for (var derived : sym.getDerived())
                tempList.remove((BaseVal)derived);
        }
        String title;
        if (group.isEmpty()) {
            title = html ? "<b>Ungrouped</b>" : TelnetCodes.TEXT_CYAN + "Ungrouped" + TelnetCodes.TEXT_DEFAULT;
        } else {
            title = html ? "<b>Group: " + group + "</b>" : TelnetCodes.TEXT_CYAN + "Group: " + group + TelnetCodes.TEXT_DEFAULT;
        }

        String eol = html ? "<br>" : "\r\n";
        StringJoiner join = new StringJoiner(eol, title + eol, "");
        join.setEmptyValue("None yet");

        int maxLength = tempList.stream().mapToInt(bv -> bv.name().length()).max().orElse(0);  // Get the max value

        for (var val : tempList) {
            if (val instanceof RealValSymbiote rSym) {
                DynamicUnit du = null;
                for (var entry : units.entrySet()) {
                    if (entry.getKey().contains(rSym.unit())) {
                        du = entry.getValue();
                        break;
                    }
                }
                join.add(LookAndFeel.prettyPrintSymbiote(rSym, "", "", crop, du));
            } else if (val instanceof IntegerValSymbiote iSym) {
                DynamicUnit du = null;
                for (var entry : units.entrySet()) {
                    if (entry.getKey().contains(iSym.unit())) {
                        du = entry.getValue();
                        break;
                    }
                }
                join.add(LookAndFeel.prettyPrintSymbiote(iSym, "", "", crop, du));
            }else {
                join.add(String.format("%-" + maxLength + "s : %s", val.name(), applyUnit(val)));
            }
        }
        var total = join.toString();
        return total.replace("NaN", TelnetCodes.TEXT_ORANGE + "NaN" + TelnetCodes.TEXT_DEFAULT);
    }

    public String applyUnit(BaseVal bv) {

        if (!(bv instanceof NumericVal nv)) {
            return bv.asString() + bv.unit();
        }
        if (units.isEmpty() || nv instanceof FlagVal)
            return nv.asString() + nv.unit() + nv.getExtraInfo();

        DynamicUnit unit = null;
        for (var set : units.entrySet()) {
            if (nv.unit().endsWith(set.getKey())) {
                unit = set.getValue();
                break;
            }
        }
        if (unit == null || unit.noSubs())
            return nv.asString() + nv.unit() + nv.getExtraInfo();

        if (nv instanceof RealVal rv) {
            if (Double.isNaN(rv.asDouble()))
                return "NaN";
            return unit.apply(rv.value(), rv.unit()) + rv.getExtraInfo();
        }
        return unit.apply(nv.asInteger(), nv.unit()) + nv.getExtraInfo();
    }

    /**
     * Get the full listing of all reals,flags and text, so both grouped and ungrouped
     *
     * @param html If true will use html newline etc
     * @return The listing
     */
    public String getRtvalsList(boolean html, boolean crop) {
        String eol = html ? "<br>" : "\r\n";
        StringJoiner join = new StringJoiner(eol, "Status at " + TimeTools.formatShortUTCNow() + eol + eol, "");
        join.setEmptyValue("None yet");

        // Find & add the groups
        for (var group : getGroups()) {
            var res = getRTValsGroupList(group, html, crop);
            if (!res.isEmpty() && !res.equalsIgnoreCase("none yet"))
                join.add(res).add("");
        }
        var res = getRTValsGroupList("", html, crop);
        if (!res.isEmpty() && !res.equalsIgnoreCase("none yet"))
            join.add(res).add("");

        if (!html)
            return join.toString();

        // Try to fix some symbols to correct html counterpart
        return join.toString().replace("°C", "&#8451") // fix the °C
                .replace("m²", "&#13217;") // Fix m²
                .replace("m³", "&#13221;"); // Fix m³
    }

    /**
     * Get a list of all the groups that exist in the rtvals
     *
     * @return The list of the groups
     */
    public List<String> getGroups() {
        return Stream.of(realVals, integerVals, flagVals, textVals)
                .flatMap(map -> map.values().stream()) // Flatten all the values from the maps
                .map(BaseVal::group) // Get the group instead
                .filter(val -> !val.isEmpty() && !val.equals("meles"))
                .distinct()
                .toList();
    }

    /* ************************** C O M M A N D A B L E ***************************************** */
    @Override
    public String replyToCommand(Datagram d) {
        var html = d.asHtml();
        var args = d.args();

        // Switch between either empty string or the telnetcode because of htm not understanding telnet
        String green = html ? "" : TelnetCodes.TEXT_GREEN;
        String reg = html ? "" : TelnetCodes.TEXT_DEFAULT;
        String ora = html ? "" : TelnetCodes.TEXT_ORANGE;

        var result = switch (d.cmd()) {
            case "rv", "reals", "iv", "integers" -> replyToNumericalCmd(d.cmd(), args);
            case "texts", "tv" -> replyToTextsCmd(args);
            case "flags", "fv" -> replyToFlagsCmd(args, html);
            case "rtvals", "rvs" -> replyToRtvalsCmd(args, html);
            case "rtval", "real", "int", "integer", "text", "flag" -> {
                int s = addRequest(d.getWritable(), d.cmd(), args);
                yield s != 0 ? "Request added to " + args : "Request failed";
            }
            case "" -> {
                removeWritable(d.getWritable());
                yield "";
            }
            default -> "! No such subcommand in rtvals: " + args;
        };
        if (result.startsWith("!"))
            return ora + result + reg;
        return green + result + reg;
    }

    public String replyToNumericalCmd(String cmd, String args) {

        var cmds = args.split(",");

        if (cmds.length == 1) {
            if (args.equalsIgnoreCase("?")) {
                if (args.startsWith("i"))
                    return "iv:update,id,value -> Update an existing int, do nothing if not found";
                return "rv:update,id,value -> Update an existing real, do nothing if not found";
            }
            return "! Not enough arguments";
        }

        return switch (cmds[1]) {
            case "update", "def" -> doUpdateNumCmd(cmd, cmds);
            case "new" -> doNewNumCmd(cmd, cmds);
            default -> "! No such subcommand in " + cmd + ": " + cmds[0];
        };
    }

    private String doUpdateNumCmd(String cmd, String[] args) {
        if (args.length < 3)
            return "! Not enough arguments, " + cmd + ":id," + args[1] + ",expression";
        NumericVal val;

        if (cmd.startsWith("r")) { // so real, rv
            var rDum = getRealVal(OneTimeValUser.get(),args[0]);
            if (rDum.isDummy())
                return "! No such real yet";
            val = rDum;
        } else { // so int,iv
            var iDum = getIntegerVal(OneTimeValUser.get(),args[0]);
            if (iDum.isDummy())
                return "! No such int yet";
            val = iDum;
        }
        var result = ValTools.processExpression(args[2], this);
        if (Double.isNaN(result))
            return "! Unknown id(s) in the expression " + args[2];
        if (val.update(result))
            return val.id() + " updated to " + result + val.unit();
        return "! " + val.id() + " not updated, check failed.";
    }

    private String doNewNumCmd(String cmd, String[] cmds) {

        // Split in group & name
        String group, name;
        if (cmds.length == 3) {
            group = cmds[2];
            name = cmds[0];
        } else if (cmds.length == 2) {
            if (!cmds[0].contains("_"))
                return "! No underscore in the id, can't split between group and name";
            group = cmds[0].substring(0, cmds[0].indexOf("_"));
            name = cmds[0].substring(group.length() + 1); //skip the group and underscore
        } else {
            return "! Not enough arguments, " + cmd + ":id,new or " + cmd + ":name,new,group";
        }

        if (hasBaseVal(group + "_" + name)) {
            return "! Already an rtval with that id";
        }

        // Build the node
        var fab = Paths.fabInSettings("rtvals")
                .selectOrAddChildAsParent("group", "id", group);
        if (cmd.startsWith("r")) { // So real
            fab.addChild("real").attr("name", name);
            addRealVal( OneTimeValUser.get(), RealVal.newVal(group, name) );
        } else if (cmd.startsWith("i")) {
            fab.addChild("int").attr("name", name);
            addIntegerVal(OneTimeValUser.get(), IntegerVal.newVal(group, name));
        }
        fab.build();
        return "Val added.";
    }

    public String replyToFlagsCmd(String cmd, boolean html) {

        if (cmd.equals("?"))
            return doFlagHelpCmd(html);

        var args = cmd.split(",");
        if (args.length < 2)
            return "! Not enough arguments, at least: fv:id,cmd";

        var flag = getFlagVal(OneTimeValUser.get(),args[0]);
        if (flag.isDummy()) {
            Logger.error("No such flag: " + args[0]);
            return "! No such flag yet";
        }

        if (args.length == 2) {
            switch (args[1]) {
                case "raise", "set" -> {
                    flag.update(true);
                    return "Flag raised";
                }
                case "lower", "clear", "reset" -> {
                    flag.update(false);
                    return "Flag lowered";
                }
                case "toggle" -> {
                    flag.toggleState();
                    return "Flag toggled";
                }
            }
        } else if (args.length == 3) {
            var src = getFlagVal(OneTimeValUser.get(),args[2]);
            if (src.isDummy())
                return "! No such flag: " + args[2];
            switch (args[1]) {
                case "update" -> {
                    return flag.parseValue(args[2]) ? "Flag updated" : "! Failed to parse state: " + args[2];
                }
                case "match" -> {
                    flag.value(src.isUp());
                    return "Flag matched accordingly";
                }
                case "negated", "negate" -> {
                    flag.value(!src.isUp());
                    return "Flag negated accordingly";
                }
            }
        }
        return "! No such subcommand in fv: " + args[1] + " or incorrect number of arguments.";
    }

    private static String doFlagHelpCmd(boolean html) {

        var join = new StringJoiner("\r\n");
        join.add("Commands that interact with the collection of flags.");
        join.add("Note: both fv and flags are valid starters")
                .add("Update")
                .add("fv:id,raise/set -> Raises the flag/Sets the bit, created if new")
                .add("fv:id,lower/clear -> Lowers the flag/Clears the bit, created if new")
                .add("fv:id,toggle -> Toggles the flag/bit, not created if new")
                .add("fv:id,update,state -> Update  the state of the flag")
                .add("fv:id,match,refid -> The state of the flag becomes the same as the ref flag")
                .add("fv:id,negated,refid  -> The state of the flag becomes the opposite of the ref flag");
        return LookAndFeel.formatHelpCmd(join.toString(), html);
    }

    public String replyToTextsCmd(String args) {

        var cmds = args.split(",");

        // Get the TextVal if it exists
        if (cmds.length < 2)
            return "! Not enough arguments, at least: tv:id,cmd";

        var txt = getTextVal(this,cmds[0]);
        if (txt.isDummy())
            return "! No such text yet";

        // Execute the commands
        if (cmds[1].equals("update")) {
            if (cmds.length < 3)
                return "! Not enough arguments: tv:id,update,value";
            int start = args.indexOf(",update") + 8; // value can contain , so get position of start
            txt.value(args.substring(start));
            return "TextVal updated";
        }
        return "! No such subcommand in tv: " + cmds[1];
    }

    public String replyToRtvalsCmd(String args, boolean html) {

        if (args.isEmpty())
            return getRtvalsList(html, true);

        String[] cmds = args.split(",");

        if (cmds.length == 1) {
            switch (cmds[0]) {
                case "?" -> {
                    var join = new StringJoiner("\r\n");
                    join.add("Interact with XML")
                            .add("rtvals:reload -> Reload all rtvals from XML")
                            .add("Get info")
                            .add("rtvals -> Get a listing of all rtvals")
                            .add("rtvals:groups -> Get a listing of all the available groups")
                            .add("rtvals:full -> Same as rtvals, but names aren't cropped in the tree views")
                            .add("rtvals:group,groupid -> Get a listing of all rtvals belonging to the group")
                            .add("rtvals:resetgroup,groupid -> Reset the values in the group to the defaults");
                    return LookAndFeel.formatHelpCmd(join.toString(), html);
                }
                case "reload" -> {
                    readFromXML(XMLdigger.goIn(Paths.settings(), "meles", "rtvals"));
                    Core.addToQueue(Datagram.system("pf", "reload"));
                    Core.addToQueue(Datagram.system("dbm", "reload"));
                    return "Reloaded rtvals and paths, databases (because might be affected).";
                }
                case "groups" -> {
                    String groups = String.join(html ? "<br>" : "\r\n", getGroups());
                    return groups.isEmpty() ? "! No groups yet" : groups;
                }
                case "full" -> {
                    return getRtvalsList(html, false);
                }
            }
        } else if (cmds.length == 2) {
            return switch (cmds[0]) {
                case "group" -> getRTValsGroupList(cmds[1], html, false);
                case "resetgroup" -> {
                    var vals = getGroupVals(cmds[1]);
                    if (vals.isEmpty()) {
                        Logger.error("No vals found in group " + cmds[1]);
                        yield "! No vals with that group";
                    }
                    getGroupVals(cmds[1]).forEach(BaseVal::resetValue);
                    yield "Values reset";
                }
                case "name" -> getNameVals(cmds[1]);
                default -> "! No such subcommand in rtvals: " + args;
            };
        }
        return "! No such subcommand in rtvals: " + args + " (Use rtvals:?, to check the options) ";
    }

    @Override
    public boolean removeWritable(Writable wr) {
        integerVals.values().stream().filter( iv -> iv instanceof IntegerValSymbiote ).forEach(
                ivs -> {
                    ((IntegerValSymbiote) ivs).removePrinterUnderling(wr);
                }
        );
        realVals.values().stream().filter( iv -> iv instanceof RealValSymbiote ).forEach(
                ivs -> {
                    ((RealValSymbiote) ivs).removePrinterUnderling(wr);
                }
        );
        return false;
    }

    public boolean isWriter(){
        return true;
    }

    @Override
    public boolean provideVal(BaseVal val) {
        return true;
    }
    public String id(){
        return "rtvals";
    }
}
