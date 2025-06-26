package net.cvs0.bytecode.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeAttribute extends Attribute {
    private int maxStack;
    private int maxLocals;
    private byte[] code;
    private final List<ExceptionHandler> exceptionHandlers = new ArrayList<>();
    private final List<Attribute> codeAttributes = new ArrayList<>();
    
    public CodeAttribute() {
        super("Code");
    }
    
    public int getMaxStack() {
        return maxStack;
    }
    
    public void setMaxStack(int maxStack) {
        this.maxStack = maxStack;
    }
    
    public int getMaxLocals() {
        return maxLocals;
    }
    
    public void setMaxLocals(int maxLocals) {
        this.maxLocals = maxLocals;
    }
    
    public byte[] getCode() {
        return code != null ? code.clone() : null;
    }
    
    public void setCode(byte[] code) {
        this.code = code != null ? code.clone() : null;
    }
    
    public void addExceptionHandler(ExceptionHandler handler) {
        exceptionHandlers.add(handler);
    }
    
    public void removeExceptionHandler(ExceptionHandler handler) {
        exceptionHandlers.remove(handler);
    }
    
    public List<ExceptionHandler> getExceptionHandlers() {
        return Collections.unmodifiableList(exceptionHandlers);
    }
    
    public void addCodeAttribute(Attribute attribute) {
        codeAttributes.add(attribute);
    }
    
    public void removeCodeAttribute(Attribute attribute) {
        codeAttributes.remove(attribute);
    }
    
    public List<Attribute> getCodeAttributes() {
        return Collections.unmodifiableList(codeAttributes);
    }
    
    public int getCodeLength() {
        return code != null ? code.length : 0;
    }
    
    public int getExceptionHandlerCount() {
        return exceptionHandlers.size();
    }
    
    public void clearExceptionHandlers() {
        exceptionHandlers.clear();
    }
    
    public void clearCodeAttributes() {
        codeAttributes.clear();
    }
    
    public static class ExceptionHandler {
        private int startPc;
        private int endPc;
        private int handlerPc;
        private String catchType;
        
        public ExceptionHandler(int startPc, int endPc, int handlerPc, String catchType) {
            this.startPc = startPc;
            this.endPc = endPc;
            this.handlerPc = handlerPc;
            this.catchType = catchType;
        }
        
        public int getStartPc() {
            return startPc;
        }
        
        public void setStartPc(int startPc) {
            this.startPc = startPc;
        }
        
        public int getEndPc() {
            return endPc;
        }
        
        public void setEndPc(int endPc) {
            this.endPc = endPc;
        }
        
        public int getHandlerPc() {
            return handlerPc;
        }
        
        public void setHandlerPc(int handlerPc) {
            this.handlerPc = handlerPc;
        }
        
        public String getCatchType() {
            return catchType;
        }
        
        public void setCatchType(String catchType) {
            this.catchType = catchType;
        }
        
        public boolean isCatchAll() {
            return catchType == null;
        }
        
        public boolean covers(int pc) {
            return pc >= startPc && pc < endPc;
        }
        
        @Override
        public String toString() {
            return "ExceptionHandler{" +
                    "startPc=" + startPc +
                    ", endPc=" + endPc +
                    ", handlerPc=" + handlerPc +
                    ", catchType='" + catchType + '\'' +
                    '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            ExceptionHandler that = (ExceptionHandler) o;
            
            if (startPc != that.startPc) return false;
            if (endPc != that.endPc) return false;
            if (handlerPc != that.handlerPc) return false;
            return catchType != null ? catchType.equals(that.catchType) : that.catchType == null;
        }
        
        @Override
        public int hashCode() {
            int result = startPc;
            result = 31 * result + endPc;
            result = 31 * result + handlerPc;
            result = 31 * result + (catchType != null ? catchType.hashCode() : 0);
            return result;
        }
    }
}