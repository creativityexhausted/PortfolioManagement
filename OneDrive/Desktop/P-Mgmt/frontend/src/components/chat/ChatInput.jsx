import { useEffect, useMemo, useRef, useState } from "react";
import { Paperclip, Send, Trash2, Volume2 } from "lucide-react";
import { LoadingDots } from "../common/LoadingDots";

const LIMIT = 1000;

export const ChatInput = ({ onSend, onClear, loading, registerFocusRef }) => {
  const [value, setValue] = useState("");
  const textAreaRef = useRef(null);

  const remaining = useMemo(() => LIMIT - value.length, [value.length]);

  const updateText = (next) => {
    setValue(next.slice(0, LIMIT));
    if (textAreaRef.current) {
      textAreaRef.current.style.height = "auto";
      textAreaRef.current.style.height = `${Math.min(textAreaRef.current.scrollHeight, 220)}px`;
    }
  };

  const submit = () => {
    const trimmed = value.trim();
    if (!trimmed || loading) return;
    onSend(trimmed);
    setValue("");
    if (textAreaRef.current) {
      textAreaRef.current.style.height = "auto";
    }
  };

  useEffect(() => {
    if (!registerFocusRef) return;
    registerFocusRef(() => textAreaRef.current?.focus());
  }, [registerFocusRef]);

  return (
    <div className="rounded-2xl border border-white/10 bg-slate-900 p-3 shadow-[0_0_0_1px_rgba(34,211,238,0.05)]">
      <div className="flex items-end gap-2">
        <button
          type="button"
          className="rounded-lg border border-white/10 p-2 text-slate-400 hover:text-cyan-200"
          aria-label="Attachment placeholder"
          title="Attachment support will be connected when backend endpoint exists"
        >
          <Paperclip className="h-4 w-4" />
        </button>

        <div className="relative flex-1">
          <textarea
            ref={textAreaRef}
            value={value}
            onChange={(event) => updateText(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                submit();
              }
            }}
            rows={1}
            placeholder="Ask about your portfolio, stock performance, or financial terms..."
            className="w-full resize-none rounded-xl border border-white/10 bg-slate-950 px-3 py-2 pr-24 text-sm text-slate-100 outline-none transition focus:border-cyan-300/60"
            aria-label="Message input"
          />
          <span className={`absolute bottom-2 right-3 text-[10px] ${remaining < 100 ? "text-amber-300" : "text-slate-500"}`}>
            {value.length}/{LIMIT}
          </span>
        </div>

        <button
          type="button"
          className="rounded-lg border border-white/10 p-2 text-slate-400 hover:text-cyan-200"
          aria-label="Voice placeholder"
          title="Voice support placeholder"
        >
          <Volume2 className="h-4 w-4" />
        </button>

        <button
          type="button"
          onClick={submit}
          disabled={loading || !value.trim()}
          className="rounded-lg bg-gradient-to-r from-cyan-500 to-indigo-500 p-2 text-white shadow-lg shadow-cyan-900/40 transition hover:shadow-cyan-700/50 disabled:cursor-not-allowed disabled:opacity-60"
          aria-label="Send message"
        >
          {loading ? <LoadingDots /> : <Send className="h-4 w-4" />}
        </button>

        <button
          type="button"
          onClick={onClear}
          className="rounded-lg border border-white/10 p-2 text-slate-400 hover:text-rose-300"
          aria-label="Clear chat"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>

      <p className="mt-2 text-[11px] text-slate-500">Enter to send, Shift+Enter for new line.</p>
    </div>
  );
};
