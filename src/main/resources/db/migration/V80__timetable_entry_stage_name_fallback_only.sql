-- timetable_entry.stage_name: stage_id FK가 있는 행에서도 무대명을 문자열로 중복 저장 (3NF 위반).
-- stage FK가 연결된 행은 stage.name을 단일 출처로 삼고, stage_name은 FK가 없는(OCR 미매칭·공지 슬롯)
-- 행의 표시용 폴백으로만 남긴다. 스테이지 삭제 시 StageService가 삭제 직전 이름을 stage_name에 스냅샷한다.
ALTER TABLE `timetable_entry` MODIFY COLUMN `stage_name` VARCHAR(255) NULL;

UPDATE `timetable_entry` SET `stage_name` = NULL WHERE `stage_id` IS NOT NULL;
