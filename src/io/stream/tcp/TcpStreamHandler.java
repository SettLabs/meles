package io.stream.tcp;

import meles.Core;
import io.netty.channel.*;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.stream.StreamListener;
import io.stream.udp.UdpStream;
import org.tinylog.Logger;
import worker.Datagram;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class TcpStreamHandler extends SimpleChannelInboundHandler<byte[]>{

    protected String id;

    protected List<StreamListener> listeners = new ArrayList<>();

    protected Channel channel;
    protected boolean log=true;
    protected InetSocketAddress remote;
    protected TcpStream stream;

    boolean udp=false;

    public TcpStreamHandler( String id ){
        this.id=id;
    }

    public TcpStreamHandler(String id, TcpStream stream) {
        this.id=id;
        this.stream=stream;
        if( stream instanceof UdpStream)
            udp=true;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent e) {
            if (e.state() == IdleState.READER_IDLE ) {
                if( !stream.isIdle()) {
                    Logger.error( id + " -> Reader Idle" );
                    stream.flagAsIdle();
                }
            }else if (e.state() == IdleState.WRITER_IDLE) {
                Logger.error( "WRITER IDLE for "+id);
            }else {
                Logger.error( "Something went Wrong");
            }
        }else{
            Logger.info(id+" -> Unknown user event... "+evt);
        }
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised, but don't send messages if it's related to remote ignore
        String address = ctx.channel().remoteAddress().toString();

        if (cause instanceof TooLongFrameException) {
            Logger.warn(id+" -> Unexpected exception caught: "+cause.getMessage(), true);
            ctx.flush();
        }else if( cause instanceof java.net.PortUnreachableException){
            if( !udp ){
                Logger.error("Device/Port unreachable, probably offline: "+address);
                ctx.flush();
                ctx.close();							// Close the channel
            }
        }else{
            Logger.error(cause);
            Logger.error( id+" -> Unexpected exception caught: " + cause.getMessage() );
            ctx.close();							// Close the channel
        }
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {

        channel = ctx.channel();			// Store the channel for future use
        stream.setChannel(channel);

        if( channel.remoteAddress() != null){					// Incase the remote address is not null
            remote = (InetSocketAddress)ctx.channel().remoteAddress();	// Store this as remote address
        }else{
            Logger.error( "Channel.remoteAddress is null in channelActive method");
        }
        Logger.info("Channel Opened: "+ctx.channel().remoteAddress() );
        stream.triggerOpened();
    }
    @Override
    public void channelInactive( ChannelHandlerContext ctx){
        Logger.info( "Channel Closed! "+ remote.toString() );
        stream.triggerClosed();
        stream.connect(true);

    }
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        // Don't care about this
    }
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        // Don't care about this
    }
    @Override
    public void channelRead0(ChannelHandlerContext ctx, byte[] data) throws Exception {

        String msg = new String( data );	// Convert the raw data to a readable string

        if( stream.isIdle() )
            stream.flagAsActive();

        if (msg.isBlank())//make sure that the received data is not 'null' or an empty string
            return;

        msg = msg.replace("\n", "");   // Remove newline characters
        msg = msg.replace("\r", "");   // Remove carriage return characters
        msg = msg.replace("\0", "");   // Remove null characters

        // Implement the use of labels
        if (!stream.getLabel().isEmpty()) { // No use adding to queue without label
            Core.addToQueue(Datagram.build(msg)
                    .label(stream.getLabel())
                    .origin(id)
                    .priority(stream.getPriority())
                    .writable(stream)
            );
        }

        // Forward data to targets
        if (stream.getRequestsSize()==0 )
            return;

        String tosend = new String(data);
        try {
            stream.getTargets().parallelStream().forEach(wr -> wr.writeLine(id, tosend));// Concurrent sending to multiple writables
            stream.getTargets().removeIf(wr -> !wr.isConnectionValid()); // Clear inactive
        } catch (ConcurrentModificationException e) {
            Logger.error(e);
        }

        // Keep the timestamp of the last message
        stream.setTimestamp( Instant.now().toEpochMilli() );            // Store the timestamp of the received message
    }
}
