<?php

/*
|--------------------------------------------------------------------------
| Application Routes
|--------------------------------------------------------------------------
|
| Here is where you can register all of the routes for an application.
| It's a breeze. Simply tell Laravel the URIs it should respond to
| and give it the Closure to execute when that URI is requested.
|
|
*/
Route::group(array('prefix' => '/stats/v2', 'before' => 'jwt'), function()
{
	Route::resource('', 'StatsController');
	Route::resource('summary', 'SummaryStatsController');
	Route::resource('traffic', 'TrafficStatsController');
	Route::resource('jobs', 'JobStatsController');
	Route::resource('data', 'DataStatsController');
	Route::resource('apps', 'AppStatsController');
	Route::resource('code', 'CodeStatsController');
	Route::resource('clients', 'ClientStatsController');
	Route::resource('users', 'UserStatsController');
	Route::resource('operations', 'OperationsStatsController');
	Route::resource('tenants', 'TenantsStatsController');
});
