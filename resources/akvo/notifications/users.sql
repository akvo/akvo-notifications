
-- name: all-users
-- Returns all users in the database
SELECT * 
FROM users

-- name: get-user-by-id
-- Returns a user for a given id
SELECT *
FROM users
WHERE id = :id

-- name: new-user!
-- Inserts a new user in the database
INSERT INTO users
VALUES (:id, :name)

