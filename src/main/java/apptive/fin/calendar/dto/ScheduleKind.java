package apptive.fin.calendar.dto;

/** 캘린더에서 회차를 보여주는 방식. */
public enum ScheduleKind {
    PERIOD,      // 시작·마감 칩을 그리드에 찍는다
    YEAR_ROUND,  // 연중 접수: 180일 이상이면서 12/31 종료. 그리드 대신 연중·상시 목록
    ALWAYS       // 상시 신청: 신청 기간이 없다. 그리드 대신 연중·상시 목록
}
