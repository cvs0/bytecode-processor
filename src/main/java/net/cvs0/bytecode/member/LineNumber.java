package net.cvs0.bytecode.member;

public class LineNumber {
    private int startPc;
    private int lineNumber;
    
    public LineNumber(int startPc, int lineNumber) {
        this.startPc = startPc;
        this.lineNumber = lineNumber;
    }
    
    public int getStartPc() {
        return startPc;
    }
    
    public void setStartPc(int startPc) {
        this.startPc = startPc;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public boolean covers(int pc) {
        return pc >= startPc;
    }
    
    @Override
    public String toString() {
        return "LineNumber{" +
                "startPc=" + startPc +
                ", lineNumber=" + lineNumber +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LineNumber that = (LineNumber) o;
        
        if (startPc != that.startPc) return false;
        return lineNumber == that.lineNumber;
    }
    
    @Override
    public int hashCode() {
        int result = startPc;
        result = 31 * result + lineNumber;
        return result;
    }
}