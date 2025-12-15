package util.data.vals.symbiote;

import io.Writable;
import org.apache.commons.lang3.ArrayUtils;
import util.data.procs.ValPrinter;
import util.data.vals.BaseVal;
import util.data.vals.NumericVal;
import util.data.vals.RealVal;
import util.data.vals.ValUser;

import java.util.Arrays;

public class RealValSymbiote extends RealVal implements Symbiote, ValUser {

    NumericVal[] underlings;
    boolean passOriginal = false;
    int level = 0;

    public RealValSymbiote(int level, RealVal... underlings) {
        super(underlings[0].group(), underlings[0].name(), underlings[0].unit());

        this.underlings = underlings;
        this.level = level;
    }

    public boolean update(double val) {
        var result = underlings[0].update(val);
        this.value = underlings[0].asDouble();

        var forwardedValue = passOriginal ? val : value;
        if (result || passOriginal)
            Arrays.stream(underlings, 1, underlings.length).forEach(rv -> rv.update(forwardedValue));

        return result;
    }

    public double value() {
        return underlings[0].asDouble();
    }

    public int level() {
        return level;
    }
    @Override
    public void resetValue() {
        underlings[0].defValue(defValue);
        value = underlings[0].asDouble();
    }

    public void defValue(double defValue) {
        underlings[0].defValue(defValue);
        this.defValue = defValue;
    }

    public void addUnderling(RealVal underling) {
        underlings = ArrayUtils.add(underlings, underling);
    }

    public NumericVal[] getUnderlings(){ return underlings; }

    public NumericVal[] getDerived() {
        return Arrays.copyOfRange(underlings, 1, underlings.length);
    }

    @Override
    public NumericVal getHost() {
        return underlings[0];
    }

    @Override
    public boolean replaceUnderling(NumericVal repl) {
        for( int i=1;i<underlings.length;i++ ){
            if( underlings[i].id().equals(repl.id() )){
                underlings[i]=repl;
                return true;
            }
        }
        return false;
    }

    @Override
    public void addUnderling(NumericVal underling) {

    }

    public void removePrinterUnderling(Writable wr){
        int index=-1;
        for( int a=1;a<underlings.length;a++ ){
            if( underlings[a] instanceof ValPrinter vp ){
                if( vp.matchWritable(wr)) {
                    index = a;
                    break;
                }
            }
        }
        if( index>=1 )
            underlings = ArrayUtils.remove(underlings,index);
    }

    public String getExtraInfo() {
        return underlings[0].getExtraInfo();
    }

    @Override
    public boolean isWriter() {
        return true;
    }

    @Override
    public boolean provideVal(BaseVal val) {
        return false;
    }
}
