const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

const ACCESS_TOKEN_KEY = "investment_tracker_access_token";
const REFRESH_TOKEN_KEY = "investment_tracker_refresh_token";

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY) || "";
}

function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || "";
}

export function setTokens(accessToken, refreshToken) {
  if (accessToken) {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  }
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  }
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export async function login(email, password) {
  return authRequest("/api/auth/login", { email, password });
}

export async function register(email, password) {
  return authRequest("/api/auth/register", { email, password });
}

export async function searchStocks(query) {
  const response = await apiFetch(`/api/stocks/search?q=${encodeURIComponent(query)}`);
  if (!response.ok) {
    const message = await readApiErrorMessage(response);
    throw new Error(message);
  }
  return response.json();
}

const STOCK_QUOTE_CACHE_PREFIX = "stock_quote_";
const STOCK_QUOTE_CACHE_MS = 24 * 60 * 60 * 1000; // 1 day

export async function getStockQuote(symbol) {
  const cacheKey = `${STOCK_QUOTE_CACHE_PREFIX}${symbol.toUpperCase()}`;
  const cached = localStorage.getItem(cacheKey);
  if (cached) {
    try {
      const { data, fetchedAt } = JSON.parse(cached);
      const hasValidPrice = data?.price != null && !Number.isNaN(Number(data.price));
      if (Date.now() - fetchedAt < STOCK_QUOTE_CACHE_MS && hasValidPrice) {
        return data;
      }
    } catch {
      // invalid cache, fetch fresh
    }
  }

  const response = await apiFetch(
    `/api/stocks/quote?symbol=${encodeURIComponent(symbol)}`
  );
  if (!response.ok) {
    if (response.status === 404) return null;
    const message = await readApiErrorMessage(response);
    throw new Error(message || "Failed to fetch quote");
  }
  const data = await response.json();
  localStorage.setItem(
    cacheKey,
    JSON.stringify({ data, fetchedAt: Date.now() })
  );
  return data;
}

export async function buyStock(payload) {
  const response = await apiFetch("/api/portfolio/buy", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    const message = await readApiErrorMessage(response);
    throw new Error(message);
  }
  return response.json();
}

export async function sellHolding(payload) {
  const response = await apiFetch("/api/portfolio/sell", {
    method: "POST",
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const message = await readApiErrorMessage(response);
    throw new Error(message || "Failed to sell holding");
  }
}

async function authRequest(path, payload) {
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
  } catch (error) {
    throw new Error("Unable to reach the server. Is it running?");
  }

  if (!response.ok) {
    const message = await readErrorMessage(response);
    throw new Error(message);
  }
  return response.json();
}

async function readErrorMessage(response) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    try {
      const data = await response.json();
      if (data?.message) {
        return data.message;
      }
      if (data?.error) {
        return data.error;
      }
    } catch {
      // fall through to generic message
    }
  }

  if (response.status === 401) {
    return "Session expired. Please log in again.";
  }
  if (response.status === 409) {
    return "Email already registered.";
  }
  if (response.status === 400) {
    return "Email and password are required.";
  }
  return "Authentication failed. Please try again.";
}

async function readApiErrorMessage(response) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    try {
      const data = await response.json();
      if (data?.message) return data.message;
      if (data?.detail) return data.detail;
      if (data?.title) return data.title;
      if (data?.error) return data.error;
    } catch {
      // fall through
    }
  }
  if (response.status === 400) {
    return "Invalid input.";
  }
  if (response.status === 404) {
    return "Holding not found.";
  }
  if (response.status === 503) {
    return "Service temporarily unavailable. Check API key and rate limits.";
  }
  return "Request failed. Please try again.";
}

export async function apiFetch(path, options = {}, retry = true) {
  const headers = new Headers(options.headers || {});
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = getAccessToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    });
  } catch (error) {
    throw new Error("Unable to reach backend server. It may be restarting.");
  }
  if (response.status === 401 && retry) {
    const refreshed = await refreshTokens();
    if (refreshed) {
      return apiFetch(path, options, false);
    }
  }
  return response;
}

async function refreshTokens() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearTokens();
    return false;
  }
  const response = await fetch(`${API_BASE}/api/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) {
    clearTokens();
    return false;
  }
  const data = await response.json();
  setTokens(data.accessToken, data.refreshToken);
  return true;
}
