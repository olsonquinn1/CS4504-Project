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
	mvn clean install
	
to run a particular module:

Server Router:
mvn -pl server_router exec:java

Server:
mvn -pl server exec:java

Client:
mvn -pl client exec:java

Test:
mvn -pl shared exec:java "-Dexec.mainClass=com.project.shared.Test" "-Dexec.args=" (if needed)


Data communication structures
Data class encapsulates all others (which implement serializable) within its payload member

ProfilingData   --> (int coreCount, double speedRating)
                    server sends to router upon creating connection, contains core count and a rough compute capability score

RequestData     --> (int threadCount)
                    client sends to router, to propse the requested computation. server attempts to allocate the necessary resources

ResponseData    --> (String message, int taskId, boolean success)
                    router responds to request, accepting or denying it. if accepted, includes the assigned task id

ResultData      --> (int[][] resultMatrix, int m (the subtask), int taskId)
                    server sends to router upon successful matrix multiplication, router forwards to client thread where it accumulates
                    until all subtasks are complete. subtasks are combined then a result data is sent to the client with the final matrix

SubTaskData     --> (int[][][] matrices, int m, int taskId)
                    router sends to server, contains the matrices to be multiplied, requested core count, task id, and subtask id

TaskData        --> (int[][] matrixA, int[][] matrixB, int taskId)
                    client sends to router, contains the two matrices. router divides into 7 subtasks via strassen and distributes to servers