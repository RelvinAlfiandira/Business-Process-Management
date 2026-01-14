import React, { useEffect, useState } from "react";
import { toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import ComponentsItem from "./palette-components/components-item/ComponentsItem";
import AddModule from "./palette-components/add-module/AddModule";
import Confirmation from "../ui/confirmation/Confirmation";
import api from "../../utils/api";
import "./Palette.css";

// 🔹 Icon bawaan (untuk tabs kategori)
import allIcon from "../../assets/icon/allIcon.png";
import senderIcon from "../../assets/icon/senderIcon.png";
import receiverIcon from "../../assets/icon/receiverIcon.png";
import objectIcon from "../../assets/icon/objectIcon.png";
import paletteIcon from "../../assets/icon/paletteIcon.png";

function Palette({ onAddComponent, token }) {
  const [components, setComponents] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("All");
  const [showModal, setShowModal] = useState(false);
  const [contextMenu, setContextMenu] = useState(null);
  const [componentToDelete, setComponentToDelete] = useState(null);

  useEffect(() => {
    const loadComponents = async () => {
      if (!token) {
        setIsLoading(false);
        return;
      }
      try {
        const data = await api.get("/components", token);
        setComponents(data || []);
        // Menghapus notifikasi memuat komponen
      } catch (error) {
        console.error("Gagal memuat komponen.", error);
        if (error.status !== 401 && error.status !== 403) {
          toast.error("Gagal memuat komponen palette.");
        }
      } finally {
        setIsLoading(false);
      }
    };
    loadComponents();
  }, [token]);

  // 🔹 Mapping tab → icon default
  const tabIcons = {
    All: allIcon,
    Sender: senderIcon,
    Receiver: receiverIcon,
    Object: objectIcon,
  };

  // 🔹 Filter sesuai tab aktif
  const filteredComponents =
    activeTab === "All"
      ? components
      : components.filter((c) => c.type === activeTab);

  // 🔹 Tabs kategori dinamis
  const tabs = ["All", ...new Set(components.map((c) => c.type))];

  // 🔹 Tambah komponen baru (AddModule sudah pastikan id dihapus & style valid)
  const handleAddComponents = async (newComp) => {
    const componentToSave = { ...newComp };
    delete componentToSave.id; // safeguard tambahan

    try {
      const savedComp = await api.post("/components", componentToSave, token);
      setComponents((prev) => [...prev, savedComp]);

      toast.success(`Component "${savedComp.label}" berhasil ditambahkan!`);

      if (onAddComponent) onAddComponent(savedComp);
    } catch (error) {
      console.log("Gagal menyimpan komponen:", error);
      toast.error(
        `Gagal menyimpan komponen: ${error.message || "Error API"}`
      );
    }
  };

  // 🔹 Hapus komponen
  const handleDelete = async () => {
    if (!componentToDelete) return;
    const { id, label } = componentToDelete;

    try {
      await api.delete(`/components/${id}`, token);
      setComponents((prev) => prev.filter((c) => c.id !== id));

      toast.success(`Component "${label}" berhasil dihapus.`);
    } catch (error) {
      console.error("Gagal menghapus komponen:", error);
      toast.error(
        `Gagal menghapus component: ${error.message || "Error API"}`
      );
    } finally {
      setComponentToDelete(null);
    }
  };

  // 🔹 Menu konteks (klik kanan)
  const handleContextMenu = (e, component) => {
    e.preventDefault();
    setContextMenu({
      x: e.clientX,
      y: e.clientY,
      component,
    });
  };

  return (
    <div className="palette">
      {/* Header */}
      <div className="palette-header">
        <img
          src={paletteIcon}
          alt="palette icon"
          className="palette-header-icon"
        />
        Palette
      </div>

      {/* Tabs kategori */}
      <div className="palette-tabs">
        {tabs.map((tab) => (
          <button
            key={tab}
            className={`tab-btn ${activeTab === tab ? "active" : ""}`}
            onClick={() => setActiveTab(tab)}
          >
            <img
              src={tabIcons[tab] || "📦"}
              alt={`${tab} icon`}
              className="tab-icon"
            />
            {tab}
          </button>
        ))}
      </div>

      {/* Body */}
      <div className="palette-body">
        {/* Tombol Add Module */}
        <div
          className="palette-item add-module"
          onClick={() => setShowModal(true)}
        >
          + Add Module
        </div>

        {/* Loading atau daftar komponen */}
        {isLoading ? (
          <p className="palette-loading">Loading components...</p>
        ) : (
          filteredComponents.map((comp) => (
            <div
              key={comp.id}
              className="palette-item-wrapper"
              onContextMenu={(e) => handleContextMenu(e, comp)}
            >
              <ComponentsItem
                id={comp.id}
                label={comp.label}
                icon={comp.icon}
                type={comp.type}
                form={comp.form}
                style={comp.style}
              />
            </div>
          ))
        )}
      </div>

      {/* 🆕 Context menu delete */}
      {contextMenu && (
        <div
          className="context-menu"
          style={{ top: contextMenu.y, left: contextMenu.x }}
          onClick={() => {
            setComponentToDelete(contextMenu.component);
            setContextMenu(null);
          }}
          onMouseLeave={() => setContextMenu(null)}
        >
          <span>❌ Delete Component</span>
        </div>
      )}

      {/* 🆕 Modal Konfirmasi Delete */}
      {componentToDelete && (
        <Confirmation
          show={!!componentToDelete}
          title="Hapus Component"
          message={`Yakin ingin menghapus component "${componentToDelete.label}"?`}
          onConfirm={handleDelete}
          onCancel={() => setComponentToDelete(null)}
        />
      )}

      {/* Modal Add Module */}
      {showModal && (
        <AddModule
          onClose={() => setShowModal(false)}
          onAdd={async (newComp) => {
            const exists = components.some(
              (comp) => comp.label === newComp.label
            );
            if (exists) {
              toast.error(`${newComp.label} sudah ada di palette!`);
              return;
            }
            await handleAddComponents(newComp);
            setShowModal(false);
          }}
        />
      )}
    </div>
  );
}

export default Palette;
