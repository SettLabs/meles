package io.stream.serialport;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.tinylog.Logger;
import util.math.MathUtils;
import util.tools.Tools;
import util.xml.XMLdigger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

public class ModbusStream extends SerialStream{

    private final byte[] rec = new byte[128];
    private int index = 0;
    private boolean readyForWorker=false;
    private long passed=0;

    public ModbusStream(XMLdigger stream) {
        super(stream);
        eol="";
    }
    @Override
    public String getType(){
        return "modbus";
    }
    @Override
    public String getInfo() {
        return "MODBUS [" + id + "] " + serialPort.getSystemPortName() + " | " + getSerialSettings();
    }
    @Override
    protected void processListenerEvent(byte[] data){

        long p = Instant.now().toEpochMilli() - timestamp.get();	// Calculate the time between 'now' and when the previous message was received
        if (p >= 0)    // If this time is valid
            passed = p; // Store it

        if (passed > 10)  // Maximum allowed time is 3.5 characters which is 5ms at 9600
            index=0;

        for( byte b : data ){
            rec[index] = b;
            index++;   
        }
       
        if( index < 4) // can't do anything with it yet anyway
            return;

        timestamp.set(Instant.now().toEpochMilli());    		    // Store the timestamp of the received message
        if( readerIdle )
            flagAsActive();

        switch( rec[1] ){
            case 0x03: // Register read
                if( index == 5+rec[2] ) // Received all the data
                    readyForWorker=true;
            break;
            case 0x06: // reply to write
                if(index == 8 )
                    readyForWorker=true;
            break;
            case 0x10:
            break;
            default: Logger.warn(id+"(mb) -> Received unknown type");
                Logger.info(Tools.fromBytesToHexString(rec));
            break;
        }
        
        if( readyForWorker ){
            // Log anything and everything (except empty strings)
            if( log )		// If the message isn't an empty string and logging is enabled, store the data with logback
                Logger.tag("RAW").warn( id + "\t[hex] " + Tools.fromBytesToHexString(rec,0,index) );

            if( verifyCRC( rec, index ) ){
                forwardData( getParsedString() );
                readyForWorker=false;
            }else{
                Logger.error(id+"(mb) -> Message failed CRC check: "+Tools.fromBytesToHexString(rec,0,index));
            }
            index=0;
        }
    }

    private String getParsedString() {
        String output;
        var slaveId = rec[0];

        if( rec[1]==0x03){
            int cnt = rec[2] & 0xFF;
            int value = 0;
            for( int i=0; i<cnt; i++ ) { // Combine the data to a single number
                value *= 256;
                value += rec[3+i] & 0xFF;
            }
            output = String.format("%d;R;%d", slaveId, value);
        }else{
            var reg = (rec[2] & 0xFF) *256 + (rec[3] & 0xFF);
            var val = (rec[4] & 0xFF) *256 + (rec[5] & 0xFF);
            output = String.format("%d;C;%d;%d", slaveId, reg, val);
        }
        return output;
    }

    @Override
    public synchronized boolean writeBytes(byte[] data) {
        return write(MathUtils.calcCRC16_modbus(data, true));
    }
    private boolean verifyCRC( byte[] data,int length){
        byte[] crc = MathUtils.calcCRC16_modbus( ArrayUtils.subarray(data,0,length-2), false);
        return crc[0]==data[length-2] && crc[1]==data[length-1];
    }
    @Override
    public synchronized boolean writeString(String message) {// 1;5;500
        var split = Tools.splitList(message);
        if( split.length!=3)
            return false;

        // Validate all parts are valid numbers within range
        if (!NumberUtils.isDigits(split[0]) || !NumberUtils.isDigits(split[1]) || !NumberUtils.isDigits(split[2])) {
            return false;
        }

        var slaveId = Integer.parseInt( split[0] );
        var register = Integer.parseInt( split[1] );
        var value = Integer.parseInt( split[2] );

        // Range validation
        if (slaveId < 1 || slaveId > 247 || // Modbus RTU slave IDs: 1-247
                register < 0 || register > 65535 ||
                value < 0 || value > 65535) {
            return false;
        }

        // Allocate 6 bytes for the message body (excluding CRC)
        // 1 byte slave ID + 1 byte function + 2 bytes register + 2 bytes value
        var buffer = ByteBuffer.allocate(6);
        buffer.order(ByteOrder.BIG_ENDIAN); // Modbus uses big-endian
        // Build the Modbus RTU frame
        buffer.put((byte) slaveId);        // Slave address
        buffer.put((byte) 0x06);           // Function code 06: Write Single Register
        buffer.putShort((short) register); // Register address
        buffer.putShort((short) value);    // Register value

        return write(MathUtils.calcCRC16_modbus(buffer.array(), true));
    }
}