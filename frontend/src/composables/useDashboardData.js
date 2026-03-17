import { computed, ref } from "vue";
import { apiFetch } from "../api";

export function useDashboardData() {
  const summary = ref({
    totalValue: 0,
    portfolioValue: 0,
    profitLoss: 0,
    holdingsCount: 0,
    assetsCount: 0,
  });
  const assets = ref([]);
  const holdings = ref([]);
  const transactions = ref([]);
  const error = ref("");
  const loading = ref(false);

  const assetTypeBreakdown = computed(() => {
    const counts = assets.value.reduce((acc, asset) => {
      const key = asset.type || "OTHER";
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});
    return Object.entries(counts).map(([type, count]) => ({ type, count }));
  });

  async function loadDashboard({ onUnauthorized } = {}) {
    loading.value = true;
    error.value = "";
    try {
      const [summaryData, assetsData, holdingsData, transactionsData] =
        await Promise.all([
          fetchJson("/api/summary"),
          fetchJson("/api/assets"),
          fetchJson("/api/holdings"),
          fetchJson("/api/transactions"),
        ]);
      summary.value = summaryData;
      assets.value = assetsData;
      holdings.value = holdingsData;
      transactions.value = transactionsData;
    } catch (err) {
      if (err.status === 401 && typeof onUnauthorized === "function") {
        onUnauthorized();
        return;
      }
      error.value = err.message || "Failed to load dashboard data.";
    } finally {
      loading.value = false;
    }
  }

  function resetDashboard() {
    summary.value = {
      totalValue: 0,
      portfolioValue: 0,
      profitLoss: 0,
      holdingsCount: 0,
      assetsCount: 0,
    };
    assets.value = [];
    holdings.value = [];
    transactions.value = [];
    error.value = "";
    loading.value = false;
  }

  async function fetchJson(path) {
    const response = await apiFetch(path);
    if (!response.ok) {
      const err = new Error(`Request failed for ${path}`);
      err.status = response.status;
      throw err;
    }
    return response.json();
  }

  return {
    summary,
    assets,
    holdings,
    transactions,
    error,
    loading,
    assetTypeBreakdown,
    loadDashboard,
    resetDashboard,
  };
}
