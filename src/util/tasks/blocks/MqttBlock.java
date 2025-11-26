package util.tasks.blocks;

import das.Core;
import io.Writable;
import io.mqtt.MqttWork;
import io.netty.channel.EventLoopGroup;
import org.tinylog.Logger;
import util.data.vals.BaseVal;
import util.data.vals.TextVal;
import worker.Datagram;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MqttBlock extends AbstractBlock implements Writable {

    private String topic;
    private BaseVal val;
    private Writable broker;
    private String brokerId;
    private long expireTime=0;

    private EventLoopGroup eventLoop;
    private ScheduledFuture<?> failFuture;

    public MqttBlock(String brokerId,String topic,  BaseVal val) {
        this.brokerId=brokerId;
        this.topic=topic;
        this.val=val;
    }
    public MqttBlock(String brokerId,String topic,  String val) {
        this.brokerId=brokerId;
        this.topic=topic;
        this.val = TextVal.newVal("meles","temp").value(val);
    }
    public void setEventLoop(EventLoopGroup eventLoop) {
        this.eventLoop = eventLoop;
    }
    public void setExpireTime( long ms ){
        this.expireTime=ms;
    }
    @Override
    public boolean start() {
        clean = false;
        if (broker != null) {
            tryPub();
        } else {
            if (!brokerId.isEmpty())
                Core.addToQueue(Datagram.system("mqtt:"+brokerId+",wr").writable(this));
        }
        return true;
    }
    private void tryPub(){
        broker.giveObject("pub", MqttWork.toTopic(topic).data(val).inform(this).expiresAfter((int)expireTime, ChronoUnit.MILLIS) );
        if(eventLoop!=null && expireTime!=0)
            failFuture = eventLoop.schedule(()->this.doAltRoute(true), expireTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void giveObject(String info, Object object) {
        if (info.equalsIgnoreCase("writable")) {
            broker = (Writable) object;
            if (broker != null) {
                tryPub();
            } else {
                Logger.info(id() + " -> Received null instead of response to wr" );
            }
        } else {
            Logger.warn(id() + " -> Given object with unknown info... ?" + info);
        }
    }
    @Override
    public boolean writeLine(String origin, String data) {
        if( !origin.equals("mqtt"))
            return false;
        switch (data){
            case "failed" -> {
                Logger.warn(id()+ " -> MQTT send "+data+" for "+topic + " at "+brokerId);
                doAltRoute(true);
            }
            case "expired" -> Logger.warn(id()+ " -> MQTT send expired for "+topic + " at "+brokerId);
            case "retrying" -> Logger.info(id()+ " -> MQTT send for "+topic + " at "+brokerId+" failed and retrying");
            case "published" ->{
                if( failFuture != null )
                    failFuture.cancel(true); // Cancel the time out
                doNext();
            }
            default -> Logger.info( "Unsupported origin received");
        }
        return true;
    }

    @Override
    public boolean isConnectionValid() {
        return true;
    }
}
