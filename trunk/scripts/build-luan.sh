. check_luan_home.sh

cd $LUAN_HOME
VERSION=`svnversion`

if echo $VERSION | grep :
then
	echo "svn update needed"
	exit 1
fi


LUAN_BUILD=~/luanbuild

set -e

rm -rf $LUAN_BUILD
mkdir $LUAN_BUILD
mkdir $LUAN_BUILD/luan
mkdir $LUAN_BUILD/luan/jars

cd $LUAN_HOME
echo "_G._VERSION = 'Luan $VERSION'" >core/src/luan/version.luan

cd $LUAN_HOME
SRC=core/src
CLASSPATH=$LUAN_HOME/$SRC
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_BUILD/luan/jars/luan-core-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=web/src
CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
for i in $LUAN_HOME/web/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_BUILD/luan/jars/luan-web-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=logging/src
CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
for i in $LUAN_HOME/logging/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_BUILD/luan/jars/luan-logging-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=mail/src
CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
for i in $LUAN_HOME/mail/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_BUILD/luan/jars/luan-mail-$VERSION.jar `find . -name *.class -o -name *.luan`

cd $LUAN_HOME
SRC=lucene/src
CLASSPATH=$LUAN_HOME/core/src:$LUAN_HOME/$SRC
for i in $LUAN_HOME/lucene/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac -classpath $CLASSPATH `find $SRC -name *.java`
cd $SRC
jar cvf $LUAN_BUILD/luan/jars/luan-lucene-$VERSION.jar `find . -name *.class -o -name *.luan`

cp $LUAN_HOME/web/ext/* $LUAN_BUILD/luan/jars
cp $LUAN_HOME/logging/ext/* $LUAN_BUILD/luan/jars
cp $LUAN_HOME/mail/ext/* $LUAN_BUILD/luan/jars
cp $LUAN_HOME/lucene/ext/* $LUAN_BUILD/luan/jars

cp $LUAN_HOME/scripts/install.sh $LUAN_BUILD/luan
chmod +x $LUAN_BUILD/luan/install.sh
cp $LUAN_HOME/scripts/uninstall.sh $LUAN_BUILD/luan

cd $LUAN_BUILD
tar -cf luan-$VERSION.tar luan

echo done
