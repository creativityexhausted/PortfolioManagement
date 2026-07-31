import { AnimatePresence, motion } from "framer-motion";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Bot,
  Download,
  Newspaper,
  Plus,
  RefreshCw,
  Save,
  Search,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { chatApi } from "./services/chatApi";
import { authApi } from "./services/authApi";
import { tokenStore } from "./services/apiClient";
import { portfolioApi } from "./services/portfolioApi";
import { useChatSessions } from "./hooks/useChatSessions";
import { useKeyboardShortcuts } from "./hooks/useKeyboardShortcuts";
import { quickActions } from "./constants/quickActions";
import { formatCurrency, formatDateTime, safeId } from "./utils/formatters";
import { computePortfolioInsights } from "./utils/insights";
import { Sidebar } from "./components/layout/Sidebar";
import { TopBar } from "./components/layout/TopBar";
import { ChatWindow } from "./components/chat/ChatWindow";
import { QuickActions } from "./components/chat/QuickActions";
import { ChatInput } from "./components/chat/ChatInput";
import { AuthPanel } from "./components/common/AuthPanel";
import { InsightsPanel } from "./components/portfolio/InsightsPanel";
import { useToast } from "./components/common/ToastProvider";

const USERNAME_KEY = "pm_username";
const THEME_KEY = "pm_theme";

const toMessage = (role, content, extra = {}) => ({
  id: safeId(),
  role,
  content,
  createdAt: new Date().toISOString(),
  ...extra,
});

const sectionTitles = {
  "/": "Dashboard",
  "/portfolio": "Portfolio",
  "/holdings": "Holdings",
  "/transactions": "Transactions",
  "/watchlist": "Watchlist",
  "/market": "Market",
  "/assistant": "AI Assistant",
  "/settings": "Settings",
};

const shellCard = "rounded-2xl border border-white/10 bg-slate-950/55 p-4";

const sectionHeader = (title, subtitle) => (
  <header className="mb-4">
    <h2 className="text-xl font-semibold text-slate-100">{title}</h2>
    <p className="text-sm text-slate-400">{subtitle}</p>
  </header>
);

const selectedOrFirstPortfolioId = (selectedPortfolioId, portfolios) => selectedPortfolioId ?? portfolios[0]?.id ?? null;

