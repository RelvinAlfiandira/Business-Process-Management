import React from "react";
import { FaFile } from "react-icons/fa";
import "./ProjectList.css";

//  Import custom icons
import scenariosIcon from "../../../../assets/icon/scenariosIcon.png";
import mappingsIcon from "../../../../assets/icon/mappingsIcon.png";
import librariesIcon from "../../../../assets/icon/librariesIcon.png";
import javaIcon from "../../../../assets/icon/javaIcon.png";
import projectIcon from "../../../../assets/icon/projectIcon.png";

//  label + icon + type folder/file
const folderConfig = {
  scenarios: { label: "Scenarios", icon: scenariosIcon, folderType: "scenarioFolder", fileType: "scenarioFile" },
  mappings: { label: "Mappings", icon: mappingsIcon, folderType: "mappingFolder", fileType: "mappingFile" },
  libraries: { label: "Libraries", icon: librariesIcon, folderType: "librariesFolder", fileType: "libraryFile" },
  java: { label: "Java", icon: javaIcon, folderType: "javaFolder", fileType: "javaFile" },
};

function ProjectList({
  folders = [],        // sekarang ini array of object {id, name, createdAt}
  files = {},
  expanded,
  setExpanded,
  setContextMenu,
  onFileSelect,
  searchTerm = "",
}) {
  const toggleExpand = (key) =>
    setExpanded({ ...expanded, [key]: !expanded[key] });

  //  highlight search
  const highlight = (text) => {
    if (!searchTerm) return text;
    const regex = new RegExp(`(${searchTerm})`, "gi");
    return text.split(regex).map((part, i) =>
      regex.test(part) ? <span key={i} className="highlight">{part}</span> : part
    );
  };

  const buildFileKey = (folderId, sub, fileName) =>
    `${folderId}-${sub}-${fileName}`;

  return (
    <div className="project-list-container">
      {folders
        .filter(
          (folder) =>
            folder.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            Object.keys(folderConfig).some((sub) =>
              (files[`${folder.id}-${sub}`] || []).some((file) =>
                file.name.toLowerCase().includes(searchTerm.toLowerCase())
              )
            )
        )
        .map((folder) => (
          <div key={folder.id}>
            {/* Folder utama */}
            <div
              className="folder-link"
              onClick={() => toggleExpand(folder.id)}
              onContextMenu={(e) => {
                e.preventDefault();
                setContextMenu({
                  mouseX: e.clientX,
                  mouseY: e.clientY,
                  type: "project",
                  folder, // masih dikirim object biar tahu id & name
                });
              }}
            >
              <span
                className="caret"
                onClick={(e) => {
                  e.stopPropagation();
                  toggleExpand(folder.id);
                }}
              >
                {expanded[folder.id] ? "▼" : "▶"}
              </span>
              <img src={projectIcon} alt="folder" className="custom-icon" />
              <span className="folder-label">{highlight(folder.name)}</span>
            </div>

            {/* Subfolder */}
            {expanded[folder.id] && (
              <div className="subfolder-container">
                {Object.keys(folderConfig).map((sub) => {
                  // Hide mappings, libraries, dan java
                  if (sub === "mappings" || sub === "libraries" || sub === "java") {
                    return null;
                  }
                  
                  return (
                  <div key={sub}>
                    <div
                      className={`subfolder ${
                        sub === "libraries" || sub === "java"
                          ? "non-expandable"
                          : "expandable"
                      }`}
                      onClick={() =>
                        !(sub === "libraries" || sub === "java") &&
                        toggleExpand(folder.id + "-" + sub)
                      }
                      onContextMenu={(e) => {
                        e.preventDefault();
                        setContextMenu({
                          mouseX: e.clientX,
                          mouseY: e.clientY,
                          type: folderConfig[sub].folderType,
                          folder,
                          sub,
                        });
                      }}
                    >
                      {/* Caret untuk expandable */}
                      {!(sub === "libraries" || sub === "java") && (
                        <span className="caret">
                          {expanded[folder.id + "-" + sub] ? "▼" : "▶"}
                        </span>
                      )}

                      {/* Icon */}
                      {folderConfig[sub]?.icon && (
                        <img
                          src={folderConfig[sub].icon}
                          alt={sub}
                          className="custom-icon"
                        />
                      )}

                      {highlight(folderConfig[sub]?.label || sub)}
                    </div>

                    {/* File list */}
                    {(expanded[folder.id + "-" + sub] ||
                      sub === "libraries" ||
                      sub === "java") && (
                      <div className="file-list">
                        {(files[`${folder.id}-${sub}`] || [])
                          .filter((file) =>
                            file.name
                              .toLowerCase()
                              .includes(searchTerm.toLowerCase())
                          )
                          .map((file, i) => (
                            <div
                              key={i}
                              className={`file-item ${
                                searchTerm &&
                                file.name
                                  .toLowerCase()
                                  .includes(searchTerm.toLowerCase())
                                  ? "highlight-bg"
                                  : ""
                              }`}
                              onClick={() =>
                                onFileSelect &&
                                onFileSelect(
                                  buildFileKey(folder.id, sub, file.name)
                                )
                              }
                              onContextMenu={(e) => {
                                e.preventDefault();

                                console.log('mengirim file ke context menu:', {
                                  fileObject: file,
                                  hasId: !!file.id,
                                  idValue: file.id,
                                  fileName: file.name,
                                });

                                setContextMenu({
                                  mouseX: e.clientX,
                                  mouseY: e.clientY,
                                  type: folderConfig[sub].fileType,
                                  folder,
                                  sub,
                                  file: file,
                                });
                              }}
                            >
                              <FaFile className="file-icon" />{" "}
                              {highlight(file.name)}
                            </div>
                          ))}
                      </div>
                    )}
                  </div>
                  );
                })}
              </div>
            )}
          </div>
        ))}
    </div>
  );
}

export default ProjectList;
