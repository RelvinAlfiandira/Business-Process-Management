import React, { useState, useContext } from "react";
import { Button } from "react-bootstrap";
import { FaPlus, FaTimes } from "react-icons/fa";
import { AuthContext } from "../../../../context/AuthContext"; // asumsi token disimpan di sini
import "./AddFolder.css";

function AddFolder({ folders, setFolders, setShowAddFolder }) {
  const [newFolderName, setNewFolderName] = useState("");
  const [loading, setLoading] = useState(false);
  const { token } = useContext(AuthContext); // ambil token dari context

  const handleAddFolder = async () => {
    if (newFolderName.trim() === "") return;

    setLoading(true);
    try {
      const response = await fetch("http://localhost:8080/api/projects", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`, // ✅ kirim token
        },
        body: JSON.stringify({ name: newFolderName }),
      });

      if (!response.ok) throw new Error("Gagal membuat project");

      const newProject = await response.json();

      // ✅ Update state dengan data dari DB
      setFolders([...folders, newProject]);
      setNewFolderName("");
      setShowAddFolder(false);
    } catch (error) {
      console.error(error);
      alert("Gagal menyimpan project");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="add-folder-overlay">
      <div className="add-folder-box">
        <div className="add-folder-header">
          <h5>Add New Project</h5>
          <FaTimes
            className="add-folder-close-icon"
            onClick={() => setShowAddFolder(false)}
          />
        </div>
        <div className="add-folder-body">
          <input
            type="text"
            placeholder="New Project Name"
            value={newFolderName}
            onChange={(e) => setNewFolderName(e.target.value)}
            disabled={loading}
          />
          <Button
            variant="warning"
            size="sm"
            className="w-100 mt-3 add-folder-btn"
            onClick={handleAddFolder}
            disabled={loading}
          >
            <FaPlus className="btn-icon" />{" "}
            {loading ? "Saving..." : "Save Project"}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default AddFolder;
