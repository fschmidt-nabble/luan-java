for i in $LUAN_HOME/dist/jars/* ; do CLASSPATH=$CLASSPATH:$i ; done

. luan-ext-classpath.sh
