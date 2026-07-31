import { useEffect } from "react";

export const useAutoScroll = (containerRef, dependency) => {
  useEffect(() => {
    if (!containerRef.current) return;
    containerRef.current.scrollTop = containerRef.current.scrollHeight;
  }, [containerRef, dependency]);
};
