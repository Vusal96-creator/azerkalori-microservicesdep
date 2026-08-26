const state = { token: null, userId: null, role: null, name: null, sex: "MALE", products: [], stomp: null };

const $ = (s) => document.querySelector(s);
const $$ = (s) => document.querySelectorAll(s);

function toast(msg, kind) {
  const t = document.createElement("div");
  t.className = "toast " + (kind || "");
  t.textContent = msg;
  $("#toasts").appendChild(t);
  setTimeout(() => t.remove(), 3200);
}

async function api(path, options = {}) {
  const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
  if (state.token) headers["Authorization"] = "Bearer " + state.token;
  const res = await fetch(path, Object.assign({}, options, { headers }));
  if (!res.ok) {
    let msg = res.status + " " + res.statusText;
    try { const b = await res.json(); if (b.message) msg = b.message; } catch (e) {}
    throw new Error(msg);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

function saveSession() {
  localStorage.setItem("azk", JSON.stringify({ token: state.token, userId: state.userId, role: state.role, name: state.name }));
}
function loadSession() {
  try {
    const s = JSON.parse(localStorage.getItem("azk"));
    if (s && s.token) { state.token = s.token; state.userId = s.userId; state.role = s.role; state.name = s.name; }
  } catch (e) {}
}
function clearSession() {
  localStorage.removeItem("azk");
  state.token = state.userId = state.role = state.name = null;
  if (state.stomp) { try { state.stomp.deactivate(); } catch (e) {} state.stomp = null; }
}

function refreshChrome() {
  const authed = !!state.token;
  $$(".auth-only").forEach((e) => e.classList.toggle("hidden", !authed));
  $$(".guest-only").forEach((e) => e.classList.toggle("hidden", authed));
  $$(".role-DOCTOR").forEach((e) => e.classList.toggle("hidden", state.role !== "DOCTOR"));
  $$(".role-ADMIN").forEach((e) => e.classList.toggle("hidden", state.role !== "ADMIN"));
  if (authed) $("#userChip").textContent = (state.name || "İstifadəçi") + " · " + state.role;
}

function go(view) {
  $$(".view").forEach((v) => v.classList.remove("active"));
  const el = $("#view-" + view);
  if (el) el.classList.add("active");
  $$("#navLinks a").forEach((a) => a.classList.toggle("active", a.dataset.nav === view));
  window.scrollTo({ top: 0, behavior: "smooth" });
  if (view === "dashboard") loadDashboard();
  if (view === "doctor") loadDoctor();
  if (view === "admin") loadAdmin();
}

document.addEventListener("click", (e) => {
  const nav = e.target.closest("[data-nav]");
  if (nav) {
    const v = nav.dataset.nav;
    if ((v === "dashboard" || v === "doctor" || v === "admin") && !state.token) return go("auth");
    return go(v);
  }
  const sc = e.target.closest("[data-scroll]");
  if (sc) { $("#" + sc.dataset.scroll).scrollIntoView({ behavior: "smooth" }); }
});

$("#sexSeg").addEventListener("click", (e) => {
  const b = e.target.closest("[data-sex]");
  if (!b) return;
  state.sex = b.dataset.sex;
  $$("#sexSeg button").forEach((x) => x.classList.toggle("on", x === b));
});

$("#calcBtn").addEventListener("click", async () => {
  const body = {
    age: +$("#c_age").value, weightKg: +$("#c_weight").value, heightCm: +$("#c_height").value,
    sex: state.sex, activityLevel: $("#c_activity").value, goal: $("#c_goal").value
  };
  try {
    const r = await api("/api/goals/calculate", { method: "POST", body: JSON.stringify(body) });
    renderCalc(r);
  } catch (err) { toast("Xəta: " + err.message, "bad"); }
});

function renderCalc(r) {
  const pk = r.proteinG * 4, fk = r.fatG * 9, ck = r.carbsG * 4;
  const tot = Math.max(pk + fk + ck, 1);

  const tdee = r.tdee, bmr = r.bmr;
  const floor = Math.round(bmr + 50); // qızıl qayda: hədəf BMR-dən aşağı olmasın

  const goal = $("#c_goal").value;
  const targetKg = +($("#c_targetKg") ? $("#c_targetKg").value : 0) || 0;
  const weeks = Math.max(1, Math.round(+($("#c_weeks") ? $("#c_weeks").value : 1) || 1));

  let chosen = Math.round(tdee), note = "", warn = "";

  if (goal !== "MAINTAIN" && targetKg > 0) {
    // 1 kq yağ ≈ 7700 kkal
    const delta = Math.min(Math.max((targetKg * 7700) / (weeks * 7), 250), 1000);
    chosen = goal === "LOSE" ? Math.round(tdee - delta) : Math.round(tdee + delta);
    note = targetKg + " kq / " + weeks + " həftə";
  } else if (goal === "LOSE") { chosen = Math.round(tdee - 500); note = "həftədə ~0.5 kq"; }
  else if (goal === "GAIN") { chosen = Math.round(tdee + 500); note = "həftədə ~0.5 kq"; }

  // Qızıl qayda: arıqlama hədəfi heç vaxt BMR-dən aşağı olmasın (min = BMR+50).
  if (goal === "LOSE" && chosen < floor) {
    chosen = floor;
    warn = "⚠ Seçdiyin müddət çox qısadır — sağlam hədəf BMR-dən aşağı ola bilməz. Minimum (BMR+50) götürüldü; müddəti uzat.";
  }

  $("#calcResult").innerHTML =
    '<div class="ring-wrap" style="align-items:flex-start">' +
      '<div style="flex:1">' +
        '<div class="kcal-big" style="color:var(--green)">' + chosen + ' <small>kkal / gün — sənin hədəfin</small></div>' +
        (note ? '<div class="stat-row"><span>Plan</span><b>' + note + '</b></div>' : '') +
        (warn ? '<p class="sub" style="color:var(--amber)">' + warn + '</p>' : '') +
        '<hr style="border:none;border-top:1px solid #eee;margin:10px 0" />' +
        '<div class="stat-row"><span>BMR — gündəlik minimum (istirahət)</span><b>' + r.bmr + ' kkal</b></div>' +
        '<div class="stat-row"><span>TDEE — gündəlik saxlama (aktivliklə)</span><b>' + r.tdee + ' kkal</b></div>' +
      '</div>' +
      '<div style="flex:1">' +
        macroBar("p", "Protein", r.proteinG, pk / tot * 100) +
        macroBar("f", "Yağ", r.fatG, fk / tot * 100) +
        macroBar("c", "Karbohidrat", r.carbsG, ck / tot * 100) +
      '</div>' +
    '</div>';
}
function macroBar(cls, label, grams, pct) {
  return '<div class="macro ' + cls + '"><div class="top"><span>' + label + '</span><b>' + grams +
    ' q</b></div><div class="bar"><i style="width:' + pct + '%"></i></div></div>';
}

$("#authTabs").addEventListener("click", (e) => {
  const b = e.target.closest("[data-tab]");
  if (!b) return;
  $$("#authTabs button").forEach((x) => x.classList.toggle("on", x === b));
  $("#tab-login").classList.toggle("hidden", b.dataset.tab !== "login");
  $("#tab-register").classList.toggle("hidden", b.dataset.tab !== "register");
});

$("#loginBtn").addEventListener("click", async () => {
  try {
    const r = await api("/api/auth/login", { method: "POST", body: JSON.stringify({ email: $("#l_email").value, password: $("#l_pass").value }) });
    afterAuth(r);
  } catch (err) { toast("Giriş alınmadı: " + err.message, "bad"); }
});

$("#registerBtn").addEventListener("click", async () => {
  const body = {
    fullName: $("#r_name").value, email: $("#r_email").value, password: $("#r_pass").value,
    age: +$("#r_age").value, weightKg: +$("#r_weight").value, heightCm: +$("#r_height").value,
    sex: $("#r_sex").value, activityLevel: $("#r_activity").value
  };
  try {
    const r = await api("/api/auth/register", { method: "POST", body: JSON.stringify(body) });
    r.name = body.fullName;
    afterAuth(r);
  } catch (err) { toast("Qeydiyyat alınmadı: " + err.message, "bad"); }
});

async function afterAuth(r) {
  state.token = r.token; state.userId = r.id; state.role = r.role; state.name = r.name || null;
  if (!state.name) { try { const me = await api("/api/auth/me"); state.name = me.fullName; } catch (e) {} }
  saveSession();
  refreshChrome();
  connectWs();
  toast("Xoş gəldin, " + (state.name || "istifadəçi") + "!", "good");
  go(state.role === "DOCTOR" ? "doctor" : state.role === "ADMIN" ? "admin" : "dashboard");
}

$("#logoutBtn").addEventListener("click", () => { clearSession(); refreshChrome(); toast("Çıxış edildi"); go("home"); });

function connectWs() {
  if (!state.token || state.stomp) return;
  const client = new StompJs.Client({
    webSocketFactory: () => new SockJS("/ws"),
    connectHeaders: { Authorization: "Bearer " + state.token },
    reconnectDelay: 4000
  });
  client.onConnect = () => {
    if (state.role === "USER") {
      client.subscribe("/user/queue/calories", (m) => applyLive(JSON.parse(m.body)));
    }
    if (state.role === "DOCTOR") {
      client.subscribe("/user/queue/alerts", (m) => pushAlert(JSON.parse(m.body)));
    }
    // Həkim ↔ pasiyent chat (hər iki rol üçün)
    client.subscribe("/user/queue/chat", (m) => onDocChat(JSON.parse(m.body)));
  };
  client.activate();
  state.stomp = client;
}

async function loadDashboard() {
  await loadProducts();
  const sel = $("#prodSelect");
  sel.innerHTML = state.products.map((p) => '<option value="' + p.id + '">' + p.name + ' — ' + p.calories + ' kkal</option>').join("");
  loadSummary();
  loadToday();
  loadPlan();
  loadProStatus();
}

async function loadProducts() {
  if (state.products.length) return;
  try { state.products = await api("/api/products"); } catch (e) { state.products = []; }
}

async function loadSummary() {
  try { applyLive(await ensureTarget(await api("/api/summary/today"))); } catch (e) {}
}

// Pəhriz planı yoxdursa, gündəlik hədəfi istifadəçinin profilindən (TDEE) hesabla.
async function ensureTarget(s) {
  s = s || {};
  if (s.targetCalories && s.targetCalories > 0) { state.dailyTarget = s.targetCalories; return s; }
  try {
    const me = await api("/api/auth/me");
    const g = await api("/api/goals/calculate", { method: "POST", body: JSON.stringify({
      age: me.age || 30, weightKg: me.weightKg || 70, heightCm: me.heightCm || 170,
      sex: me.sex || "MALE", activityLevel: me.activityLevel || "MODERATE", goal: "MAINTAIN" }) });
    state.dailyTarget = Math.round(g.tdee);
    s.targetCalories = state.dailyTarget;
  } catch (e) {}
  return s;
}

function applyLive(s) {
  const target = s.targetCalories || state.dailyTarget || 0;
  const cals = s.calories || 0;
  $("#calNow").textContent = Math.round(cals);
  $("#calTarget").textContent = Math.round(target);
  // Faizi HƏMİŞƏ göstərilən rəqəmlərdən hesabla — server 0 göndərsə belə sabit qalsın.
  const pct = target > 0 ? Math.round(100 * cals / target) : 0;
  const level = pct >= 100 ? "LIMIT" : pct >= 80 ? "WARN" : "OK";
  const ring = $("#ring");
  ring.style.setProperty("--p", Math.min(pct, 100));
  const color = level === "LIMIT" ? "var(--red)" : level === "WARN" ? "var(--amber)" : "var(--green)";
  ring.style.setProperty("--c", color);
  $("#ringPct").textContent = pct + "%";
  $("#mP").textContent = Math.round(s.proteinG || 0);
  $("#mF").textContent = Math.round(s.fatG || 0);
  $("#mC").textContent = Math.round(s.carbsG || 0);
  $("#barP").style.width = Math.min((s.proteinG || 0), 200) / 2 + "%";
  $("#barF").style.width = Math.min((s.fatG || 0), 200) / 2 + "%";
  $("#barC").style.width = Math.min((s.carbsG || 0), 400) / 4 + "%";
  const st = $("#liveState");
  if (st) st.textContent = level === "LIMIT" ? "⚠ Limit keçildi! (" + Math.round(s.calories || 0) + " / " + Math.round(target) + " kkal)"
    : level === "WARN" ? "Diqqət: 80%-i keçdin (" + pct + "%)" : "Günlük limitin: " + Math.round(target) + " kkal";
}

async function loadToday() {
  try {
    const logs = await api("/api/logs/today");
    const box = $("#todayLogs");
    if (!logs.length) { box.innerHTML = '<div class="result-empty">Hələ qeyd yoxdur.</div>'; return; }
    box.innerHTML = logs.map((l) =>
      '<div class="item"><div><div class="name">' + (l.productName || "Məhsul") + '</div>' +
      '<div class="meta">' + Math.round(l.grams) + ' q</div></div>' +
      '<div class="right">' + Math.round(l.calories) + ' kkal</div></div>').join("");
  } catch (e) {}
}

async function loadPlan() {
  try {
    const p = await api("/api/plans/my");
    $("#planBox").innerHTML = '<b>' + p.dailyCalorieTarget + ' kkal/gün</b> · P ' + p.proteinG + ' · Y ' + p.fatG + ' · K ' + p.carbsG +
      '<br><span class="meta">' + (p.notes || "") + '</span>';
  } catch (e) { $("#planBox").innerHTML = '<span class="sub">Hələ pəhriz planın yoxdur.</span>'; }
}

$("#logBtn").addEventListener("click", async () => {
  const productId = +$("#prodSelect").value;
  const grams = +$("#grams").value;
  if (!productId || !grams) return toast("Məhsul və qram seç", "warn");
  try {
    await api("/api/logs", { method: "POST", body: JSON.stringify({ productId, grams }) });
    toast("Əlavə edildi", "good");
    loadToday();
    setTimeout(loadSummary, 400);
  } catch (err) { toast("Xəta: " + err.message, "bad"); }
});

async function loadDoctor() {
  try {
    const patients = await api("/api/auth/doctor/patients");
    const list = $("#patients");
    const sel = $("#planPatient");
    // id -> ad/email xəritəsi (xəbərdarlıqda ad göstərmək üçün)
    state.patientsById = {};
    patients.forEach((p) => { state.patientsById[p.id] = p.fullName || p.email; });
    if (!patients.length) { list.innerHTML = '<div class="result-empty">Sənə təyin olunmuş pasiyent yoxdur.</div>'; sel.innerHTML = ""; return; }
    list.innerHTML = patients.map((p) =>
      '<div class="item"><div><div class="name">' + (p.fullName || p.email) + '</div>' +
      '<div class="meta">' + p.email + '</div></div>' +
      '<div class="right"><button class="btn sm ghost" data-patient="' + p.id + '">Bugünü gör</button> ' +
      '<button class="btn sm" data-chat="' + p.id + '" data-name="' + (p.fullName || p.email) + '">Yaz</button></div></div>').join("");
    sel.innerHTML = patients.map((p) => '<option value="' + p.id + '">' + (p.fullName || p.email) + '</option>').join("");
    loadAlerts();
  } catch (err) { toast("Xəta: " + err.message, "bad"); }
}

document.addEventListener("click", async (e) => {
  const b = e.target.closest("[data-patient]");
  if (!b) return;
  try {
    const s = await api("/api/summary/patient/" + b.dataset.patient + "/today");
    toast("Pasiyent bu gün: " + Math.round(s.calories) + " / " + Math.round(s.targetCalories) + " kkal (" + s.percent + "%)", s.level === "LIMIT" ? "bad" : "good");
  } catch (err) { toast("Bu gün üçün məlumat yoxdur", "warn"); }
});

function renderAlertItem(a, prepend) {
  const who = (state.patientsById && state.patientsById[a.patientId]) || ("Pasiyent #" + a.patientId);
  const box = $("#alerts");
  const empty = box.querySelector(".result-empty");
  if (empty) empty.remove();
  const el = document.createElement("div");
  el.className = "item";
  el.innerHTML = '<div><div class="name">⚠ ' + who + ' limiti keçdi</div>' +
    '<div class="meta">' + Math.round(a.calories) + ' / ' + Math.round(a.targetCalories) + ' kkal</div></div>' +
    '<div class="right"><span class="pill LIMIT">' + a.percent + '%</span></div>';
  if (prepend) box.prepend(el); else box.appendChild(el);
}

// Canlı (WebSocket) xəbərdarlıq
function pushAlert(a) {
  renderAlertItem(a, true);
  const who = (state.patientsById && state.patientsById[a.patientId]) || ("Pasiyent #" + a.patientId);
  toast("Xəbərdarlıq: " + who + " limiti keçdi", "bad");
}

// Panelə girəndə saxlanmış xəbərdarlıqları yüklə
async function loadAlerts() {
  const box = $("#alerts");
  try {
    const al = await api("/api/summary/alerts");
    box.innerHTML = al.length ? "" : '<div class="result-empty">Hələ xəbərdarlıq yoxdur.</div>';
    al.forEach((a) => renderAlertItem(a, false)); // API-dən yenidən köhnəyə
  } catch (e) { /* boş qalsın */ }
}

$("#planBtn").addEventListener("click", async () => {
  const patientId = +$("#planPatient").value;
  if (!patientId) return toast("Pasiyent seç", "warn");
  const body = {
    patientId, dailyCalorieTarget: +$("#p_target").value,
    proteinG: +$("#p_protein").value, fatG: +$("#p_fat").value, carbsG: +$("#p_carbs").value,
    notes: $("#p_notes").value
  };
  try {
    await api("/api/plans", { method: "POST", body: JSON.stringify(body) });
    toast("Plan yaradıldı", "good");
  } catch (err) { toast("Xəta: " + err.message, "bad"); }
});

async function loadAdmin() {
  try { state.products = await api("/api/products"); } catch (e) {}
  const tb = $("#prodTable tbody");
  tb.innerHTML = state.products.map((p) =>
    '<tr><td>' + p.name + '</td><td>' + (p.category || "") + '</td><td>' + p.calories + '</td><td>' +
    (p.proteinG || 0) + '</td><td>' + (p.fatG || 0) + '</td><td>' + (p.carbsG || 0) + '</td></tr>').join("");
  try {
    const users = await api("/api/auth/admin/users");
    const doctors = users.filter((u) => u.role === "DOCTOR");
    const patients = users.filter((u) => u.role === "USER");
    $("#as_patient").innerHTML = patients.map((u) => '<option value="' + u.id + '">' + (u.fullName || u.email) + '</option>').join("");
    $("#as_doctor").innerHTML = doctors.map((u) => '<option value="' + u.id + '">' + (u.fullName || u.email) + '</option>').join("");
  } catch (e) {}
}

$("#prodBtn").addEventListener("click", async () => {
  const body = {
    name: $("#a_name").value, category: $("#a_cat").value, calories: +$("#a_kcal").value,
    proteinG: +$("#a_p").value, fatG: +$("#a_f").value, carbsG: +$("#a_c").value
  };
  if (!body.name) return toast("Ad yaz", "warn");
  try { await api("/api/products", { method: "POST", body: JSON.stringify(body) }); state.products = []; toast("Məhsul əlavə edildi", "good"); loadAdmin(); }
  catch (err) { toast("Xəta: " + err.message, "bad"); }
});

$("#doctorBtn").addEventListener("click", async () => {
  const body = { fullName: $("#d_name").value, email: $("#d_email").value, password: $("#d_pass").value };
  if (!body.email) return toast("E-poçt yaz", "warn");
  try { await api("/api/auth/admin/doctors", { method: "POST", body: JSON.stringify(body) }); toast("Həkim yaradıldı", "good"); loadAdmin(); }
  catch (err) { toast("Xəta: " + err.message, "bad"); }
});

$("#assignBtn").addEventListener("click", async () => {
  const patientId = +$("#as_patient").value, doctorId = +$("#as_doctor").value;
  if (!patientId || !doctorId) return toast("Pasiyent və həkim seç", "warn");
  try { await api("/api/auth/admin/patients/" + patientId + "/doctor/" + doctorId, { method: "PUT" }); toast("Təyin edildi", "good"); }
  catch (err) { toast("Xəta: " + err.message, "bad"); }
});

loadSession();
refreshChrome();
// ---------- Pro abunə ----------
async function loadProStatus() {
  if (state.role !== "USER") return;
  const badge = $("#proBadge"), docBtn = $("#docChatBtn"), buyBtn = $("#buyProBtn"), status = $("#proStatus");
  const simBtn = $("#simProBtn");
  try {
    const st = await api("/api/billing/status");
    state.pro = !!st.pro;
    if (st.pro) {
      badge.classList.remove("hidden");
      buyBtn.classList.add("hidden");
      if (simBtn) simBtn.classList.add("hidden");
      status.textContent = "Pro aktivdir" + (st.proUntil ? " — " + new Date(st.proUntil).toLocaleDateString() + " tarixinədək" : "");
      try {
        const me = await api("/api/auth/me");
        if (me.doctorId) { state.doctorId = me.doctorId; docBtn.classList.remove("hidden"); }
      } catch (e) {}
    } else {
      badge.classList.add("hidden");
      buyBtn.classList.remove("hidden");
      if (simBtn) simBtn.classList.remove("hidden");
      docBtn.classList.add("hidden");
      status.textContent = "Pro ilə şəxsi həkiminlə birbaşa yazışa bilərsən.";
    }
  } catch (e) {}
}

$("#buyProBtn") && $("#buyProBtn").addEventListener("click", async () => {
  try {
    const r = await api("/api/billing/checkout", { method: "POST" });
    if (r.url) window.location.href = r.url;
    else toast("Ödəniş başladıla bilmədi", "bad");
  } catch (e) { toast("Ödəniş xətası (Stripe konfiqi?): " + e.message, "bad"); }
});

$("#simProBtn") && $("#simProBtn").addEventListener("click", async () => {
  try {
    await api("/api/billing/simulate", { method: "POST" });
    toast("Pro aktivləşdi (test) 🎉", "good");
    loadProStatus();
  } catch (e) { toast("Alınmadı: " + e.message, "bad"); }
});

// ---------- Həkim ↔ pasiyent chat ----------
function openDocChat(peerId, title) {
  state.chatPeer = peerId;
  $("#docChatTitle").textContent = title || "Həkim chat";
  $("#docChat").classList.remove("hidden");
  loadChatHistory(peerId);
}
function closeDocChat() { $("#docChat").classList.add("hidden"); state.chatPeer = null; }

async function loadChatHistory(peerId) {
  const body = $("#docChatBody");
  body.innerHTML = "";
  try {
    const msgs = await api("/api/chat/" + peerId);
    if (!msgs.length) addChatBubble("Söhbətə başla 👋", false);
    msgs.forEach((m) => addChatBubble(m.content, String(m.senderId) === String(state.userId)));
  } catch (e) {
    addChatBubble("Yazışma açıla bilmədi (Pro + təyin olunmuş həkim tələb olunur).", false, true);
  }
}
function addChatBubble(text, mine, isError) {
  const el = document.createElement("div");
  el.className = "chat-msg " + (isError ? "error" : mine ? "user" : "bot");
  el.textContent = text;
  const body = $("#docChatBody");
  body.appendChild(el);
  body.scrollTop = body.scrollHeight;
}
function onDocChat(msg) {
  if (msg.error) { addChatBubble(msg.error, false, true); return; }
  const peer = state.chatPeer;
  if (peer && (String(msg.senderId) === String(peer) || String(msg.recipientId) === String(peer))) {
    addChatBubble(msg.content, String(msg.senderId) === String(state.userId));
  }
}

$("#docChatClose") && $("#docChatClose").addEventListener("click", closeDocChat);
$("#docChatBtn") && $("#docChatBtn").addEventListener("click", () => {
  if (state.doctorId) openDocChat(state.doctorId, "Həkimim");
  else toast("Sənə həkim təyin olunmayıb", "warn");
});
$("#docChatForm") && $("#docChatForm").addEventListener("submit", (e) => {
  e.preventDefault();
  const input = $("#docChatText");
  const text = input.value.trim();
  if (!text || !state.chatPeer) return;
  if (!state.stomp || !state.stomp.connected) { toast("Bağlantı yoxdur", "warn"); return; }
  state.stomp.publish({ destination: "/app/chat.send", body: JSON.stringify({ recipientId: state.chatPeer, content: text }) });
  input.value = "";
});
// Həkim: pasiyent siyahısındakı "Yaz" düyməsi
document.addEventListener("click", (e) => {
  const b = e.target.closest("[data-chat]");
  if (!b) return;
  openDocChat(+b.dataset.chat, b.dataset.name || "Pasiyent");
});

// ---------- Qidanı ada görə axtar (kataloq + API) ----------
$("#foodSearchBtn") && $("#foodSearchBtn").addEventListener("click", async () => {
  const name = $("#foodSearch").value.trim();
  if (!name) return toast("Qida adını yaz", "warn");
  try {
    const found = await api("/api/products/search?name=" + encodeURIComponent(name));
    if (!found || !found.length) return toast("Bu qida tapılmadı", "warn");
    const sel = $("#prodSelect");
    found.slice().reverse().forEach((p) => {
      if (!state.products.find((x) => x.id === p.id)) state.products.unshift(p);
      const opt = document.createElement("option");
      opt.value = p.id;
      opt.textContent = p.name + " — " + Math.round(p.calories) + " kkal";
      sel.insertBefore(opt, sel.firstChild);
    });
    sel.value = found[0].id;
    toast(found.length + " nəticə tapıldı və siyahıya əlavə olundu", "good");
  } catch (e) { toast("Axtarış xətası: " + e.message, "bad"); }
});

// ---------- Dark / Light görünüş ----------
function applyTheme(theme) {
  document.documentElement.setAttribute("data-theme", theme);
  const b = $("#themeBtn");
  if (b) b.textContent = theme === "dark" ? "☀️" : "🌙";
  localStorage.setItem("azk_theme", theme);
}
applyTheme(localStorage.getItem("azk_theme") || "light");
$("#themeBtn") && $("#themeBtn").addEventListener("click", () => {
  const cur = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
  applyTheme(cur);
});

// ---------- Hesablamalar: ideal kilo + bədən yağ ----------
state.iwSex = "MALE"; state.bfSex = "MALE";
function segHandler(id, key) {
  const seg = $("#" + id);
  seg && seg.addEventListener("click", (e) => {
    const b = e.target.closest("[data-s]"); if (!b) return;
    state[key] = b.dataset.s;
    seg.querySelectorAll("button").forEach((x) => x.classList.toggle("on", x === b));
  });
}
segHandler("iwSex", "iwSex");
segHandler("bfSex", "bfSex");

$("#iwBtn") && $("#iwBtn").addEventListener("click", () => {
  const h = +$("#iw_h").value;
  if (!h) return toast("Boyunu yaz", "warn");
  const male = state.iwSex === "MALE";
  const inch = h / 2.54, over = inch - 60, hm = h / 100;
  const rows = [
    ["Peterson (2016)", 2.2 * 22 + 3.5 * 22 * (hm - 1.5)],
    ["Miller (1983)", (male ? 56.2 : 53.1) + (male ? 1.41 : 1.36) * over],
    ["Robinson (1983)", (male ? 52 : 49) + (male ? 1.9 : 1.7) * over],
    ["Devine (1974)", (male ? 50 : 45.5) + 2.3 * over],
    ["Hamwi (1964)", (male ? 48 : 45.5) + (male ? 2.7 : 2.2) * over],
  ];
  const vals = rows.map((r) => r[1]).filter((v) => v > 0);
  const avg = vals.reduce((a, b) => a + b, 0) / (vals.length || 1);
  $("#iwResult").innerHTML =
    rows.map((r) => '<div class="item"><div class="name">' + r[0] + '</div><div class="right"><b>' +
      (r[1] > 0 ? r[1].toFixed(1) : "—") + ' kq</b></div></div>').join("") +
    '<div class="item" style="border-top:2px solid var(--green)"><div class="name"><b>Orta ideal kilo</b></div>' +
    '<div class="right"><b style="color:var(--green)">' + avg.toFixed(1) + ' kq</b></div></div>';
});

// qadında kalça sahəsini göstər/gizlət
$("#bfSex") && $("#bfSex").addEventListener("click", () => {
  const hip = $("#hipField");
  if (hip) hip.style.display = state.bfSex === "FEMALE" ? "" : "none";
});
$("#bfBtn") && $("#bfBtn").addEventListener("click", () => {
  const male = state.bfSex === "MALE";
  const h = +$("#bf_h").value, neck = +$("#bf_neck").value, waist = +$("#bf_waist").value;
  const hip = +($("#bf_hip") ? $("#bf_hip").value : 0);
  if (!h || !neck || !waist || (!male && !hip)) return toast("Ölçüləri doldur", "warn");
  const log10 = (x) => Math.log(x) / Math.LN10;
  let bf = male
    ? 495 / (1.0324 - 0.19077 * log10(waist - neck) + 0.15456 * log10(h)) - 450
    : 495 / (1.29579 - 0.35004 * log10(waist + hip - neck) + 0.22100 * log10(h)) - 450;
  if (!isFinite(bf) || bf <= 0) return toast("Ölçülər uyğun deyil (bel > boyun olmalıdır)", "warn");
  let cat = "Normal", color = "var(--green)";
  if (bf < (male ? 6 : 14)) { cat = "Çox aşağı"; color = "var(--amber)"; }
  else if (bf > (male ? 25 : 32)) { cat = "Yüksək"; color = "var(--red)"; }
  else if (bf > (male ? 18 : 25)) { cat = "Orta-yuxarı"; color = "var(--amber)"; }
  $("#bfResult").innerHTML =
    '<div class="kcal-big" style="color:' + color + '">' + bf.toFixed(1) + ' <small>% bədən yağı</small></div>' +
    '<div class="stat-row"><span>Kateqoriya</span><b style="color:' + color + '">' + cat + '</b></div>' +
    '<p class="sub">U.S. Navy metodu — ölçülərə əsaslanır, təxminidir.</p>';
});
$("#bmiBtn") && $("#bmiBtn").addEventListener("click", () => {
  const h = +$("#bmi_h").value, w = +$("#bmi_w").value;
  if (!h || !w) return toast("Boy və çəkini yaz", "warn");
  const hm = h / 100, bmi = w / (hm * hm);
  let cat = "Normal", color = "var(--green)";
  if (bmi < 18.5) { cat = "Arıq"; color = "var(--amber)"; }
  else if (bmi >= 30) { cat = "Piylənmə"; color = "var(--red)"; }
  else if (bmi >= 25) { cat = "Artıq çəki"; color = "var(--amber)"; }
  $("#bmiResult").innerHTML =
    '<div class="kcal-big" style="color:' + color + '">' + bmi.toFixed(1) + ' <small>BMI</small></div>' +
    '<div class="stat-row"><span>Kateqoriya</span><b style="color:' + color + '">' + cat + '</b></div>' +
    '<p class="sub">Norma: 18.5–24.9 · Artıq çəki: 25–29.9 · Piylənmə: 30+</p>';
});

// ---------- Şəkillər (TheMealDB — pulsuz) ----------
const IMG = {
  soup: "https://www.themealdb.com/images/media/meals/60oc3k1699009846.jpg",
  beef: "https://www.themealdb.com/images/media/meals/brmxra1782681940.jpg",
  cacik: "https://www.themealdb.com/images/media/meals/16zbeu1763789342.jpg",
  fish: "https://www.themealdb.com/images/media/meals/uvuyxu1503067369.jpg",
  greens: "https://www.themealdb.com/images/media/meals/73o3vq1765317873.jpg",
  chicken: "https://www.themealdb.com/images/media/meals/vdwloy1713225718.jpg",
  grill: "https://www.themealdb.com/images/media/meals/020z181619788503.jpg",
  curry: "https://www.themealdb.com/images/media/meals/9ya6o71780262651.jpg",
};

// ---------- Reseptlər ----------
const RECIPES = [
  { img: IMG.grill, title: "Fırında toyuq döşü + tərəvəz", meta: "≈320 kkal · 35 dəq",
    desc: "Az yağlı, yüksək proteinli əsas yemək.",
    ing: ["Toyuq döşü 300q", "Bibər, kabaçkı, soğan", "1 x.q. zeytun yağı", "Duz, istiot, sarımsaq"],
    steps: ["Toyuğu ədviyyat və yağla marinad et.", "Tərəvəzi doğra.", "200°C fırında 25-30 dəq bişir.", "İsti servis et."] },
  { img: IMG.soup, title: "Yüngül kələm şorbası", meta: "≈90 kkal · 30 dəq",
    desc: "Aztkalorili, lifli, tox saxlayan şorba.",
    ing: ["Kələm 1/2", "Yerkökü, soğan, pomidor", "Su 1.5 l", "Duz, cəfəri"],
    steps: ["Tərəvəzi doğra.", "Suda 20 dəq qaynat.", "Duz və göyərti əlavə et.", "İsti servis et."] },
  { img: IMG.cacik, title: "Cacıq (qatıq-xiyar)", meta: "≈70 kkal · 10 dəq",
    desc: "Sərinləşdirici, probiotik qarnir.",
    ing: ["Qatıq 400q", "Xiyar 1", "Sarımsaq 1 diş", "Şüyüd, duz"],
    steps: ["Xiyarı rəndələ, suyunu sıx.", "Qatıqla qarışdır.", "Sarımsaq və şüyüd əlavə et.", "Soyuq servis et."] },
  { img: IMG.fish, title: "Balıqlı taco (yüngül)", meta: "≈280 kkal · 25 dəq",
    desc: "Omega-3 mənbəyi, tərəvəzli.",
    ing: ["Ağ balıq 250q", "Tam buğda tortilla", "Kələm, pomidor, limon", "Qatıq sousu"],
    steps: ["Balığı ədviyyatla bişir.", "Tərəvəzi doğra.", "Tortillaya yığ.", "Limon sıxıb servis et."] },
  { img: IMG.curry, title: "Az yağlı toyuqlu köri", meta: "≈350 kkal · 40 dəq",
    desc: "Ədviyyatlı, doyurucu əsas yemək.",
    ing: ["Toyuq 300q", "Soğan, sarımsaq, zəncəfil", "Köri ədviyyatı", "Az yağlı qatıq"],
    steps: ["Soğanı qızart.", "Toyuğu və ədviyyatı əlavə et.", "Qatıqla 20 dəq bişir.", "Düyü ilə servis et."] },
  { img: IMG.greens, title: "Göyərtili omlet", meta: "≈220 kkal · 12 dəq",
    desc: "Sürətli, proteinli səhər yeməyi.",
    ing: ["Yumurta 3", "İspanaq, cəfəri", "Pomidor", "Az duz, istiot"],
    steps: ["Yumurtanı çal.", "Göyərti və pomidoru əlavə et.", "Az yağda bişir.", "Büküb servis et."] },
];

// ---------- Məqalələr ----------
const ARTICLES = [
  { img: IMG.beef, title: "Kalori nədir və niyə vacibdir?",
    body: "Kalori qidadan aldığımız enerjidir. Gün ərzində yandırdığımızdan çox kalori alsaq çəki artır, az alsaq azalır. Balans açardır — hədəfini bil, qidanı ona görə seç. Kalkulyator ilə gündəlik ehtiyacını hesabla və jurnalını izlə." },
  { img: IMG.grill, title: "Protein: doyma və əzələ",
    body: "Protein həm əzələni qoruyur, həm də uzun müddət tox saxlayır. Hər yeməyə bir protein mənbəyi (yumurta, toyuq, balıq, mərci, süzmə) əlavə etmək iştahı idarə etməyə kömək edir. Gündə çəkinin hər kq-na 1.2–2.0 q protein məqsədəuyğundur." },
  { img: IMG.fish, title: "Sağlam yağlar və Omega-3",
    body: "Bütün yağlar pis deyil. Balıq, qoz, zeytun yağı və avokadodakı yağlar ürək və hormonlar üçün faydalıdır. Vacib olan ölçüdür — yağ kaloriyə görə ən sıxdır (1 q = 9 kkal)." },
  { img: IMG.soup, title: "Lif niyə vacibdir?",
    body: "Tərəvəz, göyərti və paxlalıdakı lif həzmi yaxşılaşdırır və uzun müddət toxluq verir. Şorbalar və salatlar az kalori ilə çox həcm verir — arıqlamağın rahat yolu." },
  { img: IMG.cacik, title: "Su və probiotiklər",
    body: "Bəzən susuzluq aclıq kimi hiss olunur. Gündə çəkinin hər kq-na 30–35 ml su hədəflə. Qatıq və ayran kimi probiotik məhsullar həzmə kömək edir." },
  { img: IMG.curry, title: "Fəallıq və gündəlik hədəf",
    body: "İdman kalori yandırır, amma əsas iş mətbəxdə görülür. TDEE-ni bil, arıqlamaq üçün ondan ~500 kkal az saxla — həftədə təxminən yarım kiloqram sağlam itki. Hədəf heç vaxt BMR-dən aşağı olmasın." },
];

function mediaCard(item, kind, idx) {
  return '<div class="card media-card" data-kind="' + kind + '" data-idx="' + idx + '">' +
    '<div class="thumb" style="background-image:url(\'' + item.img + '\')"></div>' +
    '<div class="body">' + (item.meta ? '<span class="chip tag">' + item.meta + '</span><div style="height:8px"></div>' : '') +
    '<h3>' + item.title + '</h3><p class="sub">' + (item.desc || item.body.slice(0, 90) + "…") + '</p></div></div>';
}
function renderGrids() {
  const rg = $("#recipeGrid"), ag = $("#articleGrid");
  if (rg && !rg.dataset.done) { rg.innerHTML = RECIPES.map((r, i) => mediaCard(r, "recipe", i)).join(""); rg.dataset.done = "1"; }
  if (ag && !ag.dataset.done) { ag.innerHTML = ARTICLES.map((a, i) => mediaCard(a, "article", i)).join(""); ag.dataset.done = "1"; }
}
renderGrids();

// ---------- Modal ----------
function openModal(img, html) {
  $("#modalImg").style.backgroundImage = "url('" + img + "')";
  $("#modalBody").innerHTML = html;
  $("#modal").classList.remove("hidden");
}
function closeModal() { $("#modal").classList.add("hidden"); }
$("#modalClose") && $("#modalClose").addEventListener("click", closeModal);
$("#modal") && $("#modal").addEventListener("click", (e) => { if (e.target.id === "modal") closeModal(); });

document.addEventListener("click", (e) => {
  const card = e.target.closest(".media-card");
  if (!card) return;
  const kind = card.dataset.kind, idx = +card.dataset.idx;
  if (kind === "recipe") {
    const r = RECIPES[idx];
    openModal(r.img,
      '<span class="chip tag">' + r.meta + '</span><h2>' + r.title + '</h2>' +
      '<p class="sub">' + r.desc + '</p>' +
      '<h3>Tərkib</h3><ul>' + r.ing.map((x) => '<li>' + x + '</li>').join("") + '</ul>' +
      '<h3>Hazırlanması</h3><ol>' + r.steps.map((x) => '<li>' + x + '</li>').join("") + '</ol>');
  } else if (kind === "article") {
    const a = ARTICLES[idx];
    openModal(a.img, '<h2>' + a.title + '</h2><p>' + a.body + '</p>');
  }
});

if (state.token) connectWs();
go("home");
