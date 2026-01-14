package com.uncal.bpm_backend.dto;

import com.uncal.bpm_backend.model.File;
import com.uncal.bpm_backend.model.Project;
import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * DTO untuk mengemas hasil pencarian global yang mencakup Project dan File.
 */
@Data
@Builder
public class SearchResponse {

    // List proyek yang cocok dengan query
    private List<Project> projects;

    // List file yang cocok dengan query (diconvert dari model File ke FileResponse jika perlu)
    private List<File> files;
}
