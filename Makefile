all: compile

compile:
	javac MultiThreadedChatServer.java
	javac ChatClient.java

clean:
	rm -f *.class
