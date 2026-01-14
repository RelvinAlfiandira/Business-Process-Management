package com.uncal.bpm_backend.service;

import lombok.Builder;
import lombok.Data;
import org.json.JSONObject;

@Data
@Builder
public class ComponentExecutionData {
    private String type;
    private String label;
    private JSONObject configData;
    private JSONObject rawComponentJson;
}