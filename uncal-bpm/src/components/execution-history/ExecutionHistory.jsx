import React, { useState, useEffect, useCallback } from 'react';
import './ExecutionHistory.css';

function ExecutionHistory({ scenarioFileId, token, isOpen, onClose }) {
  const [logs, setLogs] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchExecutionHistory = useCallback(async () => {
    try {
      setLoading(true);
      const response = await fetch(
        `http://localhost:8080/api/execution-logs/scenario/${scenarioFileId}?limit=50`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (response.ok) {
        const data = await response.json();
        console.log('✅ Execution logs received:', data);
        setLogs(data);
        setError(null);
      } else {
        const errorText = await response.text();
        console.error('❌ API Error:', response.status, errorText);
        setError(`Failed to fetch execution history: ${response.status} ${response.statusText}`);
      }
    } catch (err) {
      console.error('❌ Error fetching execution history:', err);
      setError(`Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  }, [scenarioFileId, token]);

  const fetchStats = useCallback(async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/execution-logs/scenario/${scenarioFileId}/stats`,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        }
      );

      if (response.ok) {
        const data = await response.json();
        console.log('✅ Stats received:', data);
        setStats(data);
      } else {
        console.error('❌ Stats API Error:', response.status);
      }
    } catch (err) {
      console.error('❌ Error fetching stats:', err);
    }
  }, [scenarioFileId, token]);

  useEffect(() => {
    if (isOpen && scenarioFileId) {
      fetchExecutionHistory();
      fetchStats();
    }
  }, [isOpen, scenarioFileId, fetchExecutionHistory, fetchStats]);

  const formatDateTime = (dateString) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('id-ID', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const formatDuration = (durationMs) => {
    if (!durationMs) return '-';
    if (durationMs < 1000) return `${durationMs}ms`;
    const seconds = Math.floor(durationMs / 1000);
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}m ${remainingSeconds}s`;
  };

  const getStatusBadge = (status) => {
    const statusClasses = {
      'SUCCESS': 'status-badge success',
      'FAILED': 'status-badge failed',
      'RUNNING': 'status-badge running'
    };
    return (
      <span className={statusClasses[status] || 'status-badge'}>
        {status === 'SUCCESS' ? '✅ Success' : 
         status === 'FAILED' ? '❌ Failed' : 
         '🔄 Running'}
      </span>
    );
  };

  const truncateFileName = (filePath, maxLength = 40) => {
    if (!filePath) return '-';
    if (filePath.length <= maxLength) return filePath;
    
    const fileName = filePath.split(/[/\\]/).pop();
    const directory = filePath.substring(0, filePath.length - fileName.length);
    
    if (fileName.length > maxLength) {
      return '...' + fileName.substring(fileName.length - maxLength + 3);
    }
    
    const truncatedDir = directory.substring(0, maxLength - fileName.length - 3);
    return truncatedDir + '...' + fileName;
  };

  if (!isOpen) return null;

  return (
    <div className="execution-history-overlay">
      <div className="execution-history-modal">
        <div className="execution-history-header">
          <h3>📊 Execution History</h3>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        {/* Statistics Section */}
        {stats && (
          <div className="execution-stats">
            <div className="stat-card">
              <span className="stat-label">Total Executions</span>
              <span className="stat-value">{stats.totalExecutions}</span>
            </div>
            <div className="stat-card success">
              <span className="stat-label">Success</span>
              <span className="stat-value">{stats.successCount}</span>
            </div>
            <div className="stat-card failed">
              <span className="stat-label">Failed</span>
              <span className="stat-value">{stats.failedCount}</span>
            </div>
            <div className="stat-card rate">
              <span className="stat-label">Success Rate</span>
              <span className="stat-value">{stats.successRate}%</span>
            </div>
          </div>
        )}

        {/* History Table */}
        <div className="execution-history-body">
          {loading ? (
            <div className="loading-message">Loading execution history...</div>
          ) : error ? (
            <div className="error-message">Error: {error}</div>
          ) : logs.length === 0 ? (
            <div className="empty-message">
              <p>📭 No execution history available</p>
              <p className="empty-subtitle">Run the scenario to see execution logs</p>
            </div>
          ) : (
            <div className="table-container">
              <table className="execution-table">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Start Time</th>
                    <th>Duration</th>
                    <th>Source File</th>
                    <th>Destination</th>
                    <th>Components</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((log) => (
                    <tr key={log.id} className={`log-row ${log.status.toLowerCase()}`}>
                      <td>{getStatusBadge(log.status)}</td>
                      <td className="datetime-cell">
                        {formatDateTime(log.startTime)}
                      </td>
                      <td className="duration-cell">
                        {formatDuration(log.durationMs)}
                      </td>
                      <td className="file-cell" title={log.sourceFile}>
                        {truncateFileName(log.sourceFile)}
                      </td>
                      <td className="file-cell" title={log.destinationFile}>
                        {truncateFileName(log.destinationFile)}
                      </td>
                      <td className="components-cell">
                        {log.componentsExecuted || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="execution-history-footer">
          <button className="refresh-btn" onClick={fetchExecutionHistory}>
            🔄 Refresh
          </button>
          <button className="close-footer-btn" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}

export default ExecutionHistory;
