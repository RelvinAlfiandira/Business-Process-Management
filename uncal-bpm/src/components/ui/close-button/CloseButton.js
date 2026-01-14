import React from "react";
import { FaTimes } from "react-icons/fa";
import "./CloseButton.css"

function CloseButton({ onClick }) {
    return (
        <div className="custom-close-button">
            <FaTimes className="custom-close-icon" onClick={onClick}/>
        </div>
    );
}

export default CloseButton;