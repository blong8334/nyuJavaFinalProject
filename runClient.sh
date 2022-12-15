echo "make out dir";
mkdir -p out/client;
echo "clean out dir";
rm -rf out/client/*;
echo "compile client";
javac -cp ".:./src:./jsoup-1.15.3.jar" src/client/Client.java -d out;
cd out;
echo "run client";
java -cp ".:./client:../jsoup-1.15.3.jar" client.Client;