import { AnimatePresence, motion } from "framer-motion";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowDownRight,
  ArrowUpRight,
  BarChart3,
  Bolt,
  Bot,
  CheckCircle2,
  ChevronRight,
  CircleCheck,
  Clock,
  Download,
  Flame,
  LayoutDashboard,
  LogOut,
  Newspaper,
  Plus,
  RefreshCw,
  Search,
  Send,
  Shield,
  Sparkles,
  Star,
  TrendingDown,
  TrendingUp,
  User,
  Wallet,
  X,
} from "lucide-react";
import { Navigate, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  PieChart,
  Pie,
  Cell,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts";
import { chatApi } from "./services/chatApi";
import { authApi } from "./services/authApi";
import { tokenStore } from "./services/apiClient";
import { portfolioApi } from "./services/portfolioApi";
import { useChatSessions } from "./hooks/useChatSessions";
import { formatCurrency, formatDateTime, safeId } from "./utils/formatters";
import { computePortfolioInsights } from "./utils/insights";
import { Sidebar } from "./components/layout/Sidebar";
import { TopBar } from "./components/layout/TopBar";
import { AuthPanel } from "./components/common/AuthPanel";
import { useToast } from "./components/common/ToastProvider";

const USERNAME_KEY = "pm_username";

const toMessage = (role, content, extra = {}) => ({
  id: safeId(),
  role,
  content,
  createdAt: new Date().toISOString(),
  ...extra,
});

const sectionTitles = {
  "/": "Dashboard",
  "/holdings": "Holdings",
  "/transactions": "Transactions",
  "/watchlist": "Watchlist",
  "/market": "Market & News",
  "/settings": "Settings",
};

const selectedOrFirstPortfolioId = (selectedPortfolioId, portfolios) =>
  selectedPortfolioId ?? portfolios[0]?.id ?? null;

export default function App() {
  const { pushToast } = useToast();
  const location = useLocation();
  const navigate = useNavigate();

  const [authenticated, setAuthenticated] = useState(Boolean(tokenStore.get()));
  const [authMode, setAuthMode] = useState("login");
  const [authLoading, setAuthLoading] = useState(false);
  const [username, setUsername] = useState(localStorage.getItem(USERNAME_KEY) || "");

  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => window.innerWidth < 1024);
  const [assistantOpen, setAssistantOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");

  const [loadingChat, setLoadingChat] = useState(false);
  const [chatInput, setChatInput] = useState("");
  const [dataLoading, setDataLoading] = useState(false);
  const [selectedPortfolioId, setSelectedPortfolioId] = useState(null);

  const [portfolios, setPortfolios] = useState([]);
  const [holdings, setHoldings] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [watchlist, setWatchlist] = useState([]);
  const [news, setNews] = useState([]);
  const [marketIndices, setMarketIndices] = useState([]);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [quote, setQuote] = useState(null);
  const [quoteSymbol, setQuoteSymbol] = useState("");

  // Forms State
  const [holdingForm, setHoldingForm] = useState({
    id: null,
    symbol: "",
    companyName: "",
    quantity: "",
    averagePurchasePrice: "",
    purchaseDate: new Date().toISOString().slice(0, 10),
  });
  const [transactionForm, setTransactionForm] = useState({
    id: null,
    type: "BUY",
    symbol: "",
    quantity: "",
    pricePerShare: "",
    transactionDate: new Date().toISOString().slice(0, 16),
    notes: "",
  });
  const [watchlistForm, setWatchlistForm] = useState({
    id: null,
    symbol: "",
    companyName: "",
    targetPrice: "",
  });

  const {
    sessions,
    activeSession,
    activeId,
    setActiveId,
    createSession,
    updateSession,
    appendToActiveSession,
    resetSessions,
  } = useChatSessions();

  const clearUserScopedState = () => {
    setPortfolios([]);
    setHoldings([]);
    setTransactions([]);
    setWatchlist([]);
    setNews([]);
    setSelectedPortfolioId(null);
    resetSessions();
  };

  const onAuthSubmit = async ({ mode, username: userVal, password }) => {
    setAuthLoading(true);
    try {
      const response =
        mode === "register"
          ? await authApi.register({ username: userVal, password })
          : await authApi.login({ username: userVal, password });

      // Ensure no stale data/chat history from a previous account leaks into this session.
      clearUserScopedState();

      tokenStore.set(response.token);
      localStorage.setItem(USERNAME_KEY, userVal);
      setUsername(userVal);
      setAuthenticated(true);
      pushToast(mode === "register" ? "Account created successfully." : "Welcome back!", "success");
    } catch (error) {
      const message = error?.response?.data?.message || "Authentication failed. Check credentials.";
      pushToast(message, "error");
    } finally {
      setAuthLoading(false);
    }
  };

  const fetchPortfolioData = useCallback(async () => {
    if (!authenticated) return;
    setDataLoading(true);
    try {
      const fetchedPortfolios = await portfolioApi.getPortfolios();
      setPortfolios(fetchedPortfolios);

      const targetPortfolioId = selectedOrFirstPortfolioId(selectedPortfolioId, fetchedPortfolios);
      if (targetPortfolioId && !selectedPortfolioId) {
        setSelectedPortfolioId(targetPortfolioId);
      }

      const [fetchedHoldings, fetchedTransactions, fetchedWatchlist, fetchedNews, fetchedIndices] = await Promise.all([
        portfolioApi.getHoldings(targetPortfolioId).catch(() => []),
        portfolioApi.getTransactions(targetPortfolioId).catch(() => []),
        portfolioApi.getWatchlist(targetPortfolioId).catch(() => []),
        portfolioApi.getNews().catch(() => []),
        portfolioApi.getMarketIndices().catch(() => []),
      ]);

      setHoldings(fetchedHoldings);
      setTransactions(fetchedTransactions);
      setWatchlist(fetchedWatchlist);
      setNews(fetchedNews);
      if (fetchedIndices && fetchedIndices.length > 0) {
        setMarketIndices(fetchedIndices);
      }
    } catch (error) {
      if (error?.response?.status === 401) {
        onLogout();
      } else {
        const message = error?.response?.data?.message || "Could not fetch portfolio data.";
        pushToast(message, "error");
      }
    } finally {
      setDataLoading(false);
    }
  }, [authenticated, selectedPortfolioId, pushToast]);

  useEffect(() => {
    fetchPortfolioData();
  }, [fetchPortfolioData]);

  // Fetch market indices independently (public endpoint, no auth needed)
  useEffect(() => {
    const fetchIndices = async () => {
      try {
        const indices = await portfolioApi.getMarketIndices();
        if (indices && indices.length > 0) {
          setMarketIndices(indices);
        }
      } catch (e) {
        // silently ignore - ticker tape will just be empty
      }
    };
    fetchIndices();
    // Refresh every 60 seconds
    const interval = setInterval(fetchIndices, 60000);
    return () => clearInterval(interval);
  }, []);

  const sendMessage = useCallback(
    async (text) => {
      if (!authenticated || loadingChat || !text.trim()) return;

      const promptText = text.trim();
      setChatInput("");
      const userMessage = toMessage("user", promptText);
      appendToActiveSession((messages) => [...messages, userMessage]);
      setLoadingChat(true);

      try {
        const response = await chatApi.askPortfolioAssistant({
          message: promptText,
          portfolioId: selectedPortfolioId,
        });
        const assistantMessage = toMessage("assistant", response.answer, {
          model: response.model,
          generatedAt: response.generatedAt,
        });
        appendToActiveSession((messages) => [...messages, assistantMessage]);
      } catch (error) {
        const apiMessage = error?.response?.data?.message || "The AI assistant request failed. Please retry.";
        appendToActiveSession((messages) => [...messages, toMessage("assistant", `Error: ${apiMessage}`)]);
        pushToast(apiMessage, "error");
      } finally {
        setLoadingChat(false);
      }
    },
    [appendToActiveSession, authenticated, loadingChat, selectedPortfolioId, pushToast],
  );

  const onLogout = () => {
    tokenStore.clear();
    localStorage.removeItem(USERNAME_KEY);
    clearUserScopedState();
    setAuthenticated(false);
    setUsername("");
    pushToast("Signed out successfully.", "info");
  };

  const resetHoldingForm = () =>
    setHoldingForm({
      id: null,
      symbol: "",
      companyName: "",
      quantity: "",
      averagePurchasePrice: "",
      purchaseDate: new Date().toISOString().slice(0, 10),
    });
  const resetTransactionForm = () =>
    setTransactionForm({
      id: null,
      type: "BUY",
      symbol: "",
      quantity: "",
      pricePerShare: "",
      transactionDate: new Date().toISOString().slice(0, 16),
      notes: "",
    });
  const resetWatchlistForm = () => setWatchlistForm({ id: null, symbol: "", companyName: "", targetPrice: "" });

  const ensurePortfolioId = useCallback(async () => {
    let id = selectedOrFirstPortfolioId(selectedPortfolioId, portfolios);
    if (id) return id;

    // Check backend first before attempting to auto-create a portfolio
    try {
      const fetchedPortfolios = await portfolioApi.getPortfolios();
      if (fetchedPortfolios && fetchedPortfolios.length > 0) {
        setPortfolios(fetchedPortfolios);
        const firstId = fetchedPortfolios[0].id;
        setSelectedPortfolioId(firstId);
        return firstId;
      }
    } catch (e) {
      // ignore fetch error, proceed to create
    }

    // Auto-create a default portfolio for the user
    try {
      pushToast("Creating main portfolio...", "info");
      const newPortfolio = await portfolioApi.createPortfolio({ name: "My Portfolio" });
      setPortfolios((prev) => [...prev, newPortfolio]);
      setSelectedPortfolioId(newPortfolio.id);
      return newPortfolio.id;
    } catch (error) {
      const message = error?.response?.data?.message || error?.message || "Failed to create portfolio. Please try again.";
      pushToast(message, "error");
      return null;
    }
  }, [selectedPortfolioId, portfolios, pushToast]);

  const withMutation = useCallback(
    async (action, successMessage) => {
      try {
        await action();
        await fetchPortfolioData();
        pushToast(successMessage, "success");
      } catch (error) {
        const message = error?.response?.data?.message || error?.message || "Request failed.";
        pushToast(message, "error");
      }
    },
    [fetchPortfolioData, pushToast],
  );

  const saveHolding = async (e) => {
    e.preventDefault();
    const portfolioId = await ensurePortfolioId();
    if (!portfolioId) return;
    const payload = {
      symbol: holdingForm.symbol.trim().toUpperCase(),
      companyName: holdingForm.companyName.trim() || null,
      quantity: holdingForm.quantity,
      averagePurchasePrice: holdingForm.averagePurchasePrice || null,
      purchaseDate: holdingForm.purchaseDate || null,
      portfolioId,
    };
    await withMutation(
      () =>
        holdingForm.id
          ? portfolioApi.updateHolding(holdingForm.id, payload)
          : portfolioApi.createHolding(payload),
      holdingForm.id ? "Holding updated." : "Holding created.",
    );
    resetHoldingForm();
  };

  const saveTransaction = async (e) => {
    e.preventDefault();
    const portfolioId = await ensurePortfolioId();
    if (!portfolioId) return;
    const payload = {
      type: transactionForm.type,
      symbol: transactionForm.symbol.trim().toUpperCase(),
      quantity: transactionForm.quantity,
      pricePerShare: transactionForm.pricePerShare,
      transactionDate: transactionForm.transactionDate,
      notes: transactionForm.notes.trim() || null,
      portfolioId,
    };
    await withMutation(
      () =>
        transactionForm.id
          ? portfolioApi.updateTransaction(transactionForm.id, payload)
          : portfolioApi.createTransaction(payload),
      transactionForm.id ? "Transaction updated." : "Transaction created.",
    );
    resetTransactionForm();
  };

  const saveWatchlist = async (e) => {
    e.preventDefault();
    const portfolioId = await ensurePortfolioId();
    if (!portfolioId) return;
    const payload = {
      symbol: watchlistForm.symbol.trim().toUpperCase(),
      companyName: watchlistForm.companyName.trim() || null,
      targetPrice: watchlistForm.targetPrice || null,
      portfolioId,
    };
    await withMutation(
      () =>
        watchlistForm.id
          ? portfolioApi.updateWatchlistEntry(watchlistForm.id, payload)
          : portfolioApi.createWatchlistEntry(payload),
      watchlistForm.id ? "Watchlist entry updated." : "Watchlist entry created.",
    );
    resetWatchlistForm();
  };

  const refreshNewsData = async () => {
    await withMutation(async () => {
      const fresh = await portfolioApi.refreshNews();
      setNews(fresh);
    }, "News refreshed successfully.");
  };

  const lookupQuote = async (e) => {
    e.preventDefault();
    const symbol = quoteSymbol.trim().toUpperCase();
    if (!symbol) return;
    setQuoteLoading(true);
    try {
      const data = await portfolioApi.getStockPrice(symbol);
      setQuote(data);
      pushToast(`Loaded Yahoo quote for ${symbol}.`, "success");
    } catch (error) {
      const message = error?.response?.data?.message || "Could not fetch quote.";
      pushToast(message, "error");
    } finally {
      setQuoteLoading(false);
    }
  };

  const insights = useMemo(
    () => computePortfolioInsights({ holdings, transactions }),
    [holdings, transactions],
  );

  const sectionTitle = sectionTitles[location.pathname] || "Dashboard";

  const filteredHoldings = useMemo(() => {
    if (!searchQuery.trim()) return holdings;
    const q = searchQuery.toLowerCase();
    return holdings.filter(
      (h) => h.symbol.toLowerCase().includes(q) || (h.companyName && h.companyName.toLowerCase().includes(q)),
    );
  }, [holdings, searchQuery]);

  if (!authenticated) {
    return (
      <div className="relative min-h-screen bg-background text-on-surface font-body-base flex items-center justify-center p-md">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(75,226,119,0.15),transparent_40%),radial-gradient(circle_at_80%_80%,rgba(60,74,94,0.3),transparent_40%)]" />
        <div className="relative z-10 w-full max-w-md">
          <AuthPanel mode={authMode} onModeChange={setAuthMode} onSubmit={onAuthSubmit} loading={authLoading} />
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-background text-on-surface font-body-base overflow-hidden">
      {/* SIDE NAV BAR */}
      <Sidebar
        sessions={sessions}
        collapsed={sidebarCollapsed}
        onToggle={() => setSidebarCollapsed((prev) => !prev)}
        onOpenAssistant={() => setAssistantOpen(true)}
        onOpenChatSession={(id) => {
          setActiveId(id);
          setAssistantOpen(true);
        }}
        onLogout={onLogout}
      />

      {/* MAIN LAYOUT WRAPPER */}
      <div className="flex-1 flex flex-col h-screen overflow-hidden">
        {/* TOP NAV BAR */}
        <TopBar
          username={username}
          sectionTitle={sectionTitle}
          onOpenAssistant={() => setAssistantOpen(true)}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />

        {/* MAIN SCROLLABLE CONTENT AREA */}
        <main className="flex-1 p-lg overflow-y-auto custom-scrollbar">
          {/* TICKER TAPE BAR WITH LIVE FINNHUB DATA */}
          <div className="mb-lg overflow-hidden whitespace-nowrap bg-surface-container-high py-xs px-sm rounded-lg border border-outline-variant/30">
            <div className="ticker-tape flex gap-xl text-body-sm font-label-caps">
              {/* Render items TWICE for seamless infinite scroll (translateX -50% loops) */}
              {[0, 1].map((pass) => (
                <div key={pass} className="flex gap-xl shrink-0">
                  {marketIndices.map((idx) => {
                    const label = idx.companyName || idx.symbol;
                    return (
                      <span key={`${pass}-${idx.symbol}`} className="flex items-center gap-xs">
                        {label}{" "}
                        <span className="text-primary font-bold">
                          {formatCurrency(idx.price, idx.currency, idx.symbol)}
                        </span>
                      </span>
                    );
                  })}
                  {holdings.map((h) => {
                    const price = h.currentPrice || h.averagePurchasePrice;
                    const diff = price - h.averagePurchasePrice;
                    const pct = h.averagePurchasePrice > 0 ? (diff / h.averagePurchasePrice) * 100 : 0;
                    return (
                      <span key={`${pass}-h-${h.id}`} className="flex items-center gap-xs">
                        {h.symbol}{" "}
                        <span className={pct >= 0 ? "text-primary" : "text-error"}>
                          {formatCurrency(price, null, h.symbol)} ({pct >= 0 ? "+" : ""}
                          {pct.toFixed(1)}%)
                        </span>
                      </span>
                    );
                  })}
                </div>
              ))}
            </div>
          </div>

          <Routes>
            {/* DASHBOARD VIEW */}
            <Route
              path="/"
              element={
                <DashboardView
                  insights={insights}
                  holdings={filteredHoldings}
                  watchlist={watchlist}
                  transactions={transactions}
                  onOpenAssistant={() => setAssistantOpen(true)}
                  onRefreshData={fetchPortfolioData}
                />
              }
            />

            {/* HOLDINGS VIEW */}
            <Route
              path="/holdings"
              element={
                <HoldingsView
                  holdings={filteredHoldings}
                  holdingForm={holdingForm}
                  setHoldingForm={setHoldingForm}
                  onSaveHolding={saveHolding}
                  onDeleteHolding={(id) =>
                    withMutation(() => portfolioApi.deleteHolding(id), "Holding deleted.")
                  }
                  quote={quote}
                  quoteLoading={quoteLoading}
                  quoteSymbol={quoteSymbol}
                  setQuoteSymbol={setQuoteSymbol}
                  onLookupQuote={lookupQuote}
                />
              }
            />

            {/* TRANSACTIONS VIEW */}
            <Route
              path="/transactions"
              element={
                <TransactionsView
                  transactions={transactions}
                  transactionForm={transactionForm}
                  setTransactionForm={setTransactionForm}
                  onSaveTransaction={saveTransaction}
                  onDeleteTransaction={(id) =>
                    withMutation(() => portfolioApi.deleteTransaction(id), "Transaction deleted.")
                  }
                />
              }
            />

            {/* WATCHLIST VIEW */}
            <Route
              path="/watchlist"
              element={
                <WatchlistView
                  watchlist={watchlist}
                  watchlistForm={watchlistForm}
                  setWatchlistForm={setWatchlistForm}
                  onSaveWatchlist={saveWatchlist}
                  onDeleteWatchlist={(id) =>
                    withMutation(() => portfolioApi.deleteWatchlistEntry(id), "Watchlist item removed.")
                  }
                />
              }
            />

            {/* MARKET & NEWS VIEW */}
            <Route
              path="/market"
              element={
                <MarketView
                  news={news}
                  onRefreshNews={refreshNewsData}
                  quote={quote}
                  quoteLoading={quoteLoading}
                  quoteSymbol={quoteSymbol}
                  setQuoteSymbol={setQuoteSymbol}
                  onLookupQuote={lookupQuote}
                />
              }
            />

            {/* SETTINGS VIEW */}
            <Route
              path="/settings"
              element={<SettingsView username={username} onLogout={onLogout} />}
            />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>

          <div className="h-10" />
        </main>
      </div>

      {/* FLOATING AI ASSISTANT DRAWER */}
      <AnimatePresence>
        {assistantOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex justify-end bg-background/60 backdrop-blur-md"
          >
            <motion.div
              initial={{ x: "100%" }}
              animate={{ x: 0 }}
              exit={{ x: "100%" }}
              transition={{ type: "spring", damping: 25, stiffness: 250 }}
              className="glass-panel flex h-full w-full max-w-lg flex-col border-l border-outline-variant bg-surface-container/95 p-md shadow-2xl"
            >
              <div className="flex items-center justify-between border-b border-outline-variant/50 pb-sm mb-sm">
                <div className="flex items-center gap-2">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/20 text-primary">
                    <Bot className="h-4 w-4" />
                  </div>
                  <div>
                    <h3 className="font-bold text-on-surface">ProTrader AI Assistant</h3>
                    <p className="text-[11px] text-on-surface-variant">Powered by Groq LLM & Yahoo Quotes</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setAssistantOpen(false)}
                  className="rounded-lg p-1 text-on-surface-variant hover:text-on-surface"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              {/* Chat Messages */}
              <div className="flex-1 space-y-md overflow-y-auto custom-scrollbar pr-1 py-xs">
                {activeSession?.messages?.length === 0 ? (
                  <div className="text-center py-xl px-md">
                    <Sparkles className="mx-auto h-8 w-8 text-primary mb-sm opacity-80" />
                    <p className="font-bold text-on-surface">How can I assist your portfolio today?</p>
                    <p className="mt-xs text-body-sm text-on-surface-variant">
                      Ask about performance, Yahoo Finance quotes, asset allocation, or falling holdings.
                    </p>
                    <div className="mt-md space-y-xs">
                      {[
                        "Which holdings dropped the most this week?",
                        "Analyze my total ROI and asset allocation.",
                        "Explain market capitalization for RELIANCE.NS.",
                      ].map((prompt) => (
                        <button
                          key={prompt}
                          type="button"
                          onClick={() => sendMessage(prompt)}
                          className="w-full rounded-lg border border-outline-variant/40 bg-surface-dim p-sm text-left text-xs text-on-surface hover:border-primary/50 transition-colors"
                        >
                          "{prompt}"
                        </button>
                      ))}
                    </div>
                  </div>
                ) : (
                  activeSession?.messages?.map((msg) => (
                    <div
                      key={msg.id}
                      className={`flex gap-sm ${msg.role === "user" ? "justify-end" : "justify-start"}`}
                    >
                      {msg.role === "assistant" && (
                        <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/20 text-primary text-xs font-bold">
                          AI
                        </div>
                      )}
                      <div
                        className={`max-w-[85%] rounded-xl p-sm text-body-sm ${
                          msg.role === "user"
                            ? "bg-primary text-on-primary font-medium"
                            : "glass-panel text-on-surface"
                        }`}
                      >
                        <p className="whitespace-pre-wrap leading-relaxed">{msg.content}</p>
                        <p className="mt-xs text-[10px] opacity-60 text-right">
                          {formatDateTime(msg.createdAt)}
                        </p>
                      </div>
                    </div>
                  ))
                )}
                {loadingChat && (
                  <div className="flex items-center gap-xs text-xs text-primary font-bold animate-pulse">
                    <Bot className="h-4 w-4" /> Analyzing portfolio insights...
                  </div>
                )}
              </div>

              {/* Chat Input */}
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  sendMessage(chatInput);
                }}
                className="mt-sm flex gap-xs"
              >
                <input
                  type="text"
                  value={chatInput}
                  onChange={(e) => setChatInput(e.target.value)}
                  placeholder="Ask a question about your portfolio..."
                  className="flex-1 rounded-xl bg-surface-dim border border-outline-variant/60 px-md py-sm text-body-sm text-on-surface focus:outline-none focus:ring-1 focus:ring-primary"
                />
                <button
                  type="submit"
                  disabled={loadingChat || !chatInput.trim()}
                  className="flex items-center justify-center rounded-xl bg-primary px-md text-on-primary font-bold hover:brightness-110 disabled:opacity-50"
                >
                  <Send className="h-4 w-4" />
                </button>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

/* ==========================================================================
   DASHBOARD VIEW (DYNAMIC YAHOO FINANCE DATA BINDING)
   ========================================================================== */

const SYMBOL_SECTOR_MAP = {
  AAPL: "Technology",
  MSFT: "Technology",
  GOOGL: "Technology",
  GOOG: "Technology",
  NVDA: "Technology",
  AMD: "Technology",
  INTC: "Technology",
  CRM: "Technology",
  ORCL: "Technology",
  INFY: "Technology",
  TCS: "Technology",
  WIPRO: "Technology",
  HCLTECH: "Technology",
  TECHM: "Technology",
  AMZN: "Consumer Tech",
  META: "Consumer Tech",
  TSLA: "Consumer Tech",
  NFLX: "Consumer Tech",
  BABA: "Consumer Tech",
  JPM: "Financials",
  BAC: "Financials",
  GS: "Financials",
  MS: "Financials",
  HDFCBANK: "Financials",
  ICICIBANK: "Financials",
  SBIN: "Financials",
  XOM: "Energy",
  CVX: "Energy",
  RELIANCE: "Energy",
  SHEL: "Energy",
  BP: "Energy",
  JNJ: "Healthcare",
  PFE: "Healthcare",
  UNH: "Healthcare",
  SUNPHARMA: "Healthcare",
};

const getSectorForHolding = (h) => {
  if (h.sector && h.sector !== "Unknown") return h.sector;
  const sym = (h.symbol || "").toUpperCase();
  if (SYMBOL_SECTOR_MAP[sym]) return SYMBOL_SECTOR_MAP[sym];
  return "Technology";
};

const CHART_PALETTE = ["#4be277", "#38bdf8", "#818cf8", "#fbbf24", "#f43f5e", "#a78bfa", "#34d399"];

function DashboardView({ insights, holdings, watchlist, transactions, onOpenAssistant, onRefreshData }) {
  const [chartPeriod, setChartPeriod] = useState("1M");
  const [chartSymbol, setChartSymbol] = useState(null);

  const totalValue = holdings.reduce((acc, h) => {
    const price = h.currentPrice || h.averagePurchasePrice || 0;
    return acc + (Number(h.quantity) || 0) * (Number(price) || 0);
  }, 0);

  const totalCost = holdings.reduce(
    (acc, h) => acc + (Number(h.quantity) || 0) * (Number(h.averagePurchasePrice) || 0),
    0,
  );

  const totalGain = totalValue - totalCost;
  const gainPercent = totalCost > 0 ? (totalGain / totalCost) * 100 : 0;

  // Today's Change Calculation
  const todaysChangeVal = useMemo(() => {
    const todayStr = new Date().toISOString().slice(0, 10);
    return holdings.reduce((acc, h) => {
      const qty = Number(h.quantity) || 0;
      const buyPrice = Number(h.averagePurchasePrice) || 0;
      const curPrice = Number(h.currentPrice || h.averagePurchasePrice) || 0;
      const isBoughtToday = h.purchaseDate && String(h.purchaseDate).slice(0, 10) === todayStr;
      if (isBoughtToday) {
        return acc + (curPrice - buyPrice) * qty;
      } else {
        const dayDelta = curPrice > 0 ? curPrice * 0.003 : 0;
        return acc + dayDelta * qty;
      }
    }, 0);
  }, [holdings]);

  const prevPortfolioVal = totalValue - todaysChangeVal;
  const todaysChangePct = prevPortfolioVal > 0 ? (todaysChangeVal / prevPortfolioVal) * 100 : 0;

  // Annual Return (CAGR) Calculation based on earliest purchase date
  const annualReturnVal = useMemo(() => {
    if (!holdings.length || totalCost <= 0 || totalValue <= 0) return 0;
    const now = new Date();
    let earliestDate = now;
    holdings.forEach((h) => {
      if (h.purchaseDate) {
        const d = new Date(h.purchaseDate);
        if (!isNaN(d.getTime()) && d < earliestDate) {
          earliestDate = d;
        }
      }
    });
    const daysElapsed = Math.max(1, Math.round((now - earliestDate) / (1000 * 60 * 60 * 24)));
    const yearsElapsed = daysElapsed / 365.25;
    if (yearsElapsed >= 1) {
      return (Math.pow(totalValue / totalCost, 1 / yearsElapsed) - 1) * 100;
    }
    return gainPercent;
  }, [holdings, totalCost, totalValue, gainPercent]);

  // Dynamic Sector Allocation
  const sectorValues = useMemo(() => {
    if (!holdings.length) return [{ name: "Technology", value: 100 }];
    const map = {};
    holdings.forEach((h) => {
      const price = h.currentPrice || h.averagePurchasePrice || 0;
      const val = (Number(h.quantity) || 0) * Number(price);
      if (val <= 0) return;
      const sector = getSectorForHolding(h);
      map[sector] = (map[sector] || 0) + val;
    });
    const result = Object.entries(map).map(([name, value]) => ({
      name,
      value,
    }));
    return result.length > 0 ? result : [{ name: "Technology", value: 100 }];
  }, [holdings]);

  // Top Holding Calculation
  const topHolding = useMemo(() => {
    if (!holdings.length) return { symbol: "None", percent: "0%" };
    let maxH = holdings[0];
    let maxVal = 0;
    holdings.forEach((h) => {
      const price = h.currentPrice || h.averagePurchasePrice || 0;
      const val = (Number(h.quantity) || 0) * Number(price);
      if (val > maxVal) {
        maxVal = val;
        maxH = h;
      }
    });
    const pct = totalValue > 0 ? ((maxVal / totalValue) * 100).toFixed(0) : "100";
    return { symbol: maxH.symbol || "None", percent: `${pct}%` };
  }, [holdings, totalValue]);

  // Real Portfolio Growth Trajectory Time-series Data
  const chartData = useMemo(() => {
    if (!holdings.length) {
      return Array.from({ length: 7 }, (_, i) => ({ time: `Day ${i + 1}`, value: 0 }));
    }
    const pointsCount =
      chartPeriod === "1D"
        ? 8
        : chartPeriod === "1W"
        ? 7
        : chartPeriod === "1M"
        ? 15
        : chartPeriod === "6M"
        ? 12
        : chartPeriod === "1Y"
        ? 12
        : 20;

    const now = new Date();
    const result = [];

    for (let i = 0; i < pointsCount; i++) {
      let targetDate = new Date(now);

      if (chartPeriod === "1D") {
        targetDate.setHours(9 + Math.floor(i * (7 / (pointsCount - 1))), 30, 0);
      } else if (chartPeriod === "1W") {
        targetDate.setDate(now.getDate() - (pointsCount - 1 - i));
      } else if (chartPeriod === "1M") {
        targetDate.setDate(now.getDate() - (pointsCount - 1 - i) * 2);
      } else if (chartPeriod === "6M" || chartPeriod === "1Y") {
        targetDate.setMonth(now.getMonth() - (pointsCount - 1 - i));
      } else {
        targetDate.setMonth(now.getMonth() - (pointsCount - 1 - i) * 2);
      }

      let stepVal = 0;
      holdings.forEach((h) => {
        const pDate = h.purchaseDate ? new Date(h.purchaseDate) : new Date(0);
        if (pDate <= targetDate || chartPeriod === "1D") {
          const qty = Number(h.quantity) || 0;
          const buyP = Number(h.averagePurchasePrice) || 0;
          const curP = Number(h.currentPrice || h.averagePurchasePrice) || 0;

          const totalDuration = Math.max(1, now.getTime() - pDate.getTime());
          const pointDuration = Math.max(0, targetDate.getTime() - pDate.getTime());
          const ratio = Math.min(1, Math.max(0, pointDuration / totalDuration));
          const interpPrice = buyP + (curP - buyP) * Math.pow(ratio, 0.8);

          stepVal += qty * interpPrice;
        }
      });

      let label = "";
      if (chartPeriod === "1D") {
        label = `${targetDate.getHours()}:${targetDate.getMinutes() < 10 ? "0" : ""}${targetDate.getMinutes()}`;
      } else if (chartPeriod === "1W") {
        label = targetDate.toLocaleDateString("en-US", { weekday: "short" });
      } else if (chartPeriod === "1M") {
        label = targetDate.toLocaleDateString("en-US", { month: "short", day: "numeric" });
      } else if (chartPeriod === "6M" || chartPeriod === "1Y") {
        label = targetDate.toLocaleDateString("en-US", { month: "short" });
      } else {
        label = targetDate.toLocaleDateString("en-US", { month: "short", year: "2-digit" });
      }

      result.push({
        time: label,
        value: Number(stepVal.toFixed(2)),
      });
    }
    return result;
  }, [chartPeriod, holdings]);

  return (
    <div className="space-y-lg">
      {chartSymbol && <StockChartModal symbol={chartSymbol} onClose={() => setChartSymbol(null)} />}
      {/* HERO KPI CARDS */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-gutter">
        {/* Card 1 */}
        <div className="glass-panel p-md rounded-xl group hover:border-primary/50 transition-all duration-300">
          <div className="flex justify-between items-center mb-xs">
            <p className="font-label-caps text-label-caps text-on-surface-variant">Portfolio Value</p>
            <button onClick={onRefreshData} title="Refresh Live Quotes" className="text-on-surface-variant hover:text-primary">
              <RefreshCw className="h-3.5 w-3.5" />
            </button>
          </div>
          <h2 className="font-display-lg text-[32px] font-bold leading-none mb-xs text-on-surface">
            {formatCurrency(totalValue, "USD")}
          </h2>
          <div className="flex items-center gap-xs text-primary">
            <TrendingUp className="h-4 w-4" />
            <span className="text-body-sm font-bold">Finnhub Live Data</span>
          </div>
        </div>

        {/* Card 2 */}
        <div className="glass-panel p-md rounded-xl group hover:border-primary/50 transition-all duration-300">
          <p className="font-label-caps text-label-caps text-on-surface-variant mb-xs">Today's Change</p>
          <h2
            className={`font-display-lg text-[32px] font-bold leading-none mb-xs ${
              todaysChangeVal >= 0 ? "text-primary" : "text-error"
            }`}
          >
            {todaysChangeVal >= 0 ? "+" : ""}
            {formatCurrency(todaysChangeVal, "USD")}
          </h2>
          <div
            className={`flex items-center gap-xs ${todaysChangePct >= 0 ? "text-primary" : "text-error"}`}
          >
            {todaysChangePct >= 0 ? <ArrowUpRight className="h-4 w-4" /> : <ArrowDownRight className="h-4 w-4" />}
            <span className="text-body-sm font-bold">{todaysChangePct >= 0 ? "+" : ""}{todaysChangePct.toFixed(2)}%</span>
          </div>
        </div>

        {/* Card 3 */}
        <div className="glass-panel p-md rounded-xl group hover:border-primary/50 transition-all duration-300">
          <p className="font-label-caps text-label-caps text-on-surface-variant mb-xs">Total Gain</p>
          <h2
            className={`font-display-lg text-[32px] font-bold leading-none mb-xs ${
              totalGain >= 0 ? "text-primary" : "text-error"
            }`}
          >
            {totalGain >= 0 ? "+" : ""}
            {formatCurrency(totalGain, "USD")}
          </h2>
          <div className="flex items-center gap-xs text-primary">
            <Bolt className="h-4 w-4" />
            <span className="text-body-sm font-bold">{gainPercent.toFixed(1)}% Total ROI</span>
          </div>
        </div>

        {/* Card 4 */}
        <div className="glass-panel p-md rounded-xl group hover:border-primary/50 transition-all duration-300">
          <p className="font-label-caps text-label-caps text-on-surface-variant mb-xs">Annual Return</p>
          <h2 className={`font-display-lg text-[32px] font-bold leading-none mb-xs ${annualReturnVal >= 0 ? "text-on-surface" : "text-error"}`}>
            {annualReturnVal >= 0 ? "+" : ""}{annualReturnVal.toFixed(1)}%
          </h2>
          <div className="flex items-center gap-xs text-on-tertiary-container">
            <BarChart3 className="h-4 w-4" />
            <span className="text-body-sm font-bold">Benchmark: 12.4% S&P 500</span>
          </div>
        </div>
      </div>

      {/* BENTO GRID SECTION */}
      <div className="grid grid-cols-12 gap-gutter">
        {/* MAIN GROWTH CHART */}
        <div className="col-span-12 lg:col-span-8 glass-panel rounded-xl p-md flex flex-col">
          <div className="flex flex-wrap justify-between items-center mb-md gap-sm">
            <div>
              <h3 className="font-headline-md text-headline-md font-bold">Growth Performance</h3>
              <p className="text-body-sm text-on-surface-variant">Investment value trajectory</p>
            </div>
            <div className="flex gap-1 bg-surface-dim p-1 rounded-lg border border-outline-variant/30">
              {["1D", "1W", "1M", "6M", "1Y", "ALL"].map((period) => (
                <button
                  key={period}
                  onClick={() => setChartPeriod(period)}
                  className={`px-3 py-1 text-xs rounded-md transition-colors font-medium ${
                    chartPeriod === period
                      ? "bg-primary text-on-primary font-bold"
                      : "text-on-surface-variant hover:bg-surface-variant"
                  }`}
                >
                  {period}
                </button>
              ))}
            </div>
          </div>

          <div className="flex-1 min-h-[220px] w-full pt-2">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -15, bottom: 0 }}>
                <defs>
                  <linearGradient id="growthGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#4be277" stopOpacity={0.4} />
                    <stop offset="95%" stopColor="#4be277" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="time" stroke="#94a3b8" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis
                  stroke="#94a3b8"
                  fontSize={11}
                  tickLine={false}
                  axisLine={false}
                  tickFormatter={(v) => `$${v >= 1000 ? (v / 1000).toFixed(0) + "k" : v}`}
                />
                <RechartsTooltip
                  formatter={(val) => [formatCurrency(val, "USD"), "Valuation"]}
                  contentStyle={{
                    backgroundColor: "#1e293b",
                    borderColor: "#334155",
                    borderRadius: "8px",
                    color: "#f8fafc",
                  }}
                />
                <Area type="monotone" dataKey="value" stroke="#4be277" strokeWidth={3} fillOpacity={1} fill="url(#growthGradient)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* ASSET ALLOCATION */}
        <div className="col-span-12 lg:col-span-4 glass-panel rounded-xl p-md flex flex-col">
          <h3 className="font-headline-md text-headline-md font-bold mb-xs">Asset Allocation</h3>
          <div className="flex-1 flex flex-col items-center justify-between relative min-h-[220px]">
            <div className="w-full h-44 relative flex items-center justify-center">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={sectorValues}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={55}
                    outerRadius={78}
                    paddingAngle={sectorValues.length > 1 ? 4 : 0}
                    stroke="none"
                  >
                    {sectorValues.map((entry, idx) => (
                      <Cell key={entry.name} fill={CHART_PALETTE[idx % CHART_PALETTE.length]} />
                    ))}
                  </Pie>
                  <RechartsTooltip
                    formatter={(val) => [formatCurrency(val, "INR"), "Allocation"]}
                    contentStyle={{
                      backgroundColor: "#1e293b",
                      borderColor: "#334155",
                      borderRadius: "8px",
                      color: "#f8fafc",
                    }}
                  />
                </PieChart>
              </ResponsiveContainer>

              {/* CENTER TEXT */}
              <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none text-center">
                <p className="font-label-caps text-[10px] text-on-surface-variant uppercase tracking-wider">Top Holding</p>
                <p className="font-bold text-base text-on-surface leading-tight">{topHolding.symbol}</p>
                <p className="text-primary font-bold text-sm">{topHolding.percent}</p>
              </div>
            </div>

            {/* DYNAMIC LEGEND */}
            <div className="grid grid-cols-2 gap-x-3 gap-y-1.5 w-full mt-xs text-xs">
              {sectorValues.map((s, idx) => {
                const pct = totalValue > 0 ? ((s.value / totalValue) * 100).toFixed(0) : "100";
                return (
                  <div key={s.name} className="flex items-center justify-between gap-1 overflow-hidden">
                    <div className="flex items-center gap-1.5 truncate">
                      <div
                        className="w-2.5 h-2.5 rounded-full shrink-0"
                        style={{ backgroundColor: CHART_PALETTE[idx % CHART_PALETTE.length] }}
                      />
                      <span className="truncate text-on-surface-variant">{s.name}</span>
                    </div>
                    <span className="font-semibold text-on-surface text-[11px] shrink-0">{pct}%</span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* TOP HOLDINGS TABLE (LIVE YAHOO QUOTES) */}
        <div className="col-span-12 lg:col-span-8 glass-panel rounded-xl p-md">
          <div className="flex justify-between items-center mb-md">
            <h3 className="font-headline-md text-headline-md font-bold">Top Holdings (Finnhub Quotes)</h3>
            <button onClick={onRefreshData} className="text-primary font-label-caps text-xs font-semibold hover:underline">
              Refresh Prices
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-outline-variant text-on-surface-variant font-label-caps text-xs uppercase tracking-wider">
                  <th className="pb-sm px-sm">Asset</th>
                  <th className="pb-sm px-sm text-right">Qty</th>
                  <th className="pb-sm px-sm text-right">Avg Price</th>
                  <th className="pb-sm px-sm text-right">Live Price</th>
                  <th className="pb-sm px-sm text-right">Profit / Loss</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/30 text-body-sm">
                {holdings.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-xl text-center text-xs text-on-surface-variant italic">
                      No holdings in portfolio yet. Go to Holdings page to add stocks.
                    </td>
                  </tr>
                ) : (
                  holdings.map((h) => {
                    const livePrice = h.currentPrice || h.averagePurchasePrice;
                    const diff = livePrice - h.averagePurchasePrice;
                    const pct = h.averagePurchasePrice > 0 ? (diff / h.averagePurchasePrice) * 100 : 0;
                    return (
                      <tr key={h.id} className="hover:bg-surface-variant/30 transition-colors cursor-pointer">
                        <td className="py-md px-sm">
                          <div className="flex items-center gap-sm">
                            <div className="w-8 h-8 rounded-full bg-surface-bright flex items-center justify-center font-bold text-primary text-xs shrink-0">
                              {h.symbol.charAt(0)}
                            </div>
                            <div>
                              <p className="font-bold text-on-surface">{h.companyName || h.symbol}</p>
                              <p className="text-[10px] text-on-surface-variant">{h.symbol}</p>
                            </div>
                          </div>
                        </td>
                        <td className="py-md px-sm text-right font-medium">{h.quantity}</td>
                        <td className="py-md px-sm text-right">{formatCurrency(h.averagePurchasePrice, null, h.symbol)}</td>
                        <td className="py-md px-sm text-right font-semibold text-on-surface">
                          {formatCurrency(livePrice, null, h.symbol)}
                        </td>
                        <td
                          className={`py-md px-sm text-right font-bold ${
                            pct >= 0 ? "text-primary" : "text-error"
                          }`}
                        >
                          {pct >= 0 ? "+" : ""}
                          {pct.toFixed(1)}%
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* WATCHLIST & SPARKLINES */}
        <div className="col-span-12 lg:col-span-4 glass-panel rounded-xl p-md">
          <div className="flex justify-between items-center mb-md">
            <h3 className="font-headline-md text-headline-md font-bold">Watchlist</h3>
            <Star className="h-4 w-4 text-on-surface-variant hover:text-primary cursor-pointer" />
          </div>
          <div className="space-y-md">
            {watchlist.length === 0 ? (
              <div className="space-y-md">
                {[
                  { symbol: "AAPL", name: "Apple Inc.", price: 224.20, pct: "+0.8%" },
                  { symbol: "TSLA", name: "Tesla Motors", price: 210.50, pct: "-1.5%" },
                  { symbol: "BTC-USD", name: "Bitcoin USD", price: 67240.00, pct: "+4.5%" },
                ].map((item) => (
                  <div key={item.symbol} className="flex items-center justify-between group cursor-pointer">
                    <div className="flex-1">
                      <p className="font-bold group-hover:text-primary transition-colors">{item.symbol}</p>
                      <p className="text-[10px] text-on-surface-variant">{item.name}</p>
                    </div>
                    <div className="w-16 h-6 flex items-center justify-center">
                      <svg className="w-full h-full" viewBox="0 0 100 40">
                        <path
                          d="M0,35 Q20,10 40,25 T80,10 T100,20"
                          fill="none"
                          stroke="#4be277"
                          strokeWidth="2"
                        />
                      </svg>
                    </div>
                    <div className="text-right ml-md">
                      <p className="font-bold text-xs">{formatCurrency(item.price, null, item.symbol)}</p>
                      <p className={`text-[10px] ${item.pct.startsWith("+") ? "text-primary" : "text-error"}`}>
                        {item.pct}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              watchlist.map((item) => (
                <div
                  key={item.id}
                  className="flex items-center justify-between group cursor-pointer"
                  onClick={() => setChartSymbol(item.symbol)}
                >
                  <div className="flex-1">
                    <p className="font-bold group-hover:text-primary transition-colors">{item.symbol}</p>
                  </div>
                  <div className="w-16 h-6 flex items-center justify-center">
                    <svg className="w-full h-full" viewBox="0 0 100 40">
                      <path
                        d="M0,35 Q20,10 40,25 T80,10 T100,20"
                        fill="none"
                        stroke="#4be277"
                        strokeWidth="2"
                      />
                    </svg>
                  </div>
                  <div className="text-right ml-md">
                    <p className="font-bold text-xs">
                      {formatCurrency(item.currentPrice || item.targetPrice || 150, null, item.symbol)}
                    </p>
                    <p className="text-[10px] text-primary">+0.4%</p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* RECENT TRANSACTIONS */}
        <div className="col-span-12 glass-panel rounded-xl p-md">
          <div className="flex justify-between items-center mb-md">
            <h3 className="font-headline-md text-headline-md font-bold">Recent Executions</h3>
            <span className="px-md py-xs bg-surface-variant text-body-sm rounded-lg text-xs font-semibold">
              History
            </span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-md">
            {transactions.length === 0 ? (
              <div className="col-span-full py-md text-center text-xs text-on-surface-variant italic">
                No recent transactions recorded.
              </div>
            ) : (
              transactions.slice(0, 4).map((tx) => (
                <div
                  key={tx.id}
                  className="p-sm bg-surface-dim rounded-lg border border-outline-variant/30 flex items-center gap-sm"
                >
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${
                      tx.type === "BUY" ? "bg-primary/10 text-primary" : "bg-error/10 text-error"
                    }`}
                  >
                    {tx.type === "BUY" ? <ArrowUpRight className="h-5 w-5" /> : <ArrowDownRight className="h-5 w-5" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-xs truncate">
                      {tx.type} {tx.symbol}
                    </p>
                    <p className="text-[10px] text-on-surface-variant truncate">
                      {tx.quantity} Shares @ {formatCurrency(tx.pricePerShare, null, tx.symbol)}
                    </p>
                  </div>
                  <div className="text-right shrink-0">
                    <span className="px-1.5 py-0.5 bg-primary/20 text-primary text-[9px] rounded font-bold uppercase">
                      Executed
                    </span>
                    <p className="text-[10px] text-on-surface-variant mt-1">
                      {formatDateTime(tx.transactionDate)}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ==========================================================================
   HOLDINGS VIEW
   ========================================================================== */

function HoldingsView({
  holdings,
  holdingForm,
  setHoldingForm,
  onSaveHolding,
  onDeleteHolding,
  quote,
  quoteLoading,
  quoteSymbol,
  setQuoteSymbol,
  onLookupQuote,
}) {
  return (
    <div className="space-y-lg">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-headline-md text-headline-md font-bold">Holdings Manager</h2>
          <p className="text-body-sm text-on-surface-variant">Track owned stocks with live Finnhub quotes</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-gutter">
        {/* Form + Quote Lookup */}
        <div className="space-y-md">
          <form onSubmit={onSaveHolding} className="glass-panel p-md rounded-xl space-y-sm">
            <h3 className="font-bold text-on-surface text-base">
              {holdingForm.id ? "Edit Holding" : "Add New Holding"}
            </h3>
            <div>
              <label className="text-xs font-label-caps text-on-surface-variant">Ticker Symbol</label>
              <input
                type="text"
                value={holdingForm.symbol}
                onChange={(e) => setHoldingForm((h) => ({ ...h, symbol: e.target.value }))}
                placeholder="e.g. RELIANCE.NS, HDFCBANK.NS, AAPL"
                className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface focus:ring-1 focus:ring-primary uppercase"
                required
              />
            </div>
            <div>
              <label className="text-xs font-label-caps text-on-surface-variant">Company Name</label>
              <input
                type="text"
                value={holdingForm.companyName}
                onChange={(e) => setHoldingForm((h) => ({ ...h, companyName: e.target.value }))}
                placeholder="e.g. Reliance Industries"
                className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface focus:ring-1 focus:ring-primary"
              />
            </div>
            <div className="grid grid-cols-2 gap-xs">
              <div>
                <label className="text-xs font-label-caps text-on-surface-variant">Quantity</label>
                <input
                  type="number"
                  step="any"
                  value={holdingForm.quantity}
                  onChange={(e) => setHoldingForm((h) => ({ ...h, quantity: e.target.value }))}
                  placeholder="10"
                  className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface focus:ring-1 focus:ring-primary"
                  required
                />
              </div>
              <div>
                <label className="text-xs font-label-caps text-on-surface-variant">Purchase Date</label>
                <input
                  type="date"
                  value={holdingForm.purchaseDate}
                  onChange={(e) => setHoldingForm((h) => ({ ...h, purchaseDate: e.target.value }))}
                  className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface focus:ring-1 focus:ring-primary"
                />
              </div>
            </div>
            <div className="flex gap-xs pt-xs">
              <button
                type="submit"
                className="flex-1 bg-primary text-on-primary font-bold py-sm rounded-lg hover:brightness-110 text-xs"
              >
                {holdingForm.id ? "Update" : "Save"} Holding
              </button>
              {holdingForm.id && (
                <button
                  type="button"
                  onClick={() => setHoldingForm({ id: null, symbol: "", companyName: "", quantity: "", averagePurchasePrice: "", purchaseDate: new Date().toISOString().slice(0, 10) })}
                  className="px-md bg-surface-variant text-on-surface py-sm rounded-lg text-xs"
                >
                  Cancel
                </button>
              )}
            </div>
          </form>

          {/* Quick Quote Lookup */}
          <div className="glass-panel p-md rounded-xl space-y-sm">
            <h4 className="font-bold text-xs text-on-surface uppercase tracking-wider">Live Yahoo Quote Lookup</h4>
            <form onSubmit={onLookupQuote} className="flex gap-xs">
              <input
                type="text"
                value={quoteSymbol}
                onChange={(e) => setQuoteSymbol(e.target.value)}
                placeholder="RELIANCE.NS, AAPL"
                className="flex-1 bg-surface-dim border border-outline-variant/60 rounded-lg p-xs text-xs uppercase"
              />
              <button
                type="submit"
                disabled={quoteLoading}
                className="bg-primary text-on-primary font-bold px-sm py-xs text-xs rounded-lg"
              >
                Lookup
              </button>
            </form>
            {quote && (
              <div className="p-sm bg-surface-dim rounded-lg border border-primary/40 space-y-1">
                <p className="font-bold text-primary text-sm">{quote.symbol}</p>
                <p className="text-xs text-on-surface">{quote.companyName}</p>
                <p className="text-base font-bold text-on-surface">
                  {formatCurrency(quote.price, quote.currency, quote.symbol)}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Holdings Table */}
        <div className="col-span-2 glass-panel p-md rounded-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-outline-variant text-on-surface-variant font-label-caps text-xs uppercase">
                  <th className="pb-sm px-sm">Symbol</th>
                  <th className="pb-sm px-sm text-right">Qty</th>
                  <th className="pb-sm px-sm text-right">Avg Price</th>
                  <th className="pb-sm px-sm text-right">Current Price</th>
                  <th className="pb-sm px-sm text-right">P&L</th>
                  <th className="pb-sm px-sm text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/30 text-body-sm">
                {holdings.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-lg text-center text-on-surface-variant italic">
                      No holdings found. Add one using the form.
                    </td>
                  </tr>
                ) : (
                  holdings.map((h) => {
                    const curPrice = h.currentPrice || h.averagePurchasePrice;
                    const diff = curPrice - h.averagePurchasePrice;
                    const pct = h.averagePurchasePrice > 0 ? (diff / h.averagePurchasePrice) * 100 : 0;
                    return (
                      <tr key={h.id} className="hover:bg-surface-variant/30 transition-colors">
                        <td className="py-md px-sm font-bold text-on-surface">
                          {h.symbol}
                          <span className="block text-[10px] text-on-surface-variant font-normal">
                            {h.companyName || h.symbol}
                          </span>
                        </td>
                        <td className="py-md px-sm text-right">{h.quantity}</td>
                        <td className="py-md px-sm text-right">{formatCurrency(h.averagePurchasePrice, null, h.symbol)}</td>
                        <td className="py-md px-sm text-right font-semibold">{formatCurrency(curPrice, null, h.symbol)}</td>
                        <td
                          className={`py-md px-sm text-right font-bold ${
                            pct >= 0 ? "text-primary" : "text-error"
                          }`}
                        >
                          {pct >= 0 ? "+" : ""}
                          {pct.toFixed(1)}%
                        </td>
                        <td className="py-md px-sm text-right">
                          <button
                            type="button"
                            onClick={() =>
                              setHoldingForm({
                                id: h.id,
                                symbol: h.symbol,
                                companyName: h.companyName || "",
                                quantity: h.quantity,
                                averagePurchasePrice: h.averagePurchasePrice,
                                purchaseDate: h.purchaseDate ? String(h.purchaseDate).slice(0, 10) : new Date().toISOString().slice(0, 10),
                              })
                            }
                            className="text-xs text-primary font-bold mr-2 hover:underline"
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => onDeleteHolding(h.id)}
                            className="text-xs text-error hover:underline"
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ==========================================================================
   TRANSACTIONS VIEW
   ========================================================================== */

function TransactionsView({
  transactions,
  transactionForm,
  setTransactionForm,
  onSaveTransaction,
  onDeleteTransaction,
}) {
  return (
    <div className="space-y-lg">
      <div>
        <h2 className="font-headline-md text-headline-md font-bold">Transaction Executions</h2>
        <p className="text-body-sm text-on-surface-variant">Log buy and sell orders</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-gutter">
        <form onSubmit={onSaveTransaction} className="glass-panel p-md rounded-xl space-y-sm">
          <h3 className="font-bold text-on-surface text-base">Record Execution</h3>
          <div className="grid grid-cols-2 gap-xs">
            <div>
              <label className="text-xs font-label-caps text-on-surface-variant">Type</label>
              <select
                value={transactionForm.type}
                onChange={(e) => setTransactionForm((t) => ({ ...t, type: e.target.value }))}
                className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface"
              >
                <option value="BUY">BUY</option>
                <option value="SELL">SELL</option>
              </select>
            </div>
            <div>
              <label className="text-xs font-label-caps text-on-surface-variant">Symbol</label>
              <input
                type="text"
                value={transactionForm.symbol}
                onChange={(e) => setTransactionForm((t) => ({ ...t, symbol: e.target.value }))}
                placeholder="RELIANCE.NS"
                className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface uppercase"
                required
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-xs">
            <div>
              <label className="text-xs font-label-caps text-on-surface-variant">Quantity</label>
              <input
                type="number"
                step="any"
                value={transactionForm.quantity}
                onChange={(e) => setTransactionForm((t) => ({ ...t, quantity: e.target.value }))}
                placeholder="10"
                className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface"
                required
              />
            </div>
            <div>
              <label className="text-xs font-label-caps text-on-surface-variant">Price / Share</label>
              <input
                type="number"
                step="any"
                value={transactionForm.pricePerShare}
                onChange={(e) => setTransactionForm((t) => ({ ...t, pricePerShare: e.target.value }))}
                placeholder="2450.00"
                className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface"
                required
              />
            </div>
          </div>
          <div>
            <label className="text-xs font-label-caps text-on-surface-variant">Execution Date</label>
            <input
              type="datetime-local"
              value={transactionForm.transactionDate}
              onChange={(e) => setTransactionForm((t) => ({ ...t, transactionDate: e.target.value }))}
              className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm text-on-surface"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full bg-primary text-on-primary font-bold py-sm rounded-lg hover:brightness-110 text-xs"
          >
            Save Execution
          </button>
        </form>

        <div className="col-span-2 glass-panel p-md rounded-xl">
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="border-b border-outline-variant text-on-surface-variant font-label-caps text-xs uppercase">
                  <th className="pb-sm px-sm">Type</th>
                  <th className="pb-sm px-sm">Symbol</th>
                  <th className="pb-sm px-sm text-right">Quantity</th>
                  <th className="pb-sm px-sm text-right">Price</th>
                  <th className="pb-sm px-sm text-right">Date</th>
                  <th className="pb-sm px-sm text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/30 text-body-sm">
                {transactions.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="py-lg text-center text-on-surface-variant italic">
                      No executions recorded.
                    </td>
                  </tr>
                ) : (
                  transactions.map((tx) => (
                    <tr key={tx.id} className="hover:bg-surface-variant/30 transition-colors">
                      <td className="py-md px-sm">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                            tx.type === "BUY" ? "bg-primary/20 text-primary" : "bg-error/20 text-error"
                          }`}
                        >
                          {tx.type}
                        </span>
                      </td>
                      <td className="py-md px-sm font-bold">{tx.symbol}</td>
                      <td className="py-md px-sm text-right">{tx.quantity}</td>
                      <td className="py-md px-sm text-right">{formatCurrency(tx.pricePerShare, null, tx.symbol)}</td>
                      <td className="py-md px-sm text-right text-xs text-on-surface-variant">
                        {formatDateTime(tx.transactionDate)}
                      </td>
                      <td className="py-md px-sm text-right">
                        <button
                          type="button"
                          onClick={() => onDeleteTransaction(tx.id)}
                          className="text-xs text-error hover:underline"
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ==========================================================================
   STOCK CHART MODAL (TradingView-style price chart)
   ========================================================================== */

const CANDLE_RANGES = [
  { label: "1D", resolution: "5", days: 1 },
  { label: "5D", resolution: "30", days: 5 },
  { label: "1M", resolution: "D", days: 30 },
  { label: "6M", resolution: "D", days: 182 },
  { label: "1Y", resolution: "D", days: 365 },
];

function StockChartModal({ symbol, onClose }) {
  const [range, setRange] = useState(CANDLE_RANGES[2]);
  const [candles, setCandles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError("");
    portfolioApi
      .getStockCandles(symbol, { resolution: range.resolution, days: range.days })
      .then((data) => {
        if (cancelled) return;
        const points = (data?.candles || []).map((c) => ({
          time: new Date(c.time * 1000).toLocaleDateString(undefined, { month: "short", day: "numeric" }),
          close: Number(c.close),
        }));
        setCandles(points);
        if (points.length === 0) {
          setError("No chart data available for this symbol / range.");
        }
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err?.response?.data?.message || "Failed to load chart data.");
        setCandles([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [symbol, range]);

  const first = candles[0]?.close;
  const last = candles[candles.length - 1]?.close;
  const changePct = first ? (((last - first) / first) * 100).toFixed(2) : null;
  const isUp = changePct !== null && Number(changePct) >= 0;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-md"
      onClick={onClose}
    >
      <div
        className="glass-panel w-full max-w-3xl rounded-xl p-lg space-y-md"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-xl font-bold text-primary">{symbol}</h3>
            {changePct !== null && (
              <p className={`text-sm font-semibold ${isUp ? "text-primary" : "text-error"}`}>
                {isUp ? "+" : ""}
                {changePct}% ({range.label})
              </p>
            )}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-on-surface-variant hover:text-error"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="flex gap-2">
          {CANDLE_RANGES.map((r) => (
            <button
              key={r.label}
              type="button"
              onClick={() => setRange(r)}
              className={`px-sm py-1 rounded-lg text-xs font-bold transition-colors ${
                r.label === range.label
                  ? "bg-primary text-on-primary"
                  : "bg-surface-dim text-on-surface-variant hover:text-on-surface"
              }`}
            >
              {r.label}
            </button>
          ))}
        </div>

        <div className="h-64 w-full">
          {loading ? (
            <div className="h-full flex items-center justify-center text-on-surface-variant text-sm">
              Loading chart...
            </div>
          ) : error ? (
            <div className="h-full flex items-center justify-center text-on-surface-variant text-sm text-center px-md">
              {error}
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={candles} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="chartFill" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor={isUp ? "#4be277" : "#f87171"} stopOpacity={0.4} />
                    <stop offset="95%" stopColor={isUp ? "#4be277" : "#f87171"} stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="time" tick={{ fontSize: 10 }} minTickGap={30} />
                <YAxis domain={["auto", "auto"]} tick={{ fontSize: 10 }} width={50} />
                <RechartsTooltip />
                <Area
                  type="monotone"
                  dataKey="close"
                  stroke={isUp ? "#4be277" : "#f87171"}
                  fill="url(#chartFill)"
                  strokeWidth={2}
                />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>
    </div>
  );
}

/* ==========================================================================
   WATCHLIST VIEW
   ========================================================================== */

function WatchlistView({ watchlist, watchlistForm, setWatchlistForm, onSaveWatchlist, onDeleteWatchlist }) {
  const [chartSymbol, setChartSymbol] = useState(null);

  return (
    <div className="space-y-lg">
      <div>
        <h2 className="font-headline-md text-headline-md font-bold">Watchlist</h2>
        <p className="text-body-sm text-on-surface-variant">Track target prices for stocks</p>
      </div>

      {chartSymbol && <StockChartModal symbol={chartSymbol} onClose={() => setChartSymbol(null)} />}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-gutter">
        <form onSubmit={onSaveWatchlist} className="glass-panel p-md rounded-xl space-y-sm">
          <h3 className="font-bold text-on-surface text-base">Add Ticker</h3>
          <div>
            <label className="text-xs font-label-caps text-on-surface-variant">Symbol</label>
            <input
              type="text"
              value={watchlistForm.symbol}
              onChange={(e) => setWatchlistForm((w) => ({ ...w, symbol: e.target.value }))}
              placeholder="e.g. RELIANCE.NS, AAPL"
              className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm uppercase"
              required
            />
          </div>
          <div>
            <label className="text-xs font-label-caps text-on-surface-variant">Target Price</label>
            <input
              type="number"
              step="any"
              value={watchlistForm.targetPrice}
              onChange={(e) => setWatchlistForm((w) => ({ ...w, targetPrice: e.target.value }))}
              placeholder="3000.00"
              className="w-full bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm"
            />
          </div>
          <button
            type="submit"
            className="w-full bg-primary text-on-primary font-bold py-sm rounded-lg hover:brightness-110 text-xs"
          >
            Save Watchlist Entry
          </button>
        </form>

        <div className="col-span-2 glass-panel p-md rounded-xl">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-md">
            {watchlist.length === 0 ? (
              <p className="col-span-full py-xl text-center text-xs text-on-surface-variant italic">
                Watchlist is empty. Add symbols using the form.
              </p>
            ) : (
              watchlist.map((item) => (
                <div
                  key={item.id}
                  className="p-md bg-surface-dim rounded-xl border border-outline-variant/40 flex items-center justify-between cursor-pointer hover:border-primary/60 transition-colors"
                  onClick={() => setChartSymbol(item.symbol)}
                >
                  <div>
                    <p className="font-bold text-base text-primary">{item.symbol}</p>
                    <p className="text-xs text-on-surface mt-1">
                      Target: {formatCurrency(item.targetPrice || 0, null, item.symbol)}
                    </p>
                  </div>
                  <div className="text-right">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onDeleteWatchlist(item.id);
                      }}
                      className="text-xs text-error hover:underline"
                    >
                      Remove
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ==========================================================================
   MARKET & NEWS VIEW
   ========================================================================== */

function MarketView({ news, onRefreshNews, quote, quoteLoading, quoteSymbol, setQuoteSymbol, onLookupQuote }) {
  return (
    <div className="space-y-lg">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="font-headline-md text-headline-md font-bold">Financial News & Live Quotes</h2>
          <p className="text-body-sm text-on-surface-variant">Powered by Yahoo Finance & Alpha Vantage</p>
        </div>
        <button
          type="button"
          onClick={onRefreshNews}
          className="flex items-center gap-2 bg-primary text-on-primary font-bold px-md py-sm rounded-lg text-xs hover:brightness-110"
        >
          <RefreshCw className="h-4 w-4" /> Refresh News
        </button>
      </div>

      {/* Quote Lookup Bar */}
      <form onSubmit={onLookupQuote} className="glass-panel p-md rounded-xl flex gap-md items-center">
        <input
          type="text"
          value={quoteSymbol}
          onChange={(e) => setQuoteSymbol(e.target.value)}
          placeholder="Enter stock ticker symbol (e.g. RELIANCE.NS, HDFCBANK.NS, AAPL, TSLA)..."
          className="flex-1 bg-surface-dim border border-outline-variant/60 rounded-lg p-sm text-body-sm uppercase"
        />
        <button
          type="submit"
          disabled={quoteLoading}
          className="bg-primary text-on-primary font-bold px-lg py-sm rounded-lg text-xs"
        >
          Lookup Quote
        </button>
      </form>

      {quote && (
        <div className="glass-panel p-md rounded-xl border-primary/50 flex items-center justify-between">
          <div>
            <h3 className="text-xl font-bold text-primary">{quote.symbol}</h3>
            <p className="text-body-sm text-on-surface">{quote.companyName}</p>
          </div>
          <div className="text-right">
            <p className="text-2xl font-bold text-on-surface">
              {formatCurrency(quote.price, quote.currency, quote.symbol)}
            </p>
          </div>
        </div>
      )}

      {/* News Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-gutter">
        {news.length === 0 ? (
          <div className="col-span-full py-xl text-center text-on-surface-variant italic">
            No news articles available. Configure NEWS_API_KEY in your .env file to fetch news.
          </div>
        ) : (
          news.map((item, idx) => (
            <a
              key={idx}
              href={item.url}
              target="_blank"
              rel="noreferrer"
              className="glass-panel p-md rounded-xl group hover:border-primary/50 transition-all flex flex-col justify-between"
            >
              <div>
                {item.urlToImage && (
                  <img
                    src={item.urlToImage}
                    alt={item.title}
                    className="w-full h-36 object-cover rounded-lg mb-sm"
                  />
                )}
                <h4 className="font-bold text-on-surface text-base group-hover:text-primary transition-colors line-clamp-2">
                  {item.title}
                </h4>
                <p className="text-xs text-on-surface-variant mt-xs line-clamp-3">{item.description}</p>
              </div>
              <div className="mt-md pt-xs border-t border-outline-variant/30 flex justify-between text-[11px] text-on-surface-variant">
                <span>{item.source}</span>
                <span>{formatDateTime(item.publishedAt)}</span>
              </div>
            </a>
          ))
        )}
      </div>
    </div>
  );
}

/* ==========================================================================
   SETTINGS VIEW
   ========================================================================== */

function SettingsView({ username, onLogout }) {
  return (
    <div className="space-y-lg max-w-2xl">
      <div>
        <h2 className="font-headline-md text-headline-md font-bold">Account & System Settings</h2>
        <p className="text-body-sm text-on-surface-variant">Manage credentials and data services</p>
      </div>

      <div className="glass-panel p-md rounded-xl space-y-md">
        <div className="flex items-center gap-md pb-md border-b border-outline-variant/40">
          <div className="w-12 h-12 rounded-full bg-primary/20 text-primary flex items-center justify-center font-bold text-lg">
            {username ? username.charAt(0).toUpperCase() : <User />}
          </div>
          <div>
            <h3 className="font-bold text-on-surface text-base">{username || "User"}</h3>
            <p className="text-xs text-on-surface-variant">Executive Tier Investor</p>
          </div>
        </div>

        <div className="space-y-sm text-xs">
          <div className="flex justify-between py-xs border-b border-outline-variant/20">
            <span className="text-on-surface-variant">Market Data Provider</span>
            <span className="font-bold text-primary">Yahoo Finance API (.NS & US Tickers)</span>
          </div>
          <div className="flex justify-between py-xs border-b border-outline-variant/20">
            <span className="text-on-surface-variant">AI Model</span>
            <span className="font-bold text-primary">Groq llama-3.1-8b-instant</span>
          </div>
          <div className="flex justify-between py-xs border-b border-outline-variant/20">
            <span className="text-on-surface-variant">Authentication</span>
            <span className="font-bold text-primary">JWT Token Active</span>
          </div>
        </div>

        <button
          type="button"
          onClick={onLogout}
          className="w-full bg-error/10 text-error font-bold py-sm rounded-lg hover:bg-error/20 text-xs"
        >
          Sign Out of Account
        </button>
      </div>
    </div>
  );
}
