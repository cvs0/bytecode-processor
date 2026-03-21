package net.cvs0.bytecode.attribute;

import net.cvs0.bytecode.member.LineNumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the LineNumberTable attribute, which stores mappings from bytecode offsets to source code line numbers.
 * Provides methods for managing and querying line numbers.
 */
public class LineNumberTableAttribute extends Attribute {
    /** List of line number mappings. */
    private final List<LineNumber> lineNumbers = new ArrayList<>();
    
    /**
     * Constructs a LineNumberTableAttribute.
     */
    public LineNumberTableAttribute() {
        super("LineNumberTable");
    }
    
    /**
     * Adds a line number entry to this attribute.
     * @param lineNumber the LineNumber to add
     */
    public void addLineNumber(LineNumber lineNumber) {
        lineNumbers.add(lineNumber);
    }
    
    /**
     * Removes a line number entry from this attribute.
     * @param lineNumber the LineNumber to remove
     */
    public void removeLineNumber(LineNumber lineNumber) {
        lineNumbers.remove(lineNumber);
    }
    
    /**
     * Returns all line numbers in this attribute.
     * @return unmodifiable list of line numbers
     */
    public List<LineNumber> getLineNumbers() {
        return Collections.unmodifiableList(lineNumbers);
    }
    
    /**
     * Returns the line number mapping for a given program counter (PC).
     * @param pc the bytecode program counter
     * @return the corresponding LineNumber, or null if not found
     */
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
    
    /**
     * Returns the number of line numbers in this attribute.
     * @return the line number count
     */
    public int getLineNumberCount() {
        return lineNumbers.size();
    }
    
    /**
     * Removes all line numbers from this attribute.
     */
    public void clearLineNumbers() {
        lineNumbers.clear();
    }
    
    /**
     * Sorts the line numbers by their start PC.
     */
    public void sortLineNumbers() {
        lineNumbers.sort((a, b) -> Integer.compare(a.getStartPc(), b.getStartPc()));
    }
    
    /**
     * Checks if a given source line number is present in the table.
     * @param lineNumber the source line number
     * @return true if present, false otherwise
     */
    public boolean hasLineNumber(int lineNumber) {
        return lineNumbers.stream().anyMatch(ln -> ln.getLineNumber() == lineNumber);
    }
    
    /**
     * Returns all LineNumber entries for a given source line number.
     * @param lineNumber the source line number
     * @return a list of matching LineNumber objects
     */
    public List<LineNumber> getLineNumbersForLine(int lineNumber) {
        return lineNumbers.stream()
                .filter(ln -> ln.getLineNumber() == lineNumber)
                .toList();
    }
    
    /**
     * Returns a string representation of this attribute.
     * @return a string with the number of line numbers
     */
    @Override
    public String toString() {
        return "LineNumberTableAttribute{" +
                "lineNumbers=" + lineNumbers.size() +
                '}';
    }

    /**
     * Checks equality with another object.
     * @param o the other object
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LineNumberTableAttribute that = (LineNumberTableAttribute) o;
        return lineNumbers.equals(that.lineNumbers);
    }

    /**
     * Returns a hash code for this attribute.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + lineNumbers.hashCode();
        return result;
    }
}