#!/usr/bin/env bash
# ==============================================================================
# admin.sh — Administración del backend Interview AI en Microsoft Azure
# ==============================================================================
# Qué es: una herramienta de operación para tareas del día a día y para
#         demostraciones (logs, estado, encendido/apagado de la base,
#         despliegue de una versión ya publicada, eliminación controlada).
#
# Qué NO es: NO construye ni publica imágenes. Eso es responsabilidad
#            exclusiva del pipeline de GitHub Actions. Este script solo
#            despliega versiones que YA existen en el ACR, es decir, imágenes
#            que pasaron por los tests y por el análisis de vulnerabilidades.
#
#            Esta es la diferencia con script_az.py del proyecto de referencia
#            (Go), donde el script local y el CI construían imágenes con
#            criterios distintos y podían desplegar cosas diferentes.
#
# Uso:       bash scripts/admin.sh
#
# Requisitos: Azure CLI instalado y sesión iniciada (el script te avisa).
# ==============================================================================

set -uo pipefail

# ------------------------------------------------------------------------------
# Configuración (se puede sobreescribir con variables de entorno)
# ------------------------------------------------------------------------------
RESOURCE_GROUP="${RESOURCE_GROUP:-rg-interview-app}"
ACR_NAME="${ACR_NAME:-acrvaniainterview}"
ACR_REGISTRY="${ACR_REGISTRY:-${ACR_NAME}.azurecr.io}"
APP_NAME="${APP_NAME:-interview-backend}"
IMAGE_NAME="${IMAGE_NAME:-interview-backend}"
DB_SERVER="${DB_SERVER:-mysql-interview-backend}"

# Colores para que la salida se lea mejor
if [[ -t 1 ]]; then
  C_OK=$'\033[0;32m'; C_ERR=$'\033[0;31m'; C_WARN=$'\033[0;33m'
  C_TIT=$'\033[1;36m'; C_OFF=$'\033[0m'
else
  C_OK=""; C_ERR=""; C_WARN=""; C_TIT=""; C_OFF=""
fi

ok()    { echo "${C_OK}  OK${C_OFF}   $*"; }
err()   { echo "${C_ERR}  ERROR${C_OFF} $*"; }
warn()  { echo "${C_WARN}  AVISO${C_OFF} $*"; }
titulo(){ echo; echo "${C_TIT}=== $* ===${C_OFF}"; }

# ------------------------------------------------------------------------------
# Verificación de sesión en Azure
# ------------------------------------------------------------------------------
verificar_login() {
  if ! az account show >/dev/null 2>&1; then
    warn "No hay sesión activa en Azure."
    echo "       Iniciando sesión con 'az login'..."
    az login >/dev/null 2>&1 || { err "No se pudo iniciar sesión."; return 1; }
  fi
  local cuenta
  cuenta=$(az account show --query "name" -o tsv 2>/dev/null)
  ok "Sesión activa en la suscripción: ${cuenta}"
  return 0
}

# ------------------------------------------------------------------------------
# 1. Estado general del sistema
# ------------------------------------------------------------------------------
estado_general() {
  titulo "ESTADO GENERAL DEL SISTEMA"

  echo "--- Aplicación (Container App) ---"
  az containerapp show -n "${APP_NAME}" -g "${RESOURCE_GROUP}" \
    --query "{estado:properties.provisioningState, imagen:properties.template.containers[0].image, fqdn:properties.configuration.ingress.fqdn, replicasMin:properties.template.scale.minReplicas, replicasMax:properties.template.scale.maxReplicas}" \
    -o table 2>/dev/null || err "No se pudo consultar la aplicación."

  echo
  echo "--- Base de datos (MySQL) ---"
  az mysql flexible-server list \
    --query "[?name=='${DB_SERVER}'].{estado:state, version:version}" -o table 2>/dev/null \
    || err "No se pudo consultar la base."

  echo
  echo "--- Revisiones ---"
  az containerapp revision list -n "${APP_NAME}" -g "${RESOURCE_GROUP}" \
    --query "[].{revision:name, activa:properties.active, imagen:properties.template.containers[0].image, salud:properties.healthState}" \
    -o table 2>/dev/null || true

  echo
  echo "--- Health check público ---"
  local fqdn
  fqdn=$(az containerapp show -n "${APP_NAME}" -g "${RESOURCE_GROUP}" \
    --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null)
  if [[ -n "${fqdn}" ]]; then
    local code
    code=$(curl -s -o /dev/null -w '%{http_code}' -m 45 "https://${fqdn}/actuator/health" 2>/dev/null || echo "000")
    if [[ "${code}" == "200" ]]; then
      ok "La aplicación responde HTTP 200"
    elif [[ "${code}" == "503" ]]; then
      warn "HTTP 503 — la aplicación no arrancó (¿la base está apagada?)"
    elif [[ "${code}" == "000" ]]; then
      warn "Sin respuesta (la aplicación puede estar escalada a cero, sin réplicas)"
    else
      warn "Respuesta inesperada: HTTP ${code}"
    fi
    echo "       URL: https://${fqdn}"
  fi
}

