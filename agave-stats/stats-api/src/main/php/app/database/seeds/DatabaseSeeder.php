<?php

class DatabaseSeeder extends Seeder {

	/**
	 * Run the database seeds.
	 *
	 * @return void
	 */
	public function run()
	{
		Eloquent::unguard();

		$seedFile = app_path().'/database/seeds/seed.sql';
		$exitCode = 0;
		$out = null;
		if (file_exists($seedFile.'.zip')) {
			$cmd = "gunzip -c < {$seedFile}.zip > {$seedFile}";

			$this->command->info("Unzipping import archive {$seedFile}.zip...");
			$this->command->info($cmd);

			echo exec($cmd, $out, $exitCode);

			if ($exitCode !== 0) {
				$this->command->error(print_r($out));
				$this->command->error("Failed to unzip {$seedFile}.zip. Seed failed.");
				return false;
			} else {
				if (!empty($out)) $this->command->error(print_r($out));
			}
		} else if (file_exists($seedFile)) {
			$this->command->info("No zipped import archive found. Using {$seedFile} instead.");
		} else {
			$this->command->error("Unable to find {$seedFile}.zip or {$seedFile}. Unable to proceed. Seed failed.");
			return false;
		}

		$cmd = sprintf('mysql --user=%s --password=%s --host=%s %s < %s',
									 $_ENV['mysql.agave.username'],
									 $_ENV['mysql.agave.password'],
									 $_ENV['mysql.agave.host'],
									 $_ENV['mysql.agave.database'],
									 $seedFile);

		$this->command->info("\nImporting tables...");
		$this->command->info($cmd);

		$exitCode = 0;
		$out = null;
		$this->command->info(exec($cmd, $out, $exitCode));

		if ($exitCode !== 0) {
			$this->command->error(print_r($out));
			$this->command->error("Failed to import sql. Seed failed.");
			return false;
		} else {
			if (!empty($out)) $this->command->error(print_r($out));
			$this->command->info("\nSeeding complete!");
		}

	}

}
