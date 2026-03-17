<template>
  <div class="app-shell">
    <AppSidebar
      :is-authenticated="isAuthenticated"
      :user-email="userEmail"
      :view-mode="viewMode"
      :refreshing-account="refreshingAccount"
      @update:view-mode="viewMode = $event"
      @refresh-account="refreshAccount"
    />

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

      <DashboardView
        v-if="isAuthenticated && viewMode === 'dashboard'"
        :summary="summary"
        :total-value="profitLossDisplay"
        :asset-type-breakdown="assetTypeBreakdown"
        :holdings="holdings"
        :transactions="transactions"
        :format-type="formatType"
        :format-currency="formatCurrency"
        :format-date="formatDate"
        @sell="openSellModal"
      />

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
import AddAssetModal from "./components/AddAssetModal.vue";
import AppHeader from "./components/AppHeader.vue";
import AppSidebar from "./components/AppSidebar.vue";
import AuthPanel from "./components/AuthPanel.vue";
import DashboardView from "./components/DashboardView.vue";
import HoldingsPanel from "./components/HoldingsPanel.vue";
import SellHoldingModal from "./components/SellHoldingModal.vue";
import {
  apiFetch,
  clearTokens,
  getAccessToken,
  login,
  register,
  setTokens,
} from "./api";
import { useDashboardData } from "./composables/useDashboardData";
import { formatCurrency, formatDate, formatType } from "./composables/useFormatters";

const {
  summary,
  holdings,
  transactions,
  error,
  loading,
  assetTypeBreakdown,
  loadDashboard: loadDashboardState,
  resetDashboard,
} = useDashboardData();
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

onMounted(() => {
  if (isAuthenticated.value) {
    loadDashboard();
  }
});

async function loadDashboard() {
  await loadDashboardData();
}

async function loadDashboardData() {
  await loadDashboardState({ onUnauthorized: logout });
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
    await loadDashboardData();
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
  resetDashboard();
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
  await loadDashboardData();
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
  await loadDashboardData();
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
    await loadDashboardData();
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

.holdings-focus {
  display: block;
}
</style>
