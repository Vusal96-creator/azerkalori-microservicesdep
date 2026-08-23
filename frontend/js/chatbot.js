// AzərKalori RAG chatbot — frontend widget.
// n8n "When chat message received" (Chat Trigger) node-una POST edir.
//
// QURAŞDIRMA: workflow-u n8n-də aktiv edəndən sonra Chat Trigger node-unun
// "Production URL"-ni götür və aşağıdakı N8N_CHAT_WEBHOOK-a yaz.
// Adətən: http://localhost:5678/webhook/<webhookId>/chat
const N8N_CHAT_WEBHOOK = "http://localhost:5678/webhook/azerkalori-chat-0001/chat";

(function () {
  const fab = document.getElementById("chatFab");
  const panel = document.getElementById("chatPanel");
  const closeBtn = document.getElementById("chatClose");
  const body = document.getElementById("chatBody");
  const form = document.getElementById("chatForm");
  const input = document.getElementById("chatText");
  if (!fab || !panel) return;

  // Hər brauzer sessiyası üçün sabit sessionId (söhbət yaddaşı üçün).
  const sessionId =
    localStorage.getItem("azk_chat_sid") ||
    (Date.now().toString(36) + Math.random().toString(36).slice(2, 8));
  localStorage.setItem("azk_chat_sid", sessionId);

  function toggle(open) {
    panel.classList.toggle("hidden", !open);
    if (open) input.focus();
  }
  fab.addEventListener("click", () => toggle(panel.classList.contains("hidden")));
  closeBtn.addEventListener("click", () => toggle(false));

  function addMsg(text, who) {
    const el = document.createElement("div");
    el.className = "chat-msg " + who;
    el.textContent = text;
    body.appendChild(el);
    body.scrollTop = body.scrollHeight;
    return el;
  }

  // n8n cavabı müxtəlif formatda gələ bilər — çoxlu açarı yoxlayırıq.
  function extractAnswer(data) {
    if (typeof data === "string") return data;
    if (!data || typeof data !== "object") return null;
    return (
      data.output ||
      data.text ||
      data.response ||
      data.answer ||
      (data.data && (data.data.output || data.data.text)) ||
      null
    );
  }

  async function send(question) {
    addMsg(question, "user");
    const typing = addMsg("yazır...", "bot typing");

    try {
      const res = await fetch(N8N_CHAT_WEBHOOK, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action: "sendMessage",
          sessionId: sessionId,
          chatInput: question,
        }),
      });

      typing.remove();

      if (!res.ok) {
        addMsg("Bağışla, köməkçi hazırda əlçatan deyil (xəta " + res.status + ").", "bot");
        return;
      }

      const data = await res.json().catch(() => null);
      const answer = extractAnswer(data);
      addMsg(answer || "Cavab tapılmadı. Sualı bir az fərqli yaz.", "bot");
    } catch (e) {
      typing.remove();
      addMsg("Bağlantı alınmadı. n8n işləyir? (chatbot.js → N8N_CHAT_WEBHOOK)", "bot");
    }
  }

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const q = input.value.trim();
    if (!q) return;
    input.value = "";
    send(q);
  });
})();
