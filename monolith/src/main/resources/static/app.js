/**
 * VehicleRent — Frontend Logic
 * Handles all API communication, UI rendering, and interactions.
 */

const API = 'http://localhost:8080/api';

// ═══════════════════════════════════════════════
//  SESSION HELPERS
// ═══════════════════════════════════════════════

function getToken()  { return localStorage.getItem('token'); }
function getUserId() { return localStorage.getItem('userId'); }
function getRole()   { return localStorage.getItem('role'); }

function getHeaders() {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${getToken()}`
  };
}

/** Redirect to login if not authenticated, or wrong role */
function checkAuth(requiredRole) {
  const role = getRole();
  if (!role || !getToken()) {
    window.location.href = 'index.html';
    return;
  }
  if (requiredRole && role !== requiredRole) {
    window.location.href = role === 'ADMIN' ? 'admin.html' : 'customer.html';
  }
}

function logout() {
  localStorage.clear();
  window.location.href = 'index.html';
}

/** HTML-escape to prevent XSS */
function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ═══════════════════════════════════════════════
//  TOAST NOTIFICATIONS
// ═══════════════════════════════════════════════

let toastTimer;
function showToast(message, type = 'success') {
  const toast = document.getElementById('toast');
  if (!toast) return;
  clearTimeout(toastTimer);
  toast.textContent = message;
  toast.className = `toast toast-${type} show`;
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3500);
}

// ═══════════════════════════════════════════════
//  TAB NAVIGATION
// ═══════════════════════════════════════════════

function switchTab(tabId) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  const btn = document.querySelector(`[data-tab="${tabId}"]`);
  if (btn) btn.classList.add('active');
  const panel = document.getElementById(tabId);
  if (panel) panel.classList.add('active');
}

// ═══════════════════════════════════════════════
//  AUTH PAGE  (index.html)
// ═══════════════════════════════════════════════

function showLogin() {
  document.getElementById('login-panel').classList.add('active');
  document.getElementById('register-panel').classList.remove('active');
  document.getElementById('tab-login').classList.add('active');
  document.getElementById('tab-register').classList.remove('active');
}

function showRegister() {
  document.getElementById('register-panel').classList.add('active');
  document.getElementById('login-panel').classList.remove('active');
  document.getElementById('tab-register').classList.add('active');
  document.getElementById('tab-login').classList.remove('active');
}

async function login() {
  const email    = (document.getElementById('login-email')?.value    || '').trim();
  const password =  document.getElementById('login-password')?.value || '';

  if (!email || !password) {
    showToast('Please enter your email and password.', 'error');
    return;
  }

  const btn = document.getElementById('login-btn');
  if (btn) { btn.disabled = true; btn.textContent = 'Signing in…'; }

  try {
    const res  = await fetch(`${API}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Login failed. Check your credentials.');

    localStorage.setItem('token',  data.token);
    localStorage.setItem('role',   data.role);
    localStorage.setItem('userId', data.userId);

    window.location.href = data.role === 'ADMIN' ? 'admin.html' : 'customer.html';
  } catch (e) {
    showToast(e.message, 'error');
    if (btn) { btn.disabled = false; btn.textContent = 'Sign In'; }
  }
}

