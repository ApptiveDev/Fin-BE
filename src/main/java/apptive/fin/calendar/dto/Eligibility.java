package apptive.fin.calendar.dto;

/** 로그인 사용자 기준 정책 자격 판정. 비로그인이면 응답에서 null. */
public enum Eligibility {
    ELIGIBLE,
    INELIGIBLE,
    NEEDS_INFO   // 정책에 제한이 있는데 프로필 값이 비어 판정하지 못함. 캘린더는 숨기지 않는다
}
