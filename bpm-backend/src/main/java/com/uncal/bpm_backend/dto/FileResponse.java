package com.uncal.bpm_backend.dto;

import com.uncal.bpm_backend.model.File;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FileResponse {
    private Long id;
    private String subfolderType;
    private String name;
    private String canvasData;
    private String metadata; 
    private Integer runStatus;

    public static FileResponse fromModel(File file) {
        return FileResponse.builder()
                .id(file.getId())
                .subfolderType(file.getSubfolderType())
                .name(file.getName())
                .canvasData(file.getCanvasData())
                .metadata(file.getMetadata())
                .runStatus(file.getRunStatus())
                .build();
    }
}