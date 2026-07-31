import { motion } from "framer-motion";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, LineChart, CartesianGrid, XAxis, YAxis, Line } from "recharts";
import { classBySign, formatCurrency, formatDateTime } from "../../utils/formatters";

const chartPalette = ["#38bdf8", "#6366f1", "#10b981", "#f59e0b", "#f43f5e", "#a78bfa"];

export const InsightsPanel = ({ insights, loading }) => {
  return (
    <aside className="hidden w-[22rem] shrink-0 border-l border-white/10 bg-slate-950/70 p-4 xl:block">
      <div className="space-y-4">
        <h3 className="text-sm font-semibold text-slate-100">Portfolio Insights</h3>

        {!insights.hasData ? (
          <div className="rounded-xl border border-dashed border-white/20 bg-slate-900/60 p-4 text-sm text-slate-400">
            {loading ? "Loading insights..." : insights.summary}
          </div>
        ) : (
          <>
            <div className="grid grid-cols-2 gap-2">
              {insights.cards.map((card) => (
                <motion.div
                  key={card.label}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="rounded-xl border border-white/10 bg-slate-900/60 p-2"
                >
                  <p className="text-[10px] uppercase tracking-wide text-slate-500">{card.label}</p>
                  <p className={`mt-1 text-sm font-semibold ${classBySign(card.numeric)}`}>{card.value}</p>
                  {card.helper && <p className="mt-1 text-[10px] text-slate-500">{card.helper}</p>}
                </motion.div>
              ))}
            </div>

            <div className="rounded-xl border border-white/10 bg-slate-900/60 p-3">
              <p className="mb-2 text-xs font-medium text-slate-200">Allocation</p>
              <div className="h-44">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie data={insights.allocation} dataKey="value" nameKey="name" innerRadius={45} outerRadius={70}>
                      {insights.allocation.map((entry, idx) => (
                        <Cell key={entry.name} fill={chartPalette[idx % chartPalette.length]} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(val) => formatCurrency(val)} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="rounded-xl border border-white/10 bg-slate-900/60 p-3">
              <p className="mb-2 text-xs font-medium text-slate-200">Investment Timeline</p>
              {insights.timeline.length ? (
                <div className="h-40">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={insights.timeline}>
                      <CartesianGrid stroke="#334155" strokeDasharray="3 3" />
                      <XAxis
                        dataKey="date"
                        stroke="#94a3b8"
                        tickFormatter={(value) => new Date(value).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                      />
                      <YAxis stroke="#94a3b8" tickFormatter={(value) => `$${value}`} />
                      <Tooltip labelFormatter={(value) => formatDateTime(value)} formatter={(val) => formatCurrency(val)} />
                      <Line type="monotone" dataKey="value" stroke="#38bdf8" strokeWidth={2} dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <p className="text-xs text-slate-500">Add transactions to render timeline history.</p>
              )}
            </div>

            <div className="rounded-xl border border-white/10 bg-slate-900/60 p-3">
              <p className="mb-2 text-xs font-medium text-slate-200">Holdings Summary</p>
              <div className="max-h-40 space-y-1 overflow-y-auto pr-1 text-xs">
                {insights.holdingRows.map((row) => (
                  <div key={row.id} className="flex items-center justify-between rounded-md bg-slate-950/70 px-2 py-1">
                    <span className="text-slate-200">{row.symbol}</span>
                    <span className={classBySign(row.pnl)}>{formatCurrency(row.pnl)}</span>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </aside>
  );
};
