package com.feple.feple_backend.admin;

final class AdminFestivalRedirects {

    private static final String BASE = "redirect:/admin/festivals/";

    private AdminFestivalRedirects() {}

    static String detail(Long festivalId)   { return BASE + festivalId; }
    static String artists(Long festivalId)  { return BASE + festivalId + "#artists"; }
    static String timetable(Long festivalId){ return BASE + festivalId + "#timetable"; }
    static String booths(Long festivalId)   { return BASE + festivalId + "#booths"; }
    static String setlist(Long festivalId)  { return BASE + festivalId + "#setlist"; }
}
