package util.tasks.blocks;

import meles.Core;
import io.Writable;
import io.netty.channel.EventLoopGroup;
import util.tools.TimeTools;
import worker.Datagram;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class ReadingBlock extends AbstractBlock implements Writable {
    String data;
    String src;
    EventLoopGroup eventLoop;
    long timeout = 0;
    ScheduledFuture<?> future;
    ScheduledFuture<?> cleanup;
    ScheduledFuture<?> email;
    boolean writableAsked = false;
    boolean active = false;
    boolean interrupted = false;
    public enum CHECK {EQUALS,EQUALSIGNORECASE,STARTSWITH,CONTAINS,REGEX};
    CHECK check;
    Pattern regex;

    public ReadingBlock(EventLoopGroup eventLoop) {
        this.eventLoop = eventLoop;
    }
    public String type(){ return "ReadingBlock";}
    public ReadingBlock setMessage(String src, String data, String timeout) {
        this.data = data;
        src = src.replace("stream", "raw");
        this.src = src;
        this.timeout = TimeTools.parsePeriodStringToSeconds(timeout);
        return this;
    }
    public void alterCheck(String checkString ){
        check = switch (checkString ){
            case "equals" -> CHECK.EQUALS;
            case "equalsignorecase" -> CHECK.EQUALSIGNORECASE;
            case "regex" -> {
                regex = Pattern.compile(data);
                yield CHECK.REGEX;
            }
            case "startswith" -> CHECK.STARTSWITH;
            case "contains" -> CHECK.CONTAINS;
            default -> CHECK.EQUALSIGNORECASE;
        };
    }
    @Override
    public boolean start() {
        if (!writableAsked) {
            Core.addToQueue(Datagram.system(src).writable(this)); // Request data updates from src
            writableAsked = true;
        }
        Core.addToQueue(Datagram.system(src+"?").writable(this)); // Request the state of the connection

        active = true;
        clean = false;
        return true;
    }
    private void doCleanup() {
        writableAsked = false;
    }

    public String toString() {
        return telnetId() + " -> Waiting for '" + data + "' from " + src + " for at most " + TimeTools.convertPeriodToString(timeout, TimeUnit.SECONDS);
    }
    @Override
    public synchronized boolean writeLine(String origin, String content) {
        if (!active)
            return false;

        var ok = switch(check){
            case EQUALS -> data.equals(content);
            case EQUALSIGNORECASE -> data.equalsIgnoreCase(content);
            case STARTSWITH -> content.startsWith(data);
            case CONTAINS -> content.contains(data);
            case REGEX -> regex.matcher(content).matches();
        };
        if (ok) {
            active = false;
            doNext();
            // Task moves on, so cancel data updates
            // Notify src that updates are no longer needed
            writableAsked = false; // Reset this for when executed again
            cancelFailureFuture();
        }
        return true;
    }
    @Override
    public void giveObject(String info, Object object) {
        if( info.equals("alive") ){
            if( object instanceof Boolean alive ){
                if( alive ){
                    if (timeout > 0) {
                        future = eventLoop.schedule(() -> doAltRoute(true), timeout, TimeUnit.SECONDS);
                        cancelCleanupFuture();
                        checkEmail();
                        cleanup = eventLoop.schedule(this::doCleanup, 5 * timeout, TimeUnit.SECONDS);
                    }
                }else{
                    doErrorRoute();// do failure
                }
            }
        }
    }
    private void checkEmail(){
        if( src.equals("email:read") )
            email=eventLoop.scheduleAtFixedRate( ()-> Core.addToQueue(Datagram.system("email:checknow")),1,3, TimeUnit.SECONDS);
    }
    public void reset() {
        cancelFailureFuture();
        cancelCleanupFuture();
        writableAsked = false;

        clean = true;

        if (next != null)
            next.reset();
        if (altRoute != null)
            altRoute.reset();
        if( errorRoute != null )
            errorRoute.reset();
    }

    private void cancelFailureFuture() {
        if (future != null && !future.isDone() && !future.isCancelled())
            future.cancel(true);
        cancelEmailFuture();
    }
    private void cancelEmailFuture() {
        if (email != null && !email.isDone() && !email.isCancelled())
            email.cancel(true);
    }
    private void cancelCleanupFuture() {
        if (cleanup != null && !cleanup.isDone() && !cleanup.isCancelled())
            cleanup.cancel(true);
        cancelEmailFuture();
    }
    @Override
    public boolean isConnectionValid() {
        return writableAsked;
    }
    @Override
    protected void doAltRoute(boolean tagAsFailure) {
        active = false;
        cancelFailureFuture();
        if (altRoute != null)
            altRoute.start();
    }
    protected void doErrorRoute() {
        active = false;
        if (errorRoute != null)
            errorRoute.start();
    }
}
