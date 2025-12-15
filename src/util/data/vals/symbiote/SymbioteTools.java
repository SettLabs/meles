package util.data.vals.symbiote;

import org.tinylog.Logger;
import util.data.vals.RealVal;

import java.util.concurrent.ConcurrentHashMap;

public class SymbioteTools {
    public RealValSymbiote upgradeToRealSymbiote(ConcurrentHashMap<String, RealVal> hm, RealVal iv ){
        var reg = hm.get(iv.id());
        RealValSymbiote rvs;
        if( iv instanceof RealValSymbiote sym ){
            rvs = sym;
        }else{
            rvs = new RealValSymbiote(0, iv );
        }
        hm.put(rvs.id(),rvs);

        if( reg.isDummy() ){// No such variable exist yet, so create it
            broadCastCreation(rvs);
        }else if( !(reg instanceof RealValSymbiote)){ // Exist, so encapsulate
            broadcastReplacement(rvs);
        }else{
            Logger.info("Already a symbiote, not touching it");
            rvs=(RealValSymbiote) reg;
        }
        applyUser(rvs,true);
        return rvs;
    }
}
