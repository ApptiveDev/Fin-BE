package apptive.fin.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 빈_DB는_Flyway_마이그레이션으로_초기화된다() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE ORDER BY installed_rank",
                String.class
        );
        Integer monthlyIncome = jdbcTemplate.queryForObject(
                "SELECT monthly_income FROM median_incomes "
                        + "WHERE year = 2026 AND household_size = 1 AND earn_percent = 100",
                Integer.class
        );

        assertThat(versions).contains("1", "2", "3", "4");
        assertThat(monthlyIncome).isEqualTo(256);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.product_raw')::text",
                String.class
        )).isEqualTo("product_raw");
    }

    @Test
    void 개인정보_수집_이용_동의는_v2_0만_현재_버전이다() {
        List<String> currentVersions = jdbcTemplate.queryForList(
                "SELECT tv.major_version || '.' || tv.minor_version FROM term_versions tv "
                        + "JOIN terms t ON t.id = tv.term_id "
                        + "WHERE t.code = 'PRIVACY_POLICY' AND tv.is_current = TRUE",
                String.class
        );
        String content = jdbcTemplate.queryForObject(
                "SELECT tv.content FROM term_versions tv JOIN terms t ON t.id = tv.term_id "
                        + "WHERE t.code = 'PRIVACY_POLICY' AND tv.is_current = TRUE",
                String.class
        );

        assertThat(currentVersions).containsExactly("2.0");
        assertThat(content)
                .startsWith("'Y-Fin.'은 「개인정보 보호법」")
                .contains("\n| 알림 정보 | 신청한 일정 알림 메일 발송, 알림 받을 이메일 인증 | 알림 해지 또는 회원 탈퇴 시까지 |\n")
                .doesNotContain("\\n", "\r");
    }
}
