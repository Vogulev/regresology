#!/usr/bin/env bash
# Скрипт первичной настройки сервера.
# Запускать один раз от root или sudoer.
# Использование: bash deploy.sh

set -euo pipefail

APP_DIR="/opt/regreso"
COMPOSE_FILE="docker-compose.prod.yml"

echo "=== Regreso: первичная установка сервера ==="

# Проверяем docker
if ! command -v docker &>/dev/null; then
  echo "Устанавливаем Docker..."
  curl -fsSL https://get.docker.com | sh
  systemctl enable docker
  systemctl start docker
fi

# Создаём директорию приложения
mkdir -p "$APP_DIR"
cd "$APP_DIR"

# Копируем docker-compose.prod.yml (должен быть рядом со скриптом)
if [ -f "$(dirname "$0")/$COMPOSE_FILE" ]; then
  cp "$(dirname "$0")/$COMPOSE_FILE" "$APP_DIR/$COMPOSE_FILE"
fi

# Создаём .env если не существует
if [ ! -f "$APP_DIR/.env" ]; then
  echo "Создаём $APP_DIR/.env — ЗАПОЛНИ ЗНАЧЕНИЯ!"
  cat > "$APP_DIR/.env" <<'EOF'
# ─── Репозиторий (GitHub owner/repo, lowercase) ───────────────────────────────
GITHUB_REPOSITORY=ваш-github-username/regreso-backend

# ─── Docker image tag (обновляется CI автоматически) ─────────────────────────
IMAGE_TAG=latest

# ─── База данных ──────────────────────────────────────────────────────────────
DB_NAME=regreso
DB_USER=regreso
DB_PASSWORD=ИЗМЕНИ_НА_СИЛЬНЫЙ_ПАРОЛЬ

# ─── JWT (минимум 32 символа) ─────────────────────────────────────────────────
JWT_SECRET=ИЗМЕНИ_НА_СИЛЬНЫЙ_SECRET_МИНИМУМ_32_СИМВОЛА

# ─── MinIO ────────────────────────────────────────────────────────────────────
MINIO_ACCESS_KEY=ИЗМЕНИ_НА_КЛЮЧ
MINIO_SECRET_KEY=ИЗМЕНИ_НА_SECRET
MINIO_BUCKET=regreso
# Публичный URL MinIO — адрес, который браузер откроет для файлов
# Если нет отдельного домена для MinIO — используй IP:9000
MINIO_PUBLIC_URL=http://ВАШ_IP:9000

# ─── Telegram Bot (необязательно) ────────────────────────────────────────────
TELEGRAM_BOT_ENABLED=false
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=RegresoBot

# ─── AI (необязательно) ──────────────────────────────────────────────────────
AI_PROVIDER=stub
GEMINI_API_KEY=

# ─── CORS ────────────────────────────────────────────────────────────────────
# Укажи домен/IP фронтенда. Пример: https://app.regreso.ru или http://ВАШ_IP:3000
CORS_ALLOWED_ORIGINS=*
EOF
  echo ""
  echo "!! Отредактируй $APP_DIR/.env перед первым запуском !!"
  echo ""
fi

echo ""
echo "=== Готово ==="
echo "Следующие шаги:"
echo "  1. Отредактируй $APP_DIR/.env"
echo "  2. Скопируй $COMPOSE_FILE в $APP_DIR/"
echo "  3. Запусти: cd $APP_DIR && docker compose -f $COMPOSE_FILE up -d"
echo ""
