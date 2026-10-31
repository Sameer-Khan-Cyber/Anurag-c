<?php
/**
 * PG Locator - Database Connection Handler (PDO Singleton)
 */

header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-Requested-With');

// Handle pre-flight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

class Database {
    private static ?PDO $instance = null;

    private static string $host = '127.0.0.1';
    private static string $db_name = 'pg_locator';
    private static string $username = 'root';
    private static string $password = '';
    private static string $charset = 'utf8mb4';

    public static function getConnection(): PDO {
        if (self::$instance === null) {
            $dsn = "mysql:host=" . self::$host . ";dbname=" . self::$db_name . ";charset=" . self::$charset;
            $options = [
                PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES   => false,
            ];

            try {
                self::$instance = new PDO($dsn, self::$username, self::$password, $options);
            } catch (PDOException $e) {
                http_response_code(500);
                echo json_encode([
                    'status'  => false,
                    'message' => 'Database connection failed: ' . $e->getMessage(),
                    'data'    => null
                ]);
                exit();
            }
        }
        return self::$instance;
    }
}

// Helper utility to send JSON responses consistently
function sendResponse(bool $status, string $message, mixed $data = null, int $httpCode = 200): void {
    http_response_code($httpCode);
    echo json_encode([
        'status'  => $status,
        'message' => $message,
        'data'    => $data
    ], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit();
}

// Helper utility to parse incoming JSON payload
function getJsonPayload(): array {
    $raw = file_get_contents('php://input');
    if (empty($raw)) {
        return [];
    }
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : [];
}
