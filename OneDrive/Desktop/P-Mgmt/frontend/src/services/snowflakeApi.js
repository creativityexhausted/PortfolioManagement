import axios from "axios";

// The fundamentals microservice is a completely separate service (own port, own deploy
// unit) from the main portfolio-manager backend, so it gets its own dedicated Axios
// instance and base URL rather than reusing `apiClient` from apiClient.js.
const SNOWFLAKE_BASE_URL = import.meta.env.VITE_SNOWFLAKE_API_URL || "http://localhost:8081";

const snowflakeClient = axios.create({
  baseURL: SNOWFLAKE_BASE_URL,
  headers: { "Content-Type": "application/json" },
  timeout: 15000,
});

export const snowflakeApi = {
  /**
   * Fetches the 5-axis Snowflake fundamentals score for a symbol from the standalone
   * fundamentals-service microservice. Public endpoint, no auth required.
   */
  getSnowflake: async (symbol) => {
    const { data } = await snowflakeClient.get(`/api/snowflake/${encodeURIComponent(symbol)}`);
    return data;
  },
};
