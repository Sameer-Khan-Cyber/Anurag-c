<?php
/**
 * PG Locator - Owner Post New PG Listing
 * POST /api/owner/post_pg.php
 * Payload: {
 *   "owner_id": 2,
 *   "title": "...",
 *   "description": "...",
 *   "address": "...",
 *   "latitude": 12.9345,
 *   "longitude": 77.6190,
 *   "monthly_rent": 12000,
 *   "amenities": ["WiFi", "AC", "Food"],
 *   "rules": "...",
 *   "image_url": "..."
 * }
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Invalid request method. Only POST allowed.', null, 405);
}

$input = getJsonPayload();
if (empty($input)) {
    $input = $_POST;
}

$ownerId     = (int)($input['owner_id'] ?? 0);
$title       = trim($input['title'] ?? '');
$description = trim($input['description'] ?? '');
$address     = trim($input['address'] ?? '');
$latitude    = filter_var($input['latitude'] ?? null, FILTER_VALIDATE_FLOAT);
$longitude   = filter_var($input['longitude'] ?? null, FILTER_VALIDATE_FLOAT);
$monthlyRent = filter_var($input['monthly_rent'] ?? null, FILTER_VALIDATE_FLOAT);
$rules       = trim($input['rules'] ?? '');
$imageUrl    = trim($input['image_url'] ?? '');

$amenitiesRaw = $input['amenities'] ?? [];
$amenitiesJson = is_array($amenitiesRaw) ? json_encode(array_values($amenitiesRaw)) : (string)$amenitiesRaw;

// Validation
if ($ownerId <= 0 || empty($title) || empty($address) || $latitude === false || $longitude === false || $monthlyRent === false || $monthlyRent <= 0) {
    sendResponse(false, 'Missing required fields: owner_id, title, address, valid latitude, longitude, and monthly_rent.', null, 400);
}

$pdo = Database::getConnection();

// Validate owner exists and has role 'owner'
$stmt = $pdo->prepare('SELECT id, role FROM users WHERE id = :id LIMIT 1');
$stmt->execute([':id' => $ownerId]);
$owner = $stmt->fetch();
if (!$owner || $owner['role'] !== 'owner') {
    sendResponse(false, 'Unauthorized. Valid owner account required to post listings.', null, 403);
}

try {
    $insert = $pdo->prepare('
        INSERT INTO pg_listings 
        (owner_id, title, description, address, latitude, longitude, monthly_rent, amenities, rules, image_url, status)
        VALUES 
        (:owner_id, :title, :description, :address, :latitude, :longitude, :monthly_rent, :amenities, :rules, :image_url, "approved")
    ');

    $insert->execute([
        ':owner_id'     => $ownerId,
        ':title'        => $title,
        ':description'  => $description,
        ':address'      => $address,
        ':latitude'     => $latitude,
        ':longitude'    => $longitude,
        ':monthly_rent' => $monthlyRent,
        ':amenities'    => $amenitiesJson,
        ':rules'        => $rules,
        ':image_url'    => $imageUrl
    ]);

    $newId = (int)$pdo->lastInsertId();

    sendResponse(true, 'PG Listing published successfully! It is now live.', [
        'id'           => $newId,
        'title'        => $title,
        'status'       => 'approved',
        'monthly_rent' => $monthlyRent,
        'image_url'    => $imageUrl
    ], 201);
} catch (PDOException $e) {
    sendResponse(false, 'Failed to create PG listing: ' . $e->getMessage(), null, 500);
}
