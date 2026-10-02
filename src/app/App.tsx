import { Navigate, Route, Routes } from "react-router-dom";
import AliPage from "../pages/AliPage";
import HomePage from "../pages/HomePage";
import NotFoundPage from "../pages/NotFoundPage";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/ali" element={<AliPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
