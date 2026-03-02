<template>
  <div class="app-shell">
    <aside v-if="isAuthenticated" class="sidebar">
      <div class="sidebar-user">
        <p class="sidebar-user-label">Signed in as</p>
        <p class="sidebar-user-email">{{ userEmail }}</p>
      </div>
      <button
        type="button"
        class="nav-button"
        :class="{ active: viewMode === 'dashboard' }"
        @click="viewMode = 'dashboard'"
      >
        Dashboard
      </button>
      <button
        type="button"
        class="nav-button"
        :class="{ active: viewMode === 'holdings' }"
        @click="viewMode = 'holdings'"
      >
        Holdings
      </button>
      <button
        type="button"
        class="nav-button refresh-account-button"
        :disabled="refreshingAccount"
        @click="refreshAccount"
      >
        {{ refreshingAccount ? "Refreshing..." : "Refresh Account" }}
      </button>
    </aside>

    <main class="page">
      <AppHeader
        :user="user"
        :error="error"
        :loading="loading"
        :is-authenticated="isAuthenticated"
        :auth-mode="authMode"
        @toggle-auth-mode="toggleAuthMode"
        @logout="logout"
        @add-asset="openAddAsset"
      />

      <AuthPanel
        v-if="!isAuthenticated"
        v-model:email="authEmail"
        v-model:password="authPassword"
        :auth-mode="authMode"
        :auth-error="authError"
        @submit="handleAuth"
      />

      <div
        v-if="isAuthenticated && viewMode === 'dashboard'"
        class="dashboard-content"
      >
        <SummaryCards
          :summary="summary"
          :total-value="profitLossDisplay"
          :asset-type-breakdown="assetTypeBreakdown"
          :holdings="holdings"
          :transactions="transactions"
          :format-type="formatType"
          :format-currency="formatCurrency"
        />

        <section class="grid dashboard-grid">
          <HoldingsPanel
            class="holdings-panel-wide"
            :holdings="holdings"
            :format-currency="formatCurrency"
            @sell="openSellModal"
          />
          <AssetTypePanel
            class="asset-type-panel-third"
            :items="assetTypeBreakdown"
            :format-type="formatType"
          />
          <ActivityPanel
            class="activity-panel-two-thirds"
            :transactions="recentTransactions"
            :format-date="formatDate"
            :format-currency="formatCurrency"
          />
        </section>
      </div>

      <section
        v-if="isAuthenticated && viewMode === 'holdings'"
        class="holdings-focus"
      >
        <HoldingsPanel
          :holdings="holdings"
          :format-currency="formatCurrency"
          @sell="openSellModal"
        />
      </section>

      <AddAssetModal
        :open="showAddAsset"
        @close="closeAddAsset"
        @added="handleAssetAdded"
      />
      <SellHoldingModal
        :open="showSellModal"
        :holding="selectedHolding"
        @close="closeSellModal"
        @sold="handleHoldingSold"
      />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import ActivityPanel from "./components/ActivityPanel.vue";
import AddAssetModal from "./components/AddAssetModal.vue";
import AppHeader from "./components/AppHeader.vue";
import AssetTypePanel from "./components/AssetTypePanel.vue";
import AuthPanel from "./components/AuthPanel.vue";
import HoldingsPanel from "./components/HoldingsPanel.vue";
import SellHoldingModal from "./components/SellHoldingModal.vue";
import SummaryCards from "./components/SummaryCards.vue";
import {
  apiFetch,
  clearTokens,
  getAccessToken,
  login,
  register,
  setTokens,
} from "./api";

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
const user = ref(null);

const authMode = ref("login");
const authEmail = ref("");
const authPassword = ref("");
const authError = ref("");

const accessToken = ref(getAccessToken());
const isAuthenticated = computed(() => Boolean(accessToken.value));
const viewMode = ref("dashboard");
const showAddAsset = ref(false);
const showSellModal = ref(false);
const selectedHolding = ref(null);
const refreshingAccount = ref(false);

const userEmail = computed(() => user.value?.email || "Unknown user");

const profitLossDisplay = computed(() =>
  formatCurrency(summary.value.profitLoss),
);

const assetTypeBreakdown = computed(() => {
  const counts = assets.value.reduce((acc, asset) => {
    const key = asset.type || "OTHER";
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});
  return Object.entries(counts).map(([type, count]) => ({ type, count }));
});

const recentTransactions = computed(() => transactions.value.slice(0, 5));

onMounted(() => {
  if (isAuthenticated.value) {
    loadDashboard();
  }
});

async function loadDashboard() {
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
    if (err.status === 401) {
      logout();
    }
    error.value = err.message || "Failed to load dashboard data.";
  } finally {
    loading.value = false;
  }
}

async function handleAuth() {
  authError.value = "";
  try {
    const response =
      authMode.value === "login"
        ? await login(authEmail.value, authPassword.value)
        : await register(authEmail.value, authPassword.value);
    setTokens(response.accessToken, response.refreshToken);
    accessToken.value = response.accessToken;
    user.value = response.user;
    authPassword.value = "";
    await loadDashboard();
  } catch (err) {
    authError.value = err.message || "Authentication failed.";
  }
}

function toggleAuthMode() {
  authMode.value = authMode.value === "login" ? "register" : "login";
  authError.value = "";
}

function logout() {
  clearTokens();
  accessToken.value = "";
  viewMode.value = "dashboard";
  user.value = null;
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
  showSellModal.value = false;
  selectedHolding.value = null;
}

function openAddAsset() {
  showAddAsset.value = true;
}

function closeAddAsset() {
  showAddAsset.value = false;
}

async function handleAssetAdded() {
  await loadDashboard();
}

function openSellModal(holding) {
  selectedHolding.value = holding;
  showSellModal.value = true;
}

function closeSellModal() {
  showSellModal.value = false;
  selectedHolding.value = null;
}

async function handleHoldingSold() {
  await loadDashboard();
}

async function refreshAccount() {
  if (refreshingAccount.value) return;
  const confirmed = window.confirm(
    "This will remove assets, holdings, and transactions for your account. Continue?",
  );
  if (!confirmed) return;
  refreshingAccount.value = true;
  error.value = "";
  try {
    const response = await apiFetch("/api/account/refresh", { method: "POST" });
    if (!response.ok) {
      const err = new Error("Failed to refresh account data.");
      err.status = response.status;
      throw err;
    }
    showSellModal.value = false;
    selectedHolding.value = null;
    await loadDashboard();
  } catch (err) {
    if (err.status === 401) {
      logout();
      return;
    }
    error.value = err.message || "Failed to refresh account data.";
  } finally {
    refreshingAccount.value = false;
  }
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

function formatCurrency(value) {
  const numeric = Number(value || 0);
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(numeric);
}

function formatType(type) {
  if (!type) return "Other";
  return type
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

function formatDate(value) {
  if (!value) return "";
  return new Date(value).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}
</script>

<style scoped>
.app-shell {
  display: flex;
  height: 100dvh;
  overflow: hidden;
}

.page {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(138, 147, 166, 0.35) transparent;
}

.page::-webkit-scrollbar {
  width: 8px;
}

.page::-webkit-scrollbar-track {
  background: transparent;
}

.page::-webkit-scrollbar-thumb {
  background: rgba(138, 147, 166, 0.35);
  border-radius: 999px;
}

.page::-webkit-scrollbar-thumb:hover {
  background: rgba(138, 147, 166, 0.55);
}

.sidebar {
  width: 200px;
  border-right: 1px solid #1f2633;
  background: #0f141c;
  padding: 24px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: sticky;
  top: 0;
  height: 100dvh;
  box-sizing: border-box;
  overflow: hidden;
}

.sidebar-user {
  border: 1px solid #1f2633;
  border-radius: 10px;
  padding: 10px 12px;
  background: #151a22;
  margin-bottom: 2px;
}

.sidebar-user-label {
  margin: 0 0 4px;
  color: #8a93a6;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.sidebar-user-email {
  margin: 0;
  color: #eef2f6;
  font-size: 14px;
  font-weight: 600;
  word-break: break-word;
}

.nav-button {
  background: #151a22;
  border: 1px solid #1f2633;
  color: #eef2f6;
  border-radius: 10px;
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
  cursor: pointer;
}

.nav-button.active {
  border-color: #4f46e5;
  background: rgba(79, 70, 229, 0.18);
}

.refresh-account-button {
  margin-top: auto;
  border-color: #7f1d1d;
  background: rgba(127, 29, 29, 0.25);
}

.refresh-account-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.holdings-focus {
  display: block;
}

.holdings-panel-wide {
  grid-column: 1 / -1;
}

.dashboard-content {
  display: grid;
  gap: 16px;
}

.dashboard-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.asset-type-panel-third {
  grid-column: span 1;
}

.activity-panel-two-thirds {
  grid-column: span 2;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .asset-type-panel-third,
  .activity-panel-two-thirds,
  .holdings-panel-wide {
    grid-column: 1 / -1;
  }
}
</style>
