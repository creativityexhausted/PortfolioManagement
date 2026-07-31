import { AnimatePresence, motion } from "framer-motion";
import { Bot, Copy, ThumbsDown, ThumbsUp, User } from "lucide-react";
import { memo, useMemo } from "react";
import ReactMarkdown from "react-markdown";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { atomDark } from "react-syntax-highlighter/dist/esm/styles/prism";
import { formatDateTime } from "../../utils/formatters";

const TICKER_REGEX = /\b[A-Z]{2,5}\b/g;

const withTickerBadges = (content) => {
  const chunks = [];
  let last = 0;
  for (const match of content.matchAll(TICKER_REGEX)) {
    const index = match.index ?? 0;
    if (index > last) chunks.push(content.slice(last, index));
    chunks.push(` <${match[0]}> `);
    last = index + match[0].length;
  }
  if (last < content.length) chunks.push(content.slice(last));
  return chunks.join("");
};

const MessageBubbleComp = ({ message, onCopy, onFeedback }) => {
  const isUser = message.role === "user";

  const rendered = useMemo(() => {
    if (isUser) return message.content;
    return withTickerBadges(message.content);
  }, [isUser, message.content]);

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      className={`flex gap-3 ${isUser ? "justify-end" : "justify-start"}`}
    >
      {!isUser && (
        <div className="grid h-9 w-9 place-items-center rounded-full border border-cyan-300/30 bg-cyan-500/10 text-cyan-200">
          <Bot className="h-4 w-4" />
        </div>
      )}

      <div className={`max-w-[82%] rounded-2xl border px-4 py-3 ${isUser ? "border-indigo-400/30 bg-indigo-500/15" : "border-white/10 bg-slate-900"}`}>
        {isUser ? (
          <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-slate-100">{message.content}</p>
        ) : (
          <div className="prose prose-invert prose-sm max-w-none text-slate-100">
            <ReactMarkdown
              components={{
                code(props) {
                  const { children, className, ...rest } = props;
                  const match = /language-(\w+)/.exec(className || "");
                  if (!match) {
                    return (
                      <code className="rounded bg-slate-800/90 px-1 py-0.5 text-cyan-200" {...rest}>
                        {children}
                      </code>
                    );
                  }
                  return (
                    <SyntaxHighlighter
                      PreTag="div"
                      language={match[1]}
                      style={atomDark}
                      customStyle={{ borderRadius: "0.6rem", padding: "0.85rem" }}
                    >
                      {String(children).replace(/\n$/, "")}
                    </SyntaxHighlighter>
                  );
                },
                p({ children }) {
                  if (typeof children?.[0] === "string" && children[0].trim().startsWith("<") && children[0].trim().endsWith(">")) {
                    return <p>{children}</p>;
                  }
                  return <p className="leading-relaxed">{children}</p>;
                },
                text({ children }) {
                  const text = Array.isArray(children) ? children.join("") : children;
                  if (typeof text !== "string") return text;
                  const nodes = [];
                  let cursor = 0;
                  for (const match of text.matchAll(/<([A-Z]{2,5})>/g)) {
                    const idx = match.index ?? 0;
                    if (idx > cursor) nodes.push(text.slice(cursor, idx));
                    nodes.push(
                      <span key={`${match[1]}-${idx}`} className="mx-1 inline-flex rounded-full border border-cyan-300/50 bg-cyan-500/15 px-2 py-0.5 text-[10px] font-semibold tracking-wide text-cyan-100">
                        {match[1]}
                      </span>,
                    );
                    cursor = idx + match[0].length;
                  }
                  if (cursor < text.length) nodes.push(text.slice(cursor));
                  return nodes;
                },
                a({ href, children }) {
                  return (
                    <a href={href} target="_blank" rel="noreferrer" className="text-cyan-300 underline-offset-2 hover:underline">
                      {children}
                    </a>
                  );
                },
              }}
            >
              {rendered}
            </ReactMarkdown>
          </div>
        )}

        <div className="mt-3 flex items-center justify-between gap-2 text-[11px] text-slate-500">
          <span>{formatDateTime(message.createdAt)}</span>
          <AnimatePresence>
            {!isUser && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center gap-1">
                <button type="button" onClick={() => onCopy(message.content)} className="rounded p-1 hover:bg-white/10" aria-label="Copy response">
                  <Copy className="h-3.5 w-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => onFeedback(message.id, "like")}
                  className={`rounded p-1 hover:bg-white/10 ${message.feedback === "like" ? "text-emerald-300" : ""}`}
                  aria-label="Like response"
                >
                  <ThumbsUp className="h-3.5 w-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => onFeedback(message.id, "dislike")}
                  className={`rounded p-1 hover:bg-white/10 ${message.feedback === "dislike" ? "text-rose-300" : ""}`}
                  aria-label="Dislike response"
                >
                  <ThumbsDown className="h-3.5 w-3.5" />
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      {isUser && (
        <div className="grid h-9 w-9 place-items-center rounded-full border border-indigo-300/30 bg-indigo-500/15 text-indigo-200">
          <User className="h-4 w-4" />
        </div>
      )}
    </motion.div>
  );
};

export const MessageBubble = memo(MessageBubbleComp);
