<?php
/**
 * PG Locator - Image Upload Endpoint (Multipart/form-data)
 * POST /api/upload/upload_image.php
 * Field: image (File)
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Invalid request method. Only POST allowed.', null, 405);
}

if (!isset($_FILES['image']) || $_FILES['image']['error'] !== UPLOAD_ERR_OK) {
    $errorCode = $_FILES['image']['error'] ?? 'missing';
    sendResponse(false, 'No image file uploaded or upload error code: ' . $errorCode, null, 400);
}

$file = $_FILES['image'];
$allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/jpg'];
$maxSize = 10 * 1024 * 1024; // 10MB

// Validate size
if ($file['size'] > $maxSize) {
    sendResponse(false, 'File size exceeds maximum limit of 10MB.', null, 400);
}

// Validate MIME type
$finfo = finfo_open(FILEINFO_MIME_TYPE);
$mimeType = finfo_file($finfo, $file['tmp_name']);
finfo_close($finfo);

if (!in_array($mimeType, $allowedTypes, true)) {
    sendResponse(false, 'Invalid image format. Only JPEG, PNG, and WebP are allowed.', null, 400);
}

// Determine extension
$extension = match ($mimeType) {
    'image/jpeg', 'image/jpg' => 'jpg',
    'image/png'               => 'png',
    'image/webp'              => 'webp',
    default                   => 'jpg'
};

$uploadDir = __DIR__ . '/../../uploads/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

$fileName = 'pg_' . time() . '_' . bin2hex(random_bytes(6)) . '.' . $extension;
$targetPath = $uploadDir . $fileName;

if (!move_uploaded_file($file['tmp_name'], $targetPath)) {
    sendResponse(false, 'Failed to save uploaded file on server.', null, 500);
}

// Construct URL
$serverHost = $_SERVER['HTTP_HOST'] ?? '10.0.2.2';
$protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off') ? 'https://' : 'http://';
$imageUrl = $protocol . $serverHost . '/pg_locator/uploads/' . $fileName;

sendResponse(true, 'Image uploaded successfully!', [
    'image_url' => $imageUrl,
    'file_name' => $fileName
], 201);
