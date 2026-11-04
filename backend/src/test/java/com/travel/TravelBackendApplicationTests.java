package com.travel;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class TravelBackendApplicationTests {

	@Autowired
	DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void migrationsUpgradeFromV6WithoutChangingAppliedHistory() {
		String schema = "migration_upgrade_" + UUID.randomUUID().toString().replace("-", "");
		var config = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
				.locations("classpath:db/migration").cleanDisabled(false);
		Flyway legacy = config.target("6").load();
		try {
			legacy.migrate();
			assertThat(legacy.info().applied()).extracting(MigrationInfo::getScript).endsWith(
					"V5__add_place_context_fields.sql", "V6__add_weekly_opening_hours_to_places.sql");
			var history = Arrays.stream(legacy.info().applied())
					.map(migration -> migration.getVersion() + ":" + migration.getChecksum()).toList();

			Flyway latest = config.target(MigrationVersion.LATEST).load();
			assertThat(latest.migrate().migrationsExecuted).isEqualTo(9);
			assertThat(latest.info().applied()).extracting(MigrationInfo::getScript).endsWith(
					"V7__auth_refresh_tokens.sql", "V8__email_verification_and_password_reset.sql",
					"V9__google_authentication.sql", "V10__trip_details_and_place_category.sql",
					"V11__seed_local_demo.sql", "V12__persist_multi_day_itinerary.sql",
					"V13__place_retrieval_profiles.sql", "V14__trip_metadata_and_days.sql",
                    "V15__retain_removed_trip_schedule.sql");
			assertThat(Arrays.stream(latest.info().applied()).limit(history.size())
					.map(migration -> migration.getVersion() + ":" + migration.getChecksum()).toList())
					.isEqualTo(history);
		} finally {
			legacy.clean();
		}
	}

}
