<template>
  <div v-if="open" class="modal-backdrop" @click.self="close">
    <div class="modal">
      <header class="modal-header">
        <div>
          <p class="eyebrow">Add asset</p>
          <h2>Search stocks</h2>
        </div>
        <button class="icon-button" type="button" @click="close">✕</button>
      </header>

      <div class="modal-body">
        <label class="field">
          Stock name or symbol
          <input v-model.trim="query" type="text" placeholder="e.g. Apple or AAPL" />
        </label>
        <button class="action secondary" type="button" @click="runSearch" :disabled="searching">
          {{ searching ? "Searching..." : "Search" }}
        </button>

        <p v-if="searchError" class="muted">{{ searchError }}</p>

        <ul v-if="results.length" class="results">
          <li
            v-for="result in results"
            :key="result.symbol"
            :class="['result-item', { selected: selected?.symbol === result.symbol }]"
            @click="selectResult(result)"
          >
            <div>
              <strong>{{ result.symbol }}</strong>
              <span class="muted">{{ result.name }}</span>
            </div>
            <span class="muted">{{ result.region }}</span>
          </li>
        </ul>

        <p v-else-if="query && !searching" class="muted">No results yet. Try another search.</p>

        <div v-if="selected" class="buy-form">
          <h3>Buy {{ selected.symbol }}</h3>
          <div class="form-grid">
            <label class="field">
              Quantity
              <input v-model.number="quantity" type="number" min="0" step="0.0001" />
            </label>
            <label class="field">
              Price paid ({{ selected.currency || "USD" }})
              <input v-model.number="price" type="number" min="0" step="0.01" />
            </label>
          </div>
          <button class="action" type="button" @click="submit" :disabled="saving">
            {{ saving ? "Saving..." : "Add to portfolio" }}
          </button>
          <p v-if="saveError" class="muted">{{ saveError }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from "vue";
import { buyStock, searchStocks } from "../api";

const props = defineProps({
  open: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(["close", "added"]);

const query = ref("");
const results = ref([]);
const selected = ref(null);
const searching = ref(false);
const searchError = ref("");

const quantity = ref(0);
const price = ref(0);
const saving = ref(false);
const saveError = ref("");

let debounceTimer;

watch(
  () => props.open,
  () => {
    if (!props.open) {
      resetState();
    }
  }
);

watch(query, () => {
  clearTimeout(debounceTimer);
  if (!query.value) {
    results.value = [];
    selected.value = null;
    return;
  }
  debounceTimer = setTimeout(runSearch, 400);
});

function close() {
  emit("close");
}

async function runSearch() {
  if (!query.value || query.value.length < 2) {
    return;
  }
  searching.value = true;
  searchError.value = "";
  try {
    results.value = await searchStocks(query.value);
  } catch (error) {
    searchError.value = error.message || "Failed to search stocks.";
  } finally {
    searching.value = false;
  }
}

function selectResult(result) {
  selected.value = result;
  quantity.value = 0;
  price.value = 0;
  saveError.value = "";
}

async function submit() {
  if (!selected.value) return;
  if (quantity.value <= 0 || price.value <= 0) {
    saveError.value = "Quantity and price must be greater than zero.";
    return;
  }
  saving.value = true;
  saveError.value = "";
  try {
    await buyStock({
      symbol: selected.value.symbol,
      name: selected.value.name,
      currency: selected.value.currency || "USD",
      quantity: quantity.value,
      price: price.value
    });
    emit("added");
    close();
  } catch (error) {
    saveError.value = error.message || "Failed to add asset.";
  } finally {
    saving.value = false;
  }
}

function resetState() {
  query.value = "";
  results.value = [];
  selected.value = null;
  searching.value = false;
  searchError.value = "";
  quantity.value = 0;
  price.value = 0;
  saving.value = false;
  saveError.value = "";
}
</script>

<style scoped src="../styles/components/add-asset-modal.css"></style>
