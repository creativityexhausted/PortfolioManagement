import { format, isValid, parseISO } from "date-fns";

export const formatCurrency = (value) => {
  if (value == null || Number.isNaN(Number(value))) return "N/A";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(Number(value));
};

export const formatPercent = (value) => {
  if (value == null || Number.isNaN(Number(value))) return "N/A";
  return `${Number(value).toFixed(2)}%`;
};

export const formatDateTime = (value) => {
  if (!value) return "N/A";
  const date = typeof value === "string" ? parseISO(value) : new Date(value);
  if (!isValid(date)) return "N/A";
  return format(date, "MMM d, yyyy h:mm a");
};

export const classBySign = (value) => {
  if (value == null || Number(value) === 0) return "text-slate-300";
  return Number(value) > 0 ? "text-emerald-400" : "text-rose-400";
};

export const safeId = () =>
  (globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`);
