<template>
  <aside v-if="isAuthenticated" class="sidebar">
    <div class="sidebar-user">
      <p class="sidebar-user-label">Signed in as</p>
      <p class="sidebar-user-email">{{ userEmail }}</p>
    </div>
    <button
      type="button"
      class="nav-button"
      :class="{ active: viewMode === 'dashboard' }"
      @click="setViewMode('dashboard')"
    >
      Dashboard
    </button>
    <button
      type="button"
      class="nav-button"
      :class="{ active: viewMode === 'holdings' }"
      @click="setViewMode('holdings')"
    >
      Holdings
    </button>
    <button
      type="button"
      class="nav-button refresh-account-button"
      :disabled="refreshingAccount"
      @click="emit('refresh-account')"
    >
      {{ refreshingAccount ? "Refreshing..." : "Refresh Account" }}
    </button>
  </aside>
</template>

<script setup>
const props = defineProps({
  isAuthenticated: {
    type: Boolean,
    default: false,
  },
  userEmail: {
    type: String,
    default: "Unknown user",
  },
  viewMode: {
    type: String,
    required: true,
  },
  refreshingAccount: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["update:viewMode", "refresh-account"]);

function setViewMode(mode) {
  if (props.viewMode === mode) return;
  emit("update:viewMode", mode);
}
</script>

<style scoped>
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
</style>
