<?php
/**
 * PG Locator - User Registration Endpoint
 * POST /api/auth/register.php
 * Payload: { "full_name": "...", "email": "...", "phone": "...", "password": "...", "role": "owner" | "user" }
 */

require_once __DIR__ . '/../../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    sendResponse(false, 'Invalid request method. Only POST allowed.', null, 405);
}

$input = getJsonPayload();
if (empty($input)) {
    $input = $_POST;
}

$fullName = trim($input['full_name'] ?? '');
$email    = trim($input['email'] ?? '');
$phone    = trim($input['phone'] ?? '');
$password = $input['password'] ?? '';
$role     = trim($input['role'] ?? 'user');

// Validation
if (empty($fullName) || empty($email) || empty($phone) || empty($password)) {
    sendResponse(false, 'All fields (full_name, email, phone, password) are required.', null, 400);
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    sendResponse(false, 'Invalid email address format.', null, 400);
}

if (strlen($password) < 6) {
    sendResponse(false, 'Password must be at least 6 characters.', null, 400);
}

$validRoles = ['user', 'owner'];
if (!in_array($role, $validRoles, true)) {
    $role = 'user';
}

$pdo = Database::getConnection();

// Check if email already exists
$stmt = $pdo->prepare('SELECT id FROM users WHERE email = :email LIMIT 1');
$stmt->execute([':email' => $email]);
if ($stmt->fetch()) {
    sendResponse(false, 'An account with this email already exists.', null, 409);
}

// Hash password securely with bcrypt
$passwordHash = password_hash($password, PASSWORD_BCRYPT);

try {
    $insert = $pdo->prepare('
        INSERT INTO users (full_name, email, phone, password_hash, role)
        VALUES (:full_name, :email, :phone, :password_hash, :role)
    ');
    $insert->execute([
        ':full_name'     => $fullName,
        ':email'         => $email,
        ':phone'         => $phone,
        ':password_hash' => $passwordHash,
        ':role'          => $role
    ]);

    $userId = (int)$pdo->lastInsertId();

    $userData = [
        'id'        => $userId,
        'full_name' => $fullName,
        'email'     => $email,
        'phone'     => $phone,
        'role'      => $role,
        'token'     => bin2hex(random_bytes(32))
    ];

    sendResponse(true, 'Registration successful!', $userData, 201);
} catch (PDOException $e) {
    sendResponse(false, 'Registration failed: ' . $e->getMessage(), null, 500);
}
