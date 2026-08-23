# AzərKalori RAG Chatbot — Quraşdırma

Bu sənəd frontend chatbot + n8n + Qdrant + Groq əsaslı RAG axınını necə işə
salmağı izah edir.

## Memarlıq

```
Frontend chatbot  →  n8n (Chat Trigger)  →  Qdrant (uyğun mətn parçası)  →  Groq (cavab)
                                              ↑ embedding: Ollama (lokal, pulsuz)
```

- **Qdrant** — vektor DB (bilik bazasının embedding-ləri burada saxlanır)
- **Ollama** — embedding modeli `nomic-embed-text` (lokal, pulsuz)
- **Groq** — LLM (`llama-3.3-70b-versatile`), pulsuz key ilə
- **n8n** — axını idarə edir, frontend üçün webhook açır

## 1. AI stack-i qaldır

```bash
docker compose -f docker-compose.ai.yml up -d
```

Bu qaldırır: `azk-qdrant` (6333), `azk-ollama` (11434), `azk-n8n` (5678).
`azk-ollama-pull` bir dəfə işləyib `nomic-embed-text` modelini yükləyir və çıxır.

> Əgər köhnə self-hosted-ai-starter-kit işləyirsə, əvvəlcə onu dayandır
> (eyni portlar): `docker stop n8n qdrant ollama`

## 2. Groq açarı al (pulsuz)

1. https://console.groq.com → qeydiyyat → **API Keys** → yeni key yarat
2. Bu key-i n8n-də credential kimi əlavə edəcəksən (aşağıda). Faylda saxlama.

## 3. n8n-i aç və workflow-u import et

1. Brauzerdə `http://localhost:5678` aç (ilk dəfə owner hesabı yarat)
2. **Workflows → Import from File** → `n8n/azerkalori-rag.workflow.json` seç
3. Import olunan workflow 3 credential istəyəcək — onları qur:

| Credential | Necə |
|---|---|
| **Groq account** | API Key = Groq-dan aldığın açar |
| **Ollama account** | Base URL = `http://ollama:11434` |
| **Qdrant account** | URL = `http://qdrant:6333` (API key boş) |

## 4. Bilik bazasını Qdrant-a yüklə (ingest)

Workflow-da **"Manual: ingest knowledge base"** node-una klik → **Test workflow**.
Bu, `shared/azerkalori-nutrition.md` faylını oxuyur, parçalara bölür, Ollama ilə
embedding çıxarır və Qdrant-dakı `azerkalori` kolleksiyasına yazır.

Yoxlama: `http://localhost:6333/dashboard` → `azerkalori` kolleksiyasında
nöqtələr (points) görünməlidir.

## 5. Workflow-u aktiv et və webhook-u götür

1. Workflow-u **Active** et (yuxarı sağ)
2. **"When chat message received"** node-una klik → **Production URL**-i kopyala
   (məs. `http://localhost:5678/webhook/azerkalori-chat-0001/chat`)
3. Bu URL-i `frontend/js/chatbot.js` faylında `N8N_CHAT_WEBHOOK`-a yaz

## 6. Frontend-i işə sal və sına

Frontend nginx ilə verilir (əsas `docker-compose.yml`). Səhifəni aç, sağ aşağıda
💬 düyməsinə bas və soruş: *"100 qram plovda neçə kalori var?"* və ya
*"Arıqlamaq üçün nə etməliyəm?"*

Chatbot Qdrant-dakı bilik bazasından uyğun məlumatı tapıb Groq ilə cavab yazacaq.

## Problemlər

- **Chatbot cavab vermir:** n8n işləyir? Workflow **Active**-dir? Webhook URL düzdür?
- **CORS xətası (konsolda):** `docker-compose.ai.yml`-də `N8N_CORS_ALLOW_ORIGIN=*` var,
  n8n-i yenidən başlat.
- **Boş/zəif cavab:** əvvəlcə ingest (addım 4) etdin? Qdrant kolleksiyası boşdursa
  cavab da boş olar.
- **Ollama embedding xətası:** `docker exec azk-ollama ollama list` ilə
  `nomic-embed-text` yükləndiyini yoxla.
