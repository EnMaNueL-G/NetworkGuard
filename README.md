# NetworkGuard

**Desarrollado por Enmanuel Gil**
Versión 1.0.0 | Android 8.0+ (API 26) | Sin root | Sin permisos especiales obligatorios

Monitor de tráfico de red en tiempo real por aplicación. Identifica qué apps consumen más datos, detecta subidas inusuales y te mantiene informado sobre tu red — sin root, sin suscripciones, código abierto.

---

## Características

### Monitor en Tiempo Real
- Velocímetro visual animado de descarga (↓) y subida (↑)
- Velocidad actual en bytes/s, KB/s o MB/s
- Tipo de red activa: Wi-Fi, 4G, 3G, Ethernet
- Total de datos consumidos en la sesión actual
- Conteo de apps con tráfico activo en tiempo real

### Análisis por Aplicación
- Ranking de todas las apps por total de datos (RX + TX)
- Velocidad instantánea por app actualizada cada 3 segundos
- Filtro: apps de usuario / apps de sistema
- Indicador visual de app activa con tráfico en ese momento
- Barra proporcional de consumo relativo

### Sistema de Alertas
- Detección automática de subidas masivas (> 500 KB/s)
- Apps de sistema con consumo inusualmente alto
- Historial de alertas recientes sin interrupciones

### Servicio de Fondo
- Notificación persistente con totales RX/TX actualizados
- Inicia automáticamente al reiniciar el dispositivo
- Consumo propio de batería: < 0.5%

### Diseño Único
- Tema oscuro diferenciado: púrpura eléctrico + cyan (estética ciberseguridad)
- Soporte tablet: NavigationRail lateral automático
- LazyColumn seguro en todos los screens

---

## Instalación

1. Descarga `NetworkGuard-v1.0.0.apk` desde [Releases](https://github.com/EnMaNueL-G/NetworkGuard/releases)
2. **Ajustes → Seguridad → Instalar apps de origen desconocido → Activar**
3. Instala y abre — el monitor inicia automáticamente

### Opcional: Acceso a estadísticas de uso

Para mejorar la correlación de tráfico por app:
```
Ajustes → Apps → Acceso especial → Acceso a datos de uso → NetworkGuard → Activar
```

---

## Cómo funciona

NetworkGuard usa `TrafficStats` — una API pública de Android que lee `/proc/net/xt_qtaguid` por UID de aplicación. Esta es la misma fuente que usa Android para el panel de "Uso de datos" del sistema.

- **Sin root** — API oficial de Android, sin privilegios especiales
- **Sin interceptar tráfico** — solo lee contadores del kernel por UID
- **Sin permisos peligrosos** — `ACCESS_NETWORK_STATE` y `ACCESS_WIFI_STATE` son los únicos obligatorios
- Los datos se actualizan comparando snapshots cada 3 segundos

---

## Compatibilidad

| Dispositivo | Android | Estado |
|-------------|---------|--------|
| vivo V2035 | Android 13 | ✅ Probado en dispositivo |
| Samsung Galaxy S21 | Android 15 | ✅ |
| Xiaomi MIUI 14 | Android 13–14 | ✅ |
| Tablet cualquier marca | Android 8+ | ✅ NavigationRail |

---

## Privacidad

NetworkGuard **no recopila, envía ni almacena** ningún dato sobre tu tráfico de red. Todo el análisis se realiza localmente en tu dispositivo. No hay servidores externos, no hay analytics, no hay seguimiento de ningún tipo.

---

## Permisos

| Permiso | Para qué |
|---------|----------|
| `ACCESS_NETWORK_STATE` | Detectar tipo de red (Wi-Fi / Datos) |
| `ACCESS_WIFI_STATE` | Obtener nombre de la red Wi-Fi |
| `FOREGROUND_SERVICE` | Monitor en background |
| `POST_NOTIFICATIONS` | Notificación con totales RX/TX |
| `RECEIVE_BOOT_COMPLETED` | Auto-inicio al encender |
| `PACKAGE_USAGE_STATS` *(opcional)* | Mejor correlación de apps |

---

## Apoya el Proyecto

NetworkGuard es **gratuito, sin anuncios y de código abierto**.

**Binance Pay ID:** `1140153333`
**BSC BEP20:** `0x0a9a0d8d816ede885d1d4a5c94369a72ef86b3c1`

---

*NetworkGuard v1.0.0 — Ve exactamente qué apps usan tu red y cuándo*
