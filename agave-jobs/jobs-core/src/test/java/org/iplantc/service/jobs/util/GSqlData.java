package org.iplantc.service.jobs.util;


/**
 * Created with IntelliJ IDEA.
 * User: steve
 * Date: 1/14/13
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * GSqlData will instantiate a connection on construction. Use the connection thru methods supplied but
 * remember to specifically close the connection using GSqlData closeConnection()
 */
public class GSqlData {
/*
    // def url = "jdbc:mysql://localhost:3306/test?sessionVariables=FOREIGN_KEY_CHECKS=0&profileSQL=true"
    def url = "jdbc:mysql://localhost:3306/test?sessionVariables=FOREIGN_KEY_CHECKS=0"
    public Sql sql = Sql.newInstance(url, "root", "", "com.mysql.jdbc.Driver")
    StringBuffer stringBuffer = new StringBuffer()
    def log = true
    List<String> tableNames;

    GSqlData(String testName){
        println "starting a new connection ${this} in $testName"
    }


    String turnOffFK = "SET foreign_key_checks = 0;"
    String turnOnFK = "SET foreign_key_checks = 1;"
    String autoCommitOff = "SET AUTOCOMMIT=0;"
    String commit = "COMMIT;"

    def condorLogContents =
        '''
        000 (154.000.000) 01/31 14:27:43 Job submitted from host: <129.116.126.94:56091>
        ...
        001 (154.000.000) 01/31 14:27:45 Job executing on host: <129.116.126.94:56092>
        ...
        006 (154.000.000) 01/31 14:27:45 Image size of job updated: 1
        \t0  -  MemoryUsage of job (MB)
        \t0  -  ResidentSetSize of job (KB)
        ...
        005 (154.000.000) 01/31 14:27:45 Job terminated.
        \t(1) Normal termination (return value 0)
        \t\tUsr 0 00:00:00, Sys 0 00:00:00  -  Run Remote Usage
        \t\tUsr 0 00:00:00, Sys 0 00:00:00  -  Run Local Usage
        \t\tUsr 0 00:00:00, Sys 0 00:00:00  -  Total Remote Usage
        \t\tUsr 0 00:00:00, Sys 0 00:00:00  -  Total Local Usage
        \t0  -  Run Bytes Sent By Job
        \t0  -  Run Bytes Received By Job
        \t0  -  Total Bytes Sent By Job
        \t0  -  Total Bytes Received By Job
        \tPartitionable Resources :    Usage  Request
        \t   Cpus                 :                 1
        \t   Disk (KB)            :        1        1
        \t   Memory (MB)          :        0        0
        ...

        '''

    def clearRecord(String tableName, String id) {
        def sqlStatement = "DELETE FROM `test`.`${tableName}` WHERE `id`='${id}';"
        sql.execute(sqlStatement)
    }

    def clearRecord(String tableName, String fieldName, Long id) {
        def sqlStatement = 'DELETE FROM test.' + tableName + ' WHERE ' + fieldName + ' = ' + id + ';'
        //println("sql : $sqlStatement")
        boolean result = !sql.execute(sqlStatement)
        // println "success : ${result}"
    }

    def clearRecord(String sqlStatement) {
        sql.execute(sqlStatement)
    }

    def insertRecord(String sqlStatement) {
        sql.execute(sqlStatement)
    }

    def sqlExecute(String sqlStatement){
        if(log) {println "sql : $sqlStatement"}
        sql.execute(sqlStatement)
    }

    def closeConnection(String testName){
        sql.close()
        println " closing connection  ${this} from $testName"
    }

    List<String> getTableNames(){
        List<GroovyRowResult> tables = new ArrayList<GroovyRowResult>();
        List<String> names = new ArrayList<String>()
        tables = sql.rows("show tables")
        tables.each{ result ->
            String name = result.getProperty("Tables_in_test")
            names << name
        }
        tableNames = names
        return tableNames
    }


    def totalTableRecords(){
        if(tableNames == null || tableNames.isEmpty()){
            getTableNames()  // the call sets up the tablenames list
        }
        def total = 0
        tableNames.each { name ->
            String query = "SELECT COUNT(*) from $name"
            List<GroovyRowResult> result = this.sql.rows(query)
            total += result[0].getProperty("COUNT(*)")
        }
        return total
    }

    def countTableRecords(){
        getTableNames()  // the call sets up the tablenames list
        tableNames.each { name ->
            String query = "SELECT COUNT(*) from $name"
            List<GroovyRowResult> result = this.sql.rows(query)
            println(" ${result[0]} records in table $name")
        }

    }

    */
/**
     * Given the table name and unique id field name this method reads and deletes each record from the table
     * @param tableName Name of the table to clear
     * @param fieldIdName Name of the field for the unique id
     * @return ?
     *//*

    def cleanTable(String tableName, String fieldIdName){
        def id
        // get all records in table
        println("   currently wiping $tableName")
        def ids = []
        String statement = "SELECT `${fieldIdName}` FROM `test`.`${tableName}`;"    // gets all fields
        try {
            sql.eachRow(statement){
                ids << it.toRowResult()
            }

            ids.each { rowResult ->
                id = rowResult.get(fieldIdName)
                clearRecord(tableName,fieldIdName,id)
            }
        } catch (Exception e) {
            println("failed to find a table "+tableName)
        }
    }

    List<GroovyRowResult> showOpenTables(){
        List<GroovyRowResult> openTables
        List<GroovyRowResult> dbTables = new ArrayList()
        openTables = sql.rows("SHOW OPEN TABLES;")
        openTables.each {row ->
            if(row.getProperty("Database").equals("test")){
                dbTables.add(row)
            }
        }
        return dbTables
    }

    List showLockedTables(){
        List<GroovyRowResult> openTables = showOpenTables()
        List lockedTables = new ArrayList()
        boolean locked = false
        for(GroovyRowResult row : openTables){
            String tableName = (String)row.getProperty("Table");
            Long j = (Long) row.getProperty("In_use");
            //println(tableName+" "+j);
            if(j == 1){
                lockedTables.add(tableName);
            }
        }
        return lockedTables
    }

    public void lockTables(){
        StringBuffer sb = new StringBuffer("LOCK TABLES ")
        getTableNames()
        def lastElement = tableNames.size() - 1
        tableNames.eachWithIndex{ tableName, idx ->
            sb.append("${tableName} WRITE")
            if(lastElement > idx ){
                sb.append(", ")
            }else{
                sb.append("; ")
            }
        }
            try {
                sqlExecute(sb.toString())
            } catch (Exception e) {
                println("Sql exception locking tables")
                unLockTables()
            }
    }

    public void lockAndWipeTables(){
        sqlExecute(autoCommitOff)
        lockTables()
        cleanAllTablesByRecord()
        sqlExecute(commit)
        unLockTables()
    }

    public void unLockTables(){
        sqlExecute("UNLOCK TABLES;")
    }

    public Job createJob(JobStatusType status, RemoteSystem storageSystem, Software software) throws Exception {
        Job job = new Job();
        job.setName("test-job");
        job.setInternalUsername("sterry1");
        job.setArchiveOutput(true);
        job.setArchivePath("/sterry1/jobs/condor");
        job.setArchiveSystem(storageSystem);
        job.setCreated(new Date());
        JSONObject inputs = new JSONObject();
        for(SoftwareInput swInput: software.getInputs()) {
            inputs.put(swInput.getKey(), swInput.getDefaultValue());
        }
        job.setInputs(inputs.toString());
        job.setMemoryRequest(512);
        job.setOwner("sterry1");
        JSONObject parameters = new JSONObject();
        for(SoftwareParameter swParameter: software.getParameters()) {
            inputs.put(swParameter.getKey(), swParameter.getDefaultValue());
        }
        job.setParameters(parameters.toString());
        job.setProcessorCount(1);
        job.setRequestedTime("1:00");
        job.setSoftwareName(software.getUniqueName());
        job.setStatus(status);
        job.setSystem(software.getExecutionSystem().getSystemId());
        job.setUpdateToken("7d7e5472e5159d726d905b4c06009c2f");
        job.setVisible(true);

        return job;
    }

    Map t = ["fred":"wilma",
            "jake":"evan"]

    Map tableList =
    [
        "AUTHCONFIGS":"id",
        "AuthenicationTokens":"id",
        "BATCHQUEUES" : "id",
        "CREDENTIALSERVERS" : "id",
        "EncodingTasks" : "id",
        "EXECUTIONSYSTEMS" : "id",
        "EXECUTIONSYSTEMS_BATCHQUEUES" : "EXECUTIONSYSTEMS_id",
        "INTERNALUSERS" : "id",
        "JobPermissions" : "id",
        "Jobs" : "id",
        "LogicalFiles" : "id",
        "LOGINCONFIGS" : "id",
        "REMOTECONFIGS" : "id",
        "REMOTECONFIGS_AUTHCONFIGS" : "REMOTECONFIGS_id",
        "REMOTEFILEPERMISSIONS" : "id",
        "STORAGESYSTEMS" : "id",
        "SoftwareInputs" : "id",
        "SoftwareOutputs" : "id",
        "SoftwareParameters" : "id",
        "SoftwarePermissions" : "id",
        "Softwares" : "id",
        "Softwares_SoftwareInputs" : "Softwares_id",
        "Softwares_SoftwareOutputs" : "Softwares_id",
        "Softwares_SoftwareParameters" : "Softwares_id",
        "Softwares" : "id",
        "StagingTasks":"id",
        "STORAGECONFIGS" : "id",
        "STORAGESYSTEMS":"id",
        "SYSTEMPERMISSIONS":"id",
        "SYSTEMROLES" : "id",
        "SYSTEMS" : "id",
        "SYSTEMS_SYSTEMROLES" : "SYSTEMS_id",
        "TRANSFERTASKS" : "id",
        "USERDEFAULTSYSTEMS" : "systemId"
    ]



    def cleanAllTablesByRecord(){
        println "Wiping data from all tables in database ..."
        //sqlExecute(turnOffFK)
        tableList.each{key,value ->
            cleanTable(key,value)
        }
    }

    private boolean isExecutable(String scriptLine){
        boolean statement = !scriptLine.startsWith("--") && !scriptLine.startsWith("*/
/*") && !scriptLine.isEmpty()
    }

    private boolean isPrintable(String scriptLine){
        scriptLine.startsWith("-- c")
    }


    private boolean runLine(String scriptLine){
        // read each line and decide whether to output to log or execute as a sql statement

    }

    */
/**
     * Runs a prepared sql script to load or change data for test
     * @param script file based sql script in a specific format
     * @return success or failure of script run
     *//*

    public boolean runSqlScript(File script){
        script.eachLine { scriptLine ->
            if(isPrintable(scriptLine)){
                println(scriptLine)
            }

            if(isExecutable(scriptLine)){
                sqlExecute(scriptLine)
            }
        }
    }

    */
/**
     * Runs a prepared sql script to load or change data for test
     * @param script path to sql script in a specific format
     * @return success or failure of script run
     *//*

    public boolean runSqlScript(String scriptPath){
        File script = new File(scriptPath)
        runSqlScript(script)
    }

    public boolean loadRecords(String testname, String sqlScriptPath){
        println("******    $testname data    ******")
        runSqlScript(sqlScriptPath)

    }

    
	/***************************************************
     * methods for specific tests, builds on top of
     * above methods for wiping and loading some data
     ***************************************************//*


    public boolean cleanAndLoadDB(String testname, String sqlScriptPath) {
        println("******    $testname setup database    ******")
        cleanAllTablesByRecord()
        loadRecords(testname,sqlScriptPath)
    }

    public void dropAndLoadSchemaForTest(String hibernateConfigFilePath, String sqlScriptPath){
        println("******    Setup database    ******")
        // use the sql script to drop and create fresh schema
        runSqlScript(sqlScriptPath)

        // use Hibernate to create new Table structure in fresh schema
        CommonHibernateTest.initdb()
    }

    public void createFreshSchema(){
        runSqlScript("src/test/resources/sql/drop_and_create_fresh_schema.sql")
    }
*/


}
