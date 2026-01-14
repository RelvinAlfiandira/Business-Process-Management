import React, { useState } from "react";
import "./SearchProject.css";

function SearchProject({ onSearch = () => {} }) {
  const [searchTerm, setSearchTerm] = useState("");

  const handleChange = (e) => {
    const value = e.target.value;
    setSearchTerm(value);
    onSearch(value); // kirim ke parent
  };

  return (
    <div className="search-container">
      <input
        type="text"
        placeholder="Search project..."
        value={searchTerm}
        onChange={handleChange}
      />
    </div>
  );
}

export default SearchProject;
