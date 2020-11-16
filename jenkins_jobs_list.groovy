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