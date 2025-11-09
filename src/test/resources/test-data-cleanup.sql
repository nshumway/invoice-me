-- Clean up test data between tests
DELETE FROM users WHERE email LIKE '%@example.com';
DELETE FROM customers WHERE email LIKE '%@example.com';
