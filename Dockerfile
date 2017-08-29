FROM	openjdk:8-jdk
COPY 	lib/monq-2.0.2.jar /app/monq.jar
COPY	automata/acc170731.mwt	/app/automata/acc170731.mwt
EXPOSE	8888
CMD	["java", "-cp", "/app/monq.jar", "monq.programs.DictFilter", "/app/automata/acc170731.mwt", "-p", "8888"]