# ------------------------------------------------------------------------------
# 2. Listar versiones disponibles en el ACR
# ------------------------------------------------------------------------------
listar_versiones() {
  titulo "VERSIONES DISPONIBLES EN EL REGISTRO (${ACR_NAME})"
  az acr repository show-tags --name "${ACR_NAME}" --repository "${IMAGE_NAME}" \
    -o tsv 2>/dev/null | while read -r tag; do
      echo "  - ${ACR_REGISTRY}/${IMAGE_NAME}:${tag}"
    done
  echo
  echo "  Recordatorio: las etiquetas con formato <sha-corto> las genera el"
  echo "  pipeline automáticamente. Las que empiezan con v son versiones."
}

# ------------------------------------------------------------------------------
# 3. Desplegar una versión ya publicada (también sirve para reversión)
# ------------------------------------------------------------------------------
desplegar_version() {
  titulo "DESPLEGAR UNA VERSIÓN PUBLICADA"
  listar_versiones

  local actual
  actual=$(az containerapp show -n "${APP_NAME}" -g "${RESOURCE_GROUP}" \
    --query "properties.template.containers[0].image" -o tsv 2>/dev/null)
  echo
  echo "  Versión ACTUALMENTE en ejecución: ${actual}"
  echo
  read -r -p "  Etiqueta a desplegar (ej: v1.0.1 o un sha corto): " tag
  [[ -z "${tag}" ]] && { warn "Cancelado."; return 0; }

  # Verificar que la etiqueta exista antes de tocar nada
  local existe
  existe=$(az acr repository show-tags --name "${ACR_NAME}" --repository "${IMAGE_NAME}" \
    --query "contains(@, '${tag}')" -o tsv 2>/dev/null)
  if [[ "${existe}" != "true" ]]; then
    err "La etiqueta '${tag}' no existe en el registro. No se desplegó nada."
    return 1
  fi

  echo
  warn "Se va a desplegar: ${ACR_REGISTRY}/${IMAGE_NAME}:${tag}"
  warn "Se creará una NUEVA REVISIÓN y el tráfico se moverá a ella."
  read -r -p "  ¿Confirmás? (s/N): " conf
  [[ "${conf}" =~ ^[sS]$ ]] || { warn "Cancelado."; return 0; }

  az containerapp update \
    --name "${APP_NAME}" \
    --resource-group "${RESOURCE_GROUP}" \
    --image "${ACR_REGISTRY}/${IMAGE_NAME}:${tag}" \
    --query "properties.template.containers[0].image" -o tsv
  ok "Despliegue enviado."

  echo "  Verificando que la aplicación responda..."
  local fqdn code
  fqdn=$(az containerapp show -n "${APP_NAME}" -g "${RESOURCE_GROUP}" \
    --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null)
  for i in $(seq 1 20); do
    code=$(curl -s -o /dev/null -w '%{http_code}' -m 30 "https://${fqdn}/actuator/health" 2>/dev/null || echo "000")
    [[ "${code}" == "200" ]] && { ok "La aplicación responde HTTP 200 (intento ${i})."; return 0; }
    echo "    Intento ${i}: HTTP ${code}... esperando"
    sleep 10
  done
  err "No respondió HTTP 200. Revisá los logs con la opción 5."
  return 1
}

