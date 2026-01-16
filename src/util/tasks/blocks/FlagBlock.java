package util.tasks.blocks;

import org.tinylog.Logger;
import util.data.vals.BaseVal;
import util.data.vals.FlagVal;
import util.data.vals.ValUser;

public class FlagBlock extends AbstractBlock implements ValUser {
    enum ACTION {RAISE, RESET, TOGGLE}

    FlagVal flag;
    ACTION action;

    public static FlagBlock raises() {
        return new FlagBlock( ACTION.RAISE );
    }

    public static FlagBlock lowers() {
        return new FlagBlock( ACTION.RESET );
    }

    public static FlagBlock toggles() {
        return new FlagBlock( ACTION.TOGGLE );
    }

    FlagBlock(ACTION action){
        this.action=action;
    }

    FlagBlock(FlagVal flag, ACTION action) {
        this.action = action;
        this.flag = flag;
    }
    public String type(){ return "FlagBlock";}
    @Override
    public boolean start() {
        switch (action) {
            case RAISE -> flag.update(true);
            case RESET -> flag.update(false);
            case TOGGLE -> flag.toggleState();
        }
        doNext();
        return true;
    }
    public boolean isWriter(){
        return true;
    }
    public boolean provideVal( BaseVal newVal ){
        if( newVal instanceof FlagVal fv ){
            if( fv.id().equals(flag.id())) {
                flag = fv;
                return true;
            }
        }
        return false;
    }
    public String getValIssues(){
        return "["+id()+" needs "+flag.id()+"]";
    }
    public void setFlag( FlagVal fv ){
        if( fv.isDummy() )
            Logger.info("Got flag dummy instead of "+fv.id());
        this.flag=fv;
    }
}
