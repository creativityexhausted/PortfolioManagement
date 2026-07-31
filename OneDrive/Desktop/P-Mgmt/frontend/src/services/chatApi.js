import { apiClient } from "./apiClient";

export const chatApi = {
  askPortfolioAssistant: async ({ message, portfolioId = null }) => {
    const { data } = await apiClient.post("/api/chat/portfolio-assistant", {
      message,
      portfolioId,
    });
    return data;
  },
};
