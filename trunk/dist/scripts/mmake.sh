CURRDIR=`pwd`
cd `dirname $0`/../..
HOME=`pwd`
cd $CURRDIR

luan.sh $HOME/dist/scripts/mmake.luan $*
echo `pwd`
