package util.data.vals;

public interface ValUser {

    String  id();
    boolean isWriter();
    boolean provideVal( BaseVal val );
    default String getValIssues(){ return id();}
}
