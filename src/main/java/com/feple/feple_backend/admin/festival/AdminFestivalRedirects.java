package com.feple.feple_backend.admin.festival;

public final class AdminFestivalRedirects {

    private static final String BASE = "redirect:/admin/festivals/";

    private AdminFestivalRedirects() {}

    public static String detail(Long festivalId)   { return BASE + festivalId; }
    // fragment anchor(#artists 등)는 서버에 전달되지 않고 브라우저가 클라이언트 측에서 처리.
    // HTTP 302 Location에 anchor를 포함하면 브라우저가 해당 섹션으로 자동 스크롤한다.
    public static String artists(Long festivalId)  { return BASE + festivalId + "#artists"; }
    public static String timetable(Long festivalId){ return BASE + festivalId + "#timetable"; }
    public static String booths(Long festivalId)   { return BASE + festivalId + "#booths"; }
    public static String setlist(Long festivalId)  { return BASE + festivalId + "#setlist"; }
}
