package com.feple.feple_backend.festival.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// DB 컬럼은 기존과 동일한 기상청 원본 코드 문자열("0"~"4")을 그대로 저장한다 —
// 운영 공유 DB라 스키마/데이터 마이그레이션 없이 자바 레이어에서만 타입 안전성을 얻기 위함.
@Converter(autoApply = true)
public class PtyCodeConverter implements AttributeConverter<PtyCode, String> {

    @Override
    public String convertToDatabaseColumn(PtyCode attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public PtyCode convertToEntityAttribute(String dbData) {
        return dbData != null ? PtyCode.fromCode(dbData) : null;
    }
}
