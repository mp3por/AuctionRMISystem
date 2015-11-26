OUT=out
ALL=velin
SERVER=velin/server
CLIENT=velin/client
TEST=velin/test
COMMON=velin/common

all:
	if [ -d "./out" ]; then compilecommon compileclient compileserver compiletest; else echo "Please create 'out' folder"


compilecommon: $(COMMON)/*.java
	javac $(COMMON)/*.java -d $(OUT)

compileserver: $(SERVER)/*.java
	javac -cp $(OUT) $(SERVER)/*.java -d $(OUT)

compileclient: $(CLIENT)/*.java
	javac -cp $(OUT) $(CLIENT)/*.java -d $(OUT)

compiletest: $(TEST)/*.java
	javac -cp $(OUT) $(TEST)/*.java -d $(OUT)

clean:
	rm -rf $(OUT)/$(ALL)