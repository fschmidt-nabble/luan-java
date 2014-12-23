. check_luan_home.sh

CLASSPATH=$LUAN_HOME/core/src
CLASSPATH=$CLASSPATH:$LUAN_HOME/web/src
CLASSPATH=$CLASSPATH:$LUAN_HOME/logging/src
CLASSPATH=$CLASSPATH:$LUAN_HOME/mail/src
CLASSPATH=$CLASSPATH:$LUAN_HOME/lucene/src

for i in $LUAN_HOME/web/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
for i in $LUAN_HOME/logging/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
for i in $LUAN_HOME/mail/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
for i in $LUAN_HOME/lucene/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done

export CLASSPATH
