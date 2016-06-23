<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;

class CreateUsageActivitiesTable extends Migration {

	/**
	 * Run the migrations.
	 *
	 * @return void
	 */
	public function up()
	{
		Schema::create('UsageActivities', function(Blueprint $table)
		{
			$table->increments('id');
			$table->string('ActivityKey', 32);
			$table->text('Description')->nullable();
			$table->string('ServiceKey', 30)->nullable();
		});
	}


	/**
	 * Reverse the migrations.
	 *
	 * @return void
	 */
	public function down()
	{
		Schema::drop('UsageActivities');
	}

}
