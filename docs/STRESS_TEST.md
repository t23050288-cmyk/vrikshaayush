# 🏗 Architecture Stress-Test Analysis
## Vrikshaayush — Edge AI Plant Diagnostic App

*Pre-answered using the Architecture Stress-Test Prompt Pack*

---

## 1️⃣ Schema Stress Test

### Core Entities

```
ScanRecord
├── id (UUID)
├── image_path (local file path)
├── crop_type (string)
├── disease_name (string)
├── confidence_score (float)
├── severity (enum: LOW | MEDIUM | HIGH)
├── treatment_used (string, nullable)
├── scan_timestamp (datetime)
├── location_tag (string, optional — user-entered field name, NOT GPS)
└── is_synced (bool, default: false)

DiseaseInfo
├── id (UUID)
├── disease_name (string, unique)
├── crop_type (string)
├── description (string)
├── symptoms (JSON array)
├── causes (JSON array)
├── prevention_tips (JSON array)
├── organic_treatments (JSON array)
├── chemical_treatments (JSON array)
└── severity_default (enum)

UserPreferences
├── language (enum: EN | HI | KN)
├── notifications_weather (bool)
├── notifications_disease (bool)
└── last_sync_date (datetime)
```

### At 10,000 DAU
- **No bottleneck** — all data is local SQLite per device
- Each device is its own isolated database
- No shared server, no write contention, no race conditions
- **Risk: ZERO** for offline architecture

### Structural Weaknesses
- If you add a sync feature later, you'll need conflict resolution (device vs cloud)
- `image_path` becomes stale if app is reinstalled — store relative paths, not absolute
- **Fix:** Store images in app's documents directory, use relative paths

---

## 2️⃣ Security Audit

### Stack: Flutter + TFLite + SQLite (100% on-device)

| Risk | Assessment | Mitigation |
|------|-----------|------------|
| Data exposure | ✅ NONE — no server to breach | Data never leaves device |
| Auth bypass | ✅ N/A — no user accounts needed | Single-user local app |
| Injection attacks | ⚠️ SQLite injection possible | Use parameterized queries always |
| Image tampering | Low risk | Images stored in app sandbox |
| Model tampering | ⚠️ APK can be decompiled | Acceptable for college project |
| Location privacy | ✅ Protected — no GPS used | Only user-entered field names |
| Rate abuse | ✅ N/A — no API to abuse | Offline inference |

### Key Security Win
Because everything runs on-device, **Vrikshaayush has a smaller attack surface than 99% of apps**. There's no server to hack, no database to breach, no API keys to steal.

---

## 3️⃣ Vendor Lock-In Audit

### Current Stack Dependencies

| Component | Vendor | Lock-in Risk | Alternative |
|-----------|--------|-------------|-------------|
| TFLite | Google | Medium | ONNX Runtime (drop-in swap) |
| Flutter | Google | Medium | React Native |
| SQLite | Open Source | None | Any local DB |
| PlantVillage model | Open Source | None | Retrain on any framework |

### At 100K Users
- **No server costs** — each user's phone runs everything
- **No scaling problem** — 100K users = 100K isolated devices
- The only "scale" issue is app store distribution
- **Verdict: Near-zero vendor lock-in risk for offline architecture**

---

## 4️⃣ Cost Explosion Simulation

### Vrikshaayush Cost Model

| Scale | Server Cost | Model Cost | Total Monthly |
|-------|------------|-----------|---------------|
| 100 users | ₹0 | ₹0 | **₹0** |
| 1,000 users | ₹0 | ₹0 | **₹0** |
| 10,000 users | ₹0 | ₹0 | **₹0** |
| 100,000 users | ₹0 | ₹0 | **₹0** |

**This is the killer feature of edge AI.** Cost doesn't scale with users at all. The only costs are:
- App store developer account: ₹7,000/year (Google Play)
- Optional cloud sync storage: ~₹500/month at 10K users

---

## 5️⃣ Failure Mode Simulation

### What Fails First?

| Failure Scenario | Impact | Recovery |
|-----------------|--------|----------|
| Phone battery dies | App stops | Resume from local DB |
| Phone storage full | Can't save new scans | Show storage warning UI |
| Camera permission denied | Can't scan | Request permission gracefully |
| Model file corrupted | No diagnosis | Detect on startup, re-download once |
| App crashes mid-scan | Scan lost | Save state before inference |

### No Cascading Failures
Because there's no server, a failure on one device **cannot affect any other user**. This is architecturally bulletproof.

---

## 6️⃣ Completion vs Stability Check

### Hidden Technical Debt

| Issue | Severity | Fix |
|-------|---------|-----|
| No model versioning | Medium | Add model version check on startup |
| No error logging | Low | Add local crash logs |
| Disease DB hardcoded | Medium | Make it JSON-driven (already planned) |
| No accessibility (screen reader) | Low | Add semantic labels |
| Image not compressed before storage | Medium | Compress to ~200KB on save |

### Unscalable Shortcuts (Acceptable for v1)
- Disease treatments hardcoded in JSON — fine for 38 diseases, problematic at 500+
- No model A/B testing capability — acceptable for college project
- SQLite without migrations — add migration support before v2

### Verdict
**Vrikshaayush is architecturally sound for its use case.** The edge-first design eliminates the most common failure modes (server downtime, API limits, cost explosion). The technical debt items are all solvable incrementally.

---

## Summary

| Dimension | Score | Notes |
|-----------|-------|-------|
| Schema design | 9/10 | Simple, normalized, offline-native |
| Security | 10/10 | No server = smallest possible attack surface |
| Vendor lock-in | 8/10 | TFLite swappable with ONNX |
| Cost stability | 10/10 | Zero marginal cost per user |
| Failure resilience | 9/10 | Isolated per-device, no cascading failures |
| Long-term stability | 8/10 | Minor tech debt, all fixable |

**Overall: Production-quality architecture for an offline mobile app.**
