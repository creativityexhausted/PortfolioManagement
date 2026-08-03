import { useCallback, useMemo, useState } from "react";
import { safeId } from "../utils/formatters";

const STORAGE_KEY = "pm_chat_sessions";
const ACTIVE_STORAGE_KEY = "pm_active_session";

const defaultSession = () => ({
  id: safeId(),
  title: "New conversation",
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  pinned: false,
  messages: [],
  portfolioId: null,
});

const loadSessions = () => {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    if (!Array.isArray(parsed) || !parsed.length) return [defaultSession()];
    return parsed;
  } catch {
    return [defaultSession()];
  }
};

export const useChatSessions = () => {
  const [sessions, setSessions] = useState(loadSessions);
  const [activeId, setActiveId] = useState(() => localStorage.getItem(ACTIVE_STORAGE_KEY) || loadSessions()[0].id);

  const activeSession = useMemo(
    () => sessions.find((session) => session.id === activeId) || sessions[0],
    [activeId, sessions],
  );

  const persist = useCallback((nextSessions, nextActiveId) => {
    setSessions(nextSessions);
    setActiveId(nextActiveId);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSessions));
    localStorage.setItem(ACTIVE_STORAGE_KEY, nextActiveId);
  }, []);

  const createSession = useCallback(() => {
    const fresh = defaultSession();
    const next = [fresh, ...sessions];
    persist(next, fresh.id);
  }, [persist, sessions]);

  const updateSession = useCallback(
    (sessionId, updater) => {
      const next = sessions.map((session) => {
        if (session.id !== sessionId) return session;
        const updated = typeof updater === "function" ? updater(session) : { ...session, ...updater };
        return { ...updated, updatedAt: new Date().toISOString() };
      });
      persist(next, activeId);
    },
    [activeId, persist, sessions],
  );

  const appendToActiveSession = useCallback(
    (messagesUpdater) => {
      setSessions((prevSessions) => {
        const next = prevSessions.map((session) => {
          if (session.id !== activeId) return session;
          const prevMessages = session.messages || [];
          const nextMessages =
            typeof messagesUpdater === "function" ? messagesUpdater(prevMessages) : messagesUpdater;
          return { ...session, messages: nextMessages, updatedAt: new Date().toISOString() };
        });
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        return next;
      });
    },
    [activeId],
  );

  const deleteSession = useCallback(
    (sessionId) => {
      const filtered = sessions.filter((session) => session.id !== sessionId);
      const safe = filtered.length ? filtered : [defaultSession()];
      const nextActive = activeId === sessionId ? safe[0].id : activeId;
      persist(safe, nextActive);
    },
    [activeId, persist, sessions],
  );

  const setSessionPinned = useCallback(
    (sessionId, pinned) => {
      updateSession(sessionId, (session) => ({ ...session, pinned }));
    },
    [updateSession],
  );

  const resetSessions = useCallback(() => {
    const fresh = defaultSession();
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(ACTIVE_STORAGE_KEY);
    persist([fresh], fresh.id);
  }, [persist]);

  return {
    sessions,
    activeId,
    activeSession,
    setActiveId,
    createSession,
    updateSession,
    appendToActiveSession,
    deleteSession,
    setSessionPinned,
    resetSessions,
  };
};
