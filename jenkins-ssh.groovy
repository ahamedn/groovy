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
This script will test SSH and TCP connection between jenkins to all the list of servers from all the environments.   
*/

//@Grab('org.yaml:snakeyaml:1.24')
import org.yaml.snakeyaml.*
import org.yaml.snakeyaml.constructor.*
import groovy.transform.*
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;

class Result {
String hostname
boolean ssh
boolean tcp
}

def tier = System.getenv("Tier")
def environment = System.getenv("Environment")
def deploy = System.getenv("Deploy")
def workspace = System.getenv("WORKSPACE")
def unavailableHosts = new ArrayList<String>()
def results = new ArrayList<Result>()


def filename = "product/hosts/" + tier + "-" + environment +".txt"
def file = new File(filename)

if (file.exists()){
//def lines = file.readLines()
file.eachLine {String hostname ->
def result = new Result()
result.hostname = hostname
if (checkConnectivity("tcp", hostname)){
println (hostname + " TCP success")
result.tcp = true
} else {
println ("Error " + hostname + " TCP Failed")
result.tcp = false
}
if (checkConnectivity("ssh", hostname)){
println (hostname + " SSH Success")
result.ssh = true
} else {
println ("Error " + hostname + " SSH Failed")
result.ssh = false
}
results.add(result)
}
} else {
println ("File not found: " + filename)
}

if (!results.isEmpty()){
println ("Hostname\t\t\t\tTCP\t\tSSH")
results.each { result ->
println (result.hostname + "\t\t" + result.tcp + "\t\t" + result.ssh)

}

outputJson(results)
}

def outputJson(results) {
def workspace = System.getenv("WORKSPACE")
def file = new File(workspace + "/results.json")
//file.createNewFile()
def fileWriter = file.newWriter()
//hosts.each { result ->
// convert to json and write to file
//fileWriter.println(result.get("hostname") + "\t\t" + result.get("tcp") + "\t\t" + result.get("ssh"))
//}
fileWriter.print(JsonOutput.toJson(results))
fileWriter.close()
}



def checkConnectivity(testType, hostname){
def workspace = System.getenv("WORKSPACE")
def script = "product/sh/test-" + testType + ".sh"
def command = "sh -x " + workspace + "/" + script + " " + hostname
return runCommand(command)
}


def runCommand(command){
def outStream = new StringBuffer()
def errStream = new StringBuffer()
def proc = command.execute()
proc.consumeProcessOutput(outStream, errStream)
proc.waitForOrKill(10000)
if (!proc.exitValue()){
return true
} else {
return false
}
}