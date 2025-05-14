CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    details TEXT,
    read BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL,
    book_id VARCHAR(255),
    borrow_id VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);