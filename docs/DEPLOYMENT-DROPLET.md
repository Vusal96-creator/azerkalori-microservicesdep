# 🚀 Deploying AzərKalori Microservices to a DigitalOcean Droplet

## 0. Sizing — read this first

| Container | RAM (capped) |
|---|---|
| discovery-server | ~220 MB (`-Xmx160m`) |
| api-gateway | ~280 MB (`-Xmx192m`) |
| auth-service | ~320 MB (`-Xmx224m`) |
| catalog-service | ~340 MB (`-Xmx256m`) |
| nutrition-service | ~340 MB (`-Xmx256m`) |
| tracking-service | ~380 MB (`-Xmx256m`) |
| Kafka (KRaft) | ~300 MB |
| PostgreSQL | ~200 MB |
| Redis | ~64 MB |
| OS + Docker | ~450 MB |
| **Total** | **~2.9 GB** |

➡️ **Recommended: 4 GB / 2 vCPU Droplet — $24/mo.**
The $12 (2 GB) droplet fits the *monolith*, not 6 JVMs.

**If you must stay on $12/2 GB**, trim to this and it fits (~1.8 GB):
merge `nutrition-service` into `tracking-service`, and skip the discovery UI by
running Eureka with `-Xmx128m`. Kafka stays (it is a core CV topic).

---

## 1. Create the Droplet

1. DigitalOcean → Create → Droplets.
2. Image: **Ubuntu 24.04 LTS**. Plan: Basic → Regular → **4 GB / 2 vCPU ($24)**.
3. Region: Frankfurt (closest to Baku).
4. Authentication: **SSH key** (never password).
5. Hostname: `azerkalori-prod` → Create.

```bash
ssh root@YOUR_DROPLET_IP
```

## 2. Server hardening (5 minutes, do not skip)

```bash
# non-root user
adduser deploy && usermod -aG sudo deploy
rsync --archive --chown=deploy:deploy ~/.ssh /home/deploy

# firewall — only SSH + HTTP(S)
ufw allow OpenSSH && ufw allow 80 && ufw allow 443
ufw enable

# disable root SSH login
sed -i 's/PermitRootLogin yes/PermitRootLogin no/' /etc/ssh/sshd_config
systemctl restart ssh
```

> ⚠️ Ports 8761 (Eureka), 8081-8084, 9090 are **never** opened in UFW.
> Only Nginx (80/443) is public; everything else lives on the internal Docker network.
> To view the Eureka dashboard: `ssh -L 8761:localhost:8761 deploy@IP` → open localhost:8761.

## 3. Swap file (safety net for JVM spikes)

```bash
fallocate -l 2G /swapfile && chmod 600 /swapfile
mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

## 4. Install Docker

```bash
su - deploy
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker deploy && newgrp docker
docker --version && docker compose version
```

## 5. Get the code & configure

```bash
git clone https://github.com/YOURNAME/azerkalori-microservices.git
cd azerkalori-microservices
cp .env.example .env
nano .env       # set POSTGRES_PASSWORD and JWT_SECRET (long random strings!)
```

Generate a strong secret: `openssl rand -base64 48`

## 6. Build & start

```bash
docker compose build          # multi-stage builds, ~10 min first time
docker compose up -d
docker compose ps             # everything should be "healthy"/"running"
docker compose logs -f api-gateway
```

Start order is handled by `depends_on` + healthchecks:
postgres/redis/kafka → discovery → services → gateway.

Smoke test from your laptop:
```bash
curl http://YOUR_DROPLET_IP/api/auth/ping   # after Nginx (step 7); before it, use SSH tunnel
```

## 7. Nginx reverse proxy + HTTPS

```bash
sudo apt install -y nginx certbot python3-certbot-nginx
sudo nano /etc/nginx/sites-available/azerkalori
```

```nginx
server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;      # api-gateway
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {                              # WebSocket upgrade
        proxy_pass http://127.0.0.1:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/azerkalori /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d api.yourdomain.com     # free TLS, auto-renews
```

No domain yet? Skip certbot and use `http://DROPLET_IP/...` for the demo.

## 8. Operations cheat-sheet

```bash
docker compose logs -f tracking-service      # follow one service
docker stats                                 # RAM per container
docker compose pull && docker compose up -d  # deploy new version
docker compose restart catalog-service       # restart one service
docker exec -it azerkalori-postgres psql -U azer azerkalori   # DB shell
```

Backups (cron, daily 03:00):
```bash
crontab -e
0 3 * * * docker exec azerkalori-postgres pg_dump -U azer azerkalori | gzip > /home/deploy/backups/db-$(date +\%F).sql.gz
```

## 9. Optional: CI/CD with GitHub Actions

`.github/workflows/deploy.yml` → on push to `main`:
build images → push to GHCR → SSH into droplet → `docker compose pull && docker compose up -d`.
Add `DROPLET_HOST`, `DROPLET_SSH_KEY` as repo secrets. (Add this in Phase 8 — nice CV bonus, not required.)
