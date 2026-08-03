import { apiClient } from "./apiClient";

export const portfolioApi = {
  getPortfolios: async () => {
    const { data } = await apiClient.get("/api/portfolios");
    return data;
  },
  createPortfolio: async (payload) => {
    const { data } = await apiClient.post("/api/portfolios", payload);
    return data;
  },
  updatePortfolio: async (id, payload) => {
    const { data } = await apiClient.put(`/api/portfolios/${id}`, payload);
    return data;
  },
  deletePortfolio: async (id) => {
    await apiClient.delete(`/api/portfolios/${id}`);
  },
  getHoldings: async (portfolioId = null) => {
    const { data } = await apiClient.get("/api/holdings", {
      params: portfolioId ? { portfolioId } : {},
    });
    return data;
  },
  createHolding: async (payload) => {
    const { data } = await apiClient.post("/api/holdings", payload);
    return data;
  },
  updateHolding: async (id, payload) => {
    const { data } = await apiClient.put(`/api/holdings/${id}`, payload);
    return data;
  },
  deleteHolding: async (id) => {
    await apiClient.delete(`/api/holdings/${id}`);
  },
  getTransactions: async (portfolioId = null) => {
    const { data } = await apiClient.get("/api/transactions", {
      params: portfolioId ? { portfolioId } : {},
    });
    return data;
  },
  createTransaction: async (payload) => {
    const { data } = await apiClient.post("/api/transactions", payload);
    return data;
  },
  updateTransaction: async (id, payload) => {
    const { data } = await apiClient.put(`/api/transactions/${id}`, payload);
    return data;
  },
  deleteTransaction: async (id) => {
    await apiClient.delete(`/api/transactions/${id}`);
  },
  getWatchlist: async (portfolioId = null) => {
    const { data } = await apiClient.get("/api/watchlist", {
      params: portfolioId ? { portfolioId } : {},
    });
    return data;
  },
  createWatchlistEntry: async (payload) => {
    const { data } = await apiClient.post("/api/watchlist", payload);
    return data;
  },
  updateWatchlistEntry: async (id, payload) => {
    const { data } = await apiClient.put(`/api/watchlist/${id}`, payload);
    return data;
  },
  deleteWatchlistEntry: async (id) => {
    await apiClient.delete(`/api/watchlist/${id}`);
  },
  getNews: async () => {
    const { data } = await apiClient.get("/api/news");
    return data;
  },
  refreshNews: async () => {
    const { data } = await apiClient.post("/api/news/refresh");
    return data;
  },
  getNewsPortfolioBrief: async (portfolioId = null) => {
    const { data } = await apiClient.get("/api/news/portfolio-brief", {
      params: portfolioId ? { portfolioId } : {},
    });
    return data;
  },
  getStockPrice: async (symbol) => {
    const { data } = await apiClient.get(`/api/stocks/${encodeURIComponent(symbol)}`);
    return data;
  },
  getStockCandles: async (symbol, { resolution = "D", days = 30 } = {}) => {
    const { data } = await apiClient.get(`/api/stocks/${encodeURIComponent(symbol)}/candles`, {
      params: { resolution, days },
    });
    return data;
  },
  getMarketIndices: async () => {
    const { data } = await apiClient.get("/api/market/indices");
    return data;
  },
};
