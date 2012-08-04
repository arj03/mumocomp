#!/bin/sh

#CLASSPATH=$CLASSPATH:/home/arj/
#CLASSPATH=$CLASSPATH:/home/arj/mumocomp/

for f in /home/arj/mumocomp/web/jars/*.jar; do
    echo "loading:" $f
    CLASSPATH=$CLASSPATH:$f
done

java -server -Djava.library.path=/home/arj/mumocomp/ -cp $CLASSPATH clojure.main -i /home/arj/mumocomp/startup.clj
#java -server -Djava.library.path=/home/arj/mumocomp/ -cp $CLASSPATH clojure.main -i /home/arj/mumocomp/web/web.clj
