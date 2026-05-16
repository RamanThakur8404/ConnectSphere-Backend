CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS post_db;
CREATE DATABASE IF NOT EXISTS comment_db;
CREATE DATABASE IF NOT EXISTS like_db;
CREATE DATABASE IF NOT EXISTS follow_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS search_db;
CREATE DATABASE IF NOT EXISTS media_db;
CREATE DATABASE IF NOT EXISTS report_db;
CREATE DATABASE IF NOT EXISTS message_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS admin_db;

CREATE USER IF NOT EXISTS 'connectsphere'@'%' IDENTIFIED BY 'connectsphere@90';

GRANT ALL PRIVILEGES ON auth_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON post_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON comment_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON like_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON follow_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON search_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON media_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON report_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON message_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'connectsphere'@'%';
GRANT ALL PRIVILEGES ON admin_db.* TO 'connectsphere'@'%';

FLUSH PRIVILEGES;
