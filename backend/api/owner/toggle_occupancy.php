<?php
/**
 * FIND MY PG - Owner Toggle PG Occupancy (Available vs Occupied)
 * POST /api/owner/toggle_occupancy.php
 * Payload: {
 *   "pg_id": 1,
 *   "owner_id": 2,
 *   "is_occupied": 1 // 1 for occupied, 0 for available
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

$pgId       = (int)($input['pg_id'] ?? 0);
$ownerId    = (int)($input['owner_id'] ?? 0);
$isOccupied = isset($input['is_occupied']) ? (int)$input['is_occupied'] : null;

if ($pgId <= 0 || $ownerId <= 0 || $isOccupied === null) {
    sendResponse(false, 'Missing required fields: pg_id, owner_id, and is_occupied (0 or 1).', null, 400);
}

$isOccupied = ($isOccupied === 1) ? 1 : 0;

$pdo = Database::getConnection();

// Verify ownership
$check = $pdo->prepare('SELECT id, owner_id, title FROM pg_listings WHERE id = :pg_id LIMIT 1');
$check->execute([':pg_id' => $pgId]);
$listing = $check->fetch();

if (!$listing) {
    sendResponse(false, 'PG listing not found.', null, 404);
}

if ((int)$listing['owner_id'] !== $ownerId) {
    sendResponse(false, 'Unauthorized. You can only modify your own PG listings.', null, 403);
}

// Update occupancy
try {
    $stmt = $pdo->prepare('
        UPDATE pg_listings 
        SET is_occupied = :is_occupied 
        WHERE id = :pg_id AND owner_id = :owner_id
    ');
    $stmt->execute([
        ':is_occupied' => $isOccupied,
        ':pg_id'       => $pgId,
        ':owner_id'    => $ownerId
    ]);

    $statusLabel = ($isOccupied === 1) ? 'Marked as Occupied (hidden from available listings)' : 'Marked as Available / Vacant';

    sendResponse(true, $statusLabel, [
        'pg_id'       => $pgId,
        'is_occupied' => $isOccupied
    ]);
} catch (PDOException $e) {
    sendResponse(false, 'Database error: ' . $e->getMessage(), null, 500);
}
