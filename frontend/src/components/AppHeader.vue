<template>
  <header class="header">
    <div>
      <p class="eyebrow">Portfolio dashboard</p>
      <h1>Investment Tracker</h1>
      <p class="muted" v-if="user">Signed in as {{ user.email }}</p>
      <p class="muted" v-if="error">{{ error }}</p>
      <p class="muted" v-if="loading && isAuthenticated">Loading data...</p>
    </div>
    <div class="header-actions">
      <button
        v-if="!isAuthenticated"
        class="action secondary"
        type="button"
        @click="$emit('toggle-auth-mode')"
      >
        {{ authMode === "login" ? "Create account" : "Have an account?" }}
      </button>
      <button v-if="isAuthenticated" class="action secondary" type="button" @click="$emit('logout')">
        Log out
      </button>
      <button class="action" :disabled="!isAuthenticated" @click="$emit('add-asset')">
        Add asset
      </button>
    </div>
  </header>
</template>

<script setup>
defineProps({
  user: {
    type: Object,
    default: null
  },
  error: {
    type: String,
    default: ""
  },
  loading: {
    type: Boolean,
    default: false
  },
  isAuthenticated: {
    type: Boolean,
    default: false
  },
  authMode: {
    type: String,
    default: "login"
  }
});

defineEmits(["toggle-auth-mode", "logout", "add-asset"]);
</script>

<style scoped src="../styles/components/app-header.css"></style>
