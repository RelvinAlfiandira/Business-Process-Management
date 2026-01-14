// src/offcanvas/offcanvas-components/ModalAction.js (FIXED VERSION)
import React, { useState, useEffect, useContext } from "react";
import { Form } from "react-bootstrap";
import { toast } from "react-toastify";
import api from "../../utils/api";

import { AuthContext } from "../../context/AuthContext";
import Confirmation from "../ui/confirmation/Confirmation";

function ModalAction({
  modal,
  setModal,
  setFolders,
  setFiles,
  setOpenFiles,
  activeFile,
  setActiveFile,
  setCanvasData,
  onImportMapping,
}) {
  const { token } = useContext(AuthContext);

  const availableMappings = modal.availableMappings || [];
  const [inputValue, setInputValue] = useState("");
  const [selectedMappingId, setSelectedMappingId] = useState("");

  useEffect(() => {
    if (!modal.show) {
      setInputValue("");
      setSelectedMappingId("");
    }
  }, [modal.show]);

  if (!modal.show) return null;
  if (!token) return null;

  // ✅ HELPER FUNCTION: Extract filename dari modal.file (support string dan object)
  const getFileName = () => {
    if (!modal.file) return '';
    
    // Jika file adalah string (nama file lama)
    if (typeof modal.file === 'string') {
      return modal.file;
    }
    
    // Jika file adalah object (dari ContextMenu baru)
    if (typeof modal.file === 'object' && modal.file !== null) {
      return modal.file.name || '';
    }
    
    return '';
  };


  const buildFileKey = (folder, sub, fileName) => `${folder}-${sub}-${fileName}`;

  // FUNGSI PEMBANTU UNTUK REFRESH DATA FILES & FOLDERS
  const refreshFilesAndFolders = async (projectId, subfolderType) => {
    try {
      // 1. REFRESH FOLDERS (Untuk update tree view, termasuk rename project)
      const updatedFoldersResponse = await api.get('/projects', token); 
      setFolders(updatedFoldersResponse); 
    } catch (fetchError) {
      console.error("Gagal memuat ulang daftar project:", fetchError);
      toast.warn("Gagal me-refresh folder list.");
    }
    
    // 2. REFRESH FILES untuk subfolder yang relevan
    const subfolderKey = `${projectId}-${subfolderType}`;
    try {
        const filesInSubfolder = await api.get(`/projects/${projectId}/files/${subfolderType}`, token); 
        
        setFiles(prev => ({
            ...prev,
            [subfolderKey]: filesInSubfolder 
        }));
    } catch (filesFetchError) {
        setFiles(prev => ({ ...prev, [subfolderKey]: [] }));
        console.error(`Gagal memuat ulang files untuk ${subfolderType}:`, filesFetchError);
        toast.warn("Files berhasil dimodifikasi, tapi gagal me-refresh daftar files.");
    }
  };

  const renderModalBody = () => {
    if (modal.type.includes("rename") || modal.type.includes("create")) {
      return (
        <Form.Control
          type="text"
          placeholder="Masukkan nama baru"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          autoFocus
        />
      );
    }
    if (modal.type === "importMapping") {
      return (
        <Form.Select
          value={selectedMappingId}
          onChange={(e) => setSelectedMappingId(e.target.value)}
        >
          <option value="">Pilih Mapping</option>
          {availableMappings.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </Form.Select>
      );
    }
    return null;
  };
  
  // === Project === 
  const handleRenameProject = async () => {
    if (!inputValue.trim()) return;
    const projectId = modal.folder.id; 
    const newName = inputValue.trim();

    try {
      const renameRequest = { newName };
      
      // 1. Lakukan operasi rename
      await api.patch(`/projects/${projectId}/rename`, renameRequest, token); 
      
      // 2. WAJIB REFRESH SEMUA FOLDERS
      const updatedFoldersResponse = await api.get('/projects', token); 
      setFolders(updatedFoldersResponse);
      
      toast.success(`Project diubah menjadi ${newName}`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal rename Project: ${error.message}`);
    }
  };

  const handleDeleteProject = async () => {
    const projectId = modal.folder.id; 

    try {
      await api.delete(`/projects/${projectId}`, token);

      setFolders((prev) => prev.filter((f) => f.id !== projectId));
      
      // Logika cleanup state files, canvasData, openFiles, activeFile 
      setFiles((prev) => {
        const updated = {};
        Object.keys(prev).forEach((key) => {
          if (!key.startsWith(projectId + "-")) { 
            updated[key] = prev[key];
          }
        });
        return updated;
      });
      setCanvasData((prev) => {
        const updated = {};
        Object.keys(prev).forEach((key) => {
          if (!key.startsWith(projectId + "-")) { 
            updated[key] = prev[key];
          }
        });
        return updated;
      });
      setOpenFiles((prev) =>
        prev.filter((f) => !f.startsWith(projectId + "-")) 
      );
      if (activeFile?.startsWith(projectId + "-")) { 
        setActiveFile(null);
      }

      toast.success(`Project ${modal.folder.name} dihapus`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal menghapus Project: ${error.message}`);
    }
  };

  // === Scenario ===
  const handleCreateScenario = async () => { 
    if (!inputValue.trim()) return;
    const projectId = modal.folder.id;
    const fileName = inputValue.trim();
    const subfolderType = modal.sub; 

    try {
      const fileRequest = {
        name: fileName,
        subfolderType: subfolderType, 
        canvasData: "{}", 
      };

      // 1. Panggil API create
      await api.post(`/projects/${projectId}/files`, fileRequest, token);
      
      // 2. REFRESH FOLDERS dan FILES
      await refreshFilesAndFolders(projectId, subfolderType);

      toast.success(`Scenario ${fileName} dibuat`);
      setModal({ ...modal, show: false });
      
    } catch (error) {
        
      // Cek apakah error disebabkan oleh JSON parsing error (Plain Text Success)
      const isPlainTextSuccess = error.message && 
                                 error.message.includes("Unexpected token 'F'") && 
                                 error.message.includes("is not valid JSON");
                                
      if (isPlainTextSuccess) {
          console.warn("API success received as plain text. Forcing frontend update.");
          
          // Jalankan logika refresh
          await refreshFilesAndFolders(projectId, subfolderType);
          
          toast.success(`Scenario ${fileName} dibuat`);
          setModal({ ...modal, show: false });
          return;
      }
      
      // Jika itu adalah error API yang sebenarnya
      toast.error(`Gagal membuat Scenario: ${error.message}`);
    }
  };

  // ✅ PERBAIKAN: Handle both string dan object file
  const handleRenameScenario = async () => {
    if (!inputValue.trim()) return;
    const projectId = modal.folder.id;
    const subfolderType = modal.sub;
    
    // ✅ GUNAKAN HELPER FUNCTION
    const oldFileName = getFileName();
    const newName = inputValue.trim();
    const oldKey = buildFileKey(projectId, subfolderType, oldFileName);
    const newKey = buildFileKey(projectId, subfolderType, newName);

    try {
      const renameRequest = { newName };
      
      // 1. Panggil API rename
      await api.patch(
        `/projects/${projectId}/files/${subfolderType}/${oldFileName}/rename`,
        renameRequest,
        token
      );

      // 2. REFRESH FILES untuk subfolder yang bersangkutan
      await refreshFilesAndFolders(projectId, subfolderType);
      
      // 3. Update state canvas, openFiles, activeFile 
      setCanvasData((prev) => {
        const updated = { ...prev };
        if (prev[oldKey]) {
          updated[newKey] = prev[oldKey];
          delete updated[oldKey];
        }
        return updated;
      });
      setOpenFiles((prev) => prev.map((f) => (f === oldKey ? newKey : f)));
      if (activeFile === oldKey) {
        setActiveFile(newKey);
      }

      toast.success(`Scenario diubah menjadi ${newName}`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal rename Scenario: ${error.message}`);
    }
  };

  // ✅ PERBAIKAN: Handle both string dan object file
  const handleDeleteScenario = async () => {
    const projectId = modal.folder.id;
    const subfolderType = modal.sub;
    
    // ✅ GUNAKAN HELPER FUNCTION
    const oldFileName = getFileName();
    const oldKey = buildFileKey(projectId, subfolderType, oldFileName);

    try {
      // 1. Panggil API delete
      await api.delete(
        `/projects/${projectId}/files/${subfolderType}/${oldFileName}`,
        token
      );

      // 2. REFRESH FILES untuk subfolder yang bersangkutan
      await refreshFilesAndFolders(projectId, subfolderType);

      // 3. Cleanup state canvas, openFiles, activeFile
      setCanvasData((prev) => {
        const updated = { ...prev };
        if (updated[oldKey]) {
          delete updated[oldKey];
        }
        return updated;
      });
      setOpenFiles((prev) => prev.filter((f) => f !== oldKey));
      if (activeFile === oldKey) {
        setActiveFile(null);
      }

      toast.success(`Scenario ${oldFileName} dihapus`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal menghapus Scenario: ${error.message}`);
    }
  };

  // === Mapping ===
  // ✅ PERBAIKAN: Handle both string dan object file
  const handleDeleteMapping = async () => {
    const projectId = modal.folder.id;
    const subfolderType = modal.sub;
    
    // ✅ GUNAKAN HELPER FUNCTION
    const oldFileName = getFileName();
    const oldKey = buildFileKey(projectId, subfolderType, oldFileName);
    
    try {
      // 1. Panggil API delete
      await api.delete(
        `/projects/${projectId}/files/${subfolderType}/${oldFileName}`,
        token
      );
      
      // 2. REFRESH FILES untuk subfolder yang bersangkutan
      await refreshFilesAndFolders(projectId, subfolderType);

      // 3. Cleanup state
      setCanvasData((prev) => {
        const updated = { ...prev };
        if (updated[oldKey]) {
          delete updated[oldKey];
        }
        return updated;
      });
      setOpenFiles((prev) => prev.filter((f) => f !== oldKey));
      if (activeFile === oldKey) {
        setActiveFile(null);
      }

      toast.success(`Mapping ${oldFileName} dihapus`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal menghapus Mapping: ${error.message}`);
    }
  };

  const handleImportMapping = async () => {
    if (!selectedMappingId) {
      toast.error("Silakan pilih mapping terlebih dahulu!");
      return;
    }
    const mappingToImport = availableMappings.find(
      (m) => m.id === parseInt(selectedMappingId)
    );

    if (!mappingToImport) return;

    const projectId = modal.folder.id;
    const subfolderType = modal.sub;
    
    try {
      // 1. Panggil API import mapping
      const importRequest = { mappingId: mappingToImport.id };
      await api.post(
        `/projects/${projectId}/files/mappings/import`, 
        importRequest,
        token
      );
      
      // 2. REFRESH FILES dan FOLDERS
      await refreshFilesAndFolders(projectId, subfolderType);

      // 3. Panggil callback onImportMapping
      onImportMapping && onImportMapping(mappingToImport, projectId, subfolderType); 

      toast.success(`Mapping '${mappingToImport.name}' berhasil diimport!`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal import Mapping: ${error.message}`);
    }
  };

  // === Java File ===
  // ✅ PERBAIKAN: Handle both string dan object file
  const handleRenameJavaFile = async () => {
    if (!inputValue.trim()) return;
    const projectId = modal.folder.id;
    const subfolderType = modal.sub;
    
    // ✅ GUNAKAN HELPER FUNCTION
    const oldFileName = getFileName();
    const newName = inputValue.trim();
    const oldKey = buildFileKey(projectId, subfolderType, oldFileName);
    const newKey = buildFileKey(projectId, subfolderType, newName);

    try {
      const renameRequest = { newName };
      // 1. Panggil API rename
      await api.patch(
        `/projects/${projectId}/files/${subfolderType}/${oldFileName}/rename`,
        renameRequest,
        token
      );
      
      // 2. REFRESH FILES untuk subfolder yang bersangkutan
      await refreshFilesAndFolders(projectId, subfolderType);

      // 3. Update state canvas, openFiles, activeFile 
      setCanvasData((prev) => {
        const updated = { ...prev };
        if (prev[oldKey]) {
          updated[newKey] = prev[oldKey];
          delete updated[oldKey];
        }
        return updated;
      });
      setOpenFiles((prev) => prev.map((f) => (f === oldKey ? newKey : f)));
      if (activeFile === oldKey) {
        setActiveFile(newKey);
      }

      toast.success(`Java File diubah menjadi ${newName}`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal rename Java File: ${error.message}`);
    }
  };

  // ✅ PERBAIKAN: Handle both string dan object file
  const handleDeleteJavaFile = async () => {
    const projectId = modal.folder.id;
    const subfolderType = modal.sub;
    
    // ✅ GUNAKAN HELPER FUNCTION
    const oldFileName = getFileName();
    const oldKey = buildFileKey(projectId, subfolderType, oldFileName);

    try {
      // 1. Panggil API delete
      await api.delete(
        `/projects/${projectId}/files/${subfolderType}/${oldFileName}`,
        token
      );

      // 2. REFRESH FILES untuk subfolder yang bersangkutan
      await refreshFilesAndFolders(projectId, subfolderType);

      // 3. Cleanup state
      setCanvasData((prev) => {
        const updated = { ...prev };
        if (updated[oldKey]) {
          delete updated[oldKey];
        }
        return updated;
      });
      setOpenFiles((prev) => prev.filter((f) => f !== oldKey));
      if (activeFile === oldKey) {
        setActiveFile(null);
      }

      toast.success(`Java File ${oldFileName} dihapus`);
      setModal({ ...modal, show: false });
    } catch (error) {
      toast.error(`Gagal menghapus Java File: ${error.message}`);
    }
  };

  // === Handler Utama ===
  const handleSubmit = () => {
    switch (modal.type) {
      case "renameProject":
        handleRenameProject();
        break;
      case "deleteProject":
        handleDeleteProject();
        break;
      case "createScenario":
        handleCreateScenario();
        break;
      case "renameScenario":
        handleRenameScenario();
        break;
      case "deleteScenario":
        handleDeleteScenario();
        break;
      case "deleteMapping":
        handleDeleteMapping();
        break;
      case "renameJavaFile":
        handleRenameJavaFile();
        break;
      case "deleteJavaFile":
        handleDeleteJavaFile();
        break;
      case "confirmLogout":
        // If modal contains an onConfirm callback (e.g., logout), call it
        try {
          if (typeof modal.onConfirm === "function") modal.onConfirm();
        } catch (err) {
          console.error("Error running confirmLogout callback:", err);
        }
        setModal({ ...modal, show: false });
        break;
      case "importMapping":
        handleImportMapping();
        break;
      case "chooseJarFile":
      case "chooseJavaFile":
      case "createJavaFileName":
        toast.success(`Aksi "${modal.type}" berhasil!`);
        setModal({ ...modal, show: false });
        break;
      default:
        toast.info("Aksi tidak diimplementasikan");
        setModal({ ...modal, show: false });
    }
    
    setInputValue("");
    setSelectedMappingId("");
  };

  // === Body Modal ===

  // Prefer explicit modal.title/modal.message if provided (e.g., custom confirmations)
  const modalTitle = modal.title || (modal.type.startsWith("delete") ? "Konfirmasi Hapus" : modal.type);
  
  // ✅ PERBAIKAN: Gunakan helper function untuk nama file
  const modalMessage =
        typeof modal.message === "string"
              ? modal.message
              : modal.type.startsWith("delete")
              ? `Apa yakin ingin menghapus "${getFileName() || modal.folder?.name}"?`
              : "";

  return (
        <Confirmation
              show={modal.show}
              title={modalTitle}
              message={modalMessage}
              onConfirm={handleSubmit}
              onCancel={() => setModal({ ...modal, show: false })}
        >
              {/* Untuk create/rename/import → render form body */}
              {!modal.type.startsWith("delete") && renderModalBody()}
        </Confirmation>
  );
}

export default ModalAction;