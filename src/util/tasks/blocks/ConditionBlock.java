package util.tasks.blocks;

import org.tinylog.Logger;
import util.data.vals.*;
import util.evalcore.Evaluator;
import util.evalcore.LogEvaluatorDummy;
import util.evalcore.ParseTools;

import java.util.ArrayList;
import java.util.Optional;

public class ConditionBlock extends AbstractBlock implements ValUser {

    Evaluator logEval;
    FlagVal flag;

    // Only to be used by LogicFab during block construction
    ConditionBlock(Evaluator logEval) {
        this.logEval=logEval;
    }

    ConditionBlock() {
    }
    public String type(){ return "ConditionBlock";}
    public static Optional<ConditionBlock> build(String condition, Rtvals rtvals, ArrayList<NumericVal> sharedMem) {
        var logEvalOpt = ParseTools.parseComparison(condition, rtvals, sharedMem);
        if (logEvalOpt == null)
            return Optional.empty();
        return Optional.of(new ConditionBlock(logEvalOpt));
    }
    public static ConditionBlock fakeBlock( AbstractBlock next){
        var b = new ConditionBlock( new LogEvaluatorDummy() );
        b.addNext(next);
        return b;
    }
    public void setFlag(FlagVal flag) {
        this.flag = flag;
    }
    @Override
    public boolean start() {
        return start(0.0);
    }

    public boolean start(double... input) {
        // After
        if (logEval == null) {
            Logger.error(id() + " -> This block probably wasn't constructed properly because no valid evaluator was found, route aborted.");
            return false;
        }
        var pass = logEval.logicEval(input);
        if (flag != null)
            flag.update(pass);
        if (pass) {
            doNext(input);
        } else {
            doAltRoute(true);
        }
        return pass;
    }

    void doNext(double... input) {
        if (next != null) {
            if (next instanceof ConditionBlock cb) {
                cb.start(input);
            } else if (next instanceof LogBlock lb) {
                lb.start(input);
            } else if (next instanceof MathBlock mb) {
                mb.start(input);
            } else {
                next.start();
            }
        }
        sendCallback(id() + " -> OK");
    }
    public String toString() {
        return telnetId() + " -> Check if " + logEval.getOriginalExpression() + (altRoute == null ? "." : ". If not, go to " + altRoute.telnetId());
    }
    public String getEvalInfo(){
        if(logEval==null)
            return "No valid LogEvaluator present.";
        return logEval.getInfo();
    }

    public boolean isWriter(){
        return false;
    }
    public boolean provideVal( BaseVal newVal){
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
}
