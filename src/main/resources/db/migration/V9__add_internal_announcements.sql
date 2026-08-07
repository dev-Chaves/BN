CREATE TABLE announcements (
    id BIGSERIAL NOT NULL,
    company_id BIGINT NOT NULL,
    author_manager_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    published_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_announcements PRIMARY KEY (id),
    CONSTRAINT fk_announcements_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_announcements_author FOREIGN KEY (author_manager_id) REFERENCES managers (id),
    CONSTRAINT ck_announcements_title_required CHECK (btrim(title) <> ''),
    CONSTRAINT ck_announcements_title_length CHECK (char_length(title) <= 160),
    CONSTRAINT ck_announcements_content_required CHECK (btrim(content) <> ''),
    CONSTRAINT ck_announcements_content_length CHECK (char_length(content) <= 4000)
);

CREATE TABLE announcement_recipients (
    id BIGSERIAL NOT NULL,
    announcement_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    read_at TIMESTAMP,
    CONSTRAINT pk_announcement_recipients PRIMARY KEY (id),
    CONSTRAINT fk_announcement_recipients_announcement
        FOREIGN KEY (announcement_id) REFERENCES announcements (id),
    CONSTRAINT fk_announcement_recipients_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT uq_announcement_recipient UNIQUE (announcement_id, employee_id)
);

CREATE INDEX idx_announcements_company_published
    ON announcements (company_id, published_at DESC, id DESC);

CREATE INDEX idx_announcement_recipients_announcement
    ON announcement_recipients (announcement_id);

CREATE INDEX idx_announcement_recipients_employee
    ON announcement_recipients (employee_id, announcement_id);

CREATE INDEX idx_announcement_recipients_employee_unread
    ON announcement_recipients (employee_id)
    WHERE read_at IS NULL;

CREATE SEQUENCE IF NOT EXISTS announcements_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS announcement_recipients_SEQ START WITH 1 INCREMENT BY 50;
