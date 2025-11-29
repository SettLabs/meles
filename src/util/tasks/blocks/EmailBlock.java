package util.tasks.blocks;

import meles.Core;
import io.Writable;
import io.email.Email;
import org.tinylog.Logger;
import worker.Datagram;

public class EmailBlock extends AbstractBlock implements Writable {
    Email email;

    public static EmailBlock sendEmail( Email email ){
        return new EmailBlock(email);
    }
    protected EmailBlock(Email email) {
        this.email = email;
        this.email.writable(this);
    }
    @Override
    public boolean start() {
        Core.addToQueue(Datagram.system("email:deliver").payload(email));//.writable(this));
        return true;
    }
    public String type(){ return "EmailBlock";}
    @Override
    public boolean writeLine(String origin, String data) {
        Logger.info(id() + " -> Reply: " + (data.length() < 30 ? data : data.substring(0, 30) + "..."));
        //doNext();
        return true;
    }
    @Override
    public void giveObject(String info, Object result) {
        if( info.equals("email") ){
            if( result instanceof Boolean res ){
                if( res ){
                    doNext();
                }else{
                    doErrorRoute(false);
                }
            }
        }
    }
    @Override
    public boolean isConnectionValid() {
        return true;
    }
    public String toString() {
        return telnetId() + " -> " + email + ".";
    }
}
