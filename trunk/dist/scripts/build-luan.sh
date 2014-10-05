VERSION=trunk

cd `dirname $0`/../..
HOME=`pwd`

rm dist/jars/*.jar

set -e

echo "_G._VERSION = 'Luan $VERSION'" >core/src/luan/version.luan

cd $HOME
SRC=core/src
CLASSPATH=$HOME/$SRC
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $HOME/dist/jars/luan-core-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $HOME
SRC=web/src
CLASSPATH=$HOME/core/src:$HOME/$SRC
for i in $HOME/web/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $HOME/dist/jars/luan-web-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $HOME
SRC=logging/src
#CLASSPATH=$HOME/core/src:$HOME/$SRC
#for i in $HOME/logging/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
#javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $HOME/dist/jars/luan-logging-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $HOME
SRC=mail/src
CLASSPATH=$HOME/core/src:$HOME/$SRC
for i in $HOME/mail/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $HOME/dist/jars/luan-mail-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $HOME
SRC=lucene/src
CLASSPATH=$HOME/core/src:$HOME/$SRC
for i in $HOME/lucene/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $HOME/dist/jars/luan-lucene-$VERSION.jar `find . -name *.class -o -name *.luan`
