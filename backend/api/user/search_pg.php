<?php
/**
 * PG Locator - User Search PG Listings
 * GET /api/user/search_pg.php?location=...&min_price=...&max_price=...&amenities=WiFi,AC
 * Only returns listings where status = 'approved'
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendResponse(false, 'Invalid request method. Only GET allowed.', null, 405);
}

$location     = trim($_GET['location'] ?? '');
$minPrice     = isset($_GET['min_price']) && is_numeric($_GET['min_price']) ? (float)$_GET['min_price'] : null;
$maxPrice     = isset($_GET['max_price']) && is_numeric($_GET['max_price']) ? (float)$_GET['max_price'] : null;
$amenities    = trim($_GET['amenities'] ?? '');
// If a PG is occupied, it is removed for users by default until it gets empty again
$includeOccupied = isset($_GET['include_occupied']) && (
    $_GET['include_occupied'] === '1' || 
    $_GET['include_occupied'] === 'true'
);

$pdo = Database::getConnection();

$sql = '
    SELECT 
        p.id, p.owner_id, p.title, p.description, p.address, 
        p.latitude, p.longitude, p.monthly_rent, p.amenities, 
        p.rules, p.image_url, p.is_occupied, p.status, p.created_at,
        u.full_name AS owner_name, u.phone AS owner_phone, u.email AS owner_email
    FROM pg_listings p
    JOIN users u ON p.owner_id = u.id
    WHERE p.status = "approved"
';

$params = [];

if (!$includeOccupied) {
    $sql .= ' AND p.is_occupied = 0';
}

if (!empty($location)) {
    $sql .= ' AND (p.title LIKE :loc_title OR p.address LIKE :loc_address OR p.description LIKE :loc_desc)';
    $params[':loc_title']   = '%' . $location . '%';
    $params[':loc_address'] = '%' . $location . '%';
    $params[':loc_desc']    = '%' . $location . '%';
}

if ($minPrice !== null && $minPrice >= 0) {
    $sql .= ' AND p.monthly_rent >= :min_price';
    $params[':min_price'] = $minPrice;
}

if ($maxPrice !== null && $maxPrice > 0) {
    $sql .= ' AND p.monthly_rent <= :max_price';
    $params[':max_price'] = $maxPrice;
}

if (!empty($amenities)) {
    $amenityList = array_filter(array_map('trim', explode(',', $amenities)));
    foreach ($amenityList as $idx => $amenity) {
        $paramKey = ':amenity_' . $idx;
        $sql .= " AND p.amenities LIKE $paramKey";
        $params[$paramKey] = '%' . $amenity . '%';
    }
}

$sql .= ' ORDER BY p.created_at DESC';

$stmt = $pdo->prepare($sql);
$stmt->execute($params);
$results = $stmt->fetchAll();

foreach ($results as &$item) {
    $decoded = json_decode($item['amenities'], true);
    $item['amenities_list'] = is_array($decoded) ? $decoded : array_filter(array_map('trim', explode(',', $item['amenities'])));
}

sendResponse(true, 'Approved PG listings fetched successfully.', $results);
