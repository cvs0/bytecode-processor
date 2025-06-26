package net.cvs0.bytecode.attribute;

import java.util.*;

public class AnnotationAttribute extends Attribute {
    private final List<AnnotationInfo> annotations = new ArrayList<>();
    private boolean visible;
    
    public AnnotationAttribute(String name, boolean visible) {
        super(name);
        this.visible = visible;
    }
    
    public void addAnnotation(AnnotationInfo annotation) {
        annotations.add(annotation);
    }
    
    public void removeAnnotation(AnnotationInfo annotation) {
        annotations.remove(annotation);
    }
    
    public List<AnnotationInfo> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public AnnotationInfo findAnnotation(String type) {
        return annotations.stream()
                .filter(ann -> ann.getType().equals(type))
                .findFirst()
                .orElse(null);
    }
    
    public boolean hasAnnotation(String type) {
        return findAnnotation(type) != null;
    }
    
    public int getAnnotationCount() {
        return annotations.size();
    }
    
    public void clearAnnotations() {
        annotations.clear();
    }
    
    public static class AnnotationInfo {
        private String type;
        private final Map<String, Object> values = new HashMap<>();
        
        public AnnotationInfo(String type) {
            this.type = type;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public void setValue(String name, Object value) {
            values.put(name, value);
        }
        
        public Object getValue(String name) {
            return values.get(name);
        }
        
        public Map<String, Object> getValues() {
            return Collections.unmodifiableMap(values);
        }
        
        public boolean hasValue(String name) {
            return values.containsKey(name);
        }
        
        public void removeValue(String name) {
            values.remove(name);
        }
        
        public String getStringValue(String name) {
            Object value = values.get(name);
            return value instanceof String ? (String) value : null;
        }
        
        public Integer getIntValue(String name) {
            Object value = values.get(name);
            return value instanceof Integer ? (Integer) value : null;
        }
        
        public Boolean getBooleanValue(String name) {
            Object value = values.get(name);
            return value instanceof Boolean ? (Boolean) value : null;
        }
        
        @Override
        public String toString() {
            return "AnnotationInfo{" +
                    "type='" + type + '\'' +
                    ", values=" + values +
                    '}';
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            AnnotationInfo that = (AnnotationInfo) o;
            
            if (!type.equals(that.type)) return false;
            return values.equals(that.values);
        }
        
        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + values.hashCode();
            return result;
        }
    }
}