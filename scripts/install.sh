cd `dirname $0`

cat >/usr/local/bin/luan <<End
for i in `pwd`/jars/* ; do CLASSPATH=\$CLASSPATH:\$i ; done

java -classpath \$CLASSPATH luan.Luan "\$@"
End

chmod +x /usr/local/bin/luan

echo "the command 'luan' has been installed"
