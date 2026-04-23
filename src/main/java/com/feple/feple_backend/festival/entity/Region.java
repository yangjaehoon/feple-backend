package com.feple.feple_backend.festival.entity;

public enum Region {
    SEOUL("서울"),
    BUSAN("부산"),
    INCHEON("인천"),
    DAEGU("대구"),
    DAEJEON("대전"),
    GWANGJU("광주"),
    ULSAN("울산"),
    SEJONG("세종"),
    GYEONGGI("경기도"),
    GANGWON("강원도"),
    CHUNGBUK("충청북도"),
    CHUNGNAM("충청남도"),
    GYEONGBUK("경상북도"),
    GYEONGNAM("경상남도"),
    JEONBUK("전라북도"),
    JEONNAM("전라남도"),
    JEJU("제주도"),
    ETC("기타");

    private final String displayName;

    Region(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
