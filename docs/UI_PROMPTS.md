# 🎨 Stitch UI Prompts — All 8 Screens
## Vrikshaayush App

---

## PROJECT INTRO
*(Paste this FIRST in Stitch before any screen prompt)*

> "I am building a mobile app called **Vrikshaayush** (meaning 'Life of the Plant') — an AI-powered plant health diagnostic tool for farmers in rural India. The app works offline using on-device AI (TensorFlow Lite). Farmers can take a photo of their plant and instantly get a disease diagnosis, treatment advice, and audit history. The app should feel trustworthy, natural, and simple enough for low-literacy rural farmers. No complex menus. Big buttons. Clear icons. Support for Hindi, Kannada, and English."

---

## COLOUR PALETTE
*(Reference in every screen)*

| Role | Color | Hex |
|------|-------|-----|
| Primary | Deep Forest Green | `#2E7D32` |
| Accent | Warm Amber/Gold | `#F9A825` |
| Background | Off-White/Cream | `#F5F5F0` |
| Text | Near Black | `#1C1C1C` |
| Danger/High Severity | Deep Red | `#C62828` |
| Card Background | Light Green | `#E8F5E9` |

---

## PAGE 1 — Splash / Welcome Screen

> "Design a splash screen for **Vrikshaayush** app. Show a large circular deep green `#2E7D32` icon with a plant-in-pot symbol at the top center. Below it the app name 'Vrikshaayush' in large bold deep green text. Below the name, tagline 'Apne Paudhe Ki Sehat Jaanein' in smaller muted text. A beautiful full-width photo of a sunlit crop field in the middle of the screen. At the bottom, a large full-width 'Get Started →' button in deep green `#2E7D32` with white text. Below the button, small text 'Available in Hindi | Kannada | English' and a 'Change Language' pill button. Top right corner shows a small badge 'Works Offline' with a sync icon. Background `#F5F5F0`. Mobile screen, clean and trustworthy."

---

## PAGE 2 — Home Screen

> "Design the Home Screen for **Vrikshaayush** app. Top bar: app leaf logo + 'Vrikshaayush' name on left, language toggle button 'EN 文A' on right. Below top bar, a pill-shaped status indicator 'Device synced with cloud' with a sync icon. Center: large circular deep green `#2E7D32` button with a camera+lightning bolt icon inside, glowing green ring around it. Below the circle, a full-width rounded 'Scan Plant' button in dark green. Below that, instructional text 'Point your camera at the crop to detect diseases instantly.' Three stat cards in a row: 'Total Scans 124', 'Diseases 8' (with amber warning triangle `#F9A825`), 'Crops 5'. Below stats, a 'Last Audit' card showing 'Wheat Field • 2 hrs ago' with a history icon and right arrow. A wide landscape crop field photo at the bottom with overlay text 'Monitoring: Sector 4 - North Ridge'. Bottom nav bar: Home (active, green highlight), Scan, History, Settings icons. Background `#F5F5F0`."

---

## PAGE 3 — Camera / Upload Screen

> "Design the Camera & Upload Screen for **Vrikshaayush** app. Top bar: back arrow on left, title 'Scan Your Plant' in bold green `#2E7D32`. Below top bar a status pill 'Ready for offline use' with cloud icon. Center: large square camera viewfinder with green corner brackets `#2E7D32` at each corner — no dashed border, just subtle corner marks. Inside viewfinder shows a large detailed green leaf photo filling the frame. Below viewfinder: two full-width stacked buttons — 'Take Photo' with camera icon (deep green `#2E7D32` background, white text), and 'Gallery' with image icon (warm amber `#F9A825` background, white text). Below buttons: a light grey info card with lightbulb icon and text 'Tip: Make sure the leaf is well-lit and fills the frame.' Bottom nav bar: Diagnose, History, Library, Profile icons. Background `#F5F5F0`."

---

## PAGE 4 — Diagnosis Result Screen

> "Design the Diagnosis Result Screen for **Vrikshaayush** app. Top: back arrow + 'Diagnosis Result' title in green `#2E7D32`. Small badge 'Saved on device' with checkbox icon. Below: a large rounded photo card showing the scanned plant leaf image, with small text below it 'Captured Field Image  Oct 24, 2023'. Result card below photo: white card with green border, showing 'Early Blight' in large bold text on left, a red HIGH severity badge `#C62828` with warning icon on right. Below disease name: tomato icon + 'Tomato' in medium text. 'Confidence Score' label with '92%' right-aligned, and a green filled progress bar below it. Below result card: 'Treatment Recommendations' section header with scissors icon. Three recommendation cards each with a circular green icon on left and treatment text: 'Remove infected leaves and burn them.', 'Apply organic fungicide spray every 7 days.', 'Avoid overhead watering to keep leaves dry.' Two full-width buttons at bottom: 'See Full Details' (green), 'Save to History' (outlined amber). Bottom nav: Home, Crops, Scanner (active), History. Background `#F5F5F0`."

