package com.feple.feple_backend.admin.log;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminActionTest {

    @Test
    void DELETE_BAN_REJECT_FAILURE는_DANGER() {
        assertThat(AdminAction.POST_DELETE.severity()).isEqualTo(AdminAction.Severity.DANGER);
        assertThat(AdminAction.USER_BAN.severity()).isEqualTo(AdminAction.Severity.DANGER);
        assertThat(AdminAction.CERTIFICATION_REJECT.severity()).isEqualTo(AdminAction.Severity.DANGER);
        assertThat(AdminAction.LOGIN_FAILURE.severity()).isEqualTo(AdminAction.Severity.DANGER);
    }

    @Test
    void CREATE_ADD_APPROVE_UNBAN은_SUCCESS() {
        assertThat(AdminAction.FESTIVAL_CREATE.severity()).isEqualTo(AdminAction.Severity.SUCCESS);
        assertThat(AdminAction.FESTIVAL_ARTIST_ADD.severity()).isEqualTo(AdminAction.Severity.SUCCESS);
        assertThat(AdminAction.CERTIFICATION_APPROVE.severity()).isEqualTo(AdminAction.Severity.SUCCESS);
        assertThat(AdminAction.USER_UNBAN.severity()).isEqualTo(AdminAction.Severity.SUCCESS);
    }

    @Test
    void 그외는_INFO() {
        assertThat(AdminAction.LOGIN_SUCCESS.severity()).isEqualTo(AdminAction.Severity.INFO);
        assertThat(AdminAction.LOGOUT.severity()).isEqualTo(AdminAction.Severity.INFO);
        assertThat(AdminAction.FESTIVAL_UPDATE.severity()).isEqualTo(AdminAction.Severity.INFO);
        assertThat(AdminAction.USER_ROLE_CHANGE.severity()).isEqualTo(AdminAction.Severity.INFO);
        assertThat(AdminAction.REPORT_DISMISS.severity()).isEqualTo(AdminAction.Severity.INFO);
    }

    @Test
    void 복구는_SUCCESS_라인업제거_신청기각은_DANGER() {
        assertThat(AdminAction.POST_RESTORE.severity()).isEqualTo(AdminAction.Severity.SUCCESS);
        assertThat(AdminAction.FESTIVAL_RESTORE.severity()).isEqualTo(AdminAction.Severity.SUCCESS);
        assertThat(AdminAction.ARTIST_RESTORE.severity()).isEqualTo(AdminAction.Severity.SUCCESS);
        assertThat(AdminAction.FESTIVAL_ARTIST_REMOVE.severity()).isEqualTo(AdminAction.Severity.DANGER);
        assertThat(AdminAction.FESTIVAL_SUGGESTION_DISMISS.severity()).isEqualTo(AdminAction.Severity.DANGER);
        assertThat(AdminAction.ARTIST_SUGGESTION_DISMISS.severity()).isEqualTo(AdminAction.Severity.DANGER);
    }
}
