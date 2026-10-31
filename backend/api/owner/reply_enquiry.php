<?php
/**
 * PG Locator - Owner Reply to Enquiry
 * POST /api/owner/reply_enquiry.php
 * Payload: { "enquiry_id": 1, "owner_id": 2, "owner_reply": "..." }
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Invalid request method. Only POST allowed.', null, 405);
}

$input = getJsonPayload();
if (empty($input)) {
    $input = $_POST;
}

$enquiryId  = (int)($input['enquiry_id'] ?? 0);
$ownerId    = (int)($input['owner_id'] ?? 0);
$ownerReply = trim($input['owner_reply'] ?? '');

if ($enquiryId <= 0 || $ownerId <= 0 || empty($ownerReply)) {
    sendResponse(false, 'enquiry_id, owner_id, and owner_reply are required.', null, 400);
}

$pdo = Database::getConnection();

// Verify ownership
$check = $pdo->prepare('SELECT id FROM enquiries WHERE id = :id AND owner_id = :owner_id LIMIT 1');
$check->execute([':id' => $enquiryId, ':owner_id' => $ownerId]);
if (!$check->fetch()) {
    sendResponse(false, 'Enquiry not found or you are not authorized to reply to this enquiry.', null, 403);
}

$stmt = $pdo->prepare('
    UPDATE enquiries 
    SET owner_reply = :owner_reply, status = "replied", updated_at = NOW() 
    WHERE id = :id AND owner_id = :owner_id
');
$stmt->execute([
    ':owner_reply' => $ownerReply,
    ':id'          => $enquiryId,
    ':owner_id'    => $ownerId
]);

sendResponse(true, 'Reply sent to user successfully!', [
    'enquiry_id'  => $enquiryId,
    'status'      => 'replied',
    'owner_reply' => $ownerReply
]);
