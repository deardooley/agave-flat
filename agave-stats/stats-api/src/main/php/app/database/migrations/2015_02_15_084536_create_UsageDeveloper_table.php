<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;

class CreateUsageDeveloperTable extends Migration {

	/**
	 * Run the migrations.
	 *
	 * @return void
	 */
	public function up()
	{
		Schema::create('UsageDeveloper', function(Blueprint $table)
		{
			$table->increments('id');
			$table->string('Username', 64);
			$table->string('ServiceKey', 32);
			$table->timestamps();
		});
	}


	/**
	 * Reverse the migrations.
	 *
	 * @return void
	 */
	public function down()
	{
		Schema::drop('UsageDeveloper');
	}

}
