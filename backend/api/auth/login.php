<?php
/**
 * PG Locator - User Login Endpoint
 * POST /api/auth/login.php
 * Payload: { "email": "...", "password": "..." }
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Invalid request method. Only POST allowed.', null, 405);
}

$input = getJsonPayload();
if (empty($input)) {
    $input = $_POST;
}

$email    = trim($input['email'] ?? '');
$password = $input['password'] ?? '';

if (empty($email) || empty($password)) {
    sendResponse(false, 'Email and password are required.', null, 400);
}

$pdo = Database::getConnection();

$stmt = $pdo->prepare('SELECT id, full_name, email, phone, password_hash, role, created_at FROM users WHERE email = :email LIMIT 1');
$stmt->execute([':email' => $email]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, $user['password_hash'])) {
    sendResponse(false, 'Invalid email or password.', null, 401);
}

// Generate session token
$token = bin2hex(random_bytes(32));

$userData = [
    'id'         => (int)$user['id'],
    'full_name'  => $user['full_name'],
    'email'      => $user['email'],
    'phone'      => $user['phone'],
    'role'       => $user['role'],
    'created_at' => $user['created_at'],
    'token'      => $token
];

sendResponse(true, 'Login successful!', $userData, 200);
