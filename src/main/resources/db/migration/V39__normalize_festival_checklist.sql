-- festival_checklist.lineup1/2/3: 같은 속성을 번호를 붙여 반복한 컬럼 (반복 그룹, 1NF 위반)
-- boothMap/timetable도 함께 이관해 단일 item 테이블로 통합

CREATE TABLE festival_checklist_item (
    checklist_id BIGINT      NOT NULL,
    item_key     VARCHAR(20) NOT NULL,
    checked      TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (checklist_id, item_key),
    CONSTRAINT fk_fci_checklist FOREIGN KEY (checklist_id) REFERENCES festival_checklist (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 데이터 이관
INSERT INTO festival_checklist_item (checklist_id, item_key, checked)
SELECT id, 'lineup1',   lineup1   FROM festival_checklist
UNION ALL
SELECT id, 'lineup2',   lineup2   FROM festival_checklist
UNION ALL
SELECT id, 'lineup3',   lineup3   FROM festival_checklist
UNION ALL
SELECT id, 'boothMap',  booth_map FROM festival_checklist
UNION ALL
SELECT id, 'timetable', timetable FROM festival_checklist;

-- 원본 컬럼 제거
ALTER TABLE festival_checklist
    DROP COLUMN lineup1,
    DROP COLUMN lineup2,
    DROP COLUMN lineup3,
    DROP COLUMN booth_map,
    DROP COLUMN timetable;
