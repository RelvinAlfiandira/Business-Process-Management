// NavbarComponent.js
import React, { useState, useEffect } from "react";
import Container from "react-bootstrap/Container";
import Nav from "react-bootstrap/Nav";
import Navbar from "react-bootstrap/Navbar";
import NavDropdown from "react-bootstrap/NavDropdown";
import { FaUser, FaBars } from "react-icons/fa";
import "./NavbarComponent.css";
import OffCanvasCustom from "../offcanvas/OffCanvasCustom";

function NavbarComponent({
  folders,
  setFolders,
  files,
  setFiles,
  setContextMenu,
  setModal,
  onFileSelect,
  onLogout,
}) {
  const [showSidebar, setShowSidebar] = useState(false);
  const [username, setUsername] = useState("User");
  
  // Mengambil username dari localStorage
  useEffect(() => {
    const storedUser = localStorage.getItem("authUser");
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        if (userData.username) {
          setUsername(userData.username);
        }
      } catch (error) {
        console.error("Error parsing user data:", error);
      }
    }
  }, []);

  return (
    <>
      <Navbar expand="lg" className="bg-darkmode">
        <Container>
          <FaBars
            size={22}
            style={{ cursor: "pointer", marginRight: "1rem" }}
            onClick={() => setShowSidebar(true)}
          />

          <Navbar.Brand href="#home">
            <strong>UNCAL BPM</strong>
          </Navbar.Brand>

          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="ms-auto">
              <NavDropdown
                title={
                  <span className="d-flex align-items-center">
                    <FaUser style={{ marginRight: "6px" }} /> {username}
                  </span>
                }
                align="end"
              >
                <NavDropdown.Item href="#action/3.1">Edit Profile</NavDropdown.Item>
                <NavDropdown.Item
                  onClick={() =>
                    setModal({
                      show: true,
                      type: "confirmLogout",
                      title: "Konfirmasi Logout",
                      message: "Apakah Anda yakin ingin keluar dari sistem?",
                      onConfirm: onLogout,
                    })
                  }
                >
                  Log Out
                </NavDropdown.Item>
              </NavDropdown>
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <OffCanvasCustom
        show={showSidebar}
        setShow={setShowSidebar}
        folders={folders}
        setFolders={setFolders}
        files={files}
        setFiles={setFiles}
        onFileSelect={onFileSelect}
        setContextMenu={setContextMenu}
        setModal={setModal}
      />
    </>
  );
}

export default NavbarComponent;
