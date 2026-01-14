import { InfinitySpin } from "react-loader-spinner";

export default function Spinner() {
  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        width: "100%",
        height: "100%",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        zIndex: 9999,
      }}
    >
      <InfinitySpin 
        visible={true}
        width="200"
        color="#d4af37"
        ariaLabel="infinity-spin-loading"
      />
    </div>
  );
}
