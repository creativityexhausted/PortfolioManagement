import { ArrowDown, RefreshCw } from "lucide-react";
import { useMemo, useRef, useState } from "react";
import { useAutoScroll } from "../../hooks/useAutoScroll";
import { MessageBubble } from "./MessageBubble";
import { WelcomeScreen } from "./WelcomeScreen";

export const ChatWindow = ({
  messages,
  quickActions,
  onPromptClick,
  onCopy,
  onFeedback,
  onRegenerate,
  loading,
}) => {
  const containerRef = useRef(null);
  const [showScroll, setShowScroll] = useState(false);
  useAutoScroll(containerRef, messages.length);

  const lastUserMessage = useMemo(() => [...messages].reverse().find((item) => item.role === "user"), [messages]);

  const onScroll = () => {
    if (!containerRef.current) return;
    const { scrollTop, scrollHeight, clientHeight } = containerRef.current;
    setShowScroll(scrollHeight - (scrollTop + clientHeight) > 220);
  };

  const jumpBottom = () => {
    if (!containerRef.current) return;
    containerRef.current.scrollTo({ top: containerRef.current.scrollHeight, behavior: "smooth" });
  };

  return (
    <div className="relative flex h-full min-h-0 flex-col rounded-2xl border border-white/10 bg-slate-950">
      <div ref={containerRef} onScroll={onScroll} className="custom-scrollbar flex-1 space-y-4 overflow-y-auto p-4 sm:p-6">
        {!messages.length ? (
          <WelcomeScreen onPromptClick={onPromptClick} prompts={quickActions} />
        ) : (
          messages.map((message) => (
            <MessageBubble key={message.id} message={message} onCopy={onCopy} onFeedback={onFeedback} />
          ))
        )}
      </div>

      {messages.length > 0 && (
        <div className="flex items-center justify-between border-t border-white/10 px-4 py-2 text-xs text-slate-500 sm:px-6">
          <span>{loading ? "Assistant is analyzing your portfolio..." : "Responses are generated from your backend data context."}</span>
          <button
            type="button"
            onClick={() => onRegenerate(lastUserMessage?.content)}
            disabled={!lastUserMessage || loading}
            className="inline-flex items-center gap-1 rounded-md border border-white/10 px-2 py-1 text-slate-300 hover:border-cyan-300/50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RefreshCw className="h-3.5 w-3.5" /> Regenerate
          </button>
        </div>
      )}

      {showScroll && (
        <button
          type="button"
          onClick={jumpBottom}
          className="absolute bottom-20 right-5 rounded-full border border-cyan-300/50 bg-cyan-500/20 p-2 text-cyan-100 shadow-lg"
          aria-label="Scroll to bottom"
        >
          <ArrowDown className="h-4 w-4" />
        </button>
      )}
    </div>
  );
};
