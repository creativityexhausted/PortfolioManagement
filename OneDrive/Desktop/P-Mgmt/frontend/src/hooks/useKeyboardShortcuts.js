import { useEffect } from "react";

export const useKeyboardShortcuts = ({ onNewChat, onToggleSidebar, onFocusInput }) => {
  useEffect(() => {
    const onKeyDown = (event) => {
      const isMeta = event.ctrlKey || event.metaKey;

      if (isMeta && event.key.toLowerCase() === "k") {
        event.preventDefault();
        onFocusInput?.();
      }

      if (isMeta && event.key.toLowerCase() === "b") {
        event.preventDefault();
        onToggleSidebar?.();
      }

      if (isMeta && event.key.toLowerCase() === "n") {
        event.preventDefault();
        onNewChat?.();
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onFocusInput, onNewChat, onToggleSidebar]);
};
