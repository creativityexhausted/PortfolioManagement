import { apiClient } from "./apiClient";

export const authApi = {
  register: async (payload) => {
    const { data } = await apiClient.post("/api/auth/register", payload);
    return data;
  },
  login: async (payload) => {
    const { data } = await apiClient.post("/api/auth/login", payload);
    return data;
  },
};
