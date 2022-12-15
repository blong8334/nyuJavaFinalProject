echo "make out dir";
mkdir -p out/server;
echo "clean out dir";
rm -rf out/server/*;
echo "compile server";
javac -cp ".:./src:./jsoup-1.15.3.jar" src/server/Server.java -d out;
cd out;
echo "run server";
java -cp ".:./server:../jsoup-1.15.3.jar" server.Server;