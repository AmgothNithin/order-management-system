import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/v1/auth": "http://localhost:8081",
      "/api/v1/orders": "http://localhost:8081",
      "/api/v1/inventory": "http://localhost:8083",
    },
  },
});
