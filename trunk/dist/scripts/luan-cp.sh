HOME=$1

for i in $HOME/dist/jars/* ; do CLASSPATH=$CLASSPATH:$i ; done

. luan-cp-ext.sh $HOME
