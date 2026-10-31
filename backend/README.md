# PG Locator - Backend Setup & Testing Guide

This backend powers the PG Locator Android app with MySQL & PHP 8.x PDO REST APIs.

## 1. Database Setup
1. Open phpMyAdmin or MySQL CLI.
2. Run the SQL script from `backend/schema.sql`:
   ```bash
   mysql -u root -p < backend/schema.sql
   ```
   This creates the `pg_locator` database with `users`, `pg_listings`, and `enquiries` tables, including sample seed data.

## 2. XAMPP Deployment
1. Copy or link the `backend` folder to your XAMPP `htdocs` directory under the name `pg_locator`:
   - Windows: `C:\xampp\htdocs\pg_locator`
   - macOS / Linux: `/opt/lampp/htdocs/pg_locator`
2. Ensure MySQL and Apache services are running.
3. Verify by visiting `http://localhost/pg_locator/api/user/search_pg.php` in your browser.
4. From the standard Android Emulator, the host machine is accessed via `http://10.0.2.2/pg_locator/api/`.

## 3. Alternative Quick Run via PHP Built-in Server
You can also run directly from the terminal:
```bash
cd backend
php -S 0.0.0.0:8000
```
Then point the Android app Base URL to `http://10.0.2.2:8000/api/` (configurable in the app's Profile / Settings screen).

## 4. Endpoints Summary
- `POST /api/auth/register.php`: Register new owner/user
- `POST /api/auth/login.php`: Login and obtain session profile
- `POST /api/upload/upload_image.php`: Multipart image upload (returns `{ "image_url": "..." }`)
- `POST /api/owner/post_pg.php`: Post new PG (status = 'approved' directly)
- `GET  /api/owner/my_pgs.php?owner_id={id}`: Get owner's listings
- `GET  /api/owner/enquiries.php?owner_id={id}`: Get owner's enquiries
- `POST /api/owner/reply_enquiry.php`: Owner reply to enquiry
- `GET  /api/user/search_pg.php`: Search approved PGs (filters: location, min_price, max_price, amenities)
- `POST /api/user/send_enquiry.php`: Submit enquiry for PG
- `GET  /api/user/my_enquiries.php?user_id={id}`: Get user's enquiries and replies
