package com.uncal.bpm_backend.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JdbcSenderHandler implements ComponentHandler {
    
    private final ConcurrentHashMap<String, Long> processedQueries = new ConcurrentHashMap<>();
    private static final long PROCESSED_QUERY_TTL = 5 * 60 * 1000;
    private final Object connectionLock = new Object();
    
    @Override
    public String getComponentType() {
        return "Sender"; // ✅ Tetap return "Sender" untuk compatibility
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
            String sourceTable = config.optString("sourceTable", "");
            String customQuery = config.optString("query", "");
            int pollingInterval = config.optInt("pollingInterval", 10);
            
            log.info("🗃️ JDBC Sender [{}] - DB: {}://{}:{}/{}, Table: {}", 
                    componentLabel, dbType, host, port, dbName, sourceTable);
            
            // Validasi konfigurasi
            if (host.isEmpty() || dbName.isEmpty() || username.isEmpty()) {
                log.error("❌ Invalid configuration for JDBC Sender: missing required database parameters");
                return false;
            }
            
            if (sourceTable.isEmpty() && customQuery.isEmpty()) {
                log.error("❌ Invalid configuration for JDBC Sender: either sourceTable or query must be provided");
                return false;
            }
            
            synchronized (connectionLock) {
                String connectionUrl = buildConnectionUrl(dbType, host, port, dbName);
                String queryKey = customQuery.isEmpty() ? sourceTable : customQuery.hashCode() + "";
                
                // Cek apakah query sudah diproses baru-baru ini
                if (isAlreadyProcessed(queryKey)) {
                    log.info("⏭️ Skipping already processed query/table: {}", 
                            customQuery.isEmpty() ? sourceTable : "custom query");
                    return false;
                }
                
                try (Connection connection = DriverManager.getConnection(connectionUrl, username, password)) {
                    log.info("🔗 Connected to database: {}", connectionUrl);
                    
                    String finalQuery = buildFinalQuery(customQuery, sourceTable, schema);
                    log.info("📊 Executing query: {}", finalQuery);
                    
                    List<Map<String, Object>> data = executeQuery(connection, finalQuery);
                    
                    if (data.isEmpty()) {
                        log.info("📭 No data found for query: {}", finalQuery);
                        cleanupProcessedQueries();
                        return false;
                    }
                    
                    log.info("📄 Retrieved {} records from database", data.size());
                    
                    // Pass data ke context
                    context.put("jdbcData", data);
                    context.put("recordCount", data.size());
                    context.put("sourceQuery", finalQuery);
                    context.put("sourceTable", sourceTable);
                    context.put("dbType", dbType);
                    context.put("schema", schema);
                    
                    // Tandai query sebagai processed
                    markAsProcessed(queryKey);
                    
                    log.info("✅ JDBC Sender execution successful - {} records retrieved", data.size());
                    return true;
                    
                } catch (Exception e) {
                    log.error("❌ Database connection/query failed: {}", e.getMessage(), e);
                    return false;
                }
            }
            
        } catch (Exception e) {
            log.error("❌ JDBC Sender execution failed [{}]: {}", componentLabel, e.getMessage(), e);
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
    
    private String buildFinalQuery(String customQuery, String sourceTable, String schema) {
        if (!customQuery.isEmpty()) {
            return customQuery;
        }
        
        if (!schema.isEmpty()) {
            return String.format("SELECT * FROM %s.%s", schema, sourceTable);
        }
        
        return String.format("SELECT * FROM %s", sourceTable);
    }
    
    private List<Map<String, Object>> executeQuery(Connection connection, String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
        
        return results;
    }
    
    private boolean isAlreadyProcessed(String queryKey) {
        Long processedTime = processedQueries.get(queryKey);
        
        if (processedTime != null) {
            if (System.currentTimeMillis() - processedTime < PROCESSED_QUERY_TTL) {
                log.debug("⏭️ Skipping already processed query: {}", queryKey);
                return true;
            } else {
                processedQueries.remove(queryKey);
            }
        }
        return false;
    }
    
    private void markAsProcessed(String queryKey) {
        processedQueries.put(queryKey, System.currentTimeMillis());
        log.debug("🔖 Marked query as processed: {}", queryKey);
    }
    
    private void cleanupProcessedQueries() {
        long currentTime = System.currentTimeMillis();
        processedQueries.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > PROCESSED_QUERY_TTL
        );
    }
    
    public void clearProcessedQueries() {
        processedQueries.clear();
        log.info("🧹 Cleared processed queries cache");
    }
}