---

## PAGE 5 — Disease Detail Screen

> "Design the Disease Detail Screen for **Vrikshaayush** app. Top: back arrow + disease name 'Early Blight' as title, three-dot menu on right. Full-width hero image of the disease-affected leaf at top, with an amber overlay banner at bottom of image '⚠ Common in Tomato & Potato'. Scrollable content below in light green `#E8F5E9` section cards: 1) '? What is it?' card — short paragraph describing the disease in simple language. 2) '👁 Symptoms' card — three white sub-cards each listing a symptom with a colored dot icon. 3) '📁 Causes' card — 2x2 grid of small icon tiles: High Humidity, Warm Temps, Infected Soil, Heavy Rain. 4) '✓ Prevention Tips' card — 4 checkmark list items. 5) Treatment section with two tabs: 'Organic' (active, green filled) and 'Chemical' (outlined) — tab switcher at top. Below tabs: numbered steps 1, 2, 3 each with bold title and description. A full-width 'Generate Full Report' button at bottom in green. Floating amber bookmark FAB on right. Bottom nav: Home, Crops, Health (active), Weather, Profile. Background `#F5F5F0`."

---

## PAGE 6 — Scan History / Audit Log Screen

> "Design the Scan History Screen for **Vrikshaayush** app. Top: back arrow + 'Scan History' title in bold green, filter icon on right. Search bar below: 'Search by crop name' placeholder with search icon, rounded grey background. Status pill below search: '☑ All scans synced and saved on device'. List of scan history cards below, each card is a white rounded rectangle with: small plant photo thumbnail (square, rounded corners) on left, disease name in bold (e.g. 'Late Blight'), crop name below in muted text (e.g. 'Potato'), date and time on right (e.g. 'Oct 24, 10:30 AM'), severity label + colored dot on bottom right (HIGH red, MEDIUM amber, LOW green). Show 4 cards: Late Blight/Potato/HIGH, Early Blight/Tomato/MEDIUM, Healthy Leaf/Maize/LOW, Cedar Apple Rust/Apple/MEDIUM. Floating dark green camera FAB button at bottom right. Bottom nav: Diagnose, History (active, green), Library, Profile. Background `#F5F5F0`, cards white with subtle shadow."

---

## PAGE 7 — Crop Library / Disease Database Screen

> "Design the Crop Library Screen for **Vrikshaayush** app. Top: cloud-leaf logo icon + 'Crop Library' title in green, user avatar circle on top right. Search bar: 'Search disease or crop' with magnifier icon. Horizontal scrollable filter chips below: 'All' (filled dark green, white text), 'Tomato' (outlined), 'Rice' (outlined), 'Wi...' (cut off, indicating more). 2-column grid of disease cards below: each card is a white rounded rectangle with a full-width plant disease photo at top (rounded corners), crop type tag in amber/orange `#F9A825` pill at bottom-left of image, colored severity dot at bottom-right, disease name in bold below image. Show 4 cards: Early Blight/Tomato (red dot), Blast/Rice (amber dot), Leaf Rust/Wheat (amber dot), Boll Rot/Cotton (red dot). Dark banner at very bottom: wifi-off icon + 'Available offline — no internet needed' in white text on dark grey. Bottom nav: Fields, Library (active, green highlight), Tasks, Sync. Background `#F5F5F0`."

---

## PAGE 8 — Settings Screen

> "Design the Settings Screen for **Vrikshaayush** app. Top: back arrow + 'Settings' title in bold green, sync icon on right. Status pill at top: '☑ All changes saved locally'. Section: 'Language selection' — subtitle label, then a white card with globe icon + 'Select Language' row, below it three equal square buttons for English (USA flag, green highlight/selected), हिंदी (India flag), ಕನ್ನಡ (India flag). Section: 'Notifications' — white card with two toggle rows: cloud icon + 'Weather Alerts' with green ON toggle, gear icon + 'Disease Outbreak Alerts' with grey OFF toggle. Section: 'About' — white card with three rows: info icon + 'App Version' + 'v2.4.0-stable' on right, document icon + 'About Vrikshaayush' + right arrow, question icon + 'How to Use' + external link icon. Section: 'Data' — two full-width buttons: green filled 'Export Reports as PDF' with PDF icon, red outlined 'Clear Scan History' with X icon. Wide landscape farm field photo at bottom with overlay text 'Sustainable Farming — Empowering rural agriculture through data.' Bottom nav: Fields, Crops, Sync, Settings (active, green highlight). Background `#F5F5F0`."

---

*All 8 screens use the same colour palette and design language for visual consistency.*
