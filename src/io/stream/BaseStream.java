package io.stream;

import meles.Core;
import io.Writable;
import io.netty.channel.EventLoopGroup;
import org.tinylog.Logger;
import util.tools.TimeTools;
import util.tools.Tools;
import util.xml.XMLdigger;
import worker.Datagram;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public abstract class BaseStream {

    /* Pretty much the local descriptor */
	protected int priority = 1;				// Priority of the messages received, used by DataWorker
	protected String label = "";			// The label that determines what needs to be done with a message
	protected String id="";				    // A readable name for the handler
    protected long readerIdleSeconds =-1;

    /* Things regarding the connection*/
    protected AtomicLong timestamp=new AtomicLong();  // Timestamp of the last received message, init so startup doesn't show error message
    protected long openedStamp;

    protected CopyOnWriteArrayList<Writable> targets = new CopyOnWriteArrayList<>();
    protected ArrayList<StreamListener> listeners = new ArrayList<>();

    protected String eol="\r\n";

    protected boolean reconnecting=false;
    protected int connectionAttempts=0;

    protected boolean clean=true;
    protected boolean log=true;
    protected boolean echo=false;
    protected boolean addDataOrigin=false;
    protected ScheduledFuture<?> reconnectFuture=null;
    protected ArrayList<TriggerAction> triggeredActions = new ArrayList<>();

    public enum TRIGGER{OPEN,IDLE,CLOSE,HELLO,WAKEUP, IDLE_END}

    protected EventLoopGroup eventLoopGroup;		    // Eventloop used by the netty stuff
    protected boolean readerIdle=false;

    protected BaseStream( String id){
        this.id=id;
        timestamp.set(System.currentTimeMillis());
    }

    protected BaseStream(XMLdigger stream) {
        readFromXML(stream);
    }
    public void setEventLoopGroup( EventLoopGroup eventLoopGroup ){
        this.eventLoopGroup = eventLoopGroup;
    }

    protected boolean readFromXML(XMLdigger dig) {

        if (!dig.attr("type", "").equalsIgnoreCase(getType())
                && !dig.attr("type", "").replace("client", "").equalsIgnoreCase(getType())) {
            Logger.error("Not a "+getType()+" stream element.");
            return false;
        }

        id = dig.attr("id", "");
        label = dig.peekAt("label").value("");    // The label associated fe. nmea,sbe38 etc
        priority = dig.peekAt("priority").value( 1);	 // Determine priority of the sensor
        log = dig.peekAt("log").value(true);
        addDataOrigin = dig.peekAt("prefixorigin").value(false);
        // delimiter
        String deli = dig.peekAt("eol").value("\r\n");
        if( deli.equalsIgnoreCase("\\0"))
            deli="";// Delimiter used, default carriage return + line feed
        eol = Tools.getDelimiterString(deli);

        // ttl
		String ttlString = dig.peekAt("ttl").value("-1");
        if( !ttlString.equals("-1") ){
			if( Tools.parseInt(ttlString, -999) != -999) // Meaning no time unit was added, use the default s
                ttlString += "s";
			readerIdleSeconds = TimeTools.parsePeriodStringToSeconds(ttlString);
        }
        if (dig.attr("echo", false))
            enableEcho();

        // cmds
        triggeredActions.clear();
        if (dig.hasPeek("triggered"))
            dig.usePeek();

        if( dig.hasPeek("cmd") ) {
            for (var cmd : dig.digOut("cmd")) {
                var c = cmd.value("");
                if (!c.isEmpty())
                    triggeredActions.add(new TriggerAction(dig.attr("when", "open"), c));
            }
            dig.goUp();
        }
        if (dig.hasPeek("write")) {
            for (var write : dig.digOut("write")) {
                String c = write.value("");
                if (!c.isEmpty())
                    triggeredActions.add(new TriggerAction(write.attr("when", "hello"), c));
            }
            dig.goUp();
        }
        return readExtraFromXML(dig);
    }

    protected abstract boolean readExtraFromXML(XMLdigger dig);

    // Abstract methods
    public abstract boolean initAndConnect();
    public abstract boolean disconnect();
    public abstract boolean isConnectionValid();
    public long getLastTimestamp(){
        return timestamp.get();
    }
    public abstract String getInfo();
    protected abstract String getType();

    /* Getters & Setters */
    public void setLabel( String label ){
        this.label=label;
    }
    public String getLabel( ){
        return label;
    }
    public void setPriority(int priority ){
		this.priority=priority;
    }
    public void addListener( StreamListener listener ){
		listeners.add(listener);
    }

    public void id(String id ){
        this.id=id;
    }
    public String id(){
        return id;
    }
    public boolean isWritable(){
        return this instanceof Writable;
    }
    public int getPriority(){ return priority; }
    public void setTimestamp( long ts ){
        timestamp.set(ts);
    }
    /**
     * Set the maximum time passed since data was received before the connection is considered idle
     * @param seconds The time in seconds
     */
    public void setReaderIdleTime(long seconds){
        this.readerIdleSeconds = seconds;
    }
    public long getReaderIdleTime(){
        return readerIdleSeconds;
    }
    /* Requesting data */
    public synchronized boolean addTarget(Writable writable ){
        if( writable == null){
            Logger.error("Tried adding request to "+id+" but writable is null");
            return false;
        }
        if( targets.contains(writable)){
            Logger.info(id +" -> Already has "+writable.id()+" as target, not adding.");
            return true;
        }

        if( writable.id().startsWith("telnet")) {
            targets.add(0,writable);
        }else{
            targets.removeIf( x -> x.id().equals(writable.id())&&writable.id().contains(":")); // if updated
            targets.add(writable);
        }

        Logger.info("Added request from "+writable.id()+ " to "+id);
        return true;
    }
    public boolean removeTarget(Writable wr ){
		return targets.remove(wr);
	}
	public int clearTargets(){
        int total=targets.size();
        targets.clear();
        return total;
    }
    public CopyOnWriteArrayList<Writable> getTargets(){
        return targets;
    }
	public int getRequestsSize(){
		return targets.size();
    }
    public String listTargets(){
        StringJoiner join = new StringJoiner(", ");
        targets.forEach(wr -> join.add(wr.id()));
        return join.toString();
    }
    /* Echo */
    public void enableEcho(){
        if( this instanceof Writable ){
            targets.add((Writable)this );
            echo=true;
        }
    }
    public void disableEcho(){
        if( this instanceof Writable ){
            echo=false;
            targets.removeIf(r -> r.id().equalsIgnoreCase(id));
        }
    }
    /* ******************************** TRIGGERED ACTIONS *****************************************************/
    private void applyTriggeredAction(TRIGGER trigger ){
        for( TriggerAction cmd : triggeredActions){
            if( cmd.trigger!=trigger) // Check if the trigger presented matched this actions trigger
                continue; // If not, check the next one

            if( cmd.trigger==TRIGGER.HELLO || cmd.trigger==TRIGGER.WAKEUP ){ // These trigger involves writing to remote
                Logger.info(id+" -> "+cmd.trigger+" => "+cmd.data);
                if( this instanceof Writable )
                    ((Writable) this).writeLine(id(), cmd.data);
                continue;
            }
            Logger.info(id+" -> "+cmd.trigger+" => "+cmd.data);
            if( this instanceof Writable ){ // All the other triggers are executing cmds
                Core.addToQueue( Datagram.system(cmd.data).writable((Writable)this) );
            }else{
                Core.addToQueue( Datagram.system(cmd.data) );
            }
        }
    }
    public void flagAsIdle(){
        readerIdle=true;
        applyTriggeredAction(TRIGGER.IDLE);
        applyTriggeredAction(TRIGGER.WAKEUP);
    }
    public boolean isIdle(){
        return readerIdle;
    }
    public void flagAsActive(){
        readerIdle=false;
        applyTriggeredAction(TRIGGER.IDLE_END);
    }

    public void triggerOpened(){
        applyTriggeredAction(TRIGGER.HELLO);
        applyTriggeredAction(TRIGGER.OPEN);
    }
    public void triggerClosed(){
        applyTriggeredAction(BaseStream.TRIGGER.CLOSE);
    }
    public void addTriggeredAction(String when, String action ){
        var t = new TriggerAction(when, action);
        if( t.trigger==null)
            return;
        triggeredActions.add(t);
    }

    public List<String> getTriggeredActions(TRIGGER trigger ){
        return triggeredActions.stream().filter(x -> x.trigger==trigger).map(x -> x.data).collect(Collectors.toList());
    }
    private static TRIGGER convertTrigger( String trigger ){
        return switch (trigger.toLowerCase()) {
            case "open" -> TRIGGER.OPEN;
            case "close" -> TRIGGER.CLOSE;
            case "idle" -> TRIGGER.IDLE;
            case "!idle" -> TRIGGER.IDLE_END;
            case "hello" -> TRIGGER.HELLO;
            case "wakeup", "asleep" -> TRIGGER.WAKEUP;
            default -> {
                Logger.error("Unknown trigger requested : " + trigger);
                yield null;
            }
        };
    }
    protected static class TriggerAction {
        String data;
        public TRIGGER trigger;

        TriggerAction(TRIGGER trigger, String data ){
            this.trigger=trigger;
            this.data =data;
            Logger.info("Added action : "+trigger+" -> "+data);
        }
        public String data(){
            return data;
        }
        TriggerAction(String trigger, String command){
            this(convertTrigger(trigger),command);
        }
    }

}
