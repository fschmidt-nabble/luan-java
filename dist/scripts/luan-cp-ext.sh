HOME=$1

for i in $HOME/web/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
for i in $HOME/logging/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
for i in $HOME/mail/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
for i in $HOME/lucene/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
