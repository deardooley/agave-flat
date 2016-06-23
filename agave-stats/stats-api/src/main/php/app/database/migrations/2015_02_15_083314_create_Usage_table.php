<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;

class CreateUsageTable extends Migration {

	/**
	 * Run the migrations.
	 *
	 * @return void
	 */
	public function up()
	{
		Schema::create('Usage', function(Blueprint $table)
		{
			$table->increments('UID');
			$table->string('Username', 64);
			$table->string('ActivityKey', 32);
			$table->string('ServiceKey', 30);
			$table->string('ActivityContext', 64)->nullable();
			$table->datetime('CreatedAt');
			$table->string('CallingIP', 15)->nullable();
			$table->string('UserIP', 15)->nullable();
			$table->string('ClientApplication', 64);
			$table->string('TenantId', 64);
			$table->string('UserAgent', 64)->nullable();
		});
	}


	/**
	 * Reverse the migrations.
	 *
	 * @return void
	 */
	public function down()
	{
		Schema::drop('Usage');
	}

}
