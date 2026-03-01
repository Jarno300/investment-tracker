<template>
  <div class="app-shell">
    <aside v-if="isAuthenticated" class="sidebar">
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

      <SummaryCards
        v-if="isAuthenticated && viewMode === 'dashboard'"
        :summary="summary"
        :total-value="totalValueDisplay"
        :asset-type-breakdown="assetTypeBreakdown"
        :holdings="holdings"
        :format-type="formatType"
      />

      <section v-if="isAuthenticated && viewMode === 'dashboard'" class="grid">
        <AssetTypePanel :items="assetTypeBreakdown" :format-type="formatType" />
        <HoldingsPanel
          :holdings="holdings"
          :format-currency="formatCurrency"
          @sell="openSellModal"
        />
        <ActivityPanel :transactions="transactions" :format-date="formatDate" />
      </section>

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

const summary = ref({ totalValue: 0, holdingsCount: 0, assetsCount: 0 });
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

const totalValueDisplay = computed(() =>
  formatCurrency(summary.value.totalValue),
);

const assetTypeBreakdown = computed(() => {
  const counts = assets.value.reduce((acc, asset) => {
    const key = asset.type || "OTHER";
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});
  return Object.entries(counts).map(([type, count]) => ({ type, count }));
});

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
    transactions.value = transactionsData.slice(0, 5);
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
  summary.value = { totalValue: 0, holdingsCount: 0, assetsCount: 0 };
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
  min-height: 100vh;
}

.page {
  flex: 1;
}

.sidebar {
  width: 200px;
  border-right: 1px solid #1f2633;
  background: #0f141c;
  padding: 24px 12px;
  display: grid;
  gap: 10px;
  align-content: start;
  position: sticky;
  top: 0;
  height: 100vh;
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

.holdings-focus {
  display: block;
}
</style>
