package net.cvs0.bytecode.attribute;

import net.cvs0.bytecode.member.LocalVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalVariableTableAttribute extends Attribute {
    private final List<LocalVariable> localVariables = new ArrayList<>();
    
    public LocalVariableTableAttribute() {
        super("LocalVariableTable");
    }
    
    public void addLocalVariable(LocalVariable localVariable) {
        localVariables.add(localVariable);
    }
    
    public void removeLocalVariable(LocalVariable localVariable) {
        localVariables.remove(localVariable);
    }
    
    public List<LocalVariable> getLocalVariables() {
        return Collections.unmodifiableList(localVariables);
    }
    
    public LocalVariable getLocalVariableByIndex(int index) {
        return localVariables.stream()
                .filter(lv -> lv.getIndex() == index)
                .findFirst()
                .orElse(null);
    }
    
    public List<LocalVariable> getActiveVariablesAtPc(int pc) {
        return localVariables.stream()
                .filter(lv -> lv.isActive(pc))
                .toList();
    }
    
    public LocalVariable getVariableByName(String name) {
        return localVariables.stream()
                .filter(lv -> name.equals(lv.getName()))
                .findFirst()
                .orElse(null);
    }
    
    public int getLocalVariableCount() {
        return localVariables.size();
    }
    
    public void clearLocalVariables() {
        localVariables.clear();
    }
    
    public void sortLocalVariables() {
        localVariables.sort((a, b) -> {
            int indexCompare = Integer.compare(a.getIndex(), b.getIndex());
            if (indexCompare != 0) {
                return indexCompare;
            }
            return Integer.compare(a.getStartPc(), b.getStartPc());
        });
    }
    
    public boolean hasVariable(String name) {
        return localVariables.stream().anyMatch(lv -> name.equals(lv.getName()));
    }
    
    public int getMaxIndex() {
        return localVariables.stream()
                .mapToInt(LocalVariable::getIndex)
                .max()
                .orElse(-1);
    }
    
    @Override
    public String toString() {
        return "LocalVariableTableAttribute{" +
                "localVariables=" + localVariables.size() +
                '}';
    }
}