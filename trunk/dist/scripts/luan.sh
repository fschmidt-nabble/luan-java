cd `dirname $0`/../..
HOME=`pwd`

. luan-cp.sh $HOME

java -classpath $CLASSPATH luan.Luan $*
