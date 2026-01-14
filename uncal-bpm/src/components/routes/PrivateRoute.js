// src/routes/PrivateRoute.js
import React, { useContext } from "react";
import { Navigate, Outlet } from "react-router-dom";
import { AuthContext } from "../../context/AuthContext";

import Spinner from "../Spinner";

const PrivateRoute = () => {
  const { token, loading } = useContext(AuthContext);

  if (loading) {
    return (
      <>
        <Outlet />   {/* Dashboard layout tetap di-render */}
        <Spinner />
      </>
    );
  }

  return token ? <Outlet /> : <Navigate to="/login" replace />;
};

export default PrivateRoute;
