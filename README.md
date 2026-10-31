# FIND MY PG

FIND MY PG is an Android app for finding and listing PG (Paying Guest) accommodation. It has an Android client built with Kotlin and Jetpack Compose, and a PHP 8 / MySQL REST backend.

**Author:** Sameer Khan

📄 **Project report:** [docs/FindMyPG_Project_Report_SameerKhan.docx](docs/FindMyPG_Project_Report_SameerKhan.docx)

---

## Roles

The app has two roles. You choose one when you register, and you can switch between them in **Settings**.

| Role | What they can do |
|------|------------------|
| **Tenant** (`user`) | Search and filter PG listings, view details and a map location, send enquiries to owners, and read owner replies under **My Enquiries**. |
| **Owner** (`owner`) | Publish PG listings, manage their own listings, mark a PG as Occupied or Vacant, and reply to tenant enquiries. |

## Features

**Authentication**
- Register as a Tenant or an Owner, and log in with email and password. The backend hashes passwords with bcrypt.
- The session is stored on the device.

**Tenant**
- Search PG listings by location.
- Filter by maximum monthly rent and required amenities, and hide occupied PGs.
- See the PG detail view: rent, description, amenities, house rules, and the location on an interactive map.
- Send an enquiry to the owner (with quick-question suggestions).
- **My Enquiries**: track your enquiries and read owner responses.

**Owner**
- **My PG Accommodations**: view your listings.
- Toggle a listing between *Available / Vacant* and *Occupied / Full*. Tenants can choose to hide occupied PGs from their search results.
- **Publish PG Accommodation**: title, monthly rent, address, description, house rules, amenities and an image (uploaded as multipart).
- Set the PG location with device GPS, or drop a pin on the map.
- **Tenant Inquiries**: view tenant enquiries and reply to them.

**Settings**
- Switch between the Tenant and Owner workspaces.
- Change the **PHP Backend Base URL** while the app is running.

**Offline fallback**
- If the PHP server can't be reached, the app falls back to local data stored on the device (`LocalDataManager`), so you can still log in, browse and test it without a backend.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Android app | Kotlin, Jetpack Compose (Material 3), ViewModel + StateFlow |
| Networking | Retrofit, OkHttp, Moshi |
| Maps | Leaflet with OpenStreetMap tiles (in a WebView) |
| Location | Google Play Services Location |
| Images | Coil |
| Backend | PHP 8 (PDO), REST endpoints returning JSON |
| Database | MySQL (InnoDB, utf8mb4) |

## Project Structure

```
.
├── app/                                   # Android application module
│   └── src/main/java/com/example/
│       ├── MainActivity.kt                # App entry, navigation, top bar
│       ├── ui/theme/                      # Compose theme (colors, typography)
│       └── pglocator/
│           ├── data/
│           │   ├── local/LocalDataManager.kt   # Offline/local fallback data
│           │   ├── model/Models.kt              # API and domain models
│           │   ├── network/ApiClient.kt         # Retrofit + Moshi client, Base URL
│           │   ├── network/ApiService.kt        # REST endpoint definitions
│           │   └── repository/PGRepository.kt  # Remote-first, local fallback
│           ├── ui/components/InteractiveMapView.kt  # Leaflet map
│           ├── ui/dialogs/SettingsDialog.kt         # Role switch + Base URL
│           ├── ui/screens/                          # Auth, Tenant, Owner screens
│           ├── util/                                # Session, location, upload helpers
│           └── viewmodel/                           # Auth, User, Owner ViewModels
├── backend/                               # PHP + MySQL REST API
│   ├── README.md                          # Backend setup guide
│   ├── schema.sql                         # Database schema + sample data
│   ├── config/db.php                      # PDO connection + JSON helpers
│   └── api/
│       ├── auth/      (login, register)
│       ├── owner/     (post_pg, my_pgs, enquiries, reply_enquiry, toggle_occupancy)
│       ├── user/      (search_pg, send_enquiry, my_enquiries)
│       └── upload/    (upload_image)
├── docs/
│   └── FindMyPG_Project_Report_SameerKhan.docx
└── gradle/libs.versions.toml              # Dependency versions
```

## Backend Setup

The full guide is in [backend/README.md](backend/README.md). In short:

1. Import the database:
   ```bash
   mysql -u root -p < backend/schema.sql
   ```
   This creates the `pg_locator` database with the `users`, `pg_listings` and `enquiries` tables, plus sample data.
2. Copy the `backend` folder into XAMPP's `htdocs` as `pg_locator` (for example `C:\xampp\htdocs\pg_locator`), and start Apache and MySQL.
3. Check that it works: open `http://localhost/pg_locator/api/user/search_pg.php` in a browser.
4. If your MySQL user or password isn't the XAMPP default (`root` with no password), edit `backend/config/db.php`.

You can also use PHP's built-in server instead of XAMPP:
```bash
cd backend
php -S 0.0.0.0:8000
```
Then set the app's Base URL to `http://10.0.2.2:8000/api/`.

**Sample accounts** (from `schema.sql`, demo use only): `owner@pglocator.com` and `user@pglocator.com`, password `Password@123`.

## Running the Android App

**Prerequisites:** [Android Studio](https://developer.android.com/studio) and an Android emulator or device (minSdk 24).

1. Open this folder in Android Studio and let Gradle sync.
2. The debug build is signed with `debug.keystore` from the project root. That file is git-ignored, so copy your local Android debug keystore there:
   - Windows: `%USERPROFILE%\.android\debug.keystore`
   - macOS / Linux: `~/.android/debug.keystore`
3. Optional: copy `.env.example` to `.env` if you need to override values. `.env` is git-ignored.
4. Run the `app` configuration on an emulator or device.

### Base URL

The default Base URL (`ApiClient.DEFAULT_BASE_URL`) is:

```
http://10.0.2.2/pg_locator/api/
```

`10.0.2.2` is how the Android emulator reaches the host machine's `localhost`. On a physical device, open **Settings → PHP Backend Base URL** and enter your computer's LAN IP, for example `http://192.168.1.10/pg_locator/api/`.

> **Note:** If the server can't be reached, the app falls back to local data on the device, so the UI still works without a backend.

## API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `auth/register.php` | Register an owner or tenant |
| POST | `auth/login.php` | Log in |
| POST | `upload/upload_image.php` | Multipart image upload |
| POST | `owner/post_pg.php` | Publish a PG listing |
| GET | `owner/my_pgs.php?owner_id=` | An owner's listings |
| GET | `owner/enquiries.php?owner_id=` | Enquiries received by an owner |
| POST | `owner/reply_enquiry.php` | Reply to an enquiry |
| POST | `owner/toggle_occupancy.php` | Mark a PG as occupied or vacant |
| GET | `user/search_pg.php` | Search PGs (`location`, `min_price`, `max_price`, `amenities`, `include_occupied`) |
| POST | `user/send_enquiry.php` | Send an enquiry |
| GET | `user/my_enquiries.php?user_id=` | A tenant's enquiries and replies |

## Author

**Sameer Khan**
