package io.mqtt;

import io.Writable;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.tinylog.Logger;
import util.data.vals.BaseVal;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MqttWork {

    private Instant createdAt = Instant.now();
    private Duration ttl = Duration.ZERO; // configurable

    private String topic;
    private int qos=0;
    private int attempt = 0;
    private int maxAttempts=5;
    private boolean valid=true;
    private byte[] data;
    private Writable origin;

    public MqttWork(String topic){
        this.topic=topic;
    }
    public static MqttWork toTopic(String topic){
        return new MqttWork(topic);
    }
    public MqttWork topic( String topic ){
        this.topic=topic;
        return this;
    }
    public MqttWork data( String data ){
        this.data=data.getBytes();
        return this;
    }
    public MqttWork data( BaseVal val ){
        this.data=val.asString().getBytes();
        return this;
    }
    public MqttWork inform( Writable wr){
        this.origin=wr;
        return this;
    }
    public MqttWork expiresAfter(int count, TemporalUnit unit){
        if( count != 0)
            ttl = Duration.of(count,unit);
        return this;
    }
    public boolean isExpired(){
        if( ttl.isZero() )
            return false;
        return Duration.between(createdAt, Instant.now()).compareTo(ttl) > 0;
    }
    /**
	 * Constructor that also adds a value 
	 * @param group The group this data is coming from
	 * @param parameter The parameter to update
	 * @param value The new value
	 */
	public MqttWork( String group, String parameter, Object value) {
		topic=group+"/"+parameter;
		setValue(value);
	}
	public MqttWork( String topic, Object value) {
        if( checkTopic(topic) )
			setValue(value);
	}

    public MqttWork( String topic, byte[] value, Writable origin){
        if( checkTopic(topic) ){
            this.origin=origin;
            data=value;
        }
    }
    private boolean checkTopic( String topic ){
        if( !topic.contains("/")){
            Logger.error( "No topic given in mqttwork: "+topic+ "(missing / )");
            valid=false;
            return false;
        }
            this.topic=topic;
        return true;
    }
    public Optional<Writable> getOrigin(){
        return Optional.ofNullable(origin);
    }
	private void setValue( Object val){
        if (val instanceof Double d) {
            data = Double.toString(d).getBytes();
        } else if (val instanceof Integer i) {
            data = Integer.toString(i).getBytes();
        } else if (val instanceof Boolean b ) {
            data = Boolean.toString(b).getBytes();
        } else if (val instanceof String s) {
            data = s.getBytes();
        }else{
            Logger.error("mqtt -> Invalid class given, topic:"+topic);
            valid=false;
        }
	}
	public String toString(){
		return "Topic: "+topic+" -> data:"+new String(data) +" -> qos: "+qos;
	}
	/*  SETTINGS */
	/**
	 * Change te QoS for this message
	 * @param qos The new QoS value to use
	 */
	public void alterQos( int qos ) {
		this.qos=qos;
	}
	/**
	 * Get the device name given 
	 * @return The name of the device this data relates to
	 */
	public String getTopic() {
		return topic;
	}
	public boolean isInvalid(){
		return !valid;
	}
	public MqttMessage getMessage(){
		return new MqttMessage(data);
	}
	/* ********************************* ADDING DATA ******************************************************** */
	public MqttWork qos(int qos){
		this.qos=qos;
		return this;
	}
	public boolean incrementAttempt() {
		attempt++;
        return attempt < maxAttempts;
    }
}
