VERSION=trunk

. check_luan_home.sh

cd $LUAN_HOME
rm dist/jars/*.jar
set -e
echo "_G._VERSION = 'Luan $VERSION'" >core/src/luan/version.luan

cd $LUAN_HOME
SRC=core/src
CLASSPATH=$LUAN_HOME/$SRC
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_HOME/dist/jars/luan-core-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=web/src
CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
for i in $LUAN_HOME/web/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_HOME/dist/jars/luan-web-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=logging/src
#CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
#for i in $LUAN_HOME/logging/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
#javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_HOME/dist/jars/luan-logging-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=mail/src
CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
for i in $LUAN_HOME/mail/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_HOME/dist/jars/luan-mail-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=lucene/src
CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
for i in $LUAN_HOME/lucene/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_HOME/dist/jars/luan-lucene-$VERSION.jar `find . -name *.class -o -name *.luan`
