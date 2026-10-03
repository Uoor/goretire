import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { ConfigProvider } from "antd";
import { BrowserRouter } from "react-router-dom";
import App from "./app/App";
import "antd/dist/reset.css";
import "../assets/site.scss";

const root = document.getElementById("root");

if (!root) throw new Error("Missing React root element");

createRoot(root).render(
  <StrictMode>
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "#ff6a00",
          colorText: "#211811",
          colorTextSecondary: "#74675e",
          colorBorder: "#ead9cc",
          colorBgContainer: "#fffdfa",
          borderRadius: 8,
          fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif'
        }
      }}
    >
      <BrowserRouter
        basename={__APP_BASE_PATH__}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <App />
      </BrowserRouter>
    </ConfigProvider>
  </StrictMode>
);