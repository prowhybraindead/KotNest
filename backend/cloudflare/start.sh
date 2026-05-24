#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck source=/dev/null
  source "${ENV_FILE}"
fi

: "${TUNNEL_METRICS:=127.0.0.1:20241}"
: "${TUNNEL_AUTO_INSTALL_CLOUDFLARED:=1}"
: "${CLOUDFLARED_BIN_PATH:=/home/container/.cloudflared/bin/cloudflared}"
: "${CLOUDFLARE_TUNNEL_TOKEN:=}"
: "${CLOUDFLARED_LOG_PATH:=${SCRIPT_DIR}/cloudflared.log}"
: "${CLOUDFLARED_PID_PATH:=${SCRIPT_DIR}/cloudflared.pid}"

if [[ -z "${CLOUDFLARE_TUNNEL_TOKEN}" ]]; then
  echo "[cloudflare] CLOUDFLARE_TUNNEL_TOKEN is empty. Skip tunnel startup."
  exit 0
fi

if [[ -f "${CLOUDFLARED_PID_PATH}" ]]; then
  EXISTING_PID="$(cat "${CLOUDFLARED_PID_PATH}" || true)"
  if [[ -n "${EXISTING_PID}" ]] && kill -0 "${EXISTING_PID}" >/dev/null 2>&1; then
    echo "[cloudflare] cloudflared already running (pid=${EXISTING_PID})."
    exit 0
  fi
fi

ensure_cloudflared() {
  if [[ -x "${CLOUDFLARED_BIN_PATH}" ]]; then
    return 0
  fi

  if [[ "${TUNNEL_AUTO_INSTALL_CLOUDFLARED}" != "1" ]]; then
    echo "[cloudflare] cloudflared missing and auto install disabled."
    exit 1
  fi

  local bin_dir
  local arch
  local pkg_arch
  local temp_deb

  bin_dir="$(dirname "${CLOUDFLARED_BIN_PATH}")"
  mkdir -p "${bin_dir}"

  arch="$(uname -m)"
  case "${arch}" in
    x86_64|amd64) pkg_arch="amd64" ;;
    aarch64|arm64) pkg_arch="arm64" ;;
    *)
      echo "[cloudflare] Unsupported architecture: ${arch}"
      exit 1
      ;;
  esac

  if curl -fsSL "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-${pkg_arch}" -o "${CLOUDFLARED_BIN_PATH}"; then
    chmod +x "${CLOUDFLARED_BIN_PATH}"
    echo "[cloudflare] cloudflared binary downloaded to ${CLOUDFLARED_BIN_PATH}"
    return 0
  fi

  temp_deb="$(mktemp /tmp/cloudflared.XXXXXX.deb)"
  curl -fsSL "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-${pkg_arch}.deb" -o "${temp_deb}"

  local extract_dir
  extract_dir="$(mktemp -d)"
  if command -v dpkg-deb >/dev/null 2>&1; then
    dpkg-deb -x "${temp_deb}" "${extract_dir}"
    install -m 0755 "${extract_dir}/usr/bin/cloudflared" "${CLOUDFLARED_BIN_PATH}"
    rm -rf "${extract_dir}" "${temp_deb}"
  else
    echo "[cloudflare] dpkg-deb not available and direct binary download failed."
    rm -rf "${extract_dir}" "${temp_deb}"
    exit 1
  fi

  echo "[cloudflare] cloudflared installed to ${CLOUDFLARED_BIN_PATH}"
}

ensure_cloudflared

nohup "${CLOUDFLARED_BIN_PATH}" tunnel \
  --metrics "${TUNNEL_METRICS}" \
  --loglevel info \
  run --token "${CLOUDFLARE_TUNNEL_TOKEN}" \
  >> "${CLOUDFLARED_LOG_PATH}" 2>&1 &

echo $! > "${CLOUDFLARED_PID_PATH}"
echo "[cloudflare] Started cloudflared tunnel with pid=$(cat "${CLOUDFLARED_PID_PATH}")"
