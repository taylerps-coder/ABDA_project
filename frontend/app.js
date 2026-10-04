// ============================================================================
// Restaurant Discovery & Analytics - Frontend Logic
// Uses Vanilla JavaScript & standard fetch() API
// ============================================================================

// Global cache for current restaurants on screen
let currentRestaurants = [];

// DOM Elements
const connectionBadge = document.getElementById("connectionStatus");
const statusText = document.getElementById("statusText");
const connectionAlert = document.getElementById("connectionAlert");
const connectionAlertMsg = document.getElementById("connectionAlertMsg");

const statRestaurants = document.getElementById("statRestaurants");
const statCuisines = document.getElementById("statCuisines");
const statBoroughs = document.getElementById("statBoroughs");
const statAvgScore = document.getElementById("statAvgScore");

const searchForm = document.getElementById("searchForm");
const btnClearSearch = document.getElementById("btnClearSearch");
const resultCount = document.getElementById("resultCount");
const restaurantsTableBody = document.getElementById("restaurantsTableBody");
const tableView = document.getElementById("tableView");
const cardView = document.getElementById("cardView");
const viewToggle = document.getElementById("viewToggle");
const btnExportCSV = document.getElementById("btnExportCSV");
const btnRefresh = document.getElementById("btnRefresh");
let currentView = localStorage.getItem("restaurantView") || "table";

const addRestaurantForm = document.getElementById("addRestaurantForm");

const editModal = document.getElementById("editModal");
const editRestaurantForm = document.getElementById("editRestaurantForm");
const btnCloseEdit = document.getElementById("btnCloseEdit");
const btnCancelEdit = document.getElementById("btnCancelEdit");

const tableCuisineBody = document.getElementById("cuisineBody");
const tableBoroughBody = document.getElementById("boroughBody");
const tableAvgScoreBody = document.getElementById("avgScoreBody");

const btnCreateIndexes = document.getElementById("btnCreateIndexes");
const statusIndexName = document.getElementById("statusIndexName");
const statusIndexCuisine = document.getElementById("statusIndexCuisine");
const statusIndexCompound = document.getElementById("statusIndexCompound");
const allIndexesText = document.getElementById("allIndexesText");

// ============================================================================
// Animated Counter Helper
// ============================================================================
function animateValue(element, end, duration = 600, isFloat = false) {
  const start = 0;
  const startTime = performance.now();

  function update(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    // Ease out cubic
    const eased = 1 - Math.pow(1 - progress, 3);
    const current = start + (end - start) * eased;

    if (isFloat) {
      element.textContent = current.toFixed(1);
    } else {
      element.textContent = Math.floor(current).toLocaleString();
    }

    if (progress < 1) {
      requestAnimationFrame(update);
    } else {
      element.textContent = isFloat ? end.toFixed(1) : Number(end).toLocaleString();
    }
  }

  requestAnimationFrame(update);
}

// ============================================================================
// Notification Toast
// ============================================================================
function showToast(message, type = "success") {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.className = `toast ${type}`;
  toast.classList.remove("hidden");
  setTimeout(() => {
    toast.classList.add("hidden");
  }, 3500);
}

// ============================================================================
// 1. Health & Connection Status
// ============================================================================
async function checkHealth() {
  try {
    const res = await fetch("/api/health");
    const data = await res.json();

    if (data.mongoConnected) {
      connectionBadge.className = "status-badge connected";
      statusText.textContent = "MongoDB Atlas: Connected";
      connectionAlert.classList.add("hidden");
    } else {
      connectionBadge.className = "status-badge error";
      statusText.textContent = "MongoDB Atlas: Connection Failed";
      connectionAlert.classList.remove("hidden");
      connectionAlertMsg.textContent = data.message || "Please check MONGODB_URI in .env";
    }
  } catch (err) {
    connectionBadge.className = "status-badge error";
    statusText.textContent = "Backend Unreachable";
    connectionAlert.classList.remove("hidden");
    connectionAlertMsg.textContent = "Could not communicate with Scala Cask server.";
  }
}

