package com.uncal.bpm_backend.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionContext {
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    
    public void put(String key, Object value) {
        data.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = data.get(key);
        if (value == null) {
            throw new RuntimeException("Context data not found: " + key);
        }
        return (T) value;
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public boolean contains(String key) {
        return data.containsKey(key);
    }
    
    public void clear() {
        data.clear();
    }
}