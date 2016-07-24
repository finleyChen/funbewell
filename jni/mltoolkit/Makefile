CC = gcc
CXX = g++

#distribution
#CFLAGS    = -Wall -O3 -std=c99  
#CXXFLAG = -Wall -O3

#debug
CFLAGS    = -Wall -std=c99 -O2
CXXFLAG = -Wall -O2

libs = -lm -lrt -ldl -lpthread #-lsqlite3 -lcurl 
FILES = mfcc.o procAudio.o classifier.o procAcc.o kiss_fft.o kiss_fftr.o buffer.o features.o MLToolKit.o main.o  mvnpdf.o #cJSON.o #comm.o dsp2.o
OUT_EXE = kit

.c.o: 
	$(CC) $(CFLAGS) -c $< $(libs)

.cpp.o:
	$(CXX) $(CXXFLAGS) -c $< $(libs)

build: $(FILES)
	$(CXX) -o $(OUT_EXE) $(CFLAGS) $(FILES) $(libs)

main.o: 
MLToolKit.o:
buffer.o: 
features.o:
procAcc.o:
classifier.o:
procAudio.o:
mfcc.o:
kiss_fftr.o: 
kiss_fft.o:
mvnpdf.o:
#comm.o:
#dsp2.o:
#cJSON.o:


clean:
	 -rm -f *.o kit

rebuild: clean build

install: 
	scp -r kit root@129.170.212.232:~/
  

