package com.feple.feple_backend.user.entity;

public enum AuthProvider {
    // EMAIL: 과거 이메일/비밀번호 로그인 기능으로 가입한 유저 16명이 이 값으로 저장돼 있어 보존 (실제 로그인 경로는 없음)
    KAKAO, EMAIL, FIREBASE
}
