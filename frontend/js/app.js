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
    if (!patients.length) { list.innerHTML = '<div class="result-empty">Sənə təyin olunmuş pasiyent yoxdur.</div>'; sel.innerHTML = ""; return; }
    list.innerHTML = patients.map((p) =>
      '<div class="item"><div><div class="name">' + (p.fullName || p.email) + '</div>' +
      '<div class="meta">' + p.email + '</div></div>' +
      '<div class="right"><button class="btn sm ghost" data-patient="' + p.id + '">Bugünü gör</button> ' +
      '<button class="btn sm" data-chat="' + p.id + '" data-name="' + (p.fullName || p.email) + '">Yaz</button></div></div>').join("");
    sel.innerHTML = patients.map((p) => '<option value="' + p.id + '">' + (p.fullName || p.email) + '</option>').join("");
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

function pushAlert(a) {
  const box = $("#alerts");
  if (box.querySelector(".result-empty")) box.innerHTML = "";
  const el = document.createElement("div");
  el.className = "item";
  el.innerHTML = '<div><div class="name">⚠ Pasiyent #' + a.patientId + ' limiti keçdi</div>' +
    '<div class="meta">' + Math.round(a.calories) + ' / ' + Math.round(a.targetCalories) + ' kkal</div></div>' +
    '<div class="right"><span class="pill LIMIT">' + a.percent + '%</span></div>';
  box.prepend(el);
  toast("Xəbərdarlıq: pasiyent #" + a.patientId + " limiti keçdi", "bad");
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

if (state.token) connectWs();
go("home");
