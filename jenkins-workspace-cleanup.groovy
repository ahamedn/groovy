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
This script will do the workspace cleanup on the Jenkins slave. I've added a whitelist option to skip the slaves which are not required to clean their workspaces.
*/

import hudson.model.*;
import hudson.util.*;
import jenkins.model.*;
import hudson.FilePath.FileCallable;
import hudson.slaves.OfflineCause;
import hudson.node_monitors.*;
import javax.mail.*
import javax.mail.internet.*


//Whitelisting the slaves which are not required to cleanup the workspace
def MyList=["Slave2, Slave5"]

def sendMail(host, sender, receivers, subject, text) {
    Properties props = System.getProperties()
    props.put("mail.smtp.host", host)
    Session session = Session.getDefaultInstance(props, null)

    MimeMessage message = new MimeMessage(session)
    message.setFrom(new InternetAddress(sender))
    receivers.split(',').each {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(it))
    }
    message.setSubject(subject)
    message.setText(text)

    println 'Sending mail to ' + receivers + '.'
    Transport.send(message)
    println 'Mail sent.'
}

def performCleanup(def node, def items) {
  
  for (item in items) {
      try {
          jobName = item.getFullDisplayName()
          
          println("Cleaning " + jobName)
          
          if(item instanceof com.cloudbees.hudson.plugins.folder.AbstractFolder) {
              performCleanup(node, item.items)
              continue
          }
          
          if (item.isBuilding()) {
              println(".. job " + jobName + " is currently running, skipped")
              continue
          }
          
          println(".. wiping out workspaces of job " + jobName)
          
          workspacePath = node.getWorkspaceFor(item)
          if (workspacePath == null) {
              println(".... could not get workspace path")
              continue
          }
          
          println(".... workspace = " + workspacePath)
          
          pathAsString = workspacePath.getRemote()
          if (workspacePath.exists()) {
              workspacePath.deleteRecursive()
              println(".... deleted from location " + pathAsString)
          } else {
              println(".... nothing to delete at " + pathAsString)
          }
      } catch(Exception ex) {
          println("... couldnot perform cleanup at workspace = " + workspacePath)
          continue
      }
  }  
}


for (node in Jenkins.instance.nodes) {
    computer = node.toComputer()

    if (node.getDisplayName() in MyList) {
             println("Skipping the Jenkins Slave : " + node.getDisplayName() + ", which is whitelisted")
    } 
    else 
    {
    if (computer.getChannel() == null) continue

    rootPath = node.getRootPath()
    size = DiskSpaceMonitor.DESCRIPTOR.get(computer).size
    roundedSize = size / (1024  1024  1024) as int

    println("Jenkins Slave : " + node.getDisplayName() + " was cleaned up and free space: " + roundedSize + "GB")
    sendMail('host_server', "mail_sender", "mail_reciever", "Jenkins Slave workspace cleanup", "${node.getDisplayName()} has been cleaned up")
    /*computer.setTemporarilyOffline(true, new hudson.slaves.OfflineCause.ByCLI("disk cleanup"))*/
  
    performCleanup(node, Jenkins.instance.items)
  
    computer.setTemporarilyOffline(false, null)
    }
} 
