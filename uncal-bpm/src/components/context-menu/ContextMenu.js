// src/offcanvas/offcanvas-components/ContextMenu.js
import React, { useEffect, useRef, useState } from "react";
import { toast } from "react-toastify"; // ✅ IMPORT TOAST
import "./ContextMenu.css";

function ContextMenu({ contextMenu, setContextMenu, setModal, onDeleteComponent }) {
  const ref = useRef(null);
  const [pos, setPos] = useState({ top: 0, left: 0 });

  // DEBUG: Log contextMenu data untuk troubleshoot
  useEffect(() => {
    if (contextMenu) {
      console.log('🔍 ContextMenu Data:', {
        type: contextMenu.type,
        file: contextMenu.file,
        fileId: contextMenu.file?.id,
        fileFullObject: contextMenu.file
      });
      
      // Jika file ada tapi id undefined, cari id di properties lain
      if (contextMenu.file && contextMenu.file.id === undefined) {
        console.log('⚠️ File ID is undefined. Checking for alternative IDs...');
        console.log('📋 File object keys:', Object.keys(contextMenu.file));
        
        // Cek kemungkinan property names untuk ID
        const possibleIdFields = ['id', '_id', 'fileId', 'fileID', 'ID'];
        possibleIdFields.forEach(field => {
          if (contextMenu.file[field] !== undefined) {
            console.log(`✅ Found ID in field "${field}":`, contextMenu.file[field]);
          }
        });
      }
    }
  }, [contextMenu]);

  // update posisi menu
  useEffect(() => {
    if (!contextMenu) return;
    const { mouseX, mouseY } = contextMenu;

    const fixPosition = () => {
      const menu = ref.current;
      if (!menu) return;
      const rect = menu.getBoundingClientRect();
      let top = mouseY;
      let left = mouseX;
      const pad = 8;

      if (left + rect.width > window.innerWidth) {
        left = Math.max(pad, window.innerWidth - rect.width - pad);
      }
      if (top + rect.height > window.innerHeight) {
        top = Math.max(pad, window.innerHeight - rect.height - pad);
      }
      setPos({ top, left });
    };

    const id = requestAnimationFrame(fixPosition);
    return () => cancelAnimationFrame(id);
  }, [contextMenu]);

  // tutup saat klik di luar
  useEffect(() => {
    const onDown = (e) => {
      if (ref.current && !ref.current.contains(e.target)) {
        setContextMenu(null);
      }
    };
    window.addEventListener("mousedown", onDown);
    return () => window.removeEventListener("mousedown", onDown);
  }, [setContextMenu]);

  // Helper function untuk mendapatkan file ID - DIPERBAIKI
  const getFileId = (file) => {
    if (!file) {
      console.error('❌ File object is null or undefined');
      return null;
    }

    // Coba berbagai kemungkinan property names untuk ID
    const possibleIdFields = ['id', '_id', 'fileId', 'fileID', 'ID'];
    
    for (let field of possibleIdFields) {
      if (file[field] !== undefined && file[field] !== null) {
        console.log(`✅ Using ID from field "${field}":`, file[field]);
        return file[field];
      }
    }

    console.error('❌ No valid ID found in file object:', file);
    console.log('📋 Available file properties:', Object.keys(file));
    return null;
  };

  // ✅ PERBAIKAN: Fungsi untuk run scenario dengan TOAST saja (tanpa loading toast)
  const handleRunScenario = async (folder, sub, file) => {
    try {
      const token = localStorage.getItem('authToken');
      const fileId = getFileId(file);
      
      if (!fileId) {
        console.error('❌ Cannot run scenario: File ID is null');
        // ✅ GUNAKAN TOAST untuk error
        toast.error('❌ Cannot run scenario: File ID is missing');
        return;
      }

      console.log('🚀 Running scenario:', fileId, file?.name);
      
      const response = await fetch(`http://localhost:8080/api/scenario/${fileId}/run`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const result = await response.json();
      console.log('Run API Response:', result);
      
      if (response.ok) {
        console.log('✅ Scenario started successfully');
        
        // UPDATE LOCAL STATE - ini yang penting!
        if (file) {
          file.runStatus = 1;
          console.log('🔄 Updated file.runStatus to:', file.runStatus);
        }
        
        // Trigger refresh data
        triggerDataRefresh();
        
        // ✅ GUNAKAN TOAST SUCCESS - Hanya toast, tidak pakai modal dan tanpa loading
        toast.success(`Scenario "${file?.name}" berhasil dijalankan!`);
        
      } else {
        console.error('Gagal menjalankan scenario:', result.error);
        // ✅ GUNAKAN TOAST ERROR
        toast.error(`❌ Gagal menjalankan scenario: ${result.error || "Unknown error"}`);
      }
    } catch (error) {
      console.error('Error menjalankan scenario:', error);
      // ✅ GUNAKAN TOAST ERROR
      toast.error(`Error menjalankan scenario: ${error.message}`);
    }
  };

  // ✅ PERBAIKAN: Fungsi untuk stop scenario dengan TOAST saja (tanpa loading toast)
  const handleStopScenario = async (folder, sub, file) => {
    try {
      const token = localStorage.getItem('authToken');
      const fileId = getFileId(file);
      
      if (!fileId) {
        console.error('❌ Cannot stop scenario: File ID is null');
        // ✅ GUNAKAN TOAST untuk error
        toast.error('❌ Cannot stop scenario: File ID is missing');
        return;
      }

      console.log('🛑 Stopping scenario:', fileId, file?.name);
      
      const response = await fetch(`http://localhost:8080/api/scenario/${fileId}/stop`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      const result = await response.json();
      console.log('Stop API Response:', result);
      
      if (response.ok) {
        console.log('✅ Scenario stopped successfully');
        
        // UPDATE LOCAL STATE - ini yang penting!
        if (file) {
          file.runStatus = 0;
          console.log('🔄 Updated file.runStatus to:', file.runStatus);
        }
        
        // Trigger refresh data
        triggerDataRefresh();
        
        // ✅ GUNAKAN TOAST SUCCESS - Hanya toast, tidak pakai modal dan tanpa loading
        toast.success(`Scenario "${file?.name}" berhasil dihentikan!`);
        
      } else {
        console.error('Gagal menghentikan scenario:', result.error);
        // ✅ GUNAKAN TOAST ERROR
        toast.error(`Gagal menghentikan scenario: ${result.error || "Unknown error"}`);
      }
    } catch (error) {
      console.error('Error menghentikan scenario:', error);
      // ✅ GUNAKAN TOAST ERROR
      toast.error(`Error menghentikan scenario: ${error.message}`);
    }
  };

  // Fungsi untuk trigger refresh data - DIPERBAIKI
  const triggerDataRefresh = () => {
    console.log('🔄 Triggering data refresh...');
    
    // Method 1: Dispatch custom event dengan detail
    const refreshEvent = new CustomEvent('refreshProjectData', { 
      detail: { 
        timestamp: Date.now(),
        source: 'contextMenu'
      } 
    });
    window.dispatchEvent(refreshEvent);
    
    // Method 2: Call global function if exists
    if (typeof window.refreshProjectData === 'function') {
      console.log('📞 Calling global refresh function');
      window.refreshProjectData();
    }
    
    // Method 3: Force context menu close dan refresh
    setTimeout(() => {
      setContextMenu(null); // Close context menu untuk force re-render
    }, 300);
  };

  // Fungsi untuk mengecek status scenario - DIPERBAIKI DENGAN DEBUG
  const getScenarioStatus = (file) => {
    // Cek jika file tidak ada atau undefined
    if (!file) {
      console.warn('⚠️ File is undefined in getScenarioStatus');
      return 0;
    }
    
    // Debug info
    console.log('🔍 Checking scenario status:', {
      fileName: file.name,
      fileId: getFileId(file),
      directRunStatus: file.runStatus,
      hasCanvasData: !!file.canvasData
    });
    
    // Priority 1: Gunakan runStatus langsung dari file (jika ada dari FileResponse)
    if (file.runStatus !== undefined && file.runStatus !== null) {
      console.log('✅ Using file.runStatus:', file.runStatus);
      return file.runStatus;
    }
    
    // Priority 2: Parse dari canvasData
    try {
      if (file.canvasData) {
        const canvasData = typeof file.canvasData === 'string' 
          ? JSON.parse(file.canvasData) 
          : file.canvasData;
        const status = canvasData.runStatus || 0;
        console.log('📄 Using canvasData.runStatus:', status);
        return status;
      }
      console.log('⚡ No canvasData, returning default 0');
      return 0;
    } catch (error) {
      console.warn('❌ Error parsing canvasData:', error);
      return 0;
    }
  };

  if (!contextMenu) return null;

  const { type, folder, sub, file, component } = contextMenu;

  const items = {
    project: [
      { label: "Rename Project", action: () => setModal({ show: true, type: "renameProject", folder }) },
      { label: "Delete Project", action: () => setModal({ show: true, type: "deleteProject", folder }) },
    ],
    scenarioFolder: [
      { label: "Create Scenario", action: () => setModal({ show: true, type: "createScenario", folder, sub }) },
    ],
    scenarioFile: [
      { label: "Rename Scenario", action: () => setModal({ show: true, type: "renameScenario", folder, sub, file }) },
      { label: "Delete Scenario", action: () => setModal({ show: true, type: "deleteScenario", folder, sub, file }) },
      { label: "Export", action: () => setModal({ show: true, type: "exportScenario", folder, sub, file }) },
      // Conditional menu untuk Run/Stop berdasarkan status - DIPERBAIKI
      ...(file ? (getScenarioStatus(file) === 0 
        ? [{ 
            label: "▶ Run Scenario", 
            action: () => handleRunScenario(folder, sub, file),
            className: "run-scenario" 
          }]
        : [{ 
            label: "⏹ Stop Scenario", 
            action: () => handleStopScenario(folder, sub, file),
            className: "stop-scenario" 
          }]
      ) : []),
    ],
    mappingFolder: [
      {
        label: "Import Mapping",
        action: async () => {
          try {
            const token = localStorage.getItem('authToken');
            const res = await fetch("http://localhost:8080/api/mappings", {
              headers: {
                'Authorization': `Bearer ${token}`
              }
            });
            const data = await res.json();
            setModal({
              show: true,
              type: "importMapping",
              folder,
              sub,
              availableMappings: data,
            });
          } catch (err) {
            console.error("Gagal fetch mapping:", err);
            setModal({
              show: true,
              type: "importMapping",
              folder,
              sub,
              availableMappings: [],
            });
          }
        },
      },
      { label: "Paste Mapping", action: () => setModal({ show: true, type: "pasteMapping", folder, sub }) },
    ],
    mappingFile: [
      { label: "Copy Mapping", action: () => setModal({ show: true, type: "copyMapping", folder, sub, file }) },
      { label: "Delete Mapping", action: () => setModal({ show: true, type: "deleteMapping", folder, sub, file }) },
    ],
    librariesFolder: [
      { label: "Import Java Library", action: () => setModal({ show: true, type: "chooseJarFile", folder, sub }) },
    ],
    javaFolder: [
      { label: "Create Java File", action: () => setModal({ show: true, type: "createJavaFileName", folder, sub }) },
      { label: "Import Java File", action: () => setModal({ show: true, type: "chooseJavaFile", folder, sub }) },
    ],
    canvasComponent: [
      { label: "❌ Delete Component", action: () => onDeleteComponent && onDeleteComponent(component) },
    ],
  };

  // DIPERBAIKI: Cek jika items[type] ada sebelum di-map
  const menuItems = items[type] || [];

  return (
    <div
      ref={ref}
      className="context-menu"
      style={{ top: pos.top, left: pos.left, position: "absolute" }}
    >
      {menuItems.map((item, i) => (
        <div
          key={i}
          className={`context-item ${item.className || ''}`}
          onClick={() => {
            item.action();
            setContextMenu(null);
          }}
        >
          {item.label}
        </div>
      ))}
    </div>
  );
}

export default ContextMenu;