-- 테스트용 ShortForm 데이터 생성
INSERT INTO short_forms (owner_pid, title, description, video_key, duration_sec, tags, status, thumbnail_key, visibility, created_at, updated_at) 
VALUES 
('testuser1', 'Test Video 1', 'Test Description 1', 'videos/testuser1/uuid1_testvideo1.mp4', 30, '["test","demo","video"]'::jsonb, 'READY', 'thumbnails/testuser1/uuid1_testvideo1_thumb.jpg', 'PUBLIC', NOW(), NOW()),
('testuser2', 'Test Video 2', 'Test Description 2', 'videos/testuser2/uuid2_testvideo2.mp4', 45, '["sample","test"]'::jsonb, 'READY', 'thumbnails/testuser2/uuid2_testvideo2_thumb.jpg', 'PUBLIC', NOW(), NOW()),
('testuser1', 'Test Video 3', 'Test Description 3', 'videos/testuser1/uuid3_testvideo3.mp4', 60, '["demo","showcase"]'::jsonb, 'READY', 'thumbnails/testuser1/uuid3_testvideo3_thumb.jpg', 'PUBLIC', NOW(), NOW());

-- 테스트용 User 데이터도 필요하면 생성 (PostgreSQL용)
INSERT INTO users (pid, display_name, login_id, password, role, profile_image_url, created_at, updated_at)
VALUES 
('testuser1', 'Test User 1', 'testuser1', 'password123', 'USER', 'https://via.placeholder.com/150', NOW(), NOW()),
('testuser2', 'Test User 2', 'testuser2', 'password123', 'USER', 'https://via.placeholder.com/150', NOW(), NOW())
ON CONFLICT (pid) DO NOTHING;