// ============================================================================
// 2. Dashboard Statistics
// ============================================================================
async function loadDashboard() {
  try {
    const res = await fetch("/api/dashboard");
    if (!res.ok) return;
    const data = await res.json();

    animateValue(statRestaurants, Number(data.totalRestaurants), 800);
    animateValue(statCuisines, Number(data.totalCuisines), 700);
    animateValue(statBoroughs, Number(data.totalBoroughs), 600);
    animateValue(statAvgScore, Number(data.averageScore), 900, true);
  } catch (err) {
    console.error("Failed to load dashboard:", err);
  }
}

// ============================================================================
// 3. Restaurants CRUD - READ & SEARCH
// ============================================================================
async function loadRestaurants(queryParams = "") {
  restaurantsTableBody.innerHTML = `<tr><td colspan="6" class="text-center">Loading restaurants from MongoDB Atlas…</td></tr>`;
  if (cardView) cardView.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 2.5rem; color: var(--text-secondary);">Loading restaurants from MongoDB Atlas…</div>`;
  resultCount.textContent = "Loading…";

  try {
    const url = queryParams ? `/api/restaurants/search?${queryParams}` : "/api/restaurants?limit=30";
    const res = await fetch(url);
    const data = await res.json();

    currentRestaurants = data;
    renderRestaurantsTable(data);
  } catch (err) {
    restaurantsTableBody.innerHTML = `<tr><td colspan="6" class="text-center" style="color: var(--accent-rose);">Failed to load restaurants: ${err.message}</td></tr>`;
    if (cardView) cardView.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 2.5rem; color: var(--accent-rose);">Failed to load restaurants: ${err.message}</div>`;
    resultCount.textContent = "0 restaurants";
  }
}

function renderRestaurantsTable(list) {
  resultCount.textContent = `Showing ${list.length} restaurant${list.length === 1 ? "" : "s"}`;

  if (!list || list.length === 0) {
    restaurantsTableBody.innerHTML = `<tr><td colspan="6" class="text-center">No restaurants found matching your criteria.</td></tr>`;
    if (cardView) {
      cardView.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 2.5rem; color: var(--text-secondary); background: var(--bg-card); border-radius: var(--radius-lg); border: 1px dashed var(--border-medium);">No restaurants found matching your criteria.</div>`;
    }
    return;
  }

  // Render Table View Rows
  restaurantsTableBody.innerHTML = list.map((r, index) => `
    <tr style="animation: fadeInUp 0.3s ease-out ${index * 0.02}s both;">
      <td><strong>${escapeHtml(r.name)}</strong></td>
      <td><span class="badge badge-cuisine">${escapeHtml(r.cuisine)}</span></td>
      <td><span class="badge badge-borough">${escapeHtml(r.borough)}</span></td>
      <td>${escapeHtml(r.zipcode || "N/A")}</td>
      <td><span class="score-pill">${r.score ? r.score.toFixed(1) : "0.0"}</span></td>
      <td class="text-right">
        <div class="actions-cell">
          <button class="btn btn-secondary btn-sm" onclick="openEditModal('${r.id}')">✏️ Edit</button>
          <button class="btn btn-danger btn-sm" onclick="deleteRestaurant('${r.id}', '${escapeQuotes(r.name)}')">🗑️ Delete</button>
        </div>
      </td>
    </tr>
  `).join("");

  // Render Card Grid View
  if (cardView) {
    cardView.innerHTML = list.map((r, index) => `
      <div class="restaurant-card" style="animation: fadeInUp 0.3s ease-out ${index * 0.02}s both;">
        <div class="restaurant-card-name">${escapeHtml(r.name)}</div>
        <div class="restaurant-card-meta">
          <span class="badge badge-cuisine">${escapeHtml(r.cuisine)}</span>
          <span class="badge badge-borough">${escapeHtml(r.borough)}</span>
          <span class="score-pill">⭐ ${r.score ? r.score.toFixed(1) : "0.0"}</span>
        </div>
        <div class="restaurant-card-detail">
          <span class="label">ZIP:</span>
          <span>${escapeHtml(r.zipcode || "N/A")}</span>
        </div>
        ${(r.street || r.building) ? `
        <div class="restaurant-card-detail">
          <span class="label">Addr:</span>
          <span>${escapeHtml((r.building ? r.building + ' ' : '') + (r.street || ''))}</span>
        </div>` : ''}
        <div class="restaurant-card-actions">
          <button class="btn btn-secondary btn-sm" onclick="openEditModal('${r.id}')">✏️ Edit</button>
          <button class="btn btn-danger btn-sm" onclick="deleteRestaurant('${r.id}', '${escapeQuotes(r.name)}')">🗑️ Delete</button>
        </div>
      </div>
    `).join("");
  }
}

