import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    strictPort: true,
    watch: {
      usePolling: true  // needed for file changes in Docker on Windows
    }
  }
});
