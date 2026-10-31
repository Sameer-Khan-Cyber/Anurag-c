<?php
/**
 * PG Locator - User Send Enquiry
 * POST /api/user/send_enquiry.php
 * Payload: { "pg_id": 1, "user_id": 3, "message": "..." }
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Invalid request method. Only POST allowed.', null, 405);
}

$input = getJsonPayload();
if (empty($input)) {
    $input = $_POST;
}

$pgId    = (int)($input['pg_id'] ?? 0);
$userId  = (int)($input['user_id'] ?? 0);
$message = trim($input['message'] ?? '');

if ($pgId <= 0 || $userId <= 0 || empty($message)) {
    sendResponse(false, 'pg_id, user_id, and message are required.', null, 400);
}

$pdo = Database::getConnection();

// Lookup PG listing and find owner_id
$pgStmt = $pdo->prepare('SELECT id, owner_id, title FROM pg_listings WHERE id = :id LIMIT 1');
$pgStmt->execute([':id' => $pgId]);
$pg = $pgStmt->fetch();

if (!$pg) {
    sendResponse(false, 'PG Listing not found.', null, 404);
}

$ownerId = (int)$pg['owner_id'];

// Save enquiry
try {
    $stmt = $pdo->prepare('
        INSERT INTO enquiries (pg_id, user_id, owner_id, message, status)
        VALUES (:pg_id, :user_id, :owner_id, :message, "pending")
    ');
    $stmt->execute([
        ':pg_id'    => $pgId,
        ':user_id'  => $userId,
        ':owner_id' => $ownerId,
        ':message'  => $message
    ]);

    $newId = (int)$pdo->lastInsertId();

    sendResponse(true, 'Enquiry submitted to property owner successfully!', [
        'enquiry_id' => $newId,
        'pg_id'      => $pgId,
        'status'     => 'pending',
        'created_at' => date('Y-m-d H:i:s')
    ], 201);
} catch (PDOException $e) {
    sendResponse(false, 'Failed to submit enquiry: ' . $e->getMessage(), null, 500);
}
