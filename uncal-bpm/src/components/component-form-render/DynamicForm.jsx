// src/components/component-form-render/DynamicForm.js
import React, { useState, useEffect, useMemo, useCallback } from "react";

function DynamicForm({ componentData, onClose, onSave }) {
  const [activeTab, setActiveTab] = useState("");
  const [formData, setFormData] = useState({});
  const [focusedField, setFocusedField] = useState(null);

  // Debug logging yang lebih detail
  console.log('🔍 DynamicForm received componentData:', {
    componentData,
    hasConfig: !!componentData?.config,
    hasForm: !!componentData?.form,
    configData: componentData?.config?.data,
    formData: componentData?.form?.data,
    configTabs: componentData?.config?.tabs,
    formTabs: componentData?.form?.tabs
  });

  const s = useMemo(() => componentData?.style || {}, [componentData]);

  // Custom scrollbar styling based on JSON
  useEffect(() => {
    const style = document.createElement('style');
    style.textContent = `
      .dynamic-form-scrollbar::-webkit-scrollbar {
        width: ${s.scrollbar?.width || '6px'};
      }
      .dynamic-form-scrollbar::-webkit-scrollbar-track {
        background: ${s.scrollbar?.trackColor || '#2a2a2a'};
      }
      .dynamic-form-scrollbar::-webkit-scrollbar-thumb {
        background: ${s.scrollbar?.thumbColor || 'rgba(212,175,55,0.6)'};
        border-radius: 3px;
      }
      .dynamic-form-scrollbar::-webkit-scrollbar-thumb:hover {
        background: ${s.scrollbar?.thumbHover || 'rgba(212,175,55,0.9)'};
      }
    `;
    
    const existingStyle = document.getElementById('dynamic-form-scrollbar-style');
    if (existingStyle) {
      document.head.removeChild(existingStyle);
    }
    
    style.id = 'dynamic-form-scrollbar-style';
    document.head.appendChild(style);
    
    return () => {
      if (document.head.contains(style)) {
        document.head.removeChild(style);
      }
    };
  }, [s.scrollbar]);

  // Keyboard escape handler
  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, [onClose]);

  const styles = useMemo(() => {
    console.log('🎨 JSON Style applied:', s.modal);
    
    const merge = (custom, base) => ({ 
      ...base,   // Structural defaults DULU
      ...custom  // JSON styles OVERRIDE
    });

    return {
      overlay: merge(
        s.overlay,
        {
          position: "fixed",
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          zIndex: 9999,
        }
      ),
      modal: merge(
        s.modal,
        {
          borderRadius: "12px",
          padding: "24px",
          width: "460px",
          maxHeight: "80vh",
          display: "flex",
          flexDirection: "column",
          overflow: "hidden",
          boxShadow: "0 10px 30px rgba(0,0,0,0.6)",
        }
      ),
      scrollArea: {
        flex: 1,
        overflowY: "auto",
        paddingRight: "8px",
        marginBottom: "16px",
      },
      label: merge(
        s.label,
        {
          fontSize: "13px",
          fontWeight: "500",
          marginBottom: "6px",
          display: "block",
        }
      ),
      input: merge(
        s.input,
        {
          padding: "10px 12px",
          borderRadius: "8px",
          fontSize: "14px",
          width: "100%",
          outline: "none",
          marginBottom: "12px",
          boxSizing: "border-box",
          transition: "all 0.2s ease",
        }
      ),
      inputFocus: merge(
        s.inputFocus,
        {
          // Empty base - semua dari JSON
        }
      ),
      tabButton: merge(
        s.tab?.default,
        {
          flex: 1,
          textAlign: "center",
          padding: "8px 12px",
          borderRadius: "6px",
          cursor: "pointer",
          border: "none",
          transition: "all 0.2s ease",
          fontSize: "13px",
          fontWeight: "500",
        }
      ),
      tabActive: merge(
        s.tab?.active,
        {
          // Empty base - semua dari JSON
        }
      ),
      buttonContainer: {
        display: "flex",
        justifyContent: "flex-end",
        gap: "8px",
        marginTop: "8px",
      },
    };
  }, [s]);

  // ✅ PERBAIKAN: Initialize form data - FIX untuk semua field termasuk conditional
  useEffect(() => {
    // Prioritaskan sumber data secara berurutan
    let savedData = {};
    let formStructure = null;

    // 1. Coba dari config.data (data yang sudah disimpan)
    if (componentData?.config?.data) {
      savedData = { ...componentData.config.data };
      formStructure = componentData.config;
      console.log('📥 Using config.data');
    }
    // 2. Coba dari form.data (data dari palette)
    else if (componentData?.form?.data) {
      savedData = { ...componentData.form.data };
      formStructure = componentData.form;
      console.log('📥 Using form.data');
    }
    // 3. Coba dari root form (fallback)
    else if (componentData?.form) {
      savedData = componentData.form.data || {};
      formStructure = componentData.form;
      console.log('📥 Using root form');
    }
    // 4. Coba dari root config (fallback terakhir)
    else if (componentData?.config) {
      savedData = componentData.config.data || {};
      formStructure = componentData.config;
      console.log('📥 Using root config');
    }

    console.log('🔄 Initial form data sources:', {
      savedData,
      formStructure: formStructure ? {
        hasTabs: !!formStructure.tabs,
        tabsCount: formStructure.tabs?.length,
        title: formStructure.title
      } : 'No structure found'
    });

    if (!formStructure?.tabs) {
      console.log('❌ No form structure found in component');
      return;
    }

    // ✅ PERBAIKAN: Collect SEMUA field keys termasuk conditional yang mungkin hidden
    const allFieldKeys = new Set();
    formStructure.tabs.forEach(tab => {
      tab.fields?.forEach(field => {
        allFieldKeys.add(field.key);
      });
    });

    console.log('📋 All field keys found:', Array.from(allFieldKeys));

    // ✅ PERBAIKAN: Initialize dengan semua field + existing values + defaults
    const initialData = {};
    
    // Process semua field yang ditemukan
    allFieldKeys.forEach(key => {
      // Pertahankan nilai yang sudah ada
      if (savedData[key] !== undefined) {
        initialData[key] = savedData[key];
        console.log(`✅ Retaining existing value for ${key}:`, savedData[key]);
      } else {
        // Cari field definition untuk default value
        const fieldDef = formStructure.tabs
          .flatMap(tab => tab.fields || [])
          .find(field => field.key === key);
        
        if (fieldDef?.default !== undefined) {
          initialData[key] = fieldDef.default;
          console.log(`✅ Set default value for ${key}:`, fieldDef.default);
        } else if (fieldDef?.type === 'checkbox') {
          initialData[key] = false;
          console.log(`✅ Set default checkbox for ${key}: false`);
        } else {
          initialData[key] = '';
          console.log(`✅ Set empty string for ${key}`);
        }
      }
    });

    // ✅ PERBAIKAN: Tambahkan emergency patch untuk field conditional yang critical
    const criticalFields = ['renameTo', 'moveTo'];
    criticalFields.forEach(field => {
      if (initialData[field] === undefined) {
        initialData[field] = '';
        console.log(`🚨 EMERGENCY: Added missing critical field ${field}`);
      }
    });

    console.log('✅ FINAL initialized form data (ALL fields):', initialData);
    setFormData(initialData);
    
    // Set active tab pertama
    if (!activeTab && formStructure.tabs.length > 0) {
      setActiveTab(formStructure.tabs[0].id);
    }
  }, [componentData]);

  const handleChange = useCallback((key, value) => {
    console.log(`✏️ Field changed: ${key} = ${value}`);
    setFormData(prev => ({ ...prev, [key]: value }));
  }, []);

  // ✅ PERBAIKAN: HandleSave dengan emergency patch untuk field conditional
  const handleSave = useCallback(() => {
    console.log('💾 Saving form data:', formData);
    
    // ✅ PERBAIKAN: Emergency patch - pastikan field critical selalu ada
    const finalFormData = { ...formData };
    const criticalFields = ['renameTo', 'moveTo'];
    
    criticalFields.forEach(field => {
      if (finalFormData[field] === undefined) {
        finalFormData[field] = '';
        console.log(`🚨 PATCH: Added missing ${field} before save`);
      }
    });

    console.log('💾 FINAL form data to save:', finalFormData);
    
    // Simpan ke BOTH config.data DAN form.data untuk konsistensi
    const updatedComponent = {
      ...componentData,
      config: {
        ...componentData.config,
        data: finalFormData, // Simpan values YANG SUDAH DIPATCH
        tabs: componentData.config?.tabs || componentData.form?.tabs, // Pertahankan structure
        title: componentData.config?.title || componentData.form?.title,
        buttons: componentData.config?.buttons || componentData.form?.buttons
      },
      form: {
        ...componentData.form,
        data: finalFormData, // Juga simpan di form.data YANG SUDAH DIPATCH
        tabs: componentData.form?.tabs || componentData.config?.tabs,
        title: componentData.form?.title || componentData.config?.title,
        buttons: componentData.form?.buttons || componentData.config?.buttons
      }
    };

    console.log('💾 Updated component after save:', {
      configData: updatedComponent.config?.data,
      formData: updatedComponent.form?.data,
      hasRenameTo: updatedComponent.config?.data?.renameTo !== undefined,
      hasMoveTo: updatedComponent.config?.data?.moveTo !== undefined
    });
    
    onSave(updatedComponent);
  }, [componentData, formData, onSave]);

  const shouldShowField = useCallback((field) => {
    if (!field.conditional) return true;
    
    const parentValue = formData[field.conditional.field];
    
    if (Array.isArray(field.conditional.value)) {
      return field.conditional.value.includes(parentValue);
    }
    
    if (field.conditional.operator === '!=') {
      return parentValue !== field.conditional.value;
    }
    
    return parentValue === field.conditional.value;
  }, [formData]);

  const getButtonStyle = useCallback((buttonType) => {
    const base = s.button?.base || {};
    const specific = s.button?.[buttonType] || {};
    
    return {
      // Structural defaults DULU
      padding: "8px 16px",
      borderRadius: "8px",
      cursor: "pointer",
      fontWeight: "600",
      transition: "all 0.2s ease",
      border: "none",
      fontSize: "13px",
      minWidth: "70px",
      
      // JSON OVERRIDE semua
      ...base,
      ...specific,
    };
  }, [s.button]);

  const renderField = useCallback((field) => {
    if (!shouldShowField(field)) {
      console.log(`👻 Field ${field.key} is hidden due to conditional`);
      return null;
    }

    const inputStyle = {
      ...styles.input,
      ...(focusedField === field.key ? styles.inputFocus : {})
    };

    const fieldId = `field-${field.key}`;
    
    const fieldValue = formData[field.key] ?? '';
    console.log(`📝 Rendering field ${field.key}:`, { 
      value: fieldValue, 
      type: field.type,
      existsInFormData: formData[field.key] !== undefined
    });

    switch (field.type) {
      case "text":
      case "number":
      case "password":
        return (
          <div key={field.key} style={{ marginBottom: "16px" }}>
            <label htmlFor={fieldId} style={styles.label}>
              {field.label}
            </label>
            <input
              id={fieldId}
              type={field.type}
              style={inputStyle}
              placeholder={field.placeholder || ""}
              value={fieldValue}
              onChange={(e) => handleChange(field.key, e.target.value)}
              onFocus={() => setFocusedField(field.key)}
              onBlur={() => setFocusedField(null)}
            />
          </div>
        );

      case "textarea":
        return (
          <div key={field.key} style={{ marginBottom: "16px" }}>
            <label htmlFor={fieldId} style={styles.label}>
              {field.label}
            </label>
            <textarea
              id={fieldId}
              rows={4}
              style={{ ...inputStyle, resize: "vertical" }}
              placeholder={field.placeholder || ""}
              value={fieldValue}
              onChange={(e) => handleChange(field.key, e.target.value)}
              onFocus={() => setFocusedField(field.key)}
              onBlur={() => setFocusedField(null)}
            />
          </div>
        );

      case "select":
        return (
          <div key={field.key} style={{ marginBottom: "16px" }}>
            <label htmlFor={fieldId} style={styles.label}>
              {field.label}
            </label>
            <select
              id={fieldId}
              style={inputStyle}
              value={fieldValue || field.default || ""}
              onChange={(e) => handleChange(field.key, e.target.value)}
              onFocus={() => setFocusedField(field.key)}
              onBlur={() => setFocusedField(null)}
            >
              <option value="">Select an option</option>
              {field.options?.map((opt, idx) => (
                <option key={idx} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </div>
        );

      case "checkbox":
        return (
          <div key={field.key} style={{ marginBottom: "16px" }}>
            <label style={{ 
              display: "flex", 
              alignItems: "center", 
              gap: "8px",
              cursor: "pointer",
              color: styles.input.color 
            }}>
              <input
                type="checkbox"
                checked={!!fieldValue}
                onChange={(e) => handleChange(field.key, e.target.checked)}
              />
              <span style={{ fontSize: "14px" }}>{field.label}</span>
            </label>
          </div>
        );

      default:
        return null;
    }
  }, [formData, focusedField, handleChange, shouldShowField, styles]);

  // Dapatkan struktur form dari config atau form
  const formStructure = componentData?.config || componentData?.form;
  const activeTabData = formStructure?.tabs?.find((tab) => tab.id === activeTab);
  const activeFields = activeTabData?.fields || [];

  if (!formStructure) {
    console.log('❌ No form structure found in componentData');
    return null;
  }

  console.log('🎯 Active tab data:', activeTabData);
  console.log('🎯 Active fields:', activeFields);
  console.log('🔍 Current formData state:', formData);

  return (
    <div style={styles.overlay} onClick={onClose}>
      <div 
        style={styles.modal} 
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-labelledby="dynamic-form-title"
        aria-modal="true"
      >
        <h3 
          id="dynamic-form-title"
          style={{ 
            marginBottom: "20px", 
            color: styles.label?.color,
            fontSize: "18px",
            fontWeight: "600"
          }}
        >
          {formStructure?.title || componentData.label}
        </h3>

        {/* Tab Content */}
        <div 
          style={styles.scrollArea}
          className="dynamic-form-scrollbar"
        >
          {activeTabData && (
            <>
              {activeFields.map(renderField)}
            </>
          )}
        </div>

        {/* Action Buttons */}
        <div style={styles.buttonContainer}>
          {formStructure.buttons?.map((btn, idx) => (
            <button
              key={idx}
              style={getButtonStyle(btn.action)}
              onClick={() => (btn.action === "save" ? handleSave() : onClose())}
            >
              {btn.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

export default DynamicForm;