# ------------------------------------------------------------------------------
# 4. Encender / detener la base de datos
# ------------------------------------------------------------------------------
iniciar_base() {
  titulo "ENCENDER LA BASE DE DATOS"
  echo "  Encender tarda 1-2 minutos. Se cobra el cómputo mientras esté encendida."
  az mysql flexible-server start -g "${RESOURCE_GROUP}" -n "${DB_SERVER}" 2>&1 | tail -2
  ok "Base encendida. Verificá que el estado sea 'Ready' antes de usar la app."
  az mysql flexible-server list --query "[?name=='${DB_SERVER}'].state" -o tsv 2>/dev/null
}

detener_base() {
  titulo "DETENER LA BASE DE DATOS"
  echo "  Al detenerla se deja de cobrar el cómputo (queda solo el almacenamiento)."
  echo "  Los datos NO se pierden."
  warn "Mientras esté detenida, el backend falla al arrancar."
  read -r -p "  ¿Confirmás? (s/N): " conf
  [[ "${conf}" =~ ^[sS]$ ]] || { warn "Cancelado."; return 0; }
  az mysql flexible-server stop -g "${RESOURCE_GROUP}" -n "${DB_SERVER}" 2>&1 | tail -2
  ok "Base detenida."
}

# ------------------------------------------------------------------------------
# 5. Ver logs de la aplicación
# ------------------------------------------------------------------------------
ver_logs() {
  titulo "LOGS DE LA APLICACIÓN"
  echo "  Nota: si la aplicación está escalada a cero (sin réplicas), no hay"
  echo "  logs en vivo. En ese caso usá la opción 7 (Log Analytics)."
  echo
  read -r -p "  Cantidad de líneas (Enter = 50): " n
  n="${n:-50}"
  az containerapp logs show -n "${APP_NAME}" -g "${RESOURCE_GROUP}" --tail "${n}" 2>&1 | tail -"${n}" \
    || err "No se pudieron obtener los logs (¿hay alguna réplica activa?)."
}

# ------------------------------------------------------------------------------
# 6. Revisiones y cambio de tráfico
# ------------------------------------------------------------------------------
ver_revisiones() {
  titulo "REVISIONES DE LA APLICACIÓN"
  az containerapp revision list -n "${APP_NAME}" -g "${RESOURCE_GROUP}" \
    --query "[].{revision:name, activa:properties.active, trafico:properties.trafficWeight, imagen:properties.template.containers[0].image, creada:properties.createdTime, salud:properties.healthState}" \
    -o table 2>/dev/null || err "No se pudieron listar las revisiones."
  echo
  echo "  En modo 'single revision' solo hay una revisión activa a la vez."
  echo "  Una reversión se hace desplegando una etiqueta anterior (opción 3)."
}

# ------------------------------------------------------------------------------
# 7. Consultar logs históricos en Log Analytics
# ------------------------------------------------------------------------------
logs_analytics() {
  titulo "LOGS HISTÓRICOS (Log Analytics)"
  local ws customer_id
  ws=$(az monitor log-analytics workspace list -g "${RESOURCE_GROUP}" --query "[0].name" -o tsv 2>/dev/null)
  if [[ -z "${ws}" ]]; then
    err "No se encontró el workspace de Log Analytics."
    return 1
  fi
  customer_id=$(az monitor log-analytics workspace show -g "${RESOURCE_GROUP}" -n "${ws}" --query customerId -o tsv 2>/dev/null)
  echo "  Workspace: ${ws}"
  echo "  Consultando las últimas 30 líneas de la aplicación..."
  echo

  local body
  body='{"query":"ContainerAppConsoleLogs_CL | order by TimeGenerated desc | take 30 | project TimeGenerated, Log_s","timespan":"PT12H"}'

  local tmp
  tmp=$(mktemp)
  printf '%s' "${body}" > "${tmp}"

  az rest --method post \
    --url "https://api.loganalytics.io/v1/workspaces/${customer_id}/query" \
    --body @"${tmp}" -o json 2>/dev/null | python -c "
import json,sys
try:
    d = json.load(sys.stdin)
except Exception:
    print('  No se pudo interpretar la respuesta.'); sys.exit(0)
for t in d.get('tables', []):
    for row in t.get('rows', []):
        print(' ', str(row[-1])[:200])
" 2>/dev/null
  rm -f "${tmp}"
}

