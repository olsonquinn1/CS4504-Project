to build/run

initial
	* have maven installed: https://maven.apache.org/download.cgi
	* ensure maven bin is in path
	* use an IDE that supports maven (i use vs code with java extension)

loading project:
	IntelliJ: file > open > select project root dir
	Eclipse: file > import > existing maven project, select project root dir
	vs code:  file > open folder, select project root dir

build:
	* open cli in project root dir
	mvn clean install -DskipTests
	
to run a particular module:

Server Router:
mvn -pl server_router exec:java "-Dexec.mainClass=com.project.server_router.RouterApp"

Server:
mvn -pl server exec:java "-Dexec.mainClass=com.project.server.ServerApp"

Client:
mvn -pl client exec:java "-Dexec.mainClass=com.project.client.ClientApp"

router ip is probably 73.137.88.81 
client port: 5556
server port: 5555