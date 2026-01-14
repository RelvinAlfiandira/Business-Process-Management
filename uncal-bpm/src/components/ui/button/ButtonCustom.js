import React from 'react';
import { Button } from "react-bootstrap";
import "./ButtonCustom.css";


function ButtonCustom({label, onClick, variant= "primary", ...props}){
    return(
        <Button
            className={`btn btn-${variant}`}
            onClick={onClick}
            {...props}
        >
            {label  }
        </Button>
    );
}

export default ButtonCustom;