

CXXFLAGS=--std=c++11 -I.
TARGET_OBJS=objs
TARGET_BIN=bin
SRC=src

objs/%.o: src/%.cpp
	$(CXX) $(CXXFLAGS) -o $@ -c $< 

bin/%: objs/%.o 
	$(CXX) $(CXXFLAGS) -o $@ $^

#$(TARGET_BIN)/check_block_device.exe: $(TARGET_OBJS)/check_block_device.o
#	$(CXX) $(CXXFLAGS) -o $@ $^

all: objs bin bin/check_device_is_zeroed
	@echo compiling...

objs bin:
	@mkdir -p $@


#.PHONY: clean 

clean:
	rm -Rfv $(TARGET_BIN) $(TARGET_OBJS)

	

