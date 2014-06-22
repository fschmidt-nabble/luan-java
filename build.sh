VERSION=trunk
echo "_G._VERSION = 'Luan $VERSION'" >`pwd`/core/src/luan/version.luan

rm dist/*.jar

SRC=`pwd`/core/src
CLASSPATH=$SRC
javac `find $SRC -name *.java`
jar cvf dist/luan-core-$VERSION.jar `find $SRC -name *.class -o -name *.luan`

SRC=`pwd`/web/src
CLASSPATH=`pwd`/core/src:$SRC
for i in `pwd`/web/ext/* ; do CLASSPATH=$CLASSPATH:$i ; done
javac `find $SRC -name *.java`
jar cvf dist/luan-web-$VERSION.jar `find $SRC -name *.class -o -name *.luan`