// View Switcher (Table / Card)
function setRestaurantView(view) {
  currentView = view;
  localStorage.setItem("restaurantView", view);

  if (viewToggle) {
    const btns = viewToggle.querySelectorAll(".view-btn");
    btns.forEach(b => {
      if (b.getAttribute("data-view") === view) {
        b.classList.add("active");
      } else {
        b.classList.remove("active");
      }
    });
  }

  if (view === "card") {
    if (tableView) tableView.classList.add("hidden");
    if (cardView) cardView.classList.remove("hidden");
  } else {
    if (tableView) tableView.classList.remove("hidden");
    if (cardView) cardView.classList.add("hidden");
  }
}

if (viewToggle) {
  viewToggle.querySelectorAll(".view-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const targetView = btn.getAttribute("data-view");
      if (targetView) setRestaurantView(targetView);
    });
  });
}

// Export CSV handler
if (btnExportCSV) {
  btnExportCSV.addEventListener("click", exportRestaurantsToCSV);
}

function exportRestaurantsToCSV() {
  if (!currentRestaurants || currentRestaurants.length === 0) {
    showToast("No restaurant data available to export.", "error");
    return;
  }

  const escapeCSV = (val) => {
    if (val === null || val === undefined) return '""';
    return `"${String(val).replace(/"/g, '""')}"`;
  };

  const headers = ["ID", "Name", "Cuisine", "Borough", "ZIP Code", "Building", "Street", "Score"];
  const rows = currentRestaurants.map(r => [
    escapeCSV(r.id),
    escapeCSV(r.name),
    escapeCSV(r.cuisine),
    escapeCSV(r.borough),
    escapeCSV(r.zipcode),
    escapeCSV(r.building),
    escapeCSV(r.street),
    r.score !== undefined && r.score !== null ? r.score : "0.0"
  ]);

  const csvContent = [headers.join(","), ...rows.map(row => row.join(","))].join("\r\n");
  const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  const timestamp = new Date().toISOString().slice(0, 10);
  a.download = `restaurants_export_${timestamp}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  showToast(`Successfully exported ${currentRestaurants.length} restaurants to CSV!`, "success");
}

// Search form submit
searchForm.addEventListener("submit", (e) => {
  e.preventDefault();
  const name = document.getElementById("searchName").value.trim();
  const cuisine = document.getElementById("searchCuisine").value.trim();
  const borough = document.getElementById("searchBorough").value;
  const zipcode = document.getElementById("searchZipcode").value.trim();
  const minScore = document.getElementById("searchMinScore").value;

  const params = new URLSearchParams();
  if (name) params.append("name", name);
  if (cuisine) params.append("cuisine", cuisine);
  if (borough) params.append("borough", borough);
  if (zipcode) params.append("zipcode", zipcode);
  if (minScore) params.append("minScore", minScore);

  loadRestaurants(params.toString());
});

// Clear search
btnClearSearch.addEventListener("click", () => {
  searchForm.reset();
  loadRestaurants();
});

btnRefresh.addEventListener("click", () => {
  // Add rotation animation to the refresh button
  btnRefresh.style.transform = "rotate(360deg)";
  btnRefresh.style.transition = "transform 0.5s ease";
  setTimeout(() => {
    btnRefresh.style.transform = "";
    btnRefresh.style.transition = "";
  }, 500);

  searchForm.reset();
  loadRestaurants();
  loadDashboard();
  loadAnalytics();
  loadIndexes();
});

// ============================================================================
// 4. CRUD - CREATE
// ============================================================================
addRestaurantForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const name = document.getElementById("addName").value.trim();
  const cuisine = document.getElementById("addCuisine").value.trim();
  const borough = document.getElementById("addBorough").value;
  const zipcode = document.getElementById("addZipcode").value.trim();
  const score = parseFloat(document.getElementById("addScore").value) || 0.0;

  try {
    const res = await fetch("/api/restaurants", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, cuisine, borough, zipcode, score })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      showToast(`Restaurant "${name}" added successfully!`, "success");
      addRestaurantForm.reset();
      loadRestaurants();
      loadDashboard();
      loadAnalytics();
    } else {
      showToast(data.message || "Failed to add restaurant.", "error");
    }
  } catch (err) {
    showToast(`Error adding restaurant: ${err.message}`, "error");
  }
});

// ============================================================================
// 5. CRUD - UPDATE
// ============================================================================
window.openEditModal = function(id) {
  const restaurant = currentRestaurants.find(r => r.id === id);
  if (!restaurant) return;

  document.getElementById("editId").value = restaurant.id;
  document.getElementById("editName").value = restaurant.name;
  document.getElementById("editCuisine").value = restaurant.cuisine;
  document.getElementById("editBorough").value = restaurant.borough;
  document.getElementById("editZipcode").value = restaurant.zipcode || "";
  document.getElementById("editScore").value = restaurant.score || 0;

  editModal.classList.remove("hidden");
};

function closeEditModal() {
  editModal.classList.add("hidden");
  editRestaurantForm.reset();
}

btnCloseEdit.addEventListener("click", closeEditModal);
btnCancelEdit.addEventListener("click", closeEditModal);

// Close modal on backdrop click
editModal.addEventListener("click", (e) => {
  if (e.target === editModal) {
    closeEditModal();
  }
});

// Close modal on Escape key
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && !editModal.classList.contains("hidden")) {
    closeEditModal();
  }
});

editRestaurantForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const id = document.getElementById("editId").value;
  const name = document.getElementById("editName").value.trim();
  const cuisine = document.getElementById("editCuisine").value.trim();
  const borough = document.getElementById("editBorough").value;
  const zipcode = document.getElementById("editZipcode").value.trim();
  const score = parseFloat(document.getElementById("editScore").value) || 0.0;

  try {
    const res = await fetch(`/api/restaurants/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, cuisine, borough, zipcode, score })
    });

    const data = await res.json();
    if (res.ok && data.success) {
      showToast(`Restaurant "${name}" updated successfully!`, "success");
      closeEditModal();
      loadRestaurants();
      loadDashboard();
    } else {
      showToast(data.message || "Failed to update restaurant.", "error");
    }
  } catch (err) {
    showToast(`Error updating restaurant: ${err.message}`, "error");
  }
});

