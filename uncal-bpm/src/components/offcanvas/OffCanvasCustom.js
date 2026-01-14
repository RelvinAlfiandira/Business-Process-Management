import React, { useState, useEffect, useContext, useCallback } from "react";
import { Offcanvas } from "react-bootstrap";
import { FaPlus, FaTimes } from "react-icons/fa";

import ProjectList from "./offcanvas-components/project-list/ProjectList";
import SearchProject from "./offcanvas-components/search-project/SearchProject";
import AddFolder from "./offcanvas-components/add-folder/AddFolder";
import ModalAction from "../manager/ModalAction";
import { AuthContext } from "../../context/AuthContext";
import api from "../../utils/api"; 

import "./OffCanvasCustom.css";

function OffCanvasCustom({
  show,
  setShow,
  folders,
  setFolders,
  files,
  onFileSelect,
  setContextMenu,
  setFiles,
  setOpenFiles,
  activeFile,
  setActiveFile,
  setCanvasData,
}) {
  const { token } = useContext(AuthContext);

  const [expanded, setExpanded] = useState(() => {
    try {
      const savedExpanded = localStorage.getItem("uncal-bpm-expanded");
      return savedExpanded ? JSON.parse(savedExpanded) : {};
    } catch (e) {
      console.error("Gagal memuat expanded dari localStorage:", e);
      return {};
    }
  });

  const [showAddFolder, setShowAddFolder] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [modal, setModal] = useState({ show: false, type: "" });

  const handleToggleAddFolder = () => setShowAddFolder(!showAddFolder);

  // 1. Simpan state expanded ke localStorage
  useEffect(() => {
    localStorage.setItem("uncal-bpm-expanded", JSON.stringify(expanded));
  }, [expanded]);
  
  // 2. Load project dari backend saat komponen pertama kali render
  useEffect(() => {
    const fetchProjects = async () => {
      if (!token) return;
      try {
        const data = await api.get("/projects", token);
        setFolders(data);
      } catch (err) {
        console.error("Error ambil project:", err);
      }
    };
    
    fetchProjects();
  }, [token, setFolders]);

  // ----------------------------------------------------
  // PERBAIKAN: LOGIKA UNTUK MEMUAT FILE SAAT SUBFOLDER DIEKSPANSI
  // ----------------------------------------------------
  useEffect(() => {
    if (!token) return;

    Object.keys(expanded).forEach(key => {
      if (expanded[key] && key.includes('-')) {
        const [projectIdStr, subfolderType] = key.split('-');
        const projectId = parseInt(projectIdStr);
        
        // Validasi projectId
        if (isNaN(projectId) || projectId <= 0) {
          console.warn(`⚠️ Project ID tidak valid: ${projectIdStr}`);
          return;
        }

        const subfolderKey = `${projectId}-${subfolderType}`;

        // Jika files belum dimuat atau perlu refresh
        if (!files.hasOwnProperty(subfolderKey) || files[subfolderKey] === null) {
          const fetchFilesForSubfolder = async () => {
            try {
              console.log(`🔍 Fetching files for project ${projectId}, type: ${subfolderType}`);
              
              const filesInSubfolder = await api.get(`/projects/${projectId}/files/${subfolderType}`, token);
              
              console.log(`✅ Berhasil memuat ${filesInSubfolder.length} files untuk ${subfolderType}`);
              
              setFiles(prev => ({
                ...prev,
                [subfolderKey]: filesInSubfolder 
              }));
            } catch (filesFetchError) {
              console.error(`❌ Gagal memuat files untuk ${subfolderType}:`, filesFetchError);
              
              // Set empty array instead of throwing error
              setFiles(prev => ({ 
                ...prev, 
                [subfolderKey]: [] 
              }));
              
              // Tampilkan toast error jika diperlukan
              if (filesFetchError.message.includes('Proyek tidak ditemukan')) {
                console.warn(`Project ${projectId} tidak ditemukan, mungkin sudah dihapus`);
              }
            }
          };

          fetchFilesForSubfolder();
        }
      }
    });
  }, [expanded, token, files, setFiles]);
  // ----------------------------------------------------

  // PERBAIKAN: FUNGSI UNTUK MANUAL REFRESH dengan error handling
  const manualRefreshData = useCallback(async () => {
    if (!token) return;
    
    try {
      console.log("🔄 Manual refresh data dimulai...");
      
      // Refresh projects
      const projectsData = await api.get("/projects", token);
      setFolders(projectsData);
      
      console.log(`✅ Loaded ${projectsData.length} projects`);
      
      // Refresh files untuk expanded folders dengan error handling
      const refreshPromises = Object.keys(expanded)
        .filter(key => expanded[key] && key.includes('-'))
        .map(async (key) => {
          const [projectIdStr, subfolderType] = key.split('-');
          const projectId = parseInt(projectIdStr);
          
          // Validasi projectId
          if (isNaN(projectId) || projectId <= 0) {
            console.warn(`⚠️ Skip refresh untuk project ID tidak valid: ${projectIdStr}`);
            return null;
          }
          
          const subfolderKey = `${projectId}-${subfolderType}`;

          try {
            const filesInSubfolder = await api.get(`/projects/${projectId}/files/${subfolderType}`, token);
            console.log(`✅ Refresh ${subfolderType}: ${filesInSubfolder.length} files`);
            return { subfolderKey, filesInSubfolder };
          } catch (filesFetchError) {
            console.error(`❌ Gagal refresh files untuk ${subfolderType}:`, filesFetchError);
            return { subfolderKey, filesInSubfolder: [] }; // Return empty array on error
          }
        });

      const results = await Promise.all(refreshPromises);
      
      // Update files state dengan data yang baru
      setFiles(prev => {
        const updatedFiles = { ...prev };
        results.forEach(result => {
          if (result) {
            updatedFiles[result.subfolderKey] = result.filesInSubfolder;
          }
        });
        return updatedFiles;
      });

      console.log("✅ Manual refresh completed successfully");
    } catch (err) {
      console.error("❌ Error manual refresh:", err);
    }
  }, [token, expanded, setFolders, setFiles]);

  // 3. EVENT LISTENER UNTUK REFRESH DATA SETELAH RUN/STOP SCENARIO
  useEffect(() => {
    const handleRefreshData = () => {
      console.log("🔄 Refresh data triggered from context menu");
      
      const refreshAllData = async () => {
        if (!token) return;
        
        try {
          // Refresh projects
          const projectsData = await api.get("/projects", token);
          setFolders(projectsData);
          
          console.log(`✅ Refresh: Loaded ${projectsData.length} projects`);
          
          // Refresh files untuk semua expanded folders dengan error handling
          const refreshPromises = Object.keys(expanded)
            .filter(key => expanded[key] && key.includes('-'))
            .map(async (key) => {
              const [projectIdStr, subfolderType] = key.split('-');
              const projectId = parseInt(projectIdStr);
              
              if (isNaN(projectId) || projectId <= 0) {
                console.warn(`⚠️ Skip refresh untuk invalid project ID: ${projectIdStr}`);
                return null;
              }
              
              const subfolderKey = `${projectId}-${subfolderType}`;

              try {
                const filesInSubfolder = await api.get(`/projects/${projectId}/files/${subfolderType}`, token);
                return { subfolderKey, filesInSubfolder };
              } catch (filesFetchError) {
                console.error(`❌ Gagal refresh files untuk ${subfolderType}:`, filesFetchError);
                return { subfolderKey, filesInSubfolder: [] };
              }
            });

          const results = await Promise.all(refreshPromises);
          
          // Update files state dengan data yang baru
          setFiles(prev => {
            const updatedFiles = { ...prev };
            results.forEach(result => {
              if (result) {
                updatedFiles[result.subfolderKey] = result.filesInSubfolder;
              }
            });
            return updatedFiles;
          });

          console.log("✅ Data refreshed successfully");
        } catch (err) {
          console.error("❌ Error refresh data:", err);
        }
      };

      refreshAllData();
    };

    // Tambahkan event listener untuk refreshProjectData
    window.addEventListener('refreshProjectData', handleRefreshData);
    
    // Cleanup: hapus event listener ketika komponen unmount
    return () => {
      window.removeEventListener('refreshProjectData', handleRefreshData);
    };
  }, [token, setFolders, expanded, setFiles]);

  // 4. EVENT LISTENER UNTUK FILE STATUS UPDATED (REAL-TIME UPDATE)
  useEffect(() => {
    const handleFileStatusUpdated = (event) => {
      const { fileId, runStatus } = event.detail;
      console.log(`🔄 File ${fileId} status updated to: ${runStatus}`);
      
      // Update files state secara langsung tanpa refresh penuh
      setFiles(prevFiles => {
        const updatedFiles = { ...prevFiles };
        
        Object.keys(updatedFiles).forEach(key => {
          updatedFiles[key] = updatedFiles[key].map(file => {
            if (file.id === fileId) {
              // Update runStatus di file object
              return {
                ...file,
                runStatus: runStatus,
                // Juga update di canvasData jika perlu
                canvasData: file.canvasData ? 
                  (typeof file.canvasData === 'string' ? 
                    JSON.stringify({ ...JSON.parse(file.canvasData), run_status: runStatus }) : 
                    JSON.stringify({ ...file.canvasData, run_status: runStatus })) 
                  : JSON.stringify({ run_status: runStatus })
              };
            }
            return file;
          });
        });
        
        return updatedFiles;
      });
    };

    window.addEventListener('fileStatusUpdated', handleFileStatusUpdated);
    
    return () => {
      window.removeEventListener('fileStatusUpdated', handleFileStatusUpdated);
    };
  }, [setFiles]);

  // 6. EXPOSE FUNGSI REFRESH KE WINDOW OBJECT (untuk dipanggil dari ContextMenu)
  useEffect(() => {
    window.refreshProjectData = manualRefreshData;
    
    // Cleanup: hapus dari window object ketika komponen unmount
    return () => {
      delete window.refreshProjectData;
    };
  }, [manualRefreshData]);

  const handleImportMapping = (mapping, folder, sub) => {
    console.log(`✅ Mapping '${mapping.name}' berhasil diimpor.`);
    const key = `${folder}-${sub}`;
    const newFileName = `${mapping.name}.xml`;

    setFiles((prev) => {
      const updatedFiles = { ...prev };
      updatedFiles[key] = [
        ...(updatedFiles[key] || []),
        {
          folder,
          sub,
          type: "mappingFile",
          name: newFileName,
          data: mapping.data,
          id: mapping.id,
        },
      ];
      return updatedFiles;
    });

    setModal({ show: false, type: "" });
  };

  return (
    <>
      <Offcanvas
        show={show}
        onHide={() => setShow(false)}
        placement="start"
        className="offcanvas-dark"
      >
        <Offcanvas.Header>
          <Offcanvas.Title className="d-flex justify-content-between align-items-center w-100">
            <span>Uncal BPM Workspace</span>
            <span className="toggle-add-folder" onClick={handleToggleAddFolder}>
              {showAddFolder ? <FaTimes size={18} /> : <FaPlus size={18} />}
            </span>
          </Offcanvas.Title>
        </Offcanvas.Header>

        <Offcanvas.Body>
          <SearchProject
            onSearch={setSearchTerm}
            folders={folders}
            files={files}
          />

          <ProjectList
            folders={folders}
            files={files}
            expanded={expanded}
            setExpanded={setExpanded}
            setContextMenu={setContextMenu}
            onFileSelect={onFileSelect}
            searchTerm={searchTerm}
          />

          {showAddFolder && (
            <AddFolder
              folders={folders}
              setFolders={setFolders}
              setShowAddFolder={setShowAddFolder}
            />
          )}
        </Offcanvas.Body>
      </Offcanvas>

      <ModalAction
        modal={modal}
        setModal={setModal}
        setFolders={setFolders}
        setFiles={setFiles}
        setOpenFiles={setOpenFiles}
        activeFile={activeFile}
        setActiveFile={setActiveFile}
        setCanvasData={setCanvasData}
        onImportMapping={handleImportMapping}
      />
    </>
  );
}

export default OffCanvasCustom;