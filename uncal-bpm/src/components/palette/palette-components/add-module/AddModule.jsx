import React, { useState } from "react";
import "./AddModule.css";

function AddModule({ onClose, onAdd }) {
  const [jsonFile, setJsonFile] = useState(null);
  const [error, setError] = useState("");

  const handleFileUpload = (e) => {
    const file = e.target.files?.[0];
    if (file) {
      setJsonFile(file);
      setError("");
    }
  };

  const handleAdd = () => {
    if (!jsonFile) {
      setError("Please upload a JSON file first!");
      return;
    }

    // parse json
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target.result);

        // Minimal validation
        if (!parsed.label || !parsed.form) {
          setError("JSON harus berisi kolom 'label' dan 'form'!");
          return;
        }

        // 💡 FIX 1: Pastikan 'style' adalah objek agar aman
        if (!parsed.style || typeof parsed.style !== "object" || Array.isArray(parsed.style)) {
          parsed.style = {};
        }

        // 💡 FIX 2: Hapus 'id' dari payload. Server/DB yang buat ID
        if (parsed.id) {
          delete parsed.id;
        }

        const newComponent = {
          label: parsed.label,
          type: parsed.type || "Custom",
          icon: parsed.icon || null,
          form: parsed.form,
          style: parsed.style, // sudah aman
        };

        onAdd(newComponent); // kirim ke parent
        onClose();
      } catch (err) {
        setError("Invalid JSON file format!");
      }
    };
    reader.readAsText(jsonFile);
  };

  return (
    <div className="add-module-overlay">
      <div
        className="add-module-box"
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-module-title"
      >
        <div className="add-module-header">
          <h3 id="add-module-title">Add Module Component</h3>
          <button
            type="button"
            className="add-module-close"
            onClick={onClose}
            aria-label="Close add module"
          >
            ×
          </button>
        </div>

        <div className="add-module-body">
          <label className="add-module-label">Upload Component JSON</label>
          <input
            className="add-module-file-input"
            type="file"
            accept=".json,application/json"
            onChange={handleFileUpload}
          />
          {jsonFile && (
            <p className="add-module-file-preview">📦 {jsonFile.name}</p>
          )}
          {error && <p className="add-module-error">{error}</p>}
        </div>

        <div className="add-module-actions">
          <button
            className="add-module-btn add-module-btn-close"
            onClick={onClose}
          >
            Close
          </button>
          <button
            className="add-module-btn add-module-btn-add"
            onClick={handleAdd}
          >
            Add Module
          </button>
        </div>
      </div>
    </div>
  );
}

export default AddModule;