# ------------------------------------------------------------------------------
# 8. Abrir la URL pública
# ------------------------------------------------------------------------------
abrir_url() {
  local fqdn
  fqdn=$(az containerapp show -n "${APP_NAME}" -g "${RESOURCE_GROUP}" \
    --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null)
  echo
  echo "  URL:        https://${fqdn}"
  echo "  Health:     https://${fqdn}/actuator/health"
  echo
  if command -v start >/dev/null 2>&1; then
    start "https://${fqdn}" 2>/dev/null || true      # Windows
  elif command -v open >/dev/null 2>&1; then
    open "https://${fqdn}" 2>/dev/null || true       # macOS
  fi
}

# ------------------------------------------------------------------------------
# 9. Eliminar la aplicación (operación destructiva, con doble confirmación)
# ------------------------------------------------------------------------------
eliminar_app() {
  titulo "ELIMINAR LA APLICACIÓN"
  err "OPERACIÓN DESTRUCTIVA E IRREVERSIBLE."
  echo "  Se eliminará la Container App '${APP_NAME}'."
  echo
  echo "  Qué se pierde:  la aplicación y su configuración (variables, secretos)."
  echo "  Qué NO se pierde: la imagen del ACR, la base de datos ni los logs."
  echo "  Costo: libera el consumo de la aplicación (ya era casi nulo con"
  echo "         minReplicas=0, así que el ahorro real es mínimo)."
  echo "  Riesgo conocido: si el entorno queda sin ninguna aplicación, Azure"
  echo "         puede marcarlo para auto-borrado. Para volver a desplegar"
  echo "         habría que crear el entorno de nuevo."
  echo
  read -r -p "  Escribí exactamente 'ELIMINAR' para confirmar: " conf
  if [[ "${conf}" != "ELIMINAR" ]]; then
    warn "Cancelado. No se eliminó nada."
    return 0
  fi
  az containerapp delete -n "${APP_NAME}" -g "${RESOURCE_GROUP}" --yes
  ok "Aplicación eliminada."
}

# ------------------------------------------------------------------------------
# Menú principal
# ------------------------------------------------------------------------------
menu() {
  while true; do
    echo
    echo "${C_TIT}========================================================${C_OFF}"
    echo "${C_TIT}  ADMINISTRACIÓN — Interview AI Backend (Azure)${C_OFF}"
    echo "${C_TIT}========================================================${C_OFF}"
    echo "   1) Estado general del sistema"
    echo "   2) Listar versiones disponibles en el ACR"
    echo "   3) Desplegar una versión publicada (o revertir)"
    echo "   4) Encender / detener la base de datos"
    echo "   5) Ver logs en vivo"
    echo "   6) Ver revisiones"
    echo "   7) Ver logs históricos (Log Analytics)"
    echo "   8) Abrir la URL pública"
    echo "   9) Eliminar la aplicación (destructivo)"
    echo "   0) Salir"
    echo
    read -r -p "  Opción: " op
    case "${op}" in
      1) estado_general ;;
      2) listar_versiones ;;
      3) desplegar_version ;;
      4)
        echo "     a) Encender    b) Detener"
        read -r -p "     Opción: " sub
        case "${sub}" in
          a|A) iniciar_base ;;
          b|B) detener_base ;;
          *) warn "Opción inválida." ;;
        esac
        ;;
      5) ver_logs ;;
      6) ver_revisiones ;;
      7) logs_analytics ;;
      8) abrir_url ;;
      9) eliminar_app ;;
      0) echo "  Chau."; exit 0 ;;
      *) warn "Opción inválida." ;;
    esac
  done
}

# ------------------------------------------------------------------------------
# Punto de entrada
# ------------------------------------------------------------------------------
echo "${C_TIT}Administración de Azure — Interview AI Backend${C_OFF}"
verificar_login || exit 1

# Si se pasa una opción como argumento, se ejecuta directo (sin menú).
# Útil para automatizar o para la demo: bash scripts/admin.sh estado
if [[ $# -gt 0 ]]; then
  case "$1" in
    estado)     estado_general ;;
    versiones)  listar_versiones ;;
    revisiones) ver_revisiones ;;
    logs)       ver_logs ;;
    encender)   iniciar_base ;;
    detener)    detener_base ;;
    url)        abrir_url ;;
    *) echo "Opciones: estado | versiones | revisiones | logs | encender | detener | url"; exit 1 ;;
  esac
  exit 0
fi

menu
