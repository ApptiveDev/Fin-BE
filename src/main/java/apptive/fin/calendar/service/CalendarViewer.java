package apptive.fin.calendar.service;

import java.util.Set;

/**
 * 자격 판정에 쓰는 로그인 사용자 정보. 비로그인이거나 약관 동의 전이면 만들지 않는다(판정하지 않음).
 *
 * @param age        만 나이. 프로필에 생년월일이 없으면 null
 * @param regionKeys 프로필의 거주지역 키(REGION_*). 없으면 빈 집합
 */
record CalendarViewer(Integer age, Set<String> regionKeys) {
}
