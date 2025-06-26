package net.cvs0.bytecode.attribute;

import net.cvs0.bytecode.member.LineNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineNumberTableAttribute extends Attribute {
    private final List<LineNumber> lineNumbers = new ArrayList<>();
    
    public LineNumberTableAttribute() {
        super("LineNumberTable");
    }
    
    public void addLineNumber(LineNumber lineNumber) {
        lineNumbers.add(lineNumber);
    }
    
    public void removeLineNumber(LineNumber lineNumber) {
        lineNumbers.remove(lineNumber);
    }
    
    public List<LineNumber> getLineNumbers() {
        return Collections.unmodifiableList(lineNumbers);
    }
    
    public LineNumber getLineNumberForPc(int pc) {
        LineNumber result = null;
        for (LineNumber lineNumber : lineNumbers) {
            if (lineNumber.getStartPc() <= pc) {
                result = lineNumber;
            } else {
                break;
            }
        }
        return result;
    }
    
    public int getLineNumberCount() {
        return lineNumbers.size();
    }
    
    public void clearLineNumbers() {
        lineNumbers.clear();
    }
    
    public void sortLineNumbers() {
        lineNumbers.sort((a, b) -> Integer.compare(a.getStartPc(), b.getStartPc()));
    }
    
    public boolean hasLineNumber(int lineNumber) {
        return lineNumbers.stream().anyMatch(ln -> ln.getLineNumber() == lineNumber);
    }
    
    public List<LineNumber> getLineNumbersForLine(int lineNumber) {
        return lineNumbers.stream()
                .filter(ln -> ln.getLineNumber() == lineNumber)
                .toList();
    }
    
    @Override
    public String toString() {
        return "LineNumberTableAttribute{" +
                "lineNumbers=" + lineNumbers.size() +
                '}';
    }
}