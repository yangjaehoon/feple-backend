package com.feple.feple_backend.admin.festival;

public final class AdminFestivalRedirects {

    private static final String BASE = "redirect:/admin/festivals/";

    private AdminFestivalRedirects() {}

    public static String detail(Long festivalId)   { return BASE + festivalId; }
    public static String artists(Long festivalId)  { return BASE + festivalId + "#artists"; }
    public static String timetable(Long festivalId){ return BASE + festivalId + "#timetable"; }
    public static String booths(Long festivalId)   { return BASE + festivalId + "#booths"; }
    public static String setlist(Long festivalId)  { return BASE + festivalId + "#setlist"; }
}
