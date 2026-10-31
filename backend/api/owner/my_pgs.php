<?php
/**
 * PG Locator - Fetch Owner's PG Listings
 * GET /api/owner/my_pgs.php?owner_id=2
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendResponse(false, 'Invalid request method. Only GET allowed.', null, 405);
}

$ownerId = (int)($_GET['owner_id'] ?? 0);
if ($ownerId <= 0) {
    sendResponse(false, 'Missing or invalid owner_id parameter.', null, 400);
}

$pdo = Database::getConnection();

$stmt = $pdo->prepare('
    SELECT 
        id, owner_id, title, description, address, 
        latitude, longitude, monthly_rent, amenities, 
        rules, image_url, is_occupied, status, created_at
    FROM pg_listings 
    WHERE owner_id = :owner_id 
    ORDER BY created_at DESC
');
$stmt->execute([':owner_id' => $ownerId]);
$listings = $stmt->fetchAll();

// Normalize amenities
foreach ($listings as &$listing) {
    $decoded = json_decode($listing['amenities'], true);
    $listing['amenities_list'] = is_array($decoded) ? $decoded : array_filter(array_map('trim', explode(',', $listing['amenities'])));
}

sendResponse(true, 'Owner listings retrieved successfully.', $listings);
