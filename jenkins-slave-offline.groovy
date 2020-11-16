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
This script will send an email, when the Jenkins slaves went offline. Dependency :: Add the javax.mail jar file in the Jenkins master/slave library to run this script.  
*/

import jenkins.model.Jenkins
import hudson.node_monitors.*;
import hudson.slaves.*;
import java.util.concurrent.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

jenkins = Jenkins.instance

def sendMail (agent, cause) {

//Add the Mailing information 
 message = agent + " agent is down. Check https://jenkins.yourdomain.com/computer/"+agent+ "\nBecause " + cause
 subject = agent + " agent is offline"
 toAddress = "abc@yourdomain.com"
 fromAddress = "no-reply-jenkins@yourdomain.com"
 host = "smtp.yourdomain.com"
 port = "25"

 Properties mprops = new Properties();
 mprops.setProperty("mail.transport.protocol","smtp");
 mprops.setProperty("mail.host",host);
 mprops.setProperty("mail.smtp.port",port);

 Session lSession = Session.getDefaultInstance(mprops,null);
 MimeMessage msg = new MimeMessage(lSession);


 //tokenize out the recipients in case they came in as a list
 StringTokenizer tok = new StringTokenizer(toAddress,";");
 ArrayList emailTos = new ArrayList();
 while(tok.hasMoreElements()) {
   emailTos.add(new InternetAddress(tok.nextElement().toString()));
 }
 InternetAddress[] to = new InternetAddress[emailTos.size()];
 to = (InternetAddress[]) emailTos.toArray(to);
 msg.setRecipients(MimeMessage.RecipientType.TO,to);
 InternetAddress fromAddr = new InternetAddress(fromAddress);
 msg.setFrom(fromAddr);
 msg.setFrom(new InternetAddress(fromAddress));
 msg.setSubject(subject);
 msg.setText(message)

 Transport transporter = lSession.getTransport("smtp");
 transporter.connect();
 transporter.send(msg);
}


def getEnviron(computer) {
   def env
   def thread = Thread.start("Getting env from ${computer.name}", { env = computer.environment })
   thread.join(2000)
   if (thread.isAlive()) thread.interrupt()
   env
}

def agentAccessible(computer) {
    getEnviron(computer)?.get('PATH') != null
}

def numberOfflineNodes = 0
def numberNodes = 0
for (agent in jenkins.getNodes()) {
   def computer = agent.computer
   numberNodes ++
   println ""
   println "Checking computer ${computer.name}:"
   def isOK = (agentAccessible(computer) && !computer.offline)
   if (isOK) {
     println "\t\tOK, got PATH back from slave ${computer.name}."
     println('\tcomputer.isOffline: ' + agent.getComputer().isOffline());
     println('\tcomputer.isTemporarilyOffline: ' + agent.getComputer().isTemporarilyOffline());
     println('\tcomputer.getOfflineCause: ' + agent.getComputer().getOfflineCause());
     println('\tcomputer.offline: ' + computer.offline);
   } else {
     numberOfflineNodes ++
     println "  ERROR: can't get PATH from agent ${computer.name}."
     println('\tcomputer.isOffline: ' + agent.getComputer().isOffline());
     println('\tcomputer.isTemporarilyOffline: ' + agent.getComputer().isTemporarilyOffline());
     println('\tcomputer.getOfflineCause: ' + agent.getComputer().getOfflineCause());
     println('\tcomputer.offline: ' + computer.offline);
     sendMail(computer.name, agent.getComputer().getOfflineCause().toString())
     if (agent.getComputer().isTemporarilyOffline()) {
       if (!agent.getComputer().getOfflineCause().toString().contains("Disconnected by")) {
         computer.setTemporarilyOffline(false, agent.getComputer().getOfflineCause())
       }
     } else {
         computer.connect(true)
     }
   }
 }
println ("Number of Offline Nodes: " + numberOfflineNodes)
println ("Number of Nodes: " + numberNodes)