// src/pages/Dashboard.js
import React, { useState, useEffect, useContext, useRef, useCallback } from "react";
import ReactDOM from "react-dom";
import { AuthContext } from "../../context/AuthContext";
import NavbarComponent from "../../components/navbar-component/NavbarComponent";
import CanvasArea from "../../components/canvas-area/CanvasArea";
import Palette from "../../components/palette/Palette";
import ContextMenu from "../../components/context-menu/ContextMenu";
import ModalAction from "../../components/manager/ModalAction";
import { DndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import DynamicForm from "../../components/component-form-render/DynamicForm";
import "../../App.css";

export default function Dashboard() {
  const { logout, token } = useContext(AuthContext);

  const [folders, setFolders] = useState([]);
  const [files, setFiles] = useState({});
  const [openFiles, setOpenFiles] = useState([]);
  const [activeFile, setActiveFile] = useState(null);
  const [canvasData, setCanvasData] = useState({});
  const [components, setComponents] = useState([]);
  const [contextMenu, setContextMenu] = useState(null);
  const [modal, setModal] = useState({ show: false });
  const [selectedComponent, setSelectedComponent] = useState(null);
  const [showForm, setShowForm] = useState(false);
  
  // Track komponen untuk mengidentifikasi adanya penambahan baru
  const prevComponentsRef = useRef({});
  const isInitialLoad = useRef(true);

  // Rehydrate localStorage
  useEffect(() => {
    const savedData = localStorage.getItem("uncal-bpm-data");
    if (savedData) {
      try {
        const parsed = JSON.parse(savedData);
        console.log("📥 Loading from localStorage:", {
          folders: parsed.folders?.length,
          files: Object.keys(parsed.files || {}).length,
          canvasData: Object.keys(parsed.canvasData || {}).length,
          openFiles: parsed.openFiles?.length,
          activeFile: parsed.activeFile
        });
        
        setFolders(parsed.folders || []);
        setFiles(parsed.files || {});
        setCanvasData(parsed.canvasData || {});
        setOpenFiles(parsed.openFiles || []);
        setActiveFile(parsed.activeFile || null);
        
        if (parsed.canvasData) {
          Object.keys(parsed.canvasData).forEach(fileKey => {
            if (parsed.canvasData[fileKey]) {
              prevComponentsRef.current[fileKey] = parsed.canvasData[fileKey].length;
            }
          });
        }
        
        isInitialLoad.current = false;
      } catch (err) {
        console.error("❌ Gagal parse localStorage:", err);
        setFolders([]);
        setFiles({});
        setCanvasData({});
        setOpenFiles([]);
        setActiveFile(null);
      }
    }
  }, []);

  // Autosave localStorage
  useEffect(() => {
    if (isInitialLoad.current) return;

    const dataToSave = { 
      folders, 
      files, 
      canvasData, 
      openFiles, 
      activeFile 
    };
    
    localStorage.setItem("uncal-bpm-data", JSON.stringify(dataToSave));
  }, [folders, files, canvasData, openFiles, activeFile]);

  // PERBAIKAN: Gunakan useCallback untuk menghindari pemanggilan berulang
  const getScenarioFileId = useCallback((activeFileKey) => {
    if (!activeFileKey) {
      console.log("❌ activeFileKey is null/undefined");
      return null;
    }
    
    // PERBAIKAN: Cek apakah data sudah siap
    if (folders.length === 0 || Object.keys(files).length === 0) {
      console.log("⏳ Data belum siap, folders atau files masih kosong");
      return null;
    }
    
    console.log("🔍 getScenarioFileId called with:", activeFileKey);

    // Handle case: activeFileKey adalah "17-scenarios-hai"
    if (activeFileKey.includes('-')) {
      const parts = activeFileKey.split('-');
      console.log("🔍 Parts from activeFileKey:", parts);
      
      if (parts.length >= 3) {
        const projectId = parts[0]; // "17"
        const fileName = parts.slice(2).join('-'); // "hai"
        
        console.log(`🔍 Extracted: projectId=${projectId}, fileName=${fileName}`);
        
        // Cari folder dengan ID yang sesuai
        const targetFolder = folders.find(folder => folder.id.toString() === projectId);
        if (targetFolder) {
          const filesKey = `${targetFolder.id}-scenarios`;
          const fileArray = files[filesKey] || [];
          
          console.log(`🔍 Searching in key: ${filesKey}`, fileArray);
          
          // Cari file dengan nama yang cocok
          const foundFile = fileArray.find(file => 
            file.name === fileName || 
            file.name === fileName + '.json' ||
            file.name === activeFileKey
          );
          
          if (foundFile) {
            console.log(`✅ Found file:`, foundFile);
            return foundFile.id;
          }
        }
      }
    }

    // Fallback: cari langsung di semua files
    console.log("🔍 Fallback: Searching in all files");
    for (const key of Object.keys(files)) {
      const fileArray = files[key];
      if (Array.isArray(fileArray)) {
        const foundFile = fileArray.find(file => 
          file.name === activeFileKey || 
          file.name === activeFileKey + '.json'
        );
        if (foundFile) {
          console.log(`✅ Found in key "${key}":`, foundFile);
          return foundFile.id;
        }
      }
    }

    console.log("❌ File ID not found");
    return null;
  }, [folders, files]); // PERBAIKAN: Tambahkan dependencies

  const handleFileSelect = (fileKey) => {
    if (!openFiles.includes(fileKey)) {
      setOpenFiles((prev) => [...prev, fileKey]);
      
      if (!prevComponentsRef.current) {
        prevComponentsRef.current = {};
      }
      
      if (canvasData[fileKey]) {
        prevComponentsRef.current[fileKey] = canvasData[fileKey].length;
      } else {
        prevComponentsRef.current[fileKey] = 0;
      }
    }
    setActiveFile(fileKey);
  };

  const handleCloseFile = (file) => {
    const filtered = openFiles.filter((f) => f !== file);
    setOpenFiles(filtered);
    if (activeFile === file) {
      setActiveFile(filtered.length > 0 ? filtered[filtered.length - 1] : null);
    }
  };

  const handleDropComponent = (component) => {
    if (!activeFile) return;

    setCanvasData((prev) => {
      const current = prev[activeFile] || [];

      if (component.type === "Sender" && current.some((c) => c.type === "Sender")) {
        toast.error('Komponen kategori "Sender" hanya boleh satu di skenario ini');
        return prev;
      }

      // PERBAIKAN: Tambahkan form kosong untuk komponen baru
      let newComp = { 
        ...component, 
        id: Date.now(), 
        style: component.style || {
          position: 'absolute',
          left: '50px',
          top: '50px'
        },
        form: component.form || {} // PERBAIKAN: Tambahkan form kosong
      };
      return { ...prev, [activeFile]: [...current, newComp] };
    });
  };

  const handleDeleteComponent = (comp) => {
    setCanvasData((prev) => {
      const updatedComponents = prev[activeFile].filter((c) => c.id !== comp.id);
      
      if (activeFile) {
        if (!prevComponentsRef.current) {
          prevComponentsRef.current = {};
        }
        prevComponentsRef.current[activeFile] = updatedComponents.length;
      }
      
      return {
        ...prev,
        [activeFile]: updatedComponents,
      };
    });
  };

  const handleSelectComponent = (comp) => {
    // PERBAIKAN: Pastikan komponen memiliki form dan style
    const componentWithForm = {
      ...comp,
      style: comp.style || {
        position: 'absolute',
        left: '50px',
        top: '50px'
      },
      form: comp.form || comp.config || {} // PERBAIKAN: Handle kedua kemungkinan (form/config)
    };
    console.log("🔍 Selected component for form:", componentWithForm);
    setSelectedComponent(componentWithForm);
    setShowForm(true);
  };

  // PERBAIKAN BESAR: Perbaiki fungsi handleSaveForm
  const handleSaveForm = (updatedData) => {
    console.log("💾 Saving form data:", updatedData);
    
    if (!activeFile) {
      toast.error("Tidak ada file aktif untuk menyimpan form");
      return;
    }

    setCanvasData((prev) => {
      const updatedCanvasData = {
        ...prev,
        [activeFile]: prev[activeFile].map((c) => {
          if (c.id === updatedData.id) {
            console.log("✅ Updating component with form data:", {
              before: { form: c.form, config: c.config },
              after: { form: updatedData.form }
            });
            
            // PERBAIKAN: Update kedua form dan config untuk konsistensi
            return { 
              ...c, 
              form: updatedData.form,
              config: updatedData.form // Sync config dengan form
            };
          }
          return c;
        })
      };
      
      console.log("📝 Updated canvas data:", updatedCanvasData[activeFile]);
      return updatedCanvasData;
    });
    
    setShowForm(false);
    setSelectedComponent(null);
    toast.success("Konfigurasi komponen berhasil disimpan!");
    
    // PERBAIKAN: Force refresh localStorage
    setTimeout(() => {
      const dataToSave = { folders, files, canvasData, openFiles, activeFile };
      localStorage.setItem("uncal-bpm-data", JSON.stringify(dataToSave));
    }, 100);
  };

  const { user } = useContext(AuthContext);
  
  // Handle save to JSON - SIMPLIFIED VERSION (Backend handles everything)
  const handleSaveToJSON = async () => {
    if (!activeFile) {
      toast.error("Tidak ada file aktif untuk disimpan");
      return;
    }

    // PERBAIKAN: Validasi tambahan sebelum melanjutkan
    if (folders.length === 0) {
      toast.error("Tidak ada project yang tersedia. Silakan buat project terlebih dahulu.");
      return;
    }

    // Dapatkan file ID terlebih dahulu
    const scenarioFileId = getScenarioFileId(activeFile);
    
    if (!scenarioFileId) {
      toast.error("Tidak dapat menemukan ID file. Pastikan file sudah tersimpan di project.");
      console.error("Save failed - scenarioFileId not found for:", activeFile);
      return;
    }

    console.log("💾 Saving with file ID:", scenarioFileId);
    
    try {
      const canvasComponents = canvasData[activeFile] || [];
      
      // DEBUG: Log detail form data sebelum save
      console.log("🔍 DETAIL Components to save:", canvasComponents.map(comp => ({
        id: comp.id,
        type: comp.type,
        label: comp.label,
        form: comp.form,
        config: comp.config,
        notes: comp.notes
      })));

      // PERBAIKAN: Hanya kirim raw data ke backend, biarkan backend yang proses
      const payload = {
        components: canvasComponents,
        project: (folders && folders[0] && folders[0].name) || "Default Project",
        scenarios: activeFile,
        metadata: {
          author: user?.username || "unknown",
          userId: user?.id || null,
          timestamp: new Date().toISOString(),
          version: 1,
        }
      };

      console.log("📤 Sending raw data to backend:", payload);

      // SATU API CALL - backend handle semua processing
      const res = await fetch(`http://localhost:8080/api/projects/files/${scenarioFileId}/save-scenario`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const errorText = await res.text();
        console.error("❌ Failed to save scenario:", errorText);
        throw new Error(errorText);
      }

      const result = await res.json();
      console.log("✅ Scenario saved successfully:", result);

      // Refresh data dari database
      window.dispatchEvent(new CustomEvent('refreshProjectData'));

      // Success message
      const componentCount = canvasComponents.length;
      const formDataCount = canvasComponents.filter(c => c.form && Object.keys(c.form).length > 0).length;
      toast.success(`Data berhasil disimpan! - ${formDataCount}/${componentCount} komponen memiliki konfigurasi`);

    } catch (err) {
      console.error("❌ Save error:", err);
      toast.error("Terjadi kesalahan saat menyimpan: " + err.message);
    }
  };

  return (
    <div className="App">
      <NavbarComponent
        folders={folders}
        setFolders={setFolders}
        files={files}
        setFiles={setFiles}
        setContextMenu={setContextMenu}
        setModal={setModal}
        onFileSelect={handleFileSelect}
        openFiles={openFiles}
        setOpenFiles={setOpenFiles}
        activeFile={activeFile}
        setActiveFile={setActiveFile}
        canvasData={canvasData}
        setCanvasData={setCanvasData}
        onLogout={logout}
      />

      <DndProvider backend={HTML5Backend}>
        <div className="app-container">
          <CanvasArea
            openFiles={openFiles}
            activeFile={activeFile}
            onCloseFile={handleCloseFile}
            onDropComponent={handleDropComponent}
            canvasComponents={canvasData[activeFile] || []}
            setActiveFile={setActiveFile}
            setContextMenu={setContextMenu}
            onSelectComponent={handleSelectComponent}
            onSaveToJSON={handleSaveToJSON}
            scenarioFileId={activeFile ? getScenarioFileId(activeFile) : null}
            setCanvasData={setCanvasData}
            canvasData={canvasData}
          />

          {openFiles.length > 0 && (
            <Palette
              components={components}
              setComponents={setComponents}
              token={token}
            />
          )}
        </div>
      </DndProvider>

      <ContextMenu
        contextMenu={contextMenu}
        setContextMenu={setContextMenu}
        setModal={setModal}
        onDeleteComponent={handleDeleteComponent}
      />

      <ModalAction
        modal={modal}
        setModal={setModal}
        folders={folders}
        setFolders={setFolders}
        files={files}
        setFiles={setFiles}
        openFiles={openFiles}
        setOpenFiles={setOpenFiles}
        activeFile={activeFile}
        setActiveFile={setActiveFile}
        canvasData={canvasData}
        setCanvasData={setCanvasData}
      />

      {showForm && selectedComponent &&
        ReactDOM.createPortal(
          <DynamicForm
            componentData={selectedComponent}
            onClose={() => setShowForm(false)}
            onSave={handleSaveForm}
          />,
          document.body
        )}

      <ToastContainer position="bottom-right" autoClose={2000} />
    </div>
  );
}