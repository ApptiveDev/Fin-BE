package apptive.fin.calendar.entity;

/** 온통청년 신청기간 구분(aplyPrdSeCd). 수집기가 코드값을 이 이름으로 바꿔 저장한다. */
public enum ApplyPeriodType {
    PERIOD,  // 0057001 특정기간: 회차(시작 ~ 종료)가 있다
    ALWAYS,  // 0057002 상시
    CLOSED   // 0057003 마감: 캘린더에 표시하지 않는다
}
