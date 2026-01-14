import React from "react";
import { useDrag } from "react-dnd";
import "./ComponentsItem.css";

/**
 * Props:
 * - id, label, icon, type, form
 *
 * NOTE: ComponentsItem hanya draggable — double click untuk membuka form
 * ditangani oleh CanvasArea (onSelectComponent).
 */
function ComponentsItem({ id, label, icon, type, form, style }) {
   console.log('🟡 ComponentsItem props - Style:', style); // Debug
  const [{ isDragging }, dragRef] = useDrag(
    () => ({
      type: "COMPONENT", 
      item: { originId: id, label, icon, type, form, style: style || {} },
      collect: (monitor) => ({
        isDragging: monitor.isDragging(),
      }),
    }),
    [id, label, icon, type, form, style]
  );

  const renderIcon = (ic) => {
    if (!ic) return <div className="ci-placeholder">📦</div>;
    if (typeof ic === "string" && ic.startsWith("data:image")) {
      return <img src={ic} alt={`${label} icon`} className="ci-img" />;
    }
    return <span className="ci-span">{ic}</span>;
  };

  return (
    <div
      ref={dragRef}
      className={`components-item ${isDragging ? "dragging" : ""}`}
      role="button"
      tabIndex={0}
      aria-label={label}
      aria-grabbed={isDragging}
    >
      <div className="components-item-inner">
        <div className="components-item-icon">{renderIcon(icon)}</div>
        <div className="components-item-label">{label}</div>
      </div>
    </div>
  );
}

export default ComponentsItem;
