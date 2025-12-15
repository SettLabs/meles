package util.data.vals.symbiote;

import io.Writable;
import org.apache.commons.lang3.ArrayUtils;
import util.data.procs.ValPrinter;
import util.data.vals.BaseVal;
import util.data.vals.IntegerVal;
import util.data.vals.NumericVal;
import util.data.vals.ValUser;

import java.util.Arrays;

public class IntegerValSymbiote extends IntegerVal implements ValUser,Symbiote {

    NumericVal[] underlings;
    boolean passOriginal = false;
    int level = 0;
    IntegerVal host;

    public IntegerValSymbiote(int level, IntegerVal host ) {
        super(host.group(), host.name(), host.unit());
        underlings = new NumericVal[]{host};
        this.level=level;
        this.host=host;
    }
    public IntegerValSymbiote(int level, IntegerVal host, NumericVal... underlings) {
        super(host.group(), host.name(), host.unit());

        this.underlings = ArrayUtils.insert(0,underlings,host);
        this.level = level;
        this.host = host;
    }
    @Override
    public boolean update(int val) {
        var result = host.update(val);
        this.value = host.value();

        var forwardedValue = passOriginal ? val : value;
        if (result || passOriginal)
            Arrays.stream(underlings, 1, underlings.length).forEach(rv -> rv.update(forwardedValue));

        return result;
    }

    public int value() {
        return host.value();
    }

    public int level() {
        return level;
    }

    @Override
    public void resetValue() {
        host.defValue(defValue);
        value = host.value();
    }

    @Override
    public void defValue(double val) {
        defValue=(int)val;
    }

    public void defValue(int defValue) {
        host.defValue(defValue);
        this.defValue = defValue;
    }
    public NumericVal[] getUnderlings(){
        return underlings;
    }
    public void addUnderling(NumericVal underling) {
        underlings = ArrayUtils.add(underlings, underling);
    }

    public NumericVal[] getDerived() {
        return Arrays.copyOfRange(underlings, 1, underlings.length);
    }
    public NumericVal getHost(){
        return host;
    }
    public boolean replaceUnderling( NumericVal repl ){
        for( int i=1;i<underlings.length;i++ ){
            if( underlings[i].id().equals(repl.id() )){
                underlings[i]=repl;
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean isWriter() {
        return true;
    }

    @Override
    public boolean provideVal(BaseVal val) {
        replaceUnderling((NumericVal) val);
        return Arrays.stream(underlings).anyMatch(NumericVal::isDummy);
    }
}
