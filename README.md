# CloudStream Extensions: Animaura & Stardima

Official extension repository for [CloudStream 3](https://github.com/recloudstream/cloudstream), providing streaming providers for:
1. **Animaura** (`https://www.animaura.xyz/anime/miraculous` and general anime)
2. **Stardima** (`https://cm.cartoon.com.im` - Arabic Cartoons, Disney & Anime dubs)

---

## 🚀 How to Install in CloudStream 3

### Option 1: Automatic URL Import
1. Copy the repository manifest URL:
   ```text
   https://YOUR_DOMAIN_OR_GITHUB_RAW/repo.json
   ```
2. Open **CloudStream 3** on your Android device or Android TV.
3. Go to **Settings** (⚙️) ➔ **Extensions** ➔ **Add Repository**.
4. Enter a repository name (e.g. `Animaura & Stardima`) and paste the URL.
5. Tap **Download** / **Save**.
6. The `Animaura` and `Stardima` extensions will appear in your plugin list ready to install!

### Option 2: QR Code Scan
1. Open CloudStream ➔ Settings ➔ Extensions.
2. Tap the **QR Code Scanner** icon in the top right.
3. Scan the QR code shown in this dashboard.

---

## 📦 Providers Included

| Provider | Target URL | Content Type | Language |
| :--- | :--- | :--- | :--- |
| **Animaura** | `https://www.animaura.xyz` | Anime, Miraculous Ladybug, Movies | Multi / English Subs & Dubs |
| **Stardima** | `https://cm.cartoon.com.im` | Cartoons, Miraculous Arabic Dub, Disney | Arabic (`ar`) |

---

## 🛠️ Building From Source

```bash
# Clone the repository
git clone https://github.com/username/cloudstream-animaura-stardima.git
cd cloudstream-animaura-stardima

# Build the plugins into .cs3 files
./gradlew makePlugins
```

The compiled `.cs3` files and `plugins.json` will be placed in the `build/` directory.
