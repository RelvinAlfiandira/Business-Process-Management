import React from "react";
import { Modal } from "react-bootstrap";
import CloseButton from "../close-button/CloseButton";
import ButtonCustom from "../button/ButtonCustom";
import "./Confirmation.css"


function Confirmation({
    show,
    title = "",
    message = "",
    onConfirm,
    onCancel,
    children,
}) {
    if (!show) return null;

    return (
        <Modal className= "confirm-modal" show={show} onHide={onCancel} centered>
            <Modal.Header >
                <Modal.Title> <h5>{title}</h5> </Modal.Title>
                <CloseButton onClick={onCancel}/>

            </Modal.Header>

            <Modal.Body>
               {message && <p className="confirm-message"> {message} </p>}
               {children && <div className="confirm-children"> {children} </div>}
            </Modal.Body>

            <Modal.Footer>
                <ButtonCustom 
                label="Close"
                variant="secondary" 
                onClick={onCancel}>
                </ButtonCustom>
                
                <ButtonCustom 
                label="Confirm"
                variant="primary" 
                onClick={onConfirm}>
                </ButtonCustom>
            </Modal.Footer>
        </Modal>
    );
}

export default Confirmation;