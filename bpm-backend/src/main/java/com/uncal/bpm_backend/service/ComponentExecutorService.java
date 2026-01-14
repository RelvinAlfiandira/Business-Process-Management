package com.uncal.bpm_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ComponentExecutorService {

    private final Map<String, ComponentHandler> componentHandlers;

    public ComponentExecutorService(List<ComponentHandler> handlers) {
        this.componentHandlers = new HashMap<>();
        
        // ✅ SMART REGISTRATION: Gunakan class name sebagai key untuk avoid conflict
        for (ComponentHandler handler : handlers) {
            String handlerKey = handler.getClass().getSimpleName();
            componentHandlers.put(handlerKey, handler);
            log.info("✅ Registered handler: {} -> {}", handlerKey, handler.getComponentType());
        }
        
        log.info("🎯 Total registered handlers: {}", componentHandlers.keySet());
    }

    public boolean executeComponent(ComponentExecutionData componentData, ExecutionContext context) {
        String componentType = componentData.getType();
        String componentLabel = componentData.getLabel();
        
        try {
            // ✅ SMART ROUTING: Cari handler berdasarkan component label
            ComponentHandler handler = findAppropriateHandler(componentLabel, componentType);
            
            if (handler == null) {
                log.error("❌ No handler found for component: {} ({})", componentLabel, componentType);
                context.put("lastError", "No handler found for component: " + componentLabel);
                return false;
            }
            
            log.info("🔧 Executing {}: {} with {}", componentType, componentLabel, 
                    handler.getClass().getSimpleName());
            
            // Add component info to context
            context.put("currentComponent", componentType);
            context.put("currentComponentLabel", componentLabel);
            
            long startTime = System.currentTimeMillis();
            boolean success = handler.execute(componentData, context);
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (success) {
                log.info("✅ {} executed successfully in {}ms: {}", 
                        componentType, executionTime, componentLabel);
                context.put("lastComponentSuccess", componentType);
            } else {
                log.error("❌ {} execution failed in {}ms: {}", 
                         componentType, executionTime, componentLabel);
                context.put("lastComponentError", componentType);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("💥 Error executing {} - {}: {}", 
                     componentType, componentLabel, e.getMessage(), e);
            context.put("lastError", "Error in " + componentType + ": " + e.getMessage());
            return false;
        }
    }

    // ✅ SMART HANDLER SELECTION berdasarkan component label
    private ComponentHandler findAppropriateHandler(String componentLabel, String componentType) {
        String labelLower = componentLabel.toLowerCase();
        
        log.debug("🔍 Finding handler for: '{}' (type: {})", componentLabel, componentType);
        
        // Priority-based routing berdasarkan label content
        if (labelLower.contains("jdbc") && labelLower.contains("sender")) {
            ComponentHandler handler = componentHandlers.get("JdbcSenderHandler");
            log.info("🎯 Routing '{}' → JdbcSenderHandler", componentLabel);
            return handler;
        }
        else if (labelLower.contains("jdbc") && labelLower.contains("receiver")) {
            ComponentHandler handler = componentHandlers.get("JdbcReceiverHandler");
            log.info("🎯 Routing '{}' → JdbcReceiverHandler", componentLabel);
            return handler;
        }
        else if (labelLower.contains("file") && labelLower.contains("sender")) {
            ComponentHandler handler = componentHandlers.get("FileSenderHandler");
            log.info("🎯 Routing '{}' → FileSenderHandler", componentLabel);
            return handler;
        }
        else if (labelLower.contains("file") && labelLower.contains("receiver")) {
            ComponentHandler handler = componentHandlers.get("FileReceiverHandler");
            log.info("🎯 Routing '{}' → FileReceiverHandler", componentLabel);
            return handler;
        }
        
        // Fallback: coba cari berdasarkan type (untuk backward compatibility)
        log.warn("⚠️ No specific handler found for '{}', trying fallback...", componentLabel);
        return findFallbackHandler(componentType);
    }
    
    // ✅ FALLBACK MECHANISM untuk backward compatibility
    private ComponentHandler findFallbackHandler(String componentType) {
        // Cari handler pertama yang match dengan type yang diminta
        for (Map.Entry<String, ComponentHandler> entry : componentHandlers.entrySet()) {
            if (entry.getValue().getComponentType().equals(componentType)) {
                log.info("🔄 Fallback routing for type '{}' → {}", componentType, entry.getKey());
                return entry.getValue();
            }
        }
        return null;
    }

    // ✅ PERBAIKAN: Tambah method executeComponentWithRetry untuk retry mechanism
    public boolean executeComponentWithRetry(ComponentExecutionData componentData, ExecutionContext context) {
        String componentType = componentData.getType();
        String componentLabel = componentData.getLabel();
        
        // Ambil retry configuration dari config data
        int maxRetries = componentData.getConfigData().optInt("maxRetries", 1);
        int retryInterval = componentData.getConfigData().optInt("retryInterval", 5); // default 5 detik
        
        // Jika maxRetries = 1, langsung execute tanpa retry
        if (maxRetries <= 1) {
            return executeComponent(componentData, context);
        }
        
        log.info("🔄 Configuring retry mechanism for {}: maxRetries={}, retryInterval={}s", 
                componentLabel, maxRetries, retryInterval);
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.info("🔄 Attempt {}/{} for {}: {}", attempt, maxRetries, componentType, componentLabel);
            
            boolean success = executeComponent(componentData, context);
            
            if (success) {
                log.info("✅ {} succeeded on attempt {}/{}", componentLabel, attempt, maxRetries);
                return true;
            }
            
            // Jika bukan attempt terakhir, tunggu sebelum retry
            if (attempt < maxRetries) {
                log.warn("⚠️ {} failed on attempt {}/{}, retrying in {} seconds...", 
                        componentLabel, attempt, maxRetries, retryInterval);
                
                try {
                    Thread.sleep(retryInterval * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ Retry interrupted for {}: {}", componentLabel, e.getMessage());
                    return false;
                }
            }
        }
        
        log.error("💥 {} failed after {} attempts", componentLabel, maxRetries);
        return false;
    }
}