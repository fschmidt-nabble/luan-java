CURRDIR=`pwd`
cd `dirname $0`/../..
HOME=`pwd`
cd $CURRDIR

. luan-cp.sh $HOME

java -classpath $CLASSPATH luan.Luan $*
