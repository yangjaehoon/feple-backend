package com.feple.feple_backend.admin.song;

import com.feple.feple_backend.admin.AdminConstants;

record SongListParams(String status, Integer page, String keyword) {
    SongListParams {
        status  = status == null ? AdminConstants.STATUS_PENDING : status;
        page    = page == null ? 0 : page;
        keyword = keyword == null ? "" : keyword;
    }
}
