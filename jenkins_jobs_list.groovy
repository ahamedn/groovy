/*
Copyright (c) 2015-2020 Ahamed N - https://github.com/ahamedn/groovy

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

/*
This script will list all the Jenkins Freestyle jobs, Declarative Pipelines and Scripted Pipelines and the import the result to Azure Postgres DB. If the Postgress part is not required, comment out from Line No 35-42. 
*/

import com.cloudbees.hudson.plugins.folder.computed.PeriodicFolderTrigger
import jenkins.model.Jenkins
import jenkins.branch.*
import hudson.model.*
import groovy.sql.*
import java.sql.*
import java.time.*
import java.time.format.DateTimeFormatter

// Importing the results to Postgres DB running on Azure
def driver = Class.forName('org.postgresql.Driver').newInstance() as Driver
def props = new Properties()
// Add the Postgres Username/Password and the jdbc URL
props.setProperty("user", "ROOT@postgresql...");
props.setProperty("password", "PASSWORD123");
def conn = driver.connect("jdbc:postgresql://XXXXX.postgres.database.azure.com:5432/jenkins_jobs_list", props)
def sql = new Sql(conn)

projects = [:]
Jenkins.instance.getAllItems(AbstractItem.class).each { j ->
String jc = j.class.simpleName
if(!(jc in projects)) {
projects[jc] = 0
}
projects[jc]++
}

def now = LocalDateTime.now()
println now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))

println "Count projects by type for: " + Jenkins.getInstance().getRootUrl()
println " Current Jenkins version: " + Jenkins.getInstance().version


for (def key in projects.keySet()) {
println "key = ${key}, value = ${projects[key]}"

sql.query ("SELECT * FROM list WHERE TYPE='${key}'") { resultSet ->
if(resultSet.next()) {
println resultSet.next()
sql.execute( "UPDATE list SET COUNT='${projects[key]}',DATE='${now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))}' WHERE TYPE='${key}'")
} else
sql.execute( "INSERT INTO list (TYPE,COUNT,DATE) VALUES ('${key}','${projects[key]}','${now.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))}')")
}
}
sql.close()
conn.close()
return