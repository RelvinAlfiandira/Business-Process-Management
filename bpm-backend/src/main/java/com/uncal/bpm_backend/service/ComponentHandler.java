package com.uncal.bpm_backend.service;

public interface ComponentHandler {
    String getComponentType();
    boolean execute(ComponentExecutionData componentData, ExecutionContext context);
}