function App() {
  const { pushToast } = useToast();
  const location = useLocation();
  const inputFocusRef = useRef(null);

  const [authenticated, setAuthenticated] = useState(Boolean(tokenStore.get()));
  const [authMode, setAuthMode] = useState("login");
  const [authLoading, setAuthLoading] = useState(false);
  const [username, setUsername] = useState(localStorage.getItem(USERNAME_KEY) || "");

  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => window.innerWidth < 1024);
  const [showInsights, setShowInsights] = useState(true);
  const [theme, setTheme] = useState(() => localStorage.getItem(THEME_KEY) || "dark");
  const [assistantOpen, setAssistantOpen] = useState(false);

  const [loadingChat, setLoadingChat] = useState(false);
  const [dataLoading, setDataLoading] = useState(false);
  const [selectedPortfolioId, setSelectedPortfolioId] = useState(null);

  const [portfolios, setPortfolios] = useState([]);
  const [holdings, setHoldings] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [watchlist, setWatchlist] = useState([]);
  const [news, setNews] = useState([]);
  const [quoteLoading, setQuoteLoading] = useState(false);
  const [quote, setQuote] = useState(null);
  const [quoteSymbol, setQuoteSymbol] = useState("");

  const [portfolioForm, setPortfolioForm] = useState({ id: null, name: "", description: "" });
  const [holdingForm, setHoldingForm] = useState({
    id: null,
    symbol: "",
    companyName: "",
    quantity: "",
    averagePurchasePrice: "",
  });
  const [transactionForm, setTransactionForm] = useState({
    id: null,
    type: "BUY",
    symbol: "",
    quantity: "",
    pricePerShare: "",
    transactionDate: "",
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
    activeId,
    activeSession,
    setActiveId,
    createSession,
    updateSession,
    deleteSession,
  } = useChatSessions();

  const currentMessages = activeSession?.messages || [];

  useEffect(() => {
    document.documentElement.classList.toggle("light", theme === "light");
    localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  const fetchPortfolioData = useCallback(async () => {
    if (!authenticated) return;
    setDataLoading(true);
    try {
      const [portfolioData, holdingsData, transactionData, watchlistData, newsData] = await Promise.all([
        portfolioApi.getPortfolios(),
        portfolioApi.getHoldings(selectedPortfolioId),
        portfolioApi.getTransactions(selectedPortfolioId),
        portfolioApi.getWatchlist(selectedPortfolioId),
        portfolioApi.getNews(),
      ]);

      setPortfolios(portfolioData);
      setHoldings(holdingsData);
      setTransactions(transactionData);
      setWatchlist(watchlistData);
      setNews(newsData);
    } catch (error) {
      const message = error?.response?.data?.message || error.message || "Could not load portfolio data.";
      pushToast(message, "error");
    } finally {
      setDataLoading(false);
    }
  }, [authenticated, selectedPortfolioId, pushToast]);

  useEffect(() => {
    fetchPortfolioData();
  }, [fetchPortfolioData]);

  const onAuthSubmit = async (payload) => {
    setAuthLoading(true);
    try {
      const response = authMode === "login" ? await authApi.login(payload) : await authApi.register(payload);
      tokenStore.set(response.token);
      localStorage.setItem(USERNAME_KEY, payload.username);
      setUsername(payload.username);
      setAuthenticated(true);
      pushToast(authMode === "login" ? "Signed in successfully." : "Account created successfully.", "success");
    } catch (error) {
      const message = error?.response?.data?.message || "Authentication failed. Check your credentials.";
      pushToast(message, "error");
    } finally {
      setAuthLoading(false);
    }
  };

  const appendToActiveSession = useCallback(
    (updater) => {
      updateSession(activeSession.id, (session) => {
        const nextMessages = updater(session.messages || []);
        const inferredTitle =
          session.title === "New conversation" && nextMessages.length
            ? nextMessages.find((message) => message.role === "user")?.content.slice(0, 40) || session.title
            : session.title;
        return {
          ...session,
          title: inferredTitle,
          messages: nextMessages,
          portfolioId: selectedPortfolioId,
        };
      });
    },
    [activeSession, selectedPortfolioId, updateSession],
  );

  const sendMessage = useCallback(
    async (text) => {
      if (!authenticated || loadingChat) return;

      const userMessage = toMessage("user", text);
      appendToActiveSession((messages) => [...messages, userMessage]);
      setLoadingChat(true);

      try {
        const response = await chatApi.askPortfolioAssistant({ message: text, portfolioId: selectedPortfolioId });
        const assistantMessage = toMessage("assistant", response.answer, {
          model: response.model,
          generatedAt: response.generatedAt,
        });
        appendToActiveSession((messages) => [...messages, assistantMessage]);
      } catch (error) {
        const apiMessage = error?.response?.data?.message || "The assistant request failed. Please retry.";
        appendToActiveSession((messages) => [...messages, toMessage("assistant", `Error: ${apiMessage}`)]);
        pushToast(apiMessage, "error");
      } finally {
        setLoadingChat(false);
      }
    },
    [appendToActiveSession, authenticated, loadingChat, selectedPortfolioId, pushToast],
  );

  const onClearChat = () => {
    updateSession(activeSession.id, { messages: [], title: "New conversation" });
  };

  const onFeedback = (messageId, feedback) => {
    updateSession(activeSession.id, (session) => ({
      ...session,
      messages: session.messages.map((message) =>
        message.id === messageId ? { ...message, feedback: message.feedback === feedback ? null : feedback } : message,
      ),
    }));
  };

  const onCopy = async (content) => {
    try {
      await navigator.clipboard.writeText(content);
      pushToast("Response copied to clipboard.", "success");
    } catch {
      pushToast("Clipboard access failed.", "error");
    }
  };

  const onRegenerate = async (lastPrompt) => {
    if (!lastPrompt) return;
    await sendMessage(lastPrompt);
  };

  const onExportConversation = () => {
    const blob = new Blob([JSON.stringify(activeSession, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = `${activeSession.title.replace(/\s+/g, "-").toLowerCase()}-${Date.now()}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const onLogout = () => {
    tokenStore.clear();
    localStorage.removeItem(USERNAME_KEY);
    setAuthenticated(false);
    setUsername("");
    pushToast("Signed out.", "info");
  };

  const resetPortfolioForm = () => setPortfolioForm({ id: null, name: "", description: "" });
  const resetHoldingForm = () => setHoldingForm({ id: null, symbol: "", companyName: "", quantity: "", averagePurchasePrice: "" });
  const resetTransactionForm = () =>
    setTransactionForm({ id: null, type: "BUY", symbol: "", quantity: "", pricePerShare: "", transactionDate: "", notes: "" });
  const resetWatchlistForm = () => setWatchlistForm({ id: null, symbol: "", companyName: "", targetPrice: "" });

  const ensurePortfolioId = useCallback(() => {
    const id = selectedOrFirstPortfolioId(selectedPortfolioId, portfolios);
    if (!id) {
      pushToast("Create a portfolio first.", "error");
      return null;
    }
    return id;
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

  const savePortfolio = async (event) => {
    event.preventDefault();
    const payload = { name: portfolioForm.name.trim(), description: portfolioForm.description.trim() || null };
    if (!payload.name) {
      pushToast("Portfolio name is required.", "error");
      return;
    }
    await withMutation(
      () =>
        portfolioForm.id
          ? portfolioApi.updatePortfolio(portfolioForm.id, payload)
          : portfolioApi.createPortfolio(payload),
      portfolioForm.id ? "Portfolio updated." : "Portfolio created.",
    );
    resetPortfolioForm();
  };

  const saveHolding = async (event) => {
    event.preventDefault();
    const portfolioId = ensurePortfolioId();
    if (!portfolioId) return;
    const payload = {
      symbol: holdingForm.symbol.trim().toUpperCase(),
      companyName: holdingForm.companyName.trim() || null,
      quantity: holdingForm.quantity,
      averagePurchasePrice: holdingForm.averagePurchasePrice,
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

  const saveTransaction = async (event) => {
    event.preventDefault();
    const portfolioId = ensurePortfolioId();
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

  const saveWatchlist = async (event) => {
    event.preventDefault();
    const portfolioId = ensurePortfolioId();
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

  const refreshNews = async () => {
    await withMutation(async () => {
      const fresh = await portfolioApi.refreshNews();
      setNews(fresh);
    }, "News refreshed.");
  };

  const lookupQuote = async (event) => {
    event.preventDefault();
    const symbol = quoteSymbol.trim().toUpperCase();
    if (!symbol) return;
    setQuoteLoading(true);
    try {
      const data = await portfolioApi.getStockPrice(symbol);
      setQuote(data);
      pushToast(`Loaded quote for ${symbol}.`, "success");
    } catch (error) {
      const message = error?.response?.data?.message || "Could not fetch quote.";
      pushToast(message, "error");
    } finally {
      setQuoteLoading(false);
    }
  };

  const insights = useMemo(() => computePortfolioInsights({ holdings, transactions }), [holdings, transactions]);

  const sectionTitle = sectionTitles[location.pathname] || "Dashboard";

  const dashboardCards = useMemo(() => insights.cards.slice(0, 6), [insights.cards]);

  useKeyboardShortcuts({
    onNewChat: () => {
      createSession();
      setAssistantOpen(true);
    },
    onToggleSidebar: () => setSidebarCollapsed((prev) => !prev),
    onFocusInput: () => {
      setAssistantOpen(true);
      inputFocusRef.current?.();
    },
  });

  if (!authenticated) {
    return (
      <div className="relative min-h-screen overflow-hidden bg-slate-950 text-slate-100">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_15%_15%,rgba(34,211,238,0.17),transparent_35%),radial-gradient(circle_at_85%_85%,rgba(99,102,241,0.2),transparent_35%)]" />
        <main className="relative z-10 grid min-h-screen place-items-center px-4">
          <AuthPanel mode={authMode} onModeChange={setAuthMode} onSubmit={onAuthSubmit} loading={authLoading} />
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="fixed inset-0 -z-10 bg-[linear-gradient(120deg,#020617_0%,#0f172a_38%,#020617_100%)]" />
      <div className="fixed inset-0 -z-10 bg-[radial-gradient(circle_at_18%_20%,rgba(56,189,248,0.15),transparent_28%),radial-gradient(circle_at_82%_12%,rgba(99,102,241,0.17),transparent_22%),radial-gradient(circle_at_70%_80%,rgba(16,185,129,0.13),transparent_24%)]" />

      <div className="flex min-h-screen">
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

        <div className="flex min-w-0 flex-1 flex-col">
          <TopBar
            username={username}
            sectionTitle={sectionTitle}
            portfolios={portfolios}
            selectedPortfolioId={selectedPortfolioId}
            onPortfolioChange={setSelectedPortfolioId}
            onToggleSidebar={() => setSidebarCollapsed((prev) => !prev)}
            onOpenAssistant={() => setAssistantOpen(true)}
            theme={theme}
            onThemeToggle={() => setTheme((prev) => (prev === "dark" ? "light" : "dark"))}
          />

          <main className="min-h-0 flex-1 overflow-y-auto p-3 sm:p-4">
            <Routes>
              <Route
                path="/"
                element={
                  <section className="grid gap-4 xl:grid-cols-[1fr_22rem]">
                    <div className="space-y-4">
                      <div className={shellCard}>
                        {sectionHeader("Portfolio Overview", "Live metrics generated from your real holdings and transactions.")}
                        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                          {dashboardCards.map((card) => (
                            <div key={card.label} className="rounded-xl border border-white/10 bg-slate-900/65 p-3">
                              <p className="text-xs uppercase tracking-wide text-slate-400">{card.label}</p>
                              <p className="mt-1 text-lg font-semibold text-slate-100">{card.value}</p>
                              {card.helper && <p className="text-xs text-slate-500">{card.helper}</p>}
                            </div>
                          ))}
                        </div>
                      </div>

                      <div className={shellCard}>
                        {sectionHeader("Recent Transactions", "Most recent records from your transaction ledger.")}
                        <div className="overflow-x-auto">
                          <table className="min-w-full text-sm">
                            <thead className="text-left text-slate-400">
                              <tr>
                                <th className="pb-2">Date</th>
                                <th className="pb-2">Type</th>
                                <th className="pb-2">Symbol</th>
                                <th className="pb-2">Qty</th>
                                <th className="pb-2">Price</th>
                              </tr>
                            </thead>
                            <tbody>
                              {transactions.slice(0, 7).map((tx) => (
                                <tr key={tx.id} className="border-t border-white/5 text-slate-200">
                                  <td className="py-2 pr-3">{formatDateTime(tx.transactionDate)}</td>
                                  <td className="py-2 pr-3">{tx.type}</td>
                                  <td className="py-2 pr-3">{tx.symbol}</td>
                                  <td className="py-2 pr-3">{tx.quantity}</td>
                                  <td className="py-2">{formatCurrency(tx.pricePerShare)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                          {!transactions.length && <p className="py-3 text-sm text-slate-500">No transactions available.</p>}
                        </div>
                      </div>

                      <div className={shellCard}>
                        <div className="mb-3 flex items-center justify-between">
                          <div>
                            <h3 className="text-lg font-semibold text-slate-100">Financial News</h3>
                            <p className="text-sm text-slate-400">Fetched from your backend market news service.</p>
                          </div>
                          <button
                            type="button"
                            onClick={refreshNews}
                            className="inline-flex items-center gap-2 rounded-lg border border-white/10 bg-slate-900/60 px-3 py-1.5 text-xs text-slate-200"
                          >
                            <RefreshCw className="h-3.5 w-3.5" /> Refresh
                          </button>
                        </div>
                        <div className="grid gap-3 md:grid-cols-2">
                          {news.slice(0, 6).map((item) => (
                            <a
                              key={`${item.url}-${item.publishedAt}`}
                              href={item.url}
                              target="_blank"
                              rel="noreferrer"
                              className="rounded-xl border border-white/10 bg-slate-900/65 p-3 transition hover:border-cyan-300/40"
                            >
                              <p className="text-sm font-semibold text-slate-100">{item.title}</p>
                              <p className="mt-1 text-xs text-slate-400">{item.source || "Unknown source"}</p>
                            </a>
                          ))}
                        </div>
                        {!news.length && <p className="text-sm text-slate-500">No news available.</p>}
                      </div>
                    </div>
                    {showInsights && <InsightsPanel insights={insights} loading={dataLoading} />}
                  </section>
                }
              />

              <Route
                path="/portfolio"
                element={
                  <section className="space-y-4">
                    <div className={shellCard}>
                      {sectionHeader("Portfolio Manager", "Create, update, and delete portfolios using backend portfolio endpoints.")}
                      <form className="grid gap-3 md:grid-cols-[1fr_1fr_auto]" onSubmit={savePortfolio}>
                        <input
                          value={portfolioForm.name}
                          onChange={(event) => setPortfolioForm((prev) => ({ ...prev, name: event.target.value }))}
                          placeholder="Portfolio name"
                          className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm outline-none"
                        />
                        <input
                          value={portfolioForm.description}
                          onChange={(event) => setPortfolioForm((prev) => ({ ...prev, description: event.target.value }))}
                          placeholder="Description"
                          className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm outline-none"
                        />
                        <div className="flex gap-2">
                          <button type="submit" className="inline-flex items-center gap-2 rounded-lg bg-cyan-500/85 px-3 py-2 text-sm font-medium text-slate-950">
                            <Save className="h-4 w-4" /> {portfolioForm.id ? "Update" : "Create"}
                          </button>
                          {portfolioForm.id && (
                            <button type="button" onClick={resetPortfolioForm} className="rounded-lg border border-white/10 px-3 py-2 text-sm">
                              Cancel
                            </button>
                          )}
                        </div>
                      </form>
                    </div>

                    <div className={shellCard}>
                      <div className="overflow-x-auto">
                        <table className="min-w-full text-sm">
                          <thead className="text-left text-slate-400">
                            <tr>
                              <th className="pb-2">Name</th>
                              <th className="pb-2">Description</th>
                              <th className="pb-2">Updated</th>
                              <th className="pb-2">Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {portfolios.map((portfolio) => (
                              <tr key={portfolio.id} className="border-t border-white/5">
                                <td className="py-2 pr-3">{portfolio.name}</td>
                                <td className="py-2 pr-3 text-slate-400">{portfolio.description || "-"}</td>
                                <td className="py-2 pr-3 text-slate-400">{formatDateTime(portfolio.updatedAt)}</td>
                                <td className="py-2">
                                  <div className="flex gap-2">
                                    <button
                                      type="button"
                                      onClick={() => setPortfolioForm({ id: portfolio.id, name: portfolio.name, description: portfolio.description || "" })}
                                      className="rounded-md border border-white/10 px-2 py-1 text-xs"
                                    >
                                      Edit
                                    </button>
                                    <button
                                      type="button"
                                      onClick={() =>
                                        withMutation(() => portfolioApi.deletePortfolio(portfolio.id), "Portfolio deleted.")
                                      }
                                      className="rounded-md border border-rose-300/30 px-2 py-1 text-xs text-rose-200"
                                    >
                                      Delete
                                    </button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {!portfolios.length && <p className="py-3 text-sm text-slate-500">No portfolios yet.</p>}
                      </div>
                    </div>
                  </section>
                }
              />

              <Route
                path="/holdings"
                element={
                  <section className="space-y-4">
                    <div className={shellCard}>
                      {sectionHeader("Holdings", "Add or edit holdings tied to the selected portfolio scope.")}
                      <form className="grid gap-3 md:grid-cols-5" onSubmit={saveHolding}>
                        <input value={holdingForm.symbol} onChange={(event) => setHoldingForm((prev) => ({ ...prev, symbol: event.target.value }))} placeholder="Symbol" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={holdingForm.companyName} onChange={(event) => setHoldingForm((prev) => ({ ...prev, companyName: event.target.value }))} placeholder="Company" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={holdingForm.quantity} onChange={(event) => setHoldingForm((prev) => ({ ...prev, quantity: event.target.value }))} placeholder="Quantity" type="number" step="0.0001" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={holdingForm.averagePurchasePrice} onChange={(event) => setHoldingForm((prev) => ({ ...prev, averagePurchasePrice: event.target.value }))} placeholder="Avg Buy Price" type="number" step="0.0001" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <div className="flex gap-2">
                          <button type="submit" className="rounded-lg bg-cyan-500/85 px-3 py-2 text-sm font-medium text-slate-950">{holdingForm.id ? "Update" : "Create"}</button>
                          {holdingForm.id && <button type="button" onClick={resetHoldingForm} className="rounded-lg border border-white/10 px-3 py-2 text-sm">Cancel</button>}
                        </div>
                      </form>
                    </div>

                    <div className={shellCard}>
                      <div className="overflow-x-auto">
                        <table className="min-w-full text-sm">
                          <thead className="text-left text-slate-400">
                            <tr>
                              <th className="pb-2">Symbol</th>
                              <th className="pb-2">Quantity</th>
                              <th className="pb-2">Avg Price</th>
                              <th className="pb-2">Current</th>
                              <th className="pb-2">Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {holdings.map((holding) => (
                              <tr key={holding.id} className="border-t border-white/5">
                                <td className="py-2 pr-3">{holding.symbol}</td>
                                <td className="py-2 pr-3">{holding.quantity}</td>
                                <td className="py-2 pr-3">{formatCurrency(holding.averagePurchasePrice)}</td>
                                <td className="py-2 pr-3">{formatCurrency(holding.currentPrice)}</td>
                                <td className="py-2">
                                  <div className="flex gap-2">
                                    <button type="button" onClick={() => setHoldingForm({ id: holding.id, symbol: holding.symbol || "", companyName: holding.companyName || "", quantity: holding.quantity || "", averagePurchasePrice: holding.averagePurchasePrice || "" })} className="rounded-md border border-white/10 px-2 py-1 text-xs">Edit</button>
                                    <button type="button" onClick={() => withMutation(() => portfolioApi.deleteHolding(holding.id), "Holding deleted.")} className="rounded-md border border-rose-300/30 px-2 py-1 text-xs text-rose-200">Delete</button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {!holdings.length && <p className="py-3 text-sm text-slate-500">No holdings available.</p>}
                      </div>
                    </div>
                  </section>
                }
              />

              <Route
                path="/transactions"
                element={
                  <section className="space-y-4">
                    <div className={shellCard}>
                      {sectionHeader("Transactions", "Write BUY/SELL transactions to backend and track timeline effects.")}
                      <form className="grid gap-3 md:grid-cols-7" onSubmit={saveTransaction}>
                        <select value={transactionForm.type} onChange={(event) => setTransactionForm((prev) => ({ ...prev, type: event.target.value }))} className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm">
                          <option value="BUY">BUY</option>
                          <option value="SELL">SELL</option>
                        </select>
                        <input value={transactionForm.symbol} onChange={(event) => setTransactionForm((prev) => ({ ...prev, symbol: event.target.value }))} placeholder="Symbol" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={transactionForm.quantity} onChange={(event) => setTransactionForm((prev) => ({ ...prev, quantity: event.target.value }))} placeholder="Quantity" type="number" step="0.0001" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={transactionForm.pricePerShare} onChange={(event) => setTransactionForm((prev) => ({ ...prev, pricePerShare: event.target.value }))} placeholder="Price" type="number" step="0.0001" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={transactionForm.transactionDate} onChange={(event) => setTransactionForm((prev) => ({ ...prev, transactionDate: event.target.value }))} type="datetime-local" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={transactionForm.notes} onChange={(event) => setTransactionForm((prev) => ({ ...prev, notes: event.target.value }))} placeholder="Notes" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <div className="flex gap-2">
                          <button type="submit" className="rounded-lg bg-cyan-500/85 px-3 py-2 text-sm font-medium text-slate-950">{transactionForm.id ? "Update" : "Create"}</button>
                          {transactionForm.id && <button type="button" onClick={resetTransactionForm} className="rounded-lg border border-white/10 px-3 py-2 text-sm">Cancel</button>}
                        </div>
                      </form>
                    </div>

                    <div className={shellCard}>
                      <div className="overflow-x-auto">
                        <table className="min-w-full text-sm">
                          <thead className="text-left text-slate-400">
                            <tr>
                              <th className="pb-2">Date</th>
                              <th className="pb-2">Type</th>
                              <th className="pb-2">Symbol</th>
                              <th className="pb-2">Qty</th>
                              <th className="pb-2">Price</th>
                              <th className="pb-2">Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {transactions.map((tx) => (
                              <tr key={tx.id} className="border-t border-white/5">
                                <td className="py-2 pr-3">{formatDateTime(tx.transactionDate)}</td>
                                <td className="py-2 pr-3">{tx.type}</td>
                                <td className="py-2 pr-3">{tx.symbol}</td>
                                <td className="py-2 pr-3">{tx.quantity}</td>
                                <td className="py-2 pr-3">{formatCurrency(tx.pricePerShare)}</td>
                                <td className="py-2">
                                  <div className="flex gap-2">
                                    <button type="button" onClick={() => setTransactionForm({ id: tx.id, type: tx.type || "BUY", symbol: tx.symbol || "", quantity: tx.quantity || "", pricePerShare: tx.pricePerShare || "", transactionDate: tx.transactionDate ? tx.transactionDate.slice(0, 16) : "", notes: tx.notes || "" })} className="rounded-md border border-white/10 px-2 py-1 text-xs">Edit</button>
                                    <button type="button" onClick={() => withMutation(() => portfolioApi.deleteTransaction(tx.id), "Transaction deleted.")} className="rounded-md border border-rose-300/30 px-2 py-1 text-xs text-rose-200">Delete</button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {!transactions.length && <p className="py-3 text-sm text-slate-500">No transactions available.</p>}
                      </div>
                    </div>
                  </section>
                }
              />

              <Route
                path="/watchlist"
                element={
                  <section className="space-y-4">
                    <div className={shellCard}>
                      {sectionHeader("Watchlist", "Track symbols and optional target prices.")}
                      <form className="grid gap-3 md:grid-cols-4" onSubmit={saveWatchlist}>
                        <input value={watchlistForm.symbol} onChange={(event) => setWatchlistForm((prev) => ({ ...prev, symbol: event.target.value }))} placeholder="Symbol" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={watchlistForm.companyName} onChange={(event) => setWatchlistForm((prev) => ({ ...prev, companyName: event.target.value }))} placeholder="Company" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <input value={watchlistForm.targetPrice} onChange={(event) => setWatchlistForm((prev) => ({ ...prev, targetPrice: event.target.value }))} placeholder="Target Price" type="number" step="0.0001" className="rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm" />
                        <div className="flex gap-2">
                          <button type="submit" className="rounded-lg bg-cyan-500/85 px-3 py-2 text-sm font-medium text-slate-950">{watchlistForm.id ? "Update" : "Create"}</button>
                          {watchlistForm.id && <button type="button" onClick={resetWatchlistForm} className="rounded-lg border border-white/10 px-3 py-2 text-sm">Cancel</button>}
                        </div>
                      </form>
                    </div>

                    <div className={shellCard}>
                      <div className="overflow-x-auto">
                        <table className="min-w-full text-sm">
                          <thead className="text-left text-slate-400">
                            <tr>
                              <th className="pb-2">Symbol</th>
                              <th className="pb-2">Target</th>
                              <th className="pb-2">Current</th>
                              <th className="pb-2">Updated</th>
                              <th className="pb-2">Actions</th>
                            </tr>
                          </thead>
                          <tbody>
                            {watchlist.map((item) => (
                              <tr key={item.id} className="border-t border-white/5">
                                <td className="py-2 pr-3">{item.symbol}</td>
                                <td className="py-2 pr-3">{formatCurrency(item.targetPrice)}</td>
                                <td className="py-2 pr-3">{formatCurrency(item.currentPrice)}</td>
                                <td className="py-2 pr-3">{formatDateTime(item.lastPriceUpdate)}</td>
                                <td className="py-2">
                                  <div className="flex gap-2">
                                    <button type="button" onClick={() => setWatchlistForm({ id: item.id, symbol: item.symbol || "", companyName: item.companyName || "", targetPrice: item.targetPrice || "" })} className="rounded-md border border-white/10 px-2 py-1 text-xs">Edit</button>
                                    <button type="button" onClick={() => withMutation(() => portfolioApi.deleteWatchlistEntry(item.id), "Watchlist entry deleted.")} className="rounded-md border border-rose-300/30 px-2 py-1 text-xs text-rose-200">Delete</button>
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {!watchlist.length && <p className="py-3 text-sm text-slate-500">No watchlist entries available.</p>}
                      </div>
                    </div>
                  </section>
                }
              />

              <Route
                path="/market"
                element={
                  <section className="space-y-4">
                    <div className={shellCard}>
                      {sectionHeader("Market Tools", "Quote lookup and curated news from backend market endpoints.")}
                      <form className="flex flex-wrap items-center gap-3" onSubmit={lookupQuote}>
                        <div className="relative w-64">
                          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
                          <input value={quoteSymbol} onChange={(event) => setQuoteSymbol(event.target.value)} placeholder="Ticker symbol (e.g., AAPL)" className="w-full rounded-lg border border-white/10 bg-slate-900/65 py-2 pl-9 pr-3 text-sm" />
                        </div>
                        <button type="submit" disabled={quoteLoading} className="rounded-lg bg-cyan-500/85 px-3 py-2 text-sm font-medium text-slate-950 disabled:opacity-50">{quoteLoading ? "Loading..." : "Get Quote"}</button>
                      </form>
                      {quote && (
                        <div className="mt-4 rounded-xl border border-white/10 bg-slate-900/60 p-3 text-sm">
                          <p className="text-slate-100">{quote.companyName || quote.symbol}</p>
                          <p className="text-lg font-semibold text-cyan-200">{formatCurrency(quote.price)}</p>
                          <p className="text-xs text-slate-400">{quote.currency || "USD"}</p>
                        </div>
                      )}
                    </div>

                    <div className={shellCard}>
                      <div className="mb-3 flex items-center justify-between">
                        <h3 className="text-lg font-semibold text-slate-100">News Feed</h3>
                        <button type="button" onClick={refreshNews} className="inline-flex items-center gap-2 rounded-lg border border-white/10 px-3 py-1.5 text-xs text-slate-200">
                          <Newspaper className="h-3.5 w-3.5" /> Refresh
                        </button>
                      </div>
                      <div className="space-y-2">
                        {news.map((item) => (
                          <a key={`${item.url}-${item.publishedAt}`} href={item.url} target="_blank" rel="noreferrer" className="block rounded-xl border border-white/10 bg-slate-900/60 p-3 hover:border-cyan-300/40">
                            <p className="text-sm font-semibold text-slate-100">{item.title}</p>
                            <p className="text-xs text-slate-400">{item.source || "Unknown source"}</p>
                          </a>
                        ))}
                      </div>
                      {!news.length && <p className="text-sm text-slate-500">No market news available.</p>}
                    </div>
                  </section>
                }
              />

              <Route
                path="/assistant"
                element={
                  <section className="space-y-3">
                    <div className={shellCard}>
                      {sectionHeader("AI Assistant", "Conversation grounded in your backend portfolio context.")}
                      <div className="h-[62vh] min-h-[28rem]">
                        <ChatWindow
                          messages={currentMessages}
                          quickActions={quickActions}
                          onPromptClick={sendMessage}
                          onCopy={onCopy}
                          onFeedback={onFeedback}
                          onRegenerate={onRegenerate}
                          loading={loadingChat}
                        />
                      </div>
                    </div>
                    <QuickActions actions={quickActions} onSend={sendMessage} disabled={loadingChat} />
                    <ChatInput
                      loading={loadingChat}
                      onSend={sendMessage}
                      onClear={onClearChat}
                      registerFocusRef={(fn) => {
                        inputFocusRef.current = fn;
                      }}
                    />
                  </section>
                }
              />

              <Route
                path="/settings"
                element={
                  <section className="space-y-4">
                    <div className={shellCard}>
                      {sectionHeader("Workspace Settings", "Client-side settings and utility actions.")}
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          onClick={onExportConversation}
                          className="inline-flex items-center gap-2 rounded-lg border border-white/10 bg-slate-900/65 px-3 py-2 text-sm"
                        >
                          <Download className="h-4 w-4" /> Export Active Conversation
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            createSession();
                            setAssistantOpen(true);
                          }}
                          className="inline-flex items-center gap-2 rounded-lg border border-cyan-300/30 bg-cyan-500/10 px-3 py-2 text-sm text-cyan-100"
                        >
                          <Plus className="h-4 w-4" /> New AI Session
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            deleteSession(activeId);
                            pushToast("Active chat session deleted.", "info");
                          }}
                          className="inline-flex items-center gap-2 rounded-lg border border-rose-300/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"
                        >
                          <Trash2 className="h-4 w-4" /> Delete Active Session
                        </button>
                      </div>
                    </div>
                  </section>
                }
              />

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </div>

      <AnimatePresence>
        {assistantOpen && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 16, scale: 0.98 }}
            className="fixed bottom-4 right-4 z-50 flex h-[78vh] w-[min(100vw-1.5rem,31rem)] flex-col rounded-2xl border border-cyan-300/20 bg-slate-950/96 p-3 shadow-2xl shadow-cyan-950/40"
          >
            <div className="mb-2 flex items-center justify-between">
              <div className="inline-flex items-center gap-2 text-sm font-semibold text-cyan-100">
                <Bot className="h-4 w-4" /> Floating Assistant
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={onExportConversation}
                  className="rounded-md border border-white/10 px-2 py-1 text-xs text-slate-300"
                >
                  Export
                </button>
                <button
                  type="button"
                  onClick={() => setAssistantOpen(false)}
                  className="rounded-md border border-white/10 p-1 text-slate-300"
                  aria-label="Close assistant"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            </div>

            <div className="min-h-0 flex-1">
              <ChatWindow
                messages={currentMessages}
                quickActions={quickActions}
                onPromptClick={sendMessage}
                onCopy={onCopy}
                onFeedback={onFeedback}
                onRegenerate={onRegenerate}
                loading={loadingChat}
              />
            </div>

            <div className="mt-2 space-y-2">
              <QuickActions actions={quickActions} onSend={sendMessage} disabled={loadingChat} />
              <ChatInput
                loading={loadingChat}
                onSend={sendMessage}
                onClear={onClearChat}
                registerFocusRef={(fn) => {
                  inputFocusRef.current = fn;
                }}
              />
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <div className="fixed bottom-4 right-4 z-40 flex gap-2">
        <button
          type="button"
          onClick={() => {
            setAssistantOpen(true);
            inputFocusRef.current?.();
          }}
          className="rounded-full border border-cyan-300/40 bg-cyan-500/20 p-3 text-cyan-100 shadow-lg"
          aria-label="Open assistant"
        >
          <Sparkles className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

export default App;
