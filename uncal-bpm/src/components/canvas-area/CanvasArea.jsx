// src/components/canvas-area/CanvasArea.js
import React, { useState, useEffect, useRef, useCallback } from "react";
import { useDrop } from "react-dnd";
import "./CanvasArea.css";
import ButtonCustom from "../ui/button/ButtonCustom";
import CloseButton from "../ui/close-button/CloseButton";
import ExecutionHistory from "../execution-history/ExecutionHistory";

function CanvasArea({
  openFiles = [],
  activeFile,
  onCloseFile,
  onDropComponent,
  canvasComponents = [],
  setActiveFile,
  setContextMenu,
  onSelectComponent,
  onSaveToJSON,
  scenarioFileId,
  setCanvasData,
  canvasData,
}) {
  const [{ isOver }, drop] = useDrop(
    () => ({
      accept: "COMPONENT",
      drop: (item) => {
        if (onDropComponent) {
          console.log('🟢 Component dropped from palette:', item);
          
          // PERBAIKAN: Pastikan struktur config lengkap dengan form data
          const newComponent = {
            ...item,
            id: Date.now().toString(),
            config: {
              // Simpan SEMUA data form dari palette
              data: item.form?.data || {}, // Data values
              tabs: item.form?.tabs || [], // Form structure
              title: item.form?.title || `${item.type} Configuration`,
              buttons: item.form?.buttons || [
                { label: "Save", action: "save" },
                { label: "Close", action: "close" }
              ],
              // Simpan juga properti asli
              ...item.form
            },
            // Simpan juga di root untuk kompatibilitas
            form: item.form || {}
          };
          
          console.log('🟢 New component with form structure:', newComponent);
          onDropComponent(newComponent);
        }
      },
      collect: (monitor) => ({ isOver: !!monitor.isOver() }),
    }),
    [onDropComponent]
  );

  const [isSaving, setIsSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [showWelcome, setShowWelcome] = useState(false);
  
  // Helper function untuk mendapatkan icon berdasarkan type
  const getIconByType = useCallback((type) => {
    const iconMap = {
      'sender-as2': '📤',
      'receiver-as2': '📥',
      'receiver-jdbc': '🗄️',
      'object-mapping': '🔄',
      'object-switching': '⚡',
      'Sender': '📤',
      'Receiver': '📥',
      'default': '📦'
    };
    return iconMap[type] || iconMap.default;
  }, []);

  // Load flow yang tersimpan dari backend - DIPERBAIKI untuk form configuration
  const loadSavedFlow = useCallback(async () => {
    if (!scenarioFileId || !activeFile) return;
    
    setIsLoading(true);
    try {
      const token = localStorage.getItem('authToken');
      const response = await fetch(`http://localhost:8080/api/projects/files/${scenarioFileId}/canvas`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (response.ok) {
        const fileData = await response.json();
        
        if (fileData.canvasData) {
          let parsedData;
          try {
            parsedData = typeof fileData.canvasData === 'string' 
              ? JSON.parse(fileData.canvasData) 
              : fileData.canvasData;
          } catch (parseError) {
            console.error('Error parsing canvasData:', parseError);
            return;
          }
          
          let savedComponents = [];
          
          console.log('📂 Loaded canvas data structure:', {
            hasFlow: !!parsedData.flow,
            hasComponents: !!parsedData.components,
            flowCount: parsedData.flow?.length,
            componentsCount: parsedData.components?.length
          });

          // Prioritaskan components array (format baru)
          if (parsedData.components && Array.isArray(parsedData.components)) {
            savedComponents = parsedData.components.map(comp => {
              console.log('📥 Loading component with form data:', {
                id: comp.id,
                type: comp.type,
                hasConfig: !!comp.config,
                configData: comp.config?.data,
                configTabs: comp.config?.tabs?.length,
                hasForm: !!comp.form,
                formData: comp.form?.data
              });

              // Gabungkan config dan form data untuk konsistensi
              const configData = comp.config?.data || comp.form?.data || {};
              const formTabs = comp.config?.tabs || comp.form?.tabs || [];
              
              return {
                id: comp.id,
                label: comp.label || comp.config?.data?.name || 'Unnamed',
                type: comp.type,
                config: {
                  data: configData, // Pastikan data values ada
                  tabs: formTabs,   // Pastikan form structure ada
                  title: comp.config?.title || comp.form?.title,
                  buttons: comp.config?.buttons || comp.form?.buttons,
                  ...comp.config
                },
                form: {
                  data: configData, // Juga simpan di form.data
                  tabs: formTabs,
                  title: comp.form?.title || comp.config?.title,
                  buttons: comp.form?.buttons || comp.config?.buttons,
                  ...comp.form
                },
                notes: comp.notes || "",
                icon: comp.icon || getIconByType(comp.type),
                style: comp.style || null
              };
            });
          } 
          // Fallback ke format flow array
          else if (parsedData.flow && Array.isArray(parsedData.flow)) {
            savedComponents = parsedData.flow.map(comp => {
              const configData = comp.config?.data || comp.form?.data || {};
              const formTabs = comp.config?.tabs || comp.form?.tabs || [];
              
              return {
                id: comp.id,
                label: comp.label || configData?.name || 'Unnamed',
                type: comp.type,
                config: {
                  data: configData,
                  tabs: formTabs,
                  title: comp.config?.title || comp.form?.title,
                  buttons: comp.config?.buttons || comp.form?.buttons,
                  ...comp.config
                },
                form: {
                  data: configData,
                  tabs: formTabs,
                  title: comp.form?.title || comp.config?.title,
                  buttons: comp.form?.buttons || comp.config?.buttons,
                  ...comp.form
                },
                notes: comp.notes || "",
                icon: getIconByType(comp.type),
                style: comp.style || null
              };
            });
          }
          
          console.log('✅ Final saved components with form data:', savedComponents);

          // Update canvasData di parent component dengan data lengkap
          if (setCanvasData && savedComponents.length > 0) {
            setCanvasData(prev => ({
              ...prev,
              [activeFile]: savedComponents
            }));
            console.log('✅ Successfully updated canvas data with form configurations');
          } else if (setCanvasData) {
            // Reset jika tidak ada komponen
            setCanvasData(prev => ({
              ...prev,
              [activeFile]: []
            }));
          }
        } else {
          console.log('ℹ️ No canvas data found in response');
          // Reset jika tidak ada data
          if (setCanvasData) {
            setCanvasData(prev => ({
              ...prev,
              [activeFile]: []
            }));
          }
        }
      } else {
        console.log('❌ Failed to load canvas data, status:', response.status);
        // Reset jika gagal load
        if (setCanvasData) {
          setCanvasData(prev => ({
            ...prev,
            [activeFile]: []
          }));
        }
      }
    } catch (error) {
      console.error('❌ Error loading saved flow:', error);
      // Reset jika error
      if (setCanvasData) {
        setCanvasData(prev => ({
          ...prev,
          [activeFile]: []
        }));
      }
    } finally {
      setIsLoading(false);
    }
  }, [scenarioFileId, activeFile, setCanvasData, getIconByType]);

  // Load saved flow ketika file aktif atau scenarioFileId berubah
  useEffect(() => {
    if (activeFile && scenarioFileId) {
      console.log('🔄 Loading flow for:', activeFile, 'with scenarioId:', scenarioFileId);
      loadSavedFlow();
    }
  }, [activeFile, scenarioFileId, loadSavedFlow]);

  // Format components ke struktur JSON engine - DIPERBAIKI
  const formatComponentsForEngine = useCallback(() => {
    // Buat struktur data lengkap sesuai format yang diinginkan
    const canvasData = {
      flow: canvasComponents.map(comp => ({
        id: comp.id,
        type: comp.type,
        label: comp.label,
        notes: comp.notes || "",
        style: comp.style || null,
        config: comp.config || {}  // Simpan SEMUA konfigurasi termasuk form
      })),
      version: 1,
      components: canvasComponents.map(comp => {
        console.log('💾 Formatting component for save:', {
          id: comp.id,
          label: comp.label,
          configData: comp.config?.data,
          configTabs: comp.config?.tabs?.length,
          formData: comp.form?.data
        });

        return {
          id: comp.id,
          icon: comp.icon || getIconByType(comp.type),
          type: comp.type,
          label: comp.label,
          notes: comp.notes || "",
          config: comp.config || {}, // Simpan SEMUA konfigurasi form termasuk data values
          form: comp.form || {}, // Juga simpan di form untuk konsistensi
          style: comp.style || null
        };
      }),
      lastModified: new Date().toString()
    };

    console.log('💾 Formatted canvas data for saving:', canvasData);
    return canvasData;
  }, [canvasComponents, getIconByType]);

  // Format meta data untuk kolom meta_data
  const formatMetaData = useCallback(() => {
    const userData = JSON.parse(localStorage.getItem('userData') || '{}');
    
    return {
      project: "current_project",
      scenario_type: "bpmn_flow",
      version: "1.0",
      description: `Scenario flow for ${activeFile}`,
      engine_version: "1.0",
      created_by: userData.username || "unknown",
      components_count: canvasComponents.length,
      configured_components: canvasComponents.filter(comp => 
        comp.config?.data && Object.keys(comp.config.data).length > 0
      ).length,
      last_modified: new Date().toISOString(),
      subfolder_type: "scenarios"
    };
  }, [activeFile, canvasComponents]);

  // Save ke backend dengan canvas_data dan meta_data - MANUAL SAVE ONLY
  const handleSaveToBackend = useCallback(async () => {
    if (!scenarioFileId || !activeFile) {
      console.error('❌ Cannot save: Missing scenarioFileId or activeFile');
      return;
    }

    setIsSaving(true);

    try {
      const canvasData = formatComponentsForEngine();
      const metaData = formatMetaData();
      
      console.log('💾 Saving complete canvas data with form values:', canvasData);
      
      const token = localStorage.getItem('authToken');
      const response = await fetch(`http://localhost:8080/api/projects/files/${scenarioFileId}`, {
        method: 'PUT',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          canvasData: JSON.stringify(canvasData),
          metadata: JSON.stringify(metaData)
        })
      });

      if (response.ok) {
        
        if (onSaveToJSON) {
          onSaveToJSON(canvasData);
        }
        
        // Reload data setelah save untuk memastikan konsistensi
        setTimeout(() => {
          if (activeFile && scenarioFileId) {
            console.log('🔄 Reloading data after save...');
            loadSavedFlow();
          }
        }, 1000);
        
      } else {
        const errorText = await response.text();
        console.error('❌ Save failed with status:', response.status, errorText);
      }
    } catch (error) {
      console.error('❌ Error saving canvas:', error);
    } finally {
      setIsSaving(false);
    }
  }, [scenarioFileId, activeFile, formatComponentsForEngine, formatMetaData, onSaveToJSON, loadSavedFlow]);

  // Manual save function - HANYA INI YANG DIPAKAI
  const handleManualSave = () => {
    handleSaveToBackend();
  };

  // Reset state ketika file aktif berubah
  useEffect(() => {
    setIsSaving(false);
  }, [activeFile]);

  const renderIcon = (icon) => {
    if (!icon) return "📦";
    return typeof icon === "string" && icon.startsWith("data:image") ? (
      <img src={icon} alt="icon" className="canvas-node-icon" />
    ) : (
      <span>{icon}</span>
    );
  };

  // Handler untuk double-click pada komponen (buka konfigurasi)
  const handleComponentDoubleClick = (comp) => {
    console.log('🖱️ Double-clicked component:', comp);
    if (onSelectComponent) {
      onSelectComponent(comp);
    }
  };

  // Handler untuk context menu pada komponen
  const handleComponentContextMenu = (e, comp) => {
    e.preventDefault();
    if (setContextMenu) {
      setContextMenu({
        mouseX: e.clientX,
        mouseY: e.clientY,
        type: "canvasComponent",
        component: comp,
      });
    }
  };

  // Debug: log current components
  useEffect(() => {
    console.log('🔍 CURRENT CANVAS COMPONENTS:', canvasComponents.map(comp => ({
      id: comp.id,
      label: comp.label,
      type: comp.type,
      hasConfig: !!comp.config,
      configData: comp.config?.data,
      configTabs: comp.config?.tabs?.length,
      hasForm: !!comp.form,
      formData: comp.form?.data
    })));
  }, [canvasComponents]);

  return (
    <div ref={drop} className="canvas-area">
      <div className="canvas-header">
        <div className="open-files-tabs">
          {openFiles.length > 0 ? (
            openFiles.map((f, idx) => (
              <span
                key={idx}
                className={`open-file-tab ${activeFile === f ? "active" : ""}`}
                onClick={() => setActiveFile(f)}
              >
                {f}
                <button
                  type="button"
                  className="close-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    onCloseFile(f);
                  }}
                >
                  ×
                </button>
              </span>
            ))
          ) : (
            <span className="no-file">No file opened</span>
          )}
        </div>
        
        <div className="canvas-actions">
          {isLoading && <span className="save-message">Loading flow...</span>}
          
          {!activeFile && (
            <ButtonCustom
              onClick={() => setShowWelcome(true)}
              label="ℹ️ Info"
              variant="secondary"
              title="Panduan dan informasi sistem"
            />
          )}
          
          {activeFile && (
            <>
              <ButtonCustom
                onClick={() => setShowHistory(true)}
                disabled={!scenarioFileId}
                label="📊 View History"
                variant="secondary"
                title={!scenarioFileId ? "No file selected" : "View execution history"}
              />
              <ButtonCustom
                onClick={handleManualSave}
                disabled={isSaving || isLoading || !scenarioFileId}
                label={!scenarioFileId ? 'No File ID' : 'Save Flow'}
                variant="primary"
                title={!scenarioFileId ? "Cannot save - file ID not found" : "Save flow to backend"}
              />
            </>
          )}
        </div>
      </div>

      <div
        className={`canvas-dropzone ${isOver ? "hovered" : ""} ${
          openFiles.length === 0 ? "empty" : ""
        }`}
      >
        {activeFile ? (
          isLoading ? (
            <p className="canvas-placeholder">Loading saved flow...</p>
          ) : canvasComponents.length === 0 ? (
            <p className="canvas-placeholder">Drop your components here...</p>
          ) : (
            <div className="flow-container">
              {canvasComponents.map((comp, i) => (
                <React.Fragment key={comp.id || i}>
                  <div
                    className="flow-node"
                    onDoubleClick={() => handleComponentDoubleClick(comp)}
                    onContextMenu={(e) => handleComponentContextMenu(e, comp)}
                    tabIndex={0}
                  >
                    <div className="node-icon">{renderIcon(comp.icon)}</div>
                    <div className="node-label">{comp.label}</div>
                    {/* Tampilkan indicator jika komponen sudah dikonfigurasi */}
                    {comp.config && comp.config.data && Object.keys(comp.config.data).length > 0 && (
                      <div className="node-config-indicator" title="Configured">⚙️</div>
                    )}
                  </div>
                  {i < canvasComponents.length - 1 && <div className="flow-arrow">➝</div>}
                </React.Fragment>
              ))}
            </div>
          )
        ) : null}
      </div>

      {/* Welcome/Info Modal */}
      {showWelcome && (
        <div className="welcome-modal-overlay" onClick={() => setShowWelcome(false)}>
          <div className="welcome-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="welcome-modal-close-wrapper">
              <CloseButton onClick={() => setShowWelcome(false)} />
            </div>
            <div className="welcome-panel">
              <div className="welcome-content">
                <div className="welcome-icon">📋</div>
                <h2>Selamat Datang di UNCAL BPM</h2>
                <p className="welcome-subtitle">Sistem Business Process Management untuk integrasi data</p>
                
                <div className="guide-section">
                  <h3>🚀 Panduan Memulai</h3>
                  <div className="guide-steps">
                    <div className="guide-step">
                      <span className="step-number">1</span>
                      <div className="step-content">
                        <h4>Buka Scenario</h4>
                        <p>Pilih file scenario dari daftar di sebelah kiri untuk memulai</p>
                      </div>
                    </div>
                    <div className="guide-step">
                      <span className="step-number">2</span>
                      <div className="step-content">
                        <h4>Tambah Komponen</h4>
                        <p>Drag & drop komponen Sender dan Receiver dari palette</p>
                      </div>
                    </div>
                    <div className="guide-step">
                      <span className="step-number">3</span>
                      <div className="step-content">
                        <h4>Konfigurasi</h4>
                        <p>Double-click komponen untuk mengatur konfigurasi</p>
                      </div>
                    </div>
                    <div className="guide-step">
                      <span className="step-number">4</span>
                      <div className="step-content">
                        <h4>Simpan & Jalankan</h4>
                        <p>Klik "Save Flow" untuk menyimpan, lalu jalankan scenario</p>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="features-section">
                  <h3>✨ Fitur Utama</h3>
                  <div className="features-grid">
                    <div className="feature-card">
                      <div className="feature-icon">📁</div>
                      <h4>File Transfer</h4>
                      <p>Transfer file antar folder dengan mudah</p>
                    </div>
                    <div className="feature-card">
                      <div className="feature-icon">🗄️</div>
                      <h4>Database Integration</h4>
                      <p>Sinkronisasi data antar database</p>
                    </div>
                    <div className="feature-card">
                      <div className="feature-icon">📊</div>
                      <h4>Execution History</h4>
                      <p>Monitor riwayat eksekusi scenario</p>
                    </div>
                  </div>
                </div>

                <div className="quick-tips">
                  <h3>💡 Tips</h3>
                  <ul>
                    <li>Gunakan context menu (klik kanan) untuk opsi tambahan</li>
                    <li>Periksa execution history untuk melihat detail proses transfer</li>
                    <li>Simpan perubahan secara berkala untuk menghindari kehilangan data</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Execution History Modal */}
      {showHistory && (
        <ExecutionHistory
          scenarioFileId={scenarioFileId}
          token={localStorage.getItem('authToken')}
          isOpen={showHistory}
          onClose={() => setShowHistory(false)}
        />
      )}
    </div>
  );
}

export default CanvasArea;