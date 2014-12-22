. check_luan_home.sh

rm -rf $LUAN_HOME/tar

mkdir $LUAN_HOME/tar
mkdir $LUAN_HOME/tar/luan
mkdir $LUAN_HOME/tar/luan/jars

cp $LUAN_HOME/dist/jars/* $LUAN_HOME/tar/luan/jars
cp $LUAN_HOME/web/ext/* $LUAN_HOME/tar/luan/jars
cp $LUAN_HOME/logging/ext/* $LUAN_HOME/tar/luan/jars
cp $LUAN_HOME/mail/ext/* $LUAN_HOME/tar/luan/jars
cp $LUAN_HOME/lucene/ext/* $LUAN_HOME/tar/luan/jars

cp $LUAN_HOME/dist/scripts/install.sh $LUAN_HOME/tar/luan
chmod +x $LUAN_HOME/tar/luan/install.sh

cd $LUAN_HOME/tar
tar -cf luan.tar luan
mv luan.tar ~/Dropbox/luan

rm -r $LUAN_HOME/tar
