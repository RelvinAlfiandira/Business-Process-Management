package com.uncal.bpm_backend.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JdbcReceiverHandler implements ComponentHandler {
    
    private final Object databaseLock = new Object();
    
    @Override
    public String getComponentType() {
        return "Receiver"; // ✅ Tetap return "Receiver" untuk compatibility
    }
    
    @Override
    public boolean execute(ComponentExecutionData componentData, ExecutionContext context) {
        String componentLabel = componentData.getLabel();
        
        try {
            JSONObject config = componentData.getConfigData();
            String dbType = config.optString("db_type", "POSTGRESQL");
            String host = config.optString("host", "localhost");
            int port = config.optInt("port", getDefaultPort(dbType));
            String dbName = config.optString("db_name", "");
            String schema = config.optString("schema", "");
            String username = config.optString("username", "");
            String password = config.optString("password", "");
            String targetTable = config.optString("targetTable", "");
            int retryInterval = config.optInt("retryInterval", 30);
            
            log.info("🗃️ JDBC Receiver [{}] - DB: {}://{}:{}/{}, Table: {}", 
                    componentLabel, dbType, host, port, dbName, targetTable);
            
            // Validasi konfigurasi
            if (host.isEmpty() || dbName.isEmpty() || username.isEmpty() || targetTable.isEmpty()) {
                log.error("❌ Invalid configuration for JDBC Receiver: missing required parameters");
                return false;
            }
            
            if (!context.contains("jdbcData")) {
                log.error("❌ No JDBC data available in context for JDBC Receiver");
                return false;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) context.get("jdbcData");
            String sourceQuery = (String) context.get("sourceQuery");
            int recordCount = (Integer) context.get("recordCount");
            
            log.info("📥 Receiving {} records from query: {}", recordCount, sourceQuery);
            log.info("🎯 Target table: {}", targetTable);
            
            boolean insertSuccess;
            synchronized (databaseLock) {
                insertSuccess = processDataInsert(data, dbType, host, port, dbName, schema, username, password, targetTable);
            }
            
            if (insertSuccess) {
                // ✅ Store destination info in context for execution log
                context.put("destinationTable", targetTable);
                context.put("destinationDbType", dbType);
                context.put("recordsProcessed", recordCount);
                
                log.info("✅ JDBC Receiver execution successful - {} records inserted into {}", 
                        recordCount, targetTable);
                return true;
            } else {
                log.error("❌ JDBC Receiver execution failed");
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ JDBC Receiver execution failed [{}]: {}", componentLabel, e.getMessage(), e);
            return false;
        }
    }
    
    private boolean processDataInsert(List<Map<String, Object>> data, String dbType, String host, 
                                    int port, String dbName, String schema, String username, 
                                    String password, String targetTable) {
        if (data == null || data.isEmpty()) {
            log.warn("⚠️ No data to insert into database");
            return true;
        }
        
        String connectionUrl = buildConnectionUrl(dbType, host, port, dbName);
        
        try (Connection connection = DriverManager.getConnection(connectionUrl, username, password)) {
            log.info("🔗 Connected to target database: {}", connectionUrl);
            
            // Ambil sample row untuk menentukan struktur data
            Map<String, Object> sampleRow = data.get(0);
            String insertQuery = buildInsertQuery(targetTable, schema, sampleRow);
            
            log.info("💾 Insert query prepared: {}", insertQuery);
            log.info("📊 Inserting {} records into {}", data.size(), targetTable);
            
            int successfulInserts = 0;
            int failedInserts = 0;
            
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                for (Map<String, Object> row : data) {
                    try {
                        int paramIndex = 1;
                        for (Object value : row.values()) {
                            statement.setObject(paramIndex++, value);
                        }
                        
                        int affectedRows = statement.executeUpdate();
                        if (affectedRows > 0) {
                            successfulInserts++;
                        } else {
                            failedInserts++;
                            log.warn("⚠️ No rows affected for record: {}", row);
                        }
                        
                    } catch (Exception e) {
                        failedInserts++;
                        log.error("❌ Failed to insert record {}: {}", row, e.getMessage());
                    }
                }
            }
            
            if (failedInserts == 0) {
                log.info("✅ All {} records successfully inserted into {}", successfulInserts, targetTable);
                return true;
            } else {
                log.warn("⚠️ Insert completed with {} successes and {} failures", successfulInserts, failedInserts);
                return successfulInserts > 0;
            }
            
        } catch (Exception e) {
            log.error("❌ Database connection/insert failed: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private String buildConnectionUrl(String dbType, String host, int port, String dbName) {
        switch (dbType.toUpperCase()) {
            case "POSTGRESQL":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
            case "MYSQL":
                return String.format("jdbc:mysql://%s:%d/%s", host, port, dbName);
            case "MSSQL":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, dbName);
            case "ORACLE":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, dbName);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }
    
    private int getDefaultPort(String dbType) {
        switch (dbType.toUpperCase()) {
            case "POSTGRESQL": return 5432;
            case "MYSQL": return 3306;
            case "MSSQL": return 1433;
            case "ORACLE": return 1521;
            default: return 5432;
        }
    }
    
    private String buildInsertQuery(String tableName, String schema, Map<String, Object> sampleRow) {
        StringBuilder query = new StringBuilder("INSERT INTO ");
        
        if (schema != null && !schema.isEmpty()) {
            query.append(schema).append(".");
        }
        
        query.append(tableName).append(" (");
        
        // Column names
        boolean first = true;
        for (String column : sampleRow.keySet()) {
            if (!first) {
                query.append(", ");
            }
            query.append(column);
            first = false;
        }
        
        query.append(") VALUES (");
        
        // Parameter placeholders
        first = true;
        for (int i = 0; i < sampleRow.size(); i++) {
            if (!first) {
                query.append(", ");
            }
            query.append("?");
            first = false;
        }
        
        query.append(")");
        
        return query.toString();
    }
}