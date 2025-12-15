package util.data.vals.symbiote;

import io.Writable;
import util.data.vals.NumericVal;

public interface Symbiote {
    int level();
    NumericVal[] getUnderlings();
    NumericVal[] getDerived();
    NumericVal getHost();
    boolean replaceUnderling( NumericVal repl );
    void addUnderling(NumericVal underling);
}
