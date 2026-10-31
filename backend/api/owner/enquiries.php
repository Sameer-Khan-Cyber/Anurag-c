<?php
/**
 * PG Locator - Owner Enquiries Management
 * GET /api/owner/enquiries.php?owner_id=2
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
        e.id, e.pg_id, e.user_id, e.owner_id, e.message, 
        e.status, e.owner_reply, e.created_at, e.updated_at,
        p.title AS pg_title, p.address AS pg_address, p.monthly_rent, p.image_url AS pg_image_url,
        u.full_name AS user_name, u.email AS user_email, u.phone AS user_phone
    FROM enquiries e
    JOIN pg_listings p ON e.pg_id = p.id
    JOIN users u ON e.user_id = u.id
    WHERE e.owner_id = :owner_id
    ORDER BY e.created_at DESC
');
$stmt->execute([':owner_id' => $ownerId]);
$enquiries = $stmt->fetchAll();

sendResponse(true, 'Enquiries retrieved successfully.', $enquiries);
