<?php
/**
 * PG Locator - User Enquiries
 * GET /api/user/my_enquiries.php?user_id=3
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    sendResponse(false, 'Invalid request method. Only GET allowed.', null, 405);
}

$userId = (int)($_GET['user_id'] ?? 0);
if ($userId <= 0) {
    sendResponse(false, 'Missing or invalid user_id parameter.', null, 400);
}

$pdo = Database::getConnection();

$stmt = $pdo->prepare('
    SELECT 
        e.id, e.pg_id, e.user_id, e.owner_id, e.message, 
        e.status, e.owner_reply, e.created_at, e.updated_at,
        p.title AS pg_title, p.address AS pg_address, p.monthly_rent, p.image_url AS pg_image_url,
        u.full_name AS owner_name, u.phone AS owner_phone, u.email AS owner_email
    FROM enquiries e
    JOIN pg_listings p ON e.pg_id = p.id
    JOIN users u ON e.owner_id = u.id
    WHERE e.user_id = :user_id
    ORDER BY e.created_at DESC
');
$stmt->execute([':user_id' => $userId]);
$enquiries = $stmt->fetchAll();

sendResponse(true, 'User enquiries retrieved successfully.', $enquiries);
