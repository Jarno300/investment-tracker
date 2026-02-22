<template>
  <section class="panel auth-panel">
    <h2>{{ authMode === "login" ? "Sign in" : "Create account" }}</h2>
    <p class="muted">
      {{ authMode === "login"
        ? "Use your account to access your portfolio."
        : "Register a new account to start tracking investments." }}
    </p>
    <form class="auth-form" @submit.prevent="$emit('submit')">
      <label class="field">
        Email
        <input
          :value="email"
          type="email"
          autocomplete="email"
          required
          @input="$emit('update:email', $event.target.value)"
        />
      </label>
      <label class="field">
        Password
        <input
          :value="password"
          type="password"
          autocomplete="current-password"
          required
          @input="$emit('update:password', $event.target.value)"
        />
      </label>
      <button class="action" type="submit">
        {{ authMode === "login" ? "Sign in" : "Register" }}
      </button>
      <p v-if="authError" class="muted">{{ authError }}</p>
    </form>
  </section>
</template>

<script setup>
defineProps({
  authMode: {
    type: String,
    default: "login"
  },
  authError: {
    type: String,
    default: ""
  },
  email: {
    type: String,
    default: ""
  },
  password: {
    type: String,
    default: ""
  }
});

defineEmits(["submit", "update:email", "update:password"]);
</script>

<style scoped src="../styles/components/auth-panel.css"></style>