async function register() {
  const name          = (document.getElementById('reg-name')?.value    || '').trim();
  const email         = (document.getElementById('reg-email')?.value   || '').trim();
  const password      =  document.getElementById('reg-password')?.value || '';
  const licenseNumber = (document.getElementById('reg-license')?.value || '').trim();

  if (!name || !email || !password || !licenseNumber) {
    showToast('Please fill in all fields.', 'error');
    return;
  }
  if (password.length < 4) {
    showToast('Password must be at least 4 characters.', 'error');
    return;
  }

  const btn = document.getElementById('register-btn');
  if (btn) { btn.disabled = true; btn.textContent = 'Creating account…'; }

  try {
    const res  = await fetch(`${API}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password, licenseNumber })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Registration failed.');

    showToast('Account created! Please sign in. 🎉', 'success');
    document.getElementById('reg-name').value     = '';
    document.getElementById('reg-password').value = '';
    document.getElementById('reg-license').value  = '';
    // Pre-fill email on login panel
    if (document.getElementById('login-email'))
      document.getElementById('login-email').value = email;
    setTimeout(showLogin, 800);
  } catch (e) {
    showToast(e.message, 'error');
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = 'Create Account'; }
  }
}

// ═══════════════════════════════════════════════
//  CUSTOMER — VEHICLE FLOW
// ═══════════════════════════════════════════════

let currentCategory = null;

function selectCategory(category) {
  currentCategory = category;
  document.getElementById('selection-screen').classList.add('hidden');
  document.getElementById('listing-screen').classList.remove('hidden');
  document.getElementById('category-title').textContent = category === 'FOUR_WHEELER' ? 'Four Wheelers' : 'Two Wheelers';
  loadAvailableVehicles();
}

function backToSelection() {
  currentCategory = null;
  document.getElementById('listing-screen').classList.add('hidden');
  document.getElementById('selection-screen').classList.remove('hidden');
}

function getRecommendation() {
  const start    = (document.getElementById('trip-start')?.value || '').toLowerCase();
  const end      = (document.getElementById('trip-end')?.value   || '').toLowerCase();
  const distance = parseFloat(document.getElementById('trip-distance')?.value || 0);
  const resultDiv = document.getElementById('recommendation-result');
  const iconEl = document.getElementById('rec-icon');
  const titleEl = document.getElementById('rec-title');
  const reasonEl = document.getElementById('rec-reason');

  if (!distance && !start && !end) {
    showToast('Please fill in distance and locations.', 'error');
    return;
  }

  let rec = 'TWO_WHEELER';
  let reason = '';

  if (distance >= 50) {
    rec = 'FOUR_WHEELER';
    reason = `For a distance of ${distance}km, a Four Wheeler is recommended for a comfortable journey.`;
  } else {
    rec = 'TWO_WHEELER';
    reason = `For a distance of ${distance}km, a Two Wheeler is fast and economical.`;
  }

  resultDiv.classList.remove('hidden');
  iconEl.textContent = rec === 'FOUR_WHEELER' ? '🚗' : '🏍️';
  titleEl.textContent = `We recommend a ${rec === 'FOUR_WHEELER' ? 'Four Wheeler' : 'Two Wheeler'}`;
  reasonEl.textContent = reason;
}

/** Loads vehicles — filters by category and optional date range */
async function loadAvailableVehicles() {
  const startDate = document.getElementById('search-start')?.value;
  const endDate   = document.getElementById('search-end')?.value;
  const grid      = document.getElementById('vehicles-grid');
  if (!grid) return;

  grid.innerHTML = '<div class="empty-state"><div class="loading-spinner"></div></div>';

  // Validate dates if both provided
  if (startDate && endDate && endDate < startDate) {
    showToast('End date must be on or after start date.', 'error');
    grid.innerHTML = '<div class="empty-state"><span class="empty-icon">📅</span><p>Invalid date range selected.</p></div>';
    return;
  }

  let url = `${API}/vehicles/category/${currentCategory}`;
  if (startDate && endDate) {
    url = `${API}/vehicles/available?startDate=${startDate}&endDate=${endDate}&category=${currentCategory}`;
  }

  try {
    const res      = await fetch(url, { headers: getHeaders() });
    let vehicles = await res.json();
    if (!res.ok) throw new Error(vehicles.error || 'Failed to load vehicles.');

    // Always filter by currentCategory on the frontend to be safe
    vehicles = vehicles.filter(v => v.category === currentCategory);

    if (!vehicles.length) {
      grid.innerHTML = `<div class="empty-state"><span class="empty-icon">🚗</span><p>No ${currentCategory === 'FOUR_WHEELER' ? 'cars' : 'bikes'} available currently.</p></div>`;
      return;
    }

    grid.innerHTML = vehicles.map(v => `
      <div class="vehicle-card" onclick="openBookingModal(${v.id}, '${esc(v.name)}', ${v.pricePerDay}, '${startDate||''}', '${endDate||''}')">
        <div class="vehicle-img" style="background-image: url('${v.imageUrl || 'https://images.unsplash.com/photo-1494976388531-d1058494cdd8?w=400'}')"></div>
        <div class="vehicle-body">
          <div class="vehicle-rating">
            <span>⭐</span> <span class="rating-val">${v.rating ? v.rating.toFixed(1) : '4.5'}</span>
          </div>
          <div class="vehicle-name">${esc(v.name)}</div>
          <span class="type-badge">${esc(v.type)}</span>
          <div class="vehicle-price">
            <span class="price-amount">₹${Math.round(v.pricePerDay).toLocaleString('en-IN')}</span>
            <span class="price-label">/ day</span>
          </div>
          <span class="availability-tag avail-yes">● Available Now</span>
          <button class="btn btn-primary btn-sm">Book This Ride</button>
        </div>
      </div>
    `).join('');
  } catch (e) {
    grid.innerHTML = `<div class="empty-state"><span class="empty-icon">⚠️</span><p>${esc(e.message)}</p></div>`;
  }
}

function clearSearch() {
  const s = document.getElementById('search-start');
  const e = document.getElementById('search-end');
  if (s) s.value = '';
  if (e) e.value = '';
  loadAvailableVehicles();
}

// ═══════════════════════════════════════════════
//  CUSTOMER — BOOKING MODAL
// ═══════════════════════════════════════════════

let _bookVehicleId    = null;
let _bookPricePerDay  = 0;

/**
 * Opens booking modal, optionally pre-fills dates from the search bar.
 */
function openBookingModal(vehicleId, vehicleName, pricePerDay, prefillStart, prefillEnd) {
  // Prevent event bubbling if triggered from card click
  // (though here it's the primary action)
  _bookVehicleId   = vehicleId;
  _bookPricePerDay = parseFloat(pricePerDay);

  document.getElementById('modal-vehicle-name').textContent  = vehicleName;
  document.getElementById('modal-price-per-day').textContent = `₹${_bookPricePerDay.toLocaleString('en-IN')} / day`;
  document.getElementById('price-preview').innerHTML = 'Select dates to see total cost.';
  
  // Reset coupon and payment
  if (document.getElementById('book-coupon'))  document.getElementById('book-coupon').value = '';
  if (document.getElementById('book-payment')) document.getElementById('book-payment').value = '';

  const today = new Date().toISOString().split('T')[0];
  const startEl = document.getElementById('book-start-date');
  const endEl   = document.getElementById('book-end-date');

  startEl.min   = today;
  endEl.min     = today;
  startEl.value = prefillStart || '';
  endEl.value   = prefillEnd   || '';

  if (prefillStart && prefillEnd) updatePricePreview();

  document.getElementById('booking-modal').classList.add('show');
}

function closeBookingModal() {
  document.getElementById('booking-modal').classList.remove('show');
  _bookVehicleId = null;
}

/** Recalculates and shows the total price when dates change */
function updatePricePreview() {
  const start   = document.getElementById('book-start-date').value;
  const end     = document.getElementById('book-end-date').value;
  const preview = document.getElementById('price-preview');
  if (!start || !end) { preview.innerHTML = 'Select dates to see total cost.'; return; }

  const days = Math.floor((new Date(end) - new Date(start)) / 86_400_000) + 1;
  if (days < 1) { preview.innerHTML = '<span style="color:var(--danger)">End date must be after start date.</span>'; return; }

  const total = (days * _bookPricePerDay).toLocaleString('en-IN');
  preview.innerHTML =
    `${days} day${days > 1 ? 's' : ''} × ₹${_bookPricePerDay.toLocaleString('en-IN')} = <span class="preview-total">₹${total}</span>`;
}

async function submitBooking() {
  const startDate     = document.getElementById('book-start-date').value;
  const endDate       = document.getElementById('book-end-date').value;
  const paymentMethod = document.getElementById('book-payment')?.value;
  const couponCode    = document.getElementById('book-coupon')?.value.trim();

  if (!startDate || !endDate)   { showToast('Please select both dates.', 'error'); return; }
  if (endDate < startDate)      { showToast('End date must be after start date.', 'error'); return; }
  if (!paymentMethod)           { showToast('Please select a payment method.', 'error'); return; }
  if (!_bookVehicleId)          { showToast('No vehicle selected.', 'error'); return; }

  const btn = document.getElementById('confirm-book-btn');
  btn.disabled = true; btn.textContent = 'Processing…';

  try {
    const res  = await fetch(`${API}/reservations`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify({
        customerId: parseInt(getUserId()),
        vehicleId:  _bookVehicleId,
        startDate,
        endDate,
        paymentMethod,
        couponCode
      })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Booking failed.');

    closeBookingModal();
    showToast('Vehicle booked successfully! 🎉', 'success');
    loadAvailableVehicles();
    loadMyReservations();
    // Switch to bookings tab to show new reservation
    switchTab('panel-bookings');
  } catch (e) {
    showToast(e.message, 'error');
  } finally {
    btn.disabled = false; btn.textContent = 'Confirm Booking';
  }
}

// ═══════════════════════════════════════════════
//  CUSTOMER — MY RESERVATIONS
// ═══════════════════════════════════════════════

async function loadMyReservations() {
  const tbody = document.getElementById('reservations-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="6" class="loading-cell"><div class="loading-spinner small"></div></td></tr>';

  try {
    const res          = await fetch(`${API}/reservations/customer/${getUserId()}`, { headers: getHeaders() });
    const reservations = await res.json();
    if (!res.ok) throw new Error(reservations.error || 'Failed to load.');

    if (!reservations.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">You have no reservations yet.</td></tr>';
      return;
    }

    tbody.innerHTML = reservations.map(r => {
      const canCancel = r.status === 'CONFIRMED' || r.status === 'PENDING';
      return `<tr>
        <td><span class="id-chip">#${r.id}</span></td>
        <td><strong>${esc(r.vehicle.name)}</strong><br><small class="text-muted">${esc(r.vehicle.type)}</small></td>
        <td>${r.startDate}</td>
        <td>${r.endDate}</td>
        <td><span class="status-badge status-${r.status.toLowerCase()}">${r.status}</span></td>
        <td>${canCancel
          ? `<button class="btn btn-sm btn-danger" onclick="cancelReservation(${r.id})">Cancel</button>`
          : '—'
        }</td>
      </tr>`;
    }).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-cell">${esc(e.message)}</td></tr>`;
  }
}

async function cancelReservation(id) {
  if (!confirm('Cancel this reservation? This cannot be undone.')) return;
  try {
    const res  = await fetch(`${API}/reservations/${id}/cancel`, { method: 'PUT', headers: getHeaders() });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Cancellation failed.');
    showToast('Reservation cancelled.', 'warning');
    loadMyReservations();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ═══════════════════════════════════════════════
//  ADMIN — VEHICLES
// ═══════════════════════════════════════════════

async function loadAdminVehicles() {
  const tbody = document.getElementById('admin-vehicles-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="7" class="loading-cell"><div class="loading-spinner small"></div></td></tr>';

  try {
    const res      = await fetch(`${API}/admin/vehicles`, { headers: getHeaders() });
    const vehicles = await res.json();
    if (!res.ok) throw new Error(vehicles.error || 'Failed.');

    // Update stat card
    const el = document.getElementById('stat-vehicles');
    if (el) el.textContent = vehicles.length;
    const avEl = document.getElementById('stat-available');
    if (avEl) avEl.textContent = vehicles.filter(v => v.status === 'AVAILABLE').length;

    if (!vehicles.length) {
      tbody.innerHTML = '<tr><td colspan="7" class="empty-cell">No vehicles yet. Add one above.</td></tr>';
      return;
    }

    tbody.innerHTML = vehicles.map(v => `<tr>
      <td><span class="id-chip">#${v.id}</span></td>
      <td><strong>${esc(v.name)}</strong></td>
      <td><small>${esc(v.category)}</small></td>
      <td>${esc(v.type)}</td>
      <td>₹${Math.round(v.pricePerDay).toLocaleString('en-IN')}</td>
      <td><span class="status-badge status-${v.status.toLowerCase()}">${v.status}</span></td>
      <td class="action-cell">
        <button class="btn btn-sm btn-secondary"
          onclick="openEditModal(${v.id},'${esc(v.name)}','${esc(v.category)}','${esc(v.type)}',${v.pricePerDay},${v.rating},'${v.imageUrl}','${v.status}')">Edit</button>
        <button class="btn btn-sm btn-danger" onclick="deleteVehicle(${v.id})">Delete</button>
      </td>
    </tr>`).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-cell">${esc(e.message)}</td></tr>`;
  }
}

async function addVehicle() {
  const name       = (document.getElementById('v-name')?.value   || '').trim();
  const category   =  document.getElementById('v-category')?.value || 'FOUR_WHEELER';
  const type       =  document.getElementById('v-type')?.value   || 'Sedan';
  const pricePerDay = document.getElementById('v-price')?.value  || '';
  const rating     = document.getElementById('v-rating')?.value  || 4.5;
  const imageUrl   = document.getElementById('v-image')?.value   || '';
  const status     =  document.getElementById('v-status')?.value || 'AVAILABLE';

  if (!name || !pricePerDay) { showToast('Please fill in Name and Price.', 'error'); return; }

  try {
    const res  = await fetch(`${API}/admin/vehicles`, {
      method: 'POST', headers: getHeaders(),
      body: JSON.stringify({ name, category, type, pricePerDay: parseFloat(pricePerDay), rating: parseFloat(rating), imageUrl, status })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to add vehicle.');

    showToast(`"${name}" added successfully! ✅`, 'success');
    document.getElementById('v-name').value  = '';
    document.getElementById('v-price').value = '';
    document.getElementById('v-rating').value = '';
    document.getElementById('v-image').value = '';
    loadAdminVehicles();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

function openEditModal(id, name, category, type, price, rating, imageUrl, status) {
  document.getElementById('edit-id').value       = id;
  document.getElementById('edit-name').value     = name;
  document.getElementById('edit-category').value = category;
  document.getElementById('edit-type').value     = type;
  document.getElementById('edit-price').value    = price;
  document.getElementById('edit-rating').value   = rating || 4.5;
  document.getElementById('edit-image').value    = imageUrl || '';
  document.getElementById('edit-status').value   = status;
  document.getElementById('edit-modal').classList.add('show');
}

function closeEditModal() {
  document.getElementById('edit-modal').classList.remove('show');
}

async function saveVehicleEdit() {
  const id       = document.getElementById('edit-id').value;
  const name     = document.getElementById('edit-name').value.trim();
  const category = document.getElementById('edit-category').value;
  const type     = document.getElementById('edit-type').value;
  const price    = document.getElementById('edit-price').value;
  const rating   = document.getElementById('edit-rating').value;
  const imageUrl = document.getElementById('edit-image').value;
  const status   = document.getElementById('edit-status').value;

  if (!name || !price) { showToast('Name and Price are required.', 'error'); return; }

  try {
    const res  = await fetch(`${API}/admin/vehicles/${id}`, {
      method: 'PUT', headers: getHeaders(),
      body: JSON.stringify({ name, category, type, pricePerDay: parseFloat(price), rating: parseFloat(rating), imageUrl, status })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Update failed.');

    closeEditModal();
    showToast('Vehicle updated! ✅', 'success');
    loadAdminVehicles();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function deleteVehicle(id) {
  if (!confirm(`Delete vehicle #${id}? This cannot be undone.`)) return;
  try {
    const res  = await fetch(`${API}/admin/vehicles/${id}`, { method: 'DELETE', headers: getHeaders() });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Delete failed.');
    showToast('Vehicle deleted.', 'warning');
    loadAdminVehicles();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ═══════════════════════════════════════════════
//  ADMIN — RESERVATIONS
// ═══════════════════════════════════════════════

async function loadAdminReservations() {
  const tbody = document.getElementById('admin-res-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="6" class="loading-cell"><div class="loading-spinner small"></div></td></tr>';

  try {
    const res          = await fetch(`${API}/admin/reservations`, { headers: getHeaders() });
    const reservations = await res.json();
    if (!res.ok) throw new Error(reservations.error || 'Failed.');

    const el = document.getElementById('stat-reservations');
    if (el) el.textContent = reservations.length;

    if (!reservations.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">No reservations yet.</td></tr>';
      return;
    }

    tbody.innerHTML = reservations.map(r => {
      const rental = r.rental || {};
      return `<tr>
        <td><span class="id-chip">#${r.id}</span></td>
        <td><strong>${esc(r.customer.name)}</strong><br><small class="text-muted">${esc(r.customer.email)}</small></td>
        <td>${esc(r.vehicle.name)} <br><small>(${esc(r.vehicle.category)})</small></td>
        <td>${r.startDate} → ${r.endDate}</td>
        <td>
          <strong>${esc(rental.paymentMethod || '—')}</strong><br>
          <small class="${(rental.paymentStatus || '').toLowerCase()}">${esc(rental.paymentStatus || 'UNPAID')}</small>
        </td>
        <td>₹${Math.round(rental.originalAmount || 0).toLocaleString('en-IN')}</td>
        <td style="color:var(--success)">${rental.discountAmount > 0 ? `-₹${Math.round(rental.discountAmount).toLocaleString('en-IN')}` : '—'}</td>
        <td><strong>₹${Math.round(rental.finalAmount || 0).toLocaleString('en-IN')}</strong></td>
        <td><span class="status-badge status-${r.status.toLowerCase()}">${r.status}</span></td>
        <td>
          <select class="status-select" onchange="updateReservationStatus(${r.id}, this.value)">
            <option value="">Change…</option>
            <option value="PENDING"   ${r.status==='PENDING'   ?'selected':''}>PENDING</option>
            <option value="CONFIRMED" ${r.status==='CONFIRMED' ?'selected':''}>CONFIRMED</option>
            <option value="COMPLETED" ${r.status==='COMPLETED' ?'selected':''}>COMPLETED</option>
            <option value="CANCELLED" ${r.status==='CANCELLED' ?'selected':''}>CANCELLED</option>
          </select>
        </td>
      </tr>`;
    }).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-cell">${esc(e.message)}</td></tr>`;
  }
}

async function updateReservationStatus(id, status) {
  if (!status) return;
  try {
    const res  = await fetch(`${API}/admin/reservations/${id}/status?status=${status}`, {
      method: 'PUT', headers: getHeaders()
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Update failed.');
    showToast(`Reservation #${id} → ${status}`, 'success');
    loadAdminReservations();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ═══════════════════════════════════════════════
//  ADMIN — USERS
// ═══════════════════════════════════════════════

async function loadAdminUsers() {
  const tbody = document.getElementById('admin-users-tbody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="5" class="loading-cell"><div class="loading-spinner small"></div></td></tr>';

  try {
    const res   = await fetch(`${API}/admin/users`, { headers: getHeaders() });
    const users = await res.json();
    if (!res.ok) throw new Error(users.error || 'Failed.');

    const el = document.getElementById('stat-users');
    if (el) el.textContent = users.filter(u => u.role === 'CUSTOMER').length;

    tbody.innerHTML = users.map(u => `<tr>
      <td><span class="id-chip">#${u.id}</span></td>
      <td><strong>${esc(u.name)}</strong></td>
      <td>${esc(u.email)}</td>
      <td>${u.licenseNumber ? esc(u.licenseNumber) : '<span class="text-muted">—</span>'}</td>
      <td><span class="role-badge role-${u.role.toLowerCase()}">${u.role}</span></td>
    </tr>`).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="5" class="empty-cell">${esc(e.message)}</td></tr>`;
  }
}
