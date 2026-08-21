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
  $("#calcResult").innerHTML =
    '<div class="ring-wrap" style="align-items:flex-start">' +
      '<div style="flex:1">' +
        '<div class="kcal-big">' + r.dailyCalories + ' <small>kkal / gün</small></div>' +
        '<div class="stat-row"><span>BMR (baza)</span><b>' + r.bmr + ' kkal</b></div>' +
        '<div class="stat-row"><span>TDEE (aktivliklə)</span><b>' + r.tdee + ' kkal</b></div>' +
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
}

async function loadProducts() {
  if (state.products.length) return;
  try { state.products = await api("/api/products"); } catch (e) { state.products = []; }
}

async function loadSummary() {
  try { applyLive(await api("/api/summary/today")); } catch (e) {}
}

function applyLive(s) {
  const target = s.targetCalories || 0;
  $("#calNow").textContent = Math.round(s.calories || 0);
  $("#calTarget").textContent = Math.round(target);
  const pct = s.percent || 0;
  const ring = $("#ring");
  ring.style.setProperty("--p", Math.min(pct, 100));
  const color = s.level === "LIMIT" ? "var(--red)" : s.level === "WARN" ? "var(--amber)" : "var(--green)";
  ring.style.setProperty("--c", color);
  $("#ringPct").textContent = pct + "%";
  $("#mP").textContent = Math.round(s.proteinG || 0);
  $("#mF").textContent = Math.round(s.fatG || 0);
  $("#mC").textContent = Math.round(s.carbsG || 0);
  $("#barP").style.width = Math.min((s.proteinG || 0), 200) / 2 + "%";
  $("#barF").style.width = Math.min((s.fatG || 0), 200) / 2 + "%";
  $("#barC").style.width = Math.min((s.carbsG || 0), 400) / 4 + "%";
  const st = $("#liveState");
  if (st) st.textContent = s.level === "LIMIT" ? "⚠ Limit keçildi!" : s.level === "WARN" ? "Diqqət: 80%-i keçdin" : "Canlı sayğac (WebSocket)";
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
      '<div class="right"><button class="btn sm ghost" data-patient="' + p.id + '">Bugünü gör</button></div></div>').join("");
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
if (state.token) connectWs();
go("home");
