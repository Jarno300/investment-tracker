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
          <input
            v-model.trim="query"
            type="text"
            placeholder="e.g. Apple or AAPL"
          />
        </label>
        <button
          class="action secondary"
          type="button"
          @click="runSearch"
          :disabled="searching"
        >
          {{ searching ? "Searching..." : "Search" }}
        </button>

        <p v-if="searchError" class="muted">{{ searchError }}</p>

        <ul v-if="results.length" class="results">
          <li
            v-for="result in results"
            :key="result.symbol"
            :class="[
              'result-item',
              { selected: selected?.symbol === result.symbol },
            ]"
            @click="selectResult(result)"
          >
            <div>
              <strong>{{ result.symbol }}</strong>
              <span class="muted">{{ result.name }}</span>
            </div>
            <span class="muted">{{ result.region }}</span>
          </li>
        </ul>

        <p v-else-if="query && !searching" class="muted">
          No results yet. Try another search.
        </p>

        <div v-if="selected" class="buy-form">
          <h3>Buy {{ selected.symbol }}</h3>
          <div class="quote-area">
            <p v-if="quoteLoading" class="muted">Fetching current price...</p>
            <p v-else-if="currentQuote?.price != null" class="current-price">
              Current price: {{ formatPrice(currentQuote.price) }}
              <button
                v-if="currentQuote?.price"
                type="button"
                class="link-button"
                @click="price = currentQuote.price"
              >
                Use this price
              </button>
            </p>
            <p v-else-if="quoteError" class="quote-error">{{ quoteError }}</p>
            <p v-else class="muted">Enter the price manually below.</p>
          </div>
          <div class="form-grid">
            <label class="field">
              Quantity
              <input
                v-model.number="quantity"
                type="number"
                min="0"
                step="0.0001"
              />
            </label>
            <label class="field">
              Price paid ({{ selected.currency || "USD" }})
              <input v-model.number="price" type="number" min="0" step="0.01" />
            </label>
            <label class="field">
              Transaction costs ({{ selected.currency || "USD" }})
              <input v-model.number="costs" type="number" min="0" step="0.01" />
            </label>
          </div>
          <button
            class="action"
            type="button"
            @click="submit"
            :disabled="saving"
          >
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
import { buyStock, getStockQuote, searchStocks } from "../api";

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["close", "added"]);

const query = ref("");
const results = ref([]);
const selected = ref(null);
const searching = ref(false);
const searchError = ref("");

const quantity = ref(0);
const price = ref(0);
const costs = ref(0);
const saving = ref(false);
const saveError = ref("");

const currentQuote = ref(null);
const quoteLoading = ref(false);
const quoteError = ref("");

let debounceTimer;

watch(
  () => props.open,
  () => {
    if (!props.open) {
      resetState();
    }
  },
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

watch(
  () => selected.value?.symbol,
  async (symbol) => {
    currentQuote.value = null;
    quoteError.value = "";
    if (!symbol) return;
    quoteLoading.value = true;
    try {
      currentQuote.value = await getStockQuote(symbol);
      if (!currentQuote.value) {
        quoteError.value = "Price unavailable for this symbol.";
      }
    } catch (err) {
      quoteError.value = err.message || "Could not fetch price.";
    } finally {
      quoteLoading.value = false;
    }
  },
);

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
  costs.value = 0;
  saveError.value = "";
}

function formatPrice(value) {
  const n = Number(value);
  if (Number.isNaN(n)) return "—";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: selected.value?.currency || "USD",
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(n);
}

async function submit() {
  if (!selected.value) return;
  if (quantity.value <= 0 || price.value <= 0 || costs.value < 0) {
    saveError.value =
      "Quantity and price must be greater than zero, and costs cannot be negative.";
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
      price: price.value,
      costs: costs.value || 0,
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
  costs.value = 0;
  saving.value = false;
  saveError.value = "";
  currentQuote.value = null;
  quoteLoading.value = false;
  quoteError.value = "";
}
</script>

<style scoped src="../styles/components/add-asset-modal.css"></style>
