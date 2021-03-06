# Embedded Grid demo

<h3>What does the sample do?</h3>
This sample demonstrates how to start and use an embedded JPPF <a href="https://www.jppf.org/doc/6.3/index.php?title=Embedded_driver_and_node#Embedded_driver">driver</a> and
<a href="https://www.jppf.org/doc/6.3/index.php?title=Embedded_driver_and_node#Embedded_node">node</a> programmatically, in the same JVM as the client.

<h3>How do I run it?</h3>
From a command prompt, type the following:
<ul class="samplesList">
  <li>On a Linux/Unix/MacOS system: <b>./run.sh</b></li>
  <li>On a Windows system: <b>run.bat</b></li>
</ul>

<p>The demo produces an output put that looks like this, where messages from the demo itself are prefixed with `>>>`:
<pre class="samples" style="white-space: pre-wrap">
>>> starting the JPPF driver
driver process id: 4444, uuid: 2C901290-ADAA-4D96-8B73-224B8594DB2B
ClientClassServer initialized
NodeClassServer initialized
ClientJobServer initialized
NodeJobServer initialized
management initialized and listening on port 11111
Acceptor initialized
-  accepting plain connections on port 11111
JPPF Driver initialization complete
>>> starting the JPPF node
>>> starting the JPPF client
client process id: 4444, uuid: B6CDC36A-F4CA-4D1B-A693-396E926A3F93
node process id: 4444, uuid: 1624196C-E0CB-4FD0-8A90-78C9FE1BA6DC
[client: driver1-1 - ClassServer] Attempting connection to the class server at localhost:11111
[client: driver1-1 - ClassServer] Reconnected to the class server
[client: driver1-1 - TasksServer] Attempting connection to the task server at localhost:11111
[client: driver1-1 - TasksServer] Reconnected to the JPPF task server
>>> client connected, now submitting a job
Attempting connection to the class server at 192.168.1.24:11111
RemoteClassLoaderConnection: Reconnected to the class server
JPPF Node management initialized on port 11111
Attempting connection to the node server at 192.168.1.24:11111
Reconnected to the node server
Node successfully initialized
Hello! from the node
>>> execution result for job 'embedded grid': Hello!
>>> connecting to local driver JMX server
>>> nb nodes in server: 1
>>> connecting to local node JMX server
>>> node state: JPPFNodeState[threadPoolSize=8, threadPriority=5, nbTasksExecuted=1, executionStatus=IDLE, connectionStatus=CONNECTED, cpuTime=0, pendingAction=NONE]
>>> shutting down node
>>> nb nodes in server: 0
>>> shutting down driver
>>> done, exiting program
</pre>

<h3>Demo source code</h3>
<ul class="samplesList">
  <li><a href="src/org/jppf/example/embedded/EmbeddedGrid.java">EmbeddedGrid.java</a>: fully commented source code for this demo</li>
</ul>

<h3>Documentation references</h3>
<ul class="samplesList">
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=Embedded_driver_and_node">Embedded driver and node</a></li>
  <li><a href="https://www.jppf.org/doc/6.3/index.php?title=The_JPPF_configuration_API">The JPPF configuration API</a></li>
</ul>

<h3>How can I build the sample?</h3>
To compile the source code, from a command prompt, type: <b>&quot;ant compile&quot;</b><br>
To generate the Javadoc, from a command prompt, type: <b>&quot;ant javadoc&quot;</b>

<h3>I have additional questions and comments, where can I go?</h3>
<p>There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums">The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.2">The JPPF documentation</a></li>
</ul>

