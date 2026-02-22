<template>
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
      v-if="isAuthenticated"
      :summary="summary"
      :total-value="totalValueDisplay"
      :asset-type-breakdown="assetTypeBreakdown"
      :holdings="holdings"
      :format-type="formatType"
    />

    <section v-if="isAuthenticated" class="grid">
      <AssetTypePanel :items="assetTypeBreakdown" :format-type="formatType" />
      <HoldingsPanel :holdings="holdings" :format-currency="formatCurrency" />
      <ActivityPanel :transactions="transactions" :format-date="formatDate" />
    </section>

    <AddAssetModal
      :open="showAddAsset"
      @close="closeAddAsset"
      @added="handleAssetAdded"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import ActivityPanel from "./components/ActivityPanel.vue";
import AddAssetModal from "./components/AddAssetModal.vue";
import AppHeader from "./components/AppHeader.vue";
import AssetTypePanel from "./components/AssetTypePanel.vue";
import AuthPanel from "./components/AuthPanel.vue";
import HoldingsPanel from "./components/HoldingsPanel.vue";
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
const showAddAsset = ref(false);

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
  user.value = null;
  summary.value = { totalValue: 0, holdingsCount: 0, assetsCount: 0 };
  assets.value = [];
  holdings.value = [];
  transactions.value = [];
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

async function fetchJson(path) {
  const response = await apiFetch(path);
  if (!response.ok) {
    throw new Error(`Request failed for ${path}`);
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
