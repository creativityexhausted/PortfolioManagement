import { useState } from "react";
import { Lock, UserRound } from "lucide-react";
import { LoadingDots } from "./LoadingDots";

export const AuthPanel = ({ mode, onModeChange, onSubmit, loading }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  return (
    <div className="mx-auto w-full max-w-md rounded-2xl border border-white/10 bg-slate-900/70 p-6 shadow-2xl backdrop-blur-xl">
      <p className="text-xs uppercase tracking-[0.25em] text-cyan-300/70">Secure Access</p>
      <h2 className="mt-1 text-2xl font-semibold text-slate-100">{mode === "login" ? "Sign in" : "Create account"}</h2>
      <p className="mt-1 text-sm text-slate-400">Connect your portfolio data before chatting with the assistant.</p>

      <form
        className="mt-5 space-y-3"
        onSubmit={(event) => {
          event.preventDefault();
          onSubmit({ mode, username, password });
        }}
      >
        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">Username</span>
          <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2">
            <UserRound className="h-4 w-4 text-slate-500" />
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              minLength={3}
              maxLength={100}
              required
              className="w-full bg-transparent text-sm text-slate-100 outline-none"
            />
          </div>
        </label>

        <label className="block">
          <span className="mb-1 block text-xs text-slate-400">Password</span>
          <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-slate-950/80 px-3 py-2">
            <Lock className="h-4 w-4 text-slate-500" />
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              minLength={8}
              maxLength={100}
              type="password"
              required
              className="w-full bg-transparent text-sm text-slate-100 outline-none"
            />
          </div>
        </label>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-gradient-to-r from-cyan-500 to-indigo-500 py-2 text-sm font-medium text-white disabled:opacity-70"
        >
          {loading ? <LoadingDots className="justify-center" /> : mode === "login" ? "Sign in" : "Create account"}
        </button>
      </form>

      <button
        type="button"
        onClick={() => onModeChange(mode === "login" ? "register" : "login")}
        className="mt-3 text-xs text-cyan-300 hover:underline"
      >
        {mode === "login" ? "No account? Register" : "Already have an account? Login"}
      </button>
    </div>
  );
};
