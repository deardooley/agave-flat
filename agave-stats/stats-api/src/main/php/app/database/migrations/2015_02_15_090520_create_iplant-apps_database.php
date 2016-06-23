<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;

class CreateIplant-AppsDatabase extends Migration {

        /**
         * Run the migrations.
         *
         * @return void
         */
         public function up()
         {
            
	    /**
	     * Table: UsageDeveloper
	     */
	    Schema::create('UsageDeveloper', function($table) {
                $table->increments('Username', 64);
                $table->increments('ServiceKey', 30);
            });


	    /**
	     * Table: authconfigs
	     */
	    Schema::create('authconfigs', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('credential', 32768)->nullable();
                $table->string('internal_username', 32)->nullable();
                $table->dateTime('last_updated');
                $table->string('password', 128)->nullable();
                $table->('system_default')->nullable();
                $table->string('login_credential_type', 16);
                $table->string('username', 32)->nullable();
                $table->bigInteger('authentication_system_id')->nullable();
                $table->bigInteger('remote_config_id')->nullable();
                $table->string('private_key', 4098)->nullable();
                $table->string('public_key', 4098)->nullable();
                $table->index('id');
                $table->index('FKAB65DAC9D0F7341D');
                $table->index('FKAB65DAC98B60DEA6');
            });