// ============================================================================
// 6. CRUD - DELETE
// ============================================================================
window.deleteRestaurant = async function(id, name) {
  if (!confirm(`Are you sure you want to delete "${name}" from MongoDB Atlas?`)) {
    return;
  }

  try {
    const res = await fetch(`/api/restaurants/${id}`, {
      method: "DELETE"
    });

    const data = await res.json();
    if (res.ok && data.success) {
      showToast(`Restaurant deleted successfully.`, "success");
      loadRestaurants();
      loadDashboard();
      loadAnalytics();
    } else {
      showToast(data.message || "Failed to delete restaurant.", "error");
    }
  } catch (err) {
    showToast(`Error deleting restaurant: ${err.message}`, "error");
  }
};

// ============================================================================
// 7. Analytics (3 Aggregations)
// ============================================================================
async function loadAnalytics() {
  // Aggregation 1: By Cuisine
  try {
    const res = await fetch("/api/analytics/cuisine?limit=10");
    const data = await res.json();
    if (Array.isArray(data) && data.length > 0) {
      tableCuisineBody.innerHTML = data.map((item, i) => `
        <tr style="animation: fadeInUp 0.3s ease-out ${i * 0.03}s both;">
          <td>${escapeHtml(item.cuisine)}</td>
          <td class="text-right"><strong>${Number(item.count).toLocaleString()}</strong></td>
        </tr>
      `).join("");
    } else {
      tableCuisineBody.innerHTML = `<tr><td colspan="2" class="text-center">No data available</td></tr>`;
    }
  } catch (err) {
    tableCuisineBody.innerHTML = `<tr><td colspan="2" class="text-center">Error loading</td></tr>`;
  }

  // Aggregation 2: By Borough
  try {
    const res = await fetch("/api/analytics/borough");
    const data = await res.json();
    if (Array.isArray(data) && data.length > 0) {
      tableBoroughBody.innerHTML = data.map((item, i) => `
        <tr style="animation: fadeInUp 0.3s ease-out ${i * 0.03}s both;">
          <td>${escapeHtml(item.borough)}</td>
          <td class="text-right"><strong>${Number(item.count).toLocaleString()}</strong></td>
        </tr>
      `).join("");
    } else {
      tableBoroughBody.innerHTML = `<tr><td colspan="2" class="text-center">No data available</td></tr>`;
    }
  } catch (err) {
    tableBoroughBody.innerHTML = `<tr><td colspan="2" class="text-center">Error loading</td></tr>`;
  }

  // Aggregation 3: Average Score by Cuisine
  try {
    const res = await fetch("/api/analytics/average-score?limit=10");
    const data = await res.json();
    if (Array.isArray(data) && data.length > 0) {
      tableAvgScoreBody.innerHTML = data.map((item, i) => `
        <tr style="animation: fadeInUp 0.3s ease-out ${i * 0.03}s both;">
          <td>${escapeHtml(item.cuisine)}</td>
          <td><span class="score-pill">${Number(item.avgScore).toFixed(1)}</span></td>
          <td class="text-right">${Number(item.count).toLocaleString()}</td>
        </tr>
      `).join("");
    } else {
      tableAvgScoreBody.innerHTML = `<tr><td colspan="3" class="text-center">No data available</td></tr>`;
    }
  } catch (err) {
    tableAvgScoreBody.innerHTML = `<tr><td colspan="3" class="text-center">Error loading</td></tr>`;
  }
}

// ============================================================================
// 8. Indexes Management
// ============================================================================
async function loadIndexes() {
  try {
    const res = await fetch("/api/indexes");
    const data = await res.json();
    const indexes = data.indexes || [];

    allIndexesText.textContent = indexes.length > 0 ? indexes.join(", ") : "No indexes found";

    const hasName = indexes.includes("name_1");
    const hasCuisine = indexes.includes("cuisine_1");
    const hasCompound = indexes.includes("borough_1_cuisine_1");

    statusIndexName.textContent = hasName ? "✅ Status: Active in Atlas" : "⚪ Status: Not created";
    statusIndexCuisine.textContent = hasCuisine ? "✅ Status: Active in Atlas" : "⚪ Status: Not created";
    statusIndexCompound.textContent = hasCompound ? "✅ Status: Active in Atlas" : "⚪ Status: Not created";
  } catch (err) {
    console.error("Failed to load indexes:", err);
  }
}

btnCreateIndexes.addEventListener("click", async () => {
  btnCreateIndexes.disabled = true;
  btnCreateIndexes.textContent = "⏳ Creating Indexes…";
  btnCreateIndexes.style.opacity = "0.7";

  try {
    const res = await fetch("/api/indexes", { method: "POST" });
    const data = await res.json();

    if (res.ok && data.success) {
      showToast("Required indexes created in MongoDB Atlas!", "success");
      loadIndexes();
    } else {
      showToast(data.message || "Failed to create indexes.", "error");
    }
  } catch (err) {
    showToast(`Error creating indexes: ${err.message}`, "error");
  } finally {
    btnCreateIndexes.disabled = false;
    btnCreateIndexes.textContent = "⚡ Create Required Indexes";
    btnCreateIndexes.style.opacity = "";
  }
});

// ============================================================================
// Helper Utilities
// ============================================================================
function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/[&<>"']/g, function(m) {
    switch (m) {
      case '&': return '&amp;';
      case '<': return '&lt;';
      case '>': return '&gt;';
      case '"': return '&quot;';
      case "'": return '&#039;';
      default: return m;
    }
  });
}

function escapeQuotes(str) {
  if (!str) return "";
  return str.replace(/'/g, "\\'");
}

// ============================================================================
// Initialization
// ============================================================================
document.addEventListener("DOMContentLoaded", () => {
  setRestaurantView(currentView);
  checkHealth();
  loadDashboard();
  loadRestaurants();
  loadAnalytics();
  loadIndexes();
});
