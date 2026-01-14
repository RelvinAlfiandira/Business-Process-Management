package com.uncal.bpm_backend.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "files", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "subfolder_type", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore
    private Project project;

    @Column(name = "subfolder_type", nullable = false, length = 50)
    private String subfolderType;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canvas_data", columnDefinition = "jsonb")
    private String canvasData = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // ✅ PERBAIKAN: Helper method untuk mendapatkan runStatus dari canvasData
    public Integer getRunStatus() {
        try {
            if (this.canvasData == null || this.canvasData.trim().isEmpty()) {
                return 0;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode canvasNode = mapper.readTree(this.canvasData);
            return canvasNode.has("runStatus") ? canvasNode.get("runStatus").asInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ✅ PERBAIKAN: Helper method untuk mengupdate runStatus di canvasData
    public void setRunStatus(Integer runStatus) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode canvasNode;
            
            if (this.canvasData == null || this.canvasData.trim().isEmpty()) {
                canvasNode = mapper.createObjectNode();
            } else {
                canvasNode = mapper.readTree(this.canvasData);
            }
            
            ObjectNode objectNode = (ObjectNode) canvasNode;
            objectNode.put("runStatus", runStatus);
            this.canvasData = mapper.writeValueAsString(objectNode);
        } catch (Exception e) {
            // Fallback: buat canvasData baru dengan runStatus
            this.canvasData = "{\"runStatus\":" + runStatus + "}";
        }
    }

    // Helper method untuk mendapatkan struktur JSON untuk engine
    public String getEngineJsonStructure() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            // Parse existing canvasData
            JsonNode canvasNode = mapper.readTree(this.canvasData);
            ObjectNode engineJson = mapper.createObjectNode();
            
            // Build the structure sesuai format yang diinginkan
            engineJson.put("project", this.project != null ? this.project.getName() : "");
            engineJson.put("scenarios", this.name);
            engineJson.put("run_status", this.getRunStatus());
            
            // Jika canvasData sudah memiliki jsonData, gunakan itu
            if (canvasNode.has("jsonData")) {
                engineJson.set("jsonData", canvasNode.get("jsonData"));
            } else {
                // Jika tidak, buat struktur default
                ObjectNode defaultJsonData = mapper.createObjectNode();
                defaultJsonData.putArray("components");
                defaultJsonData.put("name", this.name);
                defaultJsonData.put("uuid", "generated-" + this.id);
                
                ObjectNode jsonDataArray = mapper.createObjectNode();
                jsonDataArray.set("0", defaultJsonData);
                
                engineJson.set("jsonData", mapper.createArrayNode().add(defaultJsonData));
            }
            
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(engineJson);
        } catch (Exception e) {
            // Fallback structure
            return "{\n" +
                   "  \"jsonData\": [\n" +
                   "    {\n" +
                   "      \"components\": [],\n" +
                   "      \"name\": \"" + this.name + "\",\n" +
                   "      \"uuid\": \"generated-" + this.id + "\"\n" +
                   "    }\n" +
                   "  ],\n" +
                   "  \"project\": \"" + (this.project != null ? this.project.getName() : "") + "\",\n" +
                   "  \"scenarios\": \"" + this.name + "\",\n" +
                   "  \"run_status\": " + this.getRunStatus() + "\n" +
                   "}";
        }
    }
}