	    /**
	     * Table: batchqueues
	     */
	    Schema::create('batchqueues', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('custom_directives', 32768)->nullable();
                $table->dateTime('last_updated');
                $table->bigInteger('max_jobs');
                $table->bigInteger('max_memory');
                $table->string('name', 128);
                $table->('system_default')->nullable();
                $table->bigInteger('execution_system_id')->nullable();
                $table->bigInteger('max_nodes');
                $table->bigInteger('max_procesors');
                $table->string('max_requested_time', 255)->nullable();
                $table->bigInteger('max_user_jobs');
                $table->index('id');
                $table->index('FK2F730D3CD7AE66CC');
            });


	    /**
	     * Table: credentialservers
	     */
	    Schema::create('credentialservers', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('endpoint', 255);
                $table->dateTime('last_updated');
                $table->string('name', 64);
                $table->integer('port')->nullable();
                $table->string('protocol', 16);
                $table->index('id');
            });


	    /**
	     * Table: decoding_tasks
	     */
	    Schema::create('decoding_tasks', function($table) {
                $table->increments('id');
                $table->string('callback_key', 64);
                $table->dateTime('created');
                $table->string('current_filter', 64);
                $table->string('dest_path', 255);
                $table->string('dest_transform', 64);
                $table->string('destination_uri', 255);
                $table->string('source_path', 255);
                $table->string('src_transform', 64);
                $table->string('status', 8);
                $table->bigInteger('logical_file_id')->nullable();
                $table->bigInteger('system_id')->nullable();
                $table->index('id');
                $table->index('FKAE027D7A1DCDC7B0');
                $table->index('FKAE027D7ABBBF083F');
            });


	    /**
	     * Table: encoding_tasks
	     */
	    Schema::create('encoding_tasks', function($table) {
                $table->increments('id');
                $table->string('callback_key', 64);
                $table->dateTime('created');
                $table->string('dest_path', 255);
                $table->string('source_path', 255);
                $table->string('status', 32);
                $table->string('transform_name', 32);
                $table->string('transform_filter_name', 32);
                $table->bigInteger('logical_file_id')->nullable();
                $table->bigInteger('system_id')->nullable();
                $table->index('id');
                $table->index('FKF27B81A21DCDC7B0');
                $table->index('FKF27B81A2BBBF083F');
            });


	    /**
	     * Table: executionsystems
	     */
	    Schema::create('executionsystems', function($table) {
                $table->string('environment', 32768)->nullable();
                $table->string('execution_type', 16);
                $table->integer('max_system_jobs')->nullable();
                $table->integer('max_system_jobs_per_user')->nullable();
                $table->string('scheduler_type', 16);
                $table->string('scratch_dir', 255)->nullable();
                $table->string('startup_script', 255)->nullable();
                $table->string('type', 16);
                $table->string('work_dir', 255)->nullable();
                $table->increments('id');
                $table->bigInteger('login_config')->nullable();
                $table->index('FK29629E0C7871F82F');
                $table->index('FK29629E0C8DC88804');
            });


	    /**
	     * Table: fileevents
	     */
	    Schema::create('fileevents', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('description', 32768)->nullable();
                $table->string('ip_address', 15);
                $table->string('status', 32);
                $table->bigInteger('logicalfile_id')->nullable();
                $table->bigInteger('transfertask')->nullable();
                $table->index('id');
                $table->index('FK8A30C99573DE1B78');
                $table->index('FK8A30C99541C615BD');
            });


	    /**
	     * Table: internalusers
	     */
	    Schema::create('internalusers', function($table) {
                $table->increments('id');
                $table->('currently_active')->nullable();
                $table->string('city', 32)->nullable();
                $table->string('country', 32)->nullable();
                $table->dateTime('created');
                $table->string('created_by', 32);
                $table->string('department', 64)->nullable();
                $table->string('email', 128)->nullable();
                $table->string('fax', 32)->nullable();
                $table->string('first_name', 32)->nullable();
                $table->integer('gender')->nullable();
                $table->string('institution', 64)->nullable();
                $table->string('last_name', 32)->nullable();
                $table->dateTime('last_updated');
                $table->string('phone', 15)->nullable();
                $table->string('position', 32)->nullable();
                $table->string('research_area', 64)->nullable();
                $table->string('state', 32)->nullable();
                $table->string('tenant_id', 128);
                $table->string('username', 32);
                $table->string('uuid', 64);
                $table->string('street1', 64)->nullable();
                $table->string('street2', 255)->nullable();
                $table->index('id');
                $table->index('uuid');
                $table->index('username');
                $table->index('username');
            });


	    /**
	     * Table: job_permissions
	     */
	    Schema::create('job_permissions', function($table) {
                $table->increments('id');
                $table->bigInteger('job_id');
                $table->dateTime('last_updated');
                $table->string('permission', 16);
                $table->string('tenant_id', 128);
                $table->string('username', 32);
                $table->index('id');
                $table->index('job_id');
                $table->index('job_id');
            });


	    /**
	     * Table: jobevents
	     */
	    Schema::create('jobevents', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('created_by', 128);
                $table->string('description', 32768)->nullable();
                $table->string('ip_address', 15);
                $table->string('status', 32);
                $table->string('tenant_id', 128);
                $table->bigInteger('job_id');
                $table->bigInteger('transfertask')->nullable();
                $table->index('id');
                $table->index('FK6222FB1673DE1B78');
                $table->index('FK6222FB1678E880CD');
            });


	    /**
	     * Table: jobs
	     */
	    Schema::create('jobs', function($table) {
                $table->increments('id');
                $table->('archive_output')->nullable();
                $table->string('archive_path', 255)->nullable();
                $table->string('callback_url', 255)->nullable();
                $table->float('charge')->nullable();
                $table->dateTime('created');
                $table->dateTime('end_time')->nullable();
                $table->string('error_message', 16384)->nullable();
                $table->string('inputs', 16384)->nullable();
                $table->string('internal_username', 32)->nullable();
                $table->dateTime('last_updated');
                $table->string('local_job_id', 255)->nullable();
                $table->integer('memory_request');
                $table->string('name', 64);
                $table->string('output_path', 255)->nullable();
                $table->string('owner', 32);
                $table->string('parameters', 16384)->nullable();
                $table->integer('processor_count');
                $table->string('requested_time', 19)->nullable();
                $table->integer('retries')->nullable();
                $table->string('scheduler_job_id', 255)->nullable();
                $table->string('software_name', 80);
                $table->dateTime('start_time')->nullable();
                $table->string('status', 32);
                $table->dateTime('submit_time')->nullable();
                $table->string('execution_system', 64);
                $table->string('tenant_id', 128);
                $table->string('update_token', 64)->nullable();
                $table->string('uuid', 64);
                $table->integer('optlock')->nullable();
                $table->('visible')->nullable();
                $table->string('work_path', 255)->nullable();
                $table->bigInteger('archive_system')->nullable();
                $table->string('queue_request', 80);
                $table->bigInteger('node_count');
                $table->integer('status_checks');
                $table->index('id');
                $table->index('uuid');
                $table->index('FK31DC56AC7D7B60');
            });


	    /**
	     * Table: logical_files
	     */
	    Schema::create('logical_files', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('internal_username', 32)->nullable();
                $table->dateTime('last_updated');
                $table->string('name', 64);
                $table->string('native_format', 32)->nullable();
                $table->string('owner', 32);
                $table->string('path', 255);
                $table->string('source', 255)->nullable();
                $table->string('status', 32)->nullable();
                $table->string('uuid', 255);
                $table->bigInteger('system_id')->nullable();
                $table->string('tenant_id', 128);
                $table->index('id');
                $table->index('uuid');
                $table->index('FKBB45CEC1BBBF083F');
            });


	    /**
	     * Table: logicalfilenotifications
	     */
	    Schema::create('logicalfilenotifications', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->dateTime('last_sent');
                $table->string('status', 32);
                $table->('still_pending')->nullable();
                $table->string('callback', 1024)->nullable();
                $table->bigInteger('logicalfile_id')->nullable();
                $table->index('id');
                $table->index('FK2ECF400341C615BD');
            });


	    /**
	     * Table: loginconfigs
	     */
	    Schema::create('loginconfigs', function($table) {
                $table->string('protocol', 16);
                $table->increments('id');
                $table->index('FKC32B7DE85C950942');
            });


	    /**
	     * Table: metadata_permissions
	     */
	    Schema::create('metadata_permissions', function($table) {
                $table->increments('id');
                $table->dateTime('last_updated');
                $table->string('permission', 16);
                $table->string('username', 32);
                $table->string('uuid', 255);
                $table->string('tenant_id', 255)->nullable();
                $table->index('id');
                $table->index('uuid');
                $table->index('uuid');
            });


	    /**
	     * Table: metadata_schema_permissions
	     */
	    Schema::create('metadata_schema_permissions', function($table) {
                $table->increments('id');
                $table->dateTime('last_updated');
                $table->string('permission', 16);
                $table->string('schema_id', 255);
                $table->string('username', 32);
                $table->string('tenant_id', 255)->nullable();
                $table->index('id');
                $table->index('schema_id');
                $table->index('schema_id');
            });


	    /**
	     * Table: monitor_checks
	     */
	    Schema::create('monitor_checks', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('message', 2048)->nullable();
                $table->string('result', 32);
                $table->string('tenant_id', 128);
                $table->string('uuid', 64);
                $table->bigInteger('monitor')->nullable();
                $table->string('type', 16)->nullable();
                $table->index('id');
                $table->index('uuid');
                $table->index('FK83E322F026AC90B');
            });


	    /**
	     * Table: monitors
	     */
	    Schema::create('monitors', function($table) {
                $table->increments('id');
                $table->('is_active')->nullable();
                $table->dateTime('created');
                $table->integer('frequency')->nullable();
                $table->string('internal_username', 64)->nullable();
                $table->dateTime('last_success')->nullable();
                $table->dateTime('last_updated');
                $table->dateTime('next_update_time');
                $table->string('owner', 32);
                $table->string('tenant_id', 128);
                $table->('update_system_status')->nullable();
                $table->string('uuid', 64);
                $table->bigInteger('system')->nullable();
                $table->index('id');
                $table->index('uuid');
                $table->index('system');
                $table->index('system');
                $table->index('system');
                $table->index('FKEC66EE59438E5D43');
            });


	    /**
	     * Table: notifications
	     */
	    Schema::create('notifications', function($table) {
                $table->increments('id');
                $table->string('associated_uuid', 64);
                $table->integer('attempts');
                $table->string('callback_url', 1024);
                $table->dateTime('created');
                $table->dateTime('last_sent')->nullable();
                $table->dateTime('last_updated');
                $table->string('notification_event', 32);
                $table->string('owner', 32);
                $table->('is_persistent')->nullable();
                $table->integer('response_code')->nullable();
                $table->('is_success')->nullable();
                $table->string('tenant_id', 128);
                $table->string('uuid', 64);
                $table->('is_terminated')->nullable()->default("b'0'");
                $table->index('id');
                $table->index('uuid');
            });


	    /**
	     * Table: postits
	     */
	    Schema::create('postits', function($table) {
                $table->increments('id');
                $table->string('target_url', 32768);
                $table->string('target_method', 6)->default("GET");
                $table->string('postit_key', 64);
                $table->string('creator', 32);
                $table->string('token', 64);
                $table->string('ip_address', 15);
                $table->timestamp('created_at')->default("CURRENT_TIMESTAMP");
                $table->timestamp('expires_at')->default("0000-00-00 00:00:00");
                $table->integer('remaining_uses')->default("-1");
                $table->string('internal_username', 32)->nullable();
                $table->string('tenant_id', 128)->default("iplantc.org");
            });


	    /**
	     * Table: proxyservers
	     */
	    Schema::create('proxyservers', function($table) {
                $table->increments('id');
                $table->string('host', 256);
                $table->string('name', 64)->nullable();
                $table->integer('port')->nullable();
                $table->bigInteger('remote_config_id')->nullable();
                $table->index('id');
                $table->index('FKA72DF7628B60DEA6');
            });


	    /**
	     * Table: remoteconfigs
	     */
	    Schema::create('remoteconfigs', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('host', 256);
                $table->dateTime('last_updated');
                $table->integer('port')->nullable();
                $table->bigInteger('proxy_server_id')->nullable();
                $table->index('id');
                $table->index('FKF431326BE2764978');
            });


	    /**
	     * Table: remotefilepermissions
	     */
	    Schema::create('remotefilepermissions', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('internal_username', 32)->nullable();
                $table->dateTime('last_updated');
                $table->bigInteger('logical_file_id');
                $table->string('permission', 32);
                $table->string('tenant_id', 128);
                $table->string('username', 32);
                $table->('is_recursive')->nullable();
                $table->index('id');
            });


	    /**
	     * Table: software_inputs
	     */
	    Schema::create('software_inputs', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('default_value', 255)->nullable();
                $table->string('description', 32768)->nullable();
                $table->string('file_types', 128)->nullable();
                $table->string('output_key', 64);
                $table->string('label', 64)->nullable();
                $table->dateTime('last_updated');
                $table->integer('min_cardinality')->nullable();
                $table->string('ontology', 255)->nullable();
                $table->integer('display_order');
                $table->('required')->nullable();
                $table->string('validator', 255)->nullable();
                $table->('visible')->nullable();
                $table->bigInteger('software')->nullable();
                $table->string('cli_argument', 64)->nullable();
                $table->('show_cli_argument');
                $table->('enquote');
                $table->integer('max_cardinality')->default("-1");
                $table->('repeat_cli_argument');
                $table->index('id');
                $table->index('FKF4C1638159B3FD5F');
            });


	    /**
	     * Table: software_outputs
	     */
	    Schema::create('software_outputs', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('default_value', 255)->nullable();
                $table->string('description', 32768)->nullable();
                $table->string('file_types', 128)->nullable();
                $table->string('output_key', 64);
                $table->string('label', 64)->nullable();
                $table->dateTime('last_updated');
                $table->integer('max_cardinality')->nullable();
                $table->integer('min_cardinality')->nullable();
                $table->string('ontology', 255)->nullable();
                $table->string('pattern', 255)->nullable();
                $table->bigInteger('software')->nullable();
                $table->integer('display_order');
                $table->index('id');
                $table->index('FKECF878FA59B3FD5F');
            });


	    /**
	     * Table: software_parameters
	     */
	    Schema::create('software_parameters', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->string('default_value', 255)->nullable();
                $table->string('description', 32768)->nullable();
                $table->string('output_key', 64);
                $table->string('label', 64)->nullable();
                $table->dateTime('last_updated');
                $table->string('ontology', 255)->nullable();
                $table->integer('display_order');
                $table->('required')->nullable();
                $table->string('value_type', 16);
                $table->string('validator', 255)->nullable();
                $table->('visible')->nullable();
                $table->bigInteger('software')->nullable();
                $table->string('cli_argument', 64)->nullable();
                $table->('show_cli_argument');
                $table->('enquoted');
                $table->integer('max_cardinality')->nullable()->default("1");
                $table->integer('min_cardinality')->nullable();
                $table->('repeat_cli_argument');
                $table->index('id');
                $table->index('FKEE3EF78259B3FD5F');
            });


	    /**
	     * Table: software_permissions
	     */
	    Schema::create('software_permissions', function($table) {
                $table->increments('id');
                $table->dateTime('last_updated');
                $table->string('permission', 16);
                $table->string('username', 32);
                $table->bigInteger('software_id')->nullable();
                $table->index('id');
                $table->index('FKCD9271EC41F2F66B');
            });


	    /**
	     * Table: softwares
	     */
	    Schema::create('softwares', function($table) {
                $table->increments('id');
                $table->('available')->nullable();
                $table->('checkpointable')->nullable();
                $table->string('checksum', 64)->nullable();
                $table->dateTime('created');
                $table->string('deployment_path', 255);
                $table->string('executable_path', 255);
                $table->string('execution_type', 8);
                $table->string('helpuri', 128)->nullable();
                $table->string('icon', 128)->nullable();
                $table->string('label', 64)->nullable();
                $table->dateTime('last_updated');
                $table->string('long_description', 32768)->nullable();
                $table->string('modules', 255)->nullable();
                $table->string('name', 64);
                $table->string('ontology', 255)->nullable();
                $table->string('owner', 32);
                $table->string('parallelism', 8);
                $table->('publicly_available')->nullable();
                $table->integer('revision_count')->nullable();
                $table->string('short_description', 255)->nullable();
                $table->string('tags', 255)->nullable();
                $table->string('tenant_id', 128);
                $table->string('test_path', 255);
                $table->string('uuid', 128);
                $table->string('version', 16);
                $table->bigInteger('system_id')->nullable();
                $table->bigInteger('storage_system_id')->nullable();
                $table->float('default_memory')->nullable();
                $table->integer('default_procesors')->nullable();
                $table->string('default_queue', 12)->nullable();
                $table->string('default_requested_time', 19)->nullable();
                $table->bigInteger('default_nodes')->nullable();
                $table->index('id');
                $table->index('uuid');
                $table->index('name');
                $table->index('name');
                $table->index('name');
                $table->index('name');
                $table->index('name');
                $table->index('FK85C8D3AC62ED13D2');
                $table->index('FK85C8D3AC4B955F33');
            });


	    /**
	     * Table: softwares_inputs
	     */
	    Schema::create('softwares_inputs', function($table) {
                $table->increments('softwares');
                $table->increments('inputs');
                $table->index('inputs');
                $table->index('FKA75D91DC90D96F64');
                $table->index('FKA75D91DCD5BC00DB');
            });


	    /**
	     * Table: softwares_outputs
	     */
	    Schema::create('softwares_outputs', function($table) {
                $table->increments('softwares');
                $table->increments('outputs');
                $table->index('outputs');
                $table->index('FK8DE215FF90D96F64');
                $table->index('FK8DE215FF35F2FE6B');
            });


	    /**
	     * Table: softwares_parameters
	     */
	    Schema::create('softwares_parameters', function($table) {
                $table->increments('softwares');
                $table->increments('parameters');
                $table->index('parameters');
                $table->index('FK8016805D90D96F64');
                $table->index('FK8016805D7A7FA8BB');
            });


	    /**
	     * Table: staging_tasks
	     */
	    Schema::create('staging_tasks', function($table) {
                $table->increments('id');
                $table->bigInteger('bytes_transferred')->nullable();
                $table->dateTime('created');
                $table->dateTime('last_updated');
                $table->integer('retry_count');
                $table->string('status', 32);
                $table->bigInteger('total_bytes')->nullable();
                $table->bigInteger('logical_file_id')->nullable();
                $table->index('id');
                $table->index('FKB9B09E8A1DCDC7B0');
            });


	    /**
	     * Table: storageconfigs
	     */
	    Schema::create('storageconfigs', function($table) {
                $table->string('home_dir', 255)->nullable();
                $table->('mirror_permissions');
                $table->string('protocol', 16);
                $table->string('resource', 255)->nullable();
                $table->string('root_dir', 255)->nullable();
                $table->string('zone', 255)->nullable();
                $table->increments('id');
                $table->string('public_apps_dir', 255)->nullable();
                $table->string('container', 255)->nullable();
                $table->index('FK99C2F2965C950942');
            });


	    /**
	     * Table: storagesystems
	     */
	    Schema::create('storagesystems', function($table) {
                $table->string('type', 16);
                $table->increments('id');
                $table->index('FKF983E1497871F82F');
            });


	    /**
	     * Table: systempermissions
	     */
	    Schema::create('systempermissions', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->dateTime('last_updated');
                $table->string('permission', 32);
                $table->string('username', 32);
                $table->index('id');
            });


	    /**
	     * Table: systemroles
	     */
	    Schema::create('systemroles', function($table) {
                $table->increments('id');
                $table->dateTime('created');
                $table->dateTime('last_updated');
                $table->string('role', 32);
                $table->string('username', 32);
                $table->index('id');
            });


	    /**
	     * Table: systems
	     */
	    Schema::create('systems', function($table) {
                $table->increments('id');
                $table->('available')->nullable();
                $table->dateTime('created');
                $table->string('description', 32768)->nullable();
                $table->('global_default')->nullable();
                $table->dateTime('last_updated');
                $table->string('name', 64);
                $table->string('owner', 32);
                $table->('publicly_available')->nullable();
                $table->integer('revision')->nullable();
                $table->string('site', 64)->nullable();
                $table->string('status', 8);
                $table->string('system_id', 64);
                $table->string('tenant_id', 128);
                $table->string('type', 32);
                $table->string('uuid', 128);
                $table->bigInteger('storage_config')->nullable();
                $table->index('id');
                $table->index('uuid');
                $table->index('system_id_tenant');
                $table->index('system_id_tenant');
                $table->index('FK9871D424DA9BF604');
            });


	    /**
	     * Table: systems_systemroles
	     */
	    Schema::create('systems_systemroles', function($table) {
                $table->bigInteger('systems');
                $table->increments('roles');
                $table->index('roles');
                $table->index('roles_2');
                $table->index('FK3363E5328A8DAC1');
                $table->index('FK3363E5310E3BF38');
            });


	    /**
	     * Table: tenants
	     */
	    Schema::create('tenants', function($table) {
                $table->increments('id')->unsigned();
                $table->string('name', 64)->nullable();
                $table->string('base_url', 255);
                $table->string('contact_email', 128)->nullable();
                $table->string('contact_name', 64)->nullable();
                $table->dateTime('created');
                $table->dateTime('last_updated');
                $table->string('status', 64)->nullable();
                $table->string('tenant_id', 64);
                $table->string('uuid', 128);
            });


	    /**
	     * Table: transfertasks
	     */
	    Schema::create('transfertasks', function($table) {
                $table->increments('id');
                $table->integer('attempts')->nullable();
                $table->double('bytes_transferred', oubl)->nullable();
                $table->dateTime('created');
                $table->string('dest', 2048);
                $table->dateTime('end_time')->nullable();
                $table->string('event_id', 255)->nullable();
                $table->dateTime('last_updated');
                $table->string('owner', 32);
                $table->string('source', 2048);
                $table->dateTime('start_time')->nullable();
                $table->string('status', 16)->nullable();
                $table->string('tenant_id', 128);
                $table->double('total_size', oubl)->nullable();
                $table->double('transfer_rate', oubl)->nullable();
                $table->bigInteger('parent_task')->nullable();
                $table->bigInteger('root_task')->nullable();
                $table->string('uuid', 64);
                $table->index('id');
                $table->index('FK8914FE833015DB82');
                $table->index('FK8914FE83BFE5C64A');
            });


	    /**
	     * Table: userdefaultsystems
	     */
	    Schema::create('userdefaultsystems', function($table) {
                $table->bigInteger('system_id');
                $table->string('username', 255)->nullable();
                $table->index('FKC1EA8F4EBBBF083F');
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
	            Schema::drop('authconfigs');
	            Schema::drop('batchqueues');
	            Schema::drop('credentialservers');
	            Schema::drop('decoding_tasks');
	            Schema::drop('encoding_tasks');
	            Schema::drop('executionsystems');
	            Schema::drop('fileevents');
	            Schema::drop('internalusers');
	            Schema::drop('job_permissions');
	            Schema::drop('jobevents');
	            Schema::drop('jobs');
	            Schema::drop('logical_files');
	            Schema::drop('logicalfilenotifications');
	            Schema::drop('loginconfigs');
	            Schema::drop('metadata_permissions');
	            Schema::drop('metadata_schema_permissions');
	            Schema::drop('monitor_checks');
	            Schema::drop('monitors');
	            Schema::drop('notifications');
	            Schema::drop('postits');
	            Schema::drop('proxyservers');
	            Schema::drop('remoteconfigs');
	            Schema::drop('remotefilepermissions');
	            Schema::drop('software_inputs');
	            Schema::drop('software_outputs');
	            Schema::drop('software_parameters');
	            Schema::drop('software_permissions');
	            Schema::drop('softwares');
	            Schema::drop('softwares_inputs');
	            Schema::drop('softwares_outputs');
	            Schema::drop('softwares_parameters');
	            Schema::drop('staging_tasks');
	            Schema::drop('storageconfigs');
	            Schema::drop('storagesystems');
	            Schema::drop('systempermissions');
	            Schema::drop('systemroles');
	            Schema::drop('systems');
	            Schema::drop('systems_systemroles');
	            Schema::drop('tenants');
	            Schema::drop('transfertasks');
	            Schema::drop('userdefaultsystems');
         }

}