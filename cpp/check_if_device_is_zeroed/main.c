#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/fs.h>

typedef int bool;
#define false (0)
#define true  (!(false))

const double byte_to_mega_cc = 1024 * 1024;
const double mega_to_giga_cc = 1024;

double byte_to_mega(u_int64_t size) {return (double)size / byte_to_mega_cc; }
double mega_to_giga(double    mega) {return mega         / mega_to_giga_cc; }

void convert(u_int64_t size, double* mega, double* giga) {
  *mega = byte_to_mega(size);
  *giga = mega_to_giga(*mega);
}

char progress_char(u_int64_t offset) {
  static const char progression[] = {'/', '-', '\\'};
  return progression[offset %sizeof(progression)/sizeof(char)];
} 
            
double progress(u_int64_t offset, u_int64_t size) {
  return 100 * ((double) offset / (double) size);
}

int main(const int argc, const char* const argv[]) {
  if (argc < 2) {
    fprintf(stderr, "error: please provide at least one path\n");
    return EXIT_FAILURE;
  }
  
  char progression[] = {'/', '-', '\\'};

  char buffer[16*8192];
  const size_t count = sizeof(buffer) / sizeof(char);

  for (int i = 1; i < argc; ++i) {
    const char* path = argv[i];

    printf("checking [%s]\n", path);
    int fd = open(path, O_RDONLY);

    if (-1 == fd) {
      printf("error: could not open path [%s]: %s.\n", path, strerror(errno));
      continue;
    }
    
    u_int64_t device_size;
    bool device_size_known = ioctl(fd,BLKGETSIZE64, &device_size) != -1;
    if (!device_size_known) {
      fprintf(stderr, "error: could not get device size: %s.\n", strerror(errno));
      device_size = 0;
    }
                
    double mega = 0;
    double giga = 0;
    if (device_size_known) {
      convert(device_size, &mega, &giga);
      printf(        "device size....: %20lu %9.3f MiO %9.3f GiO\n", device_size, mega, giga);      
      fflush(stdout);
    }

    
    size_t offset = 0;
    bool zeroed = true;
    int k = 0;
    
    ssize_t n = 0;
    while (zeroed && (n = read(fd, buffer, count)) > 0) {   
      convert(offset, &mega, &giga);
      if (device_size_known && device_size > 0) {
        printf("\e\033[Kchecking offset: %20lu %9.3f MiO %9.3f GiO (buffer: %zd) %c %6.2f%%\r", 
               offset, mega, giga, n, progress_char(offset), progress(offset, device_size) );      
      } else {
        printf("\e\033[Kchecking offset: %20lu %9.3f MiO %9.3f GiO (buffer: %zd) %c\r", 
               offset, mega, giga, n, progress_char(offset));              
      }
      fflush(stdout);
      for (ssize_t j = 0; j < n; ++j, ++offset) {
        if (buffer[j]) {
          zeroed = false;
          break;
        }
      }
    }
    convert(offset, &mega, &giga);
    if (device_size_known && device_size > 0) {
      printf("\e\033[Kchecking offset: %20lu %9.3f MiO %9.3f GiO (buffer: %zd) %c %6.2f%%\r", 
             offset, mega, giga, n, progress_char(offset), progress(offset, device_size) );      
    } else {
      printf("\e\033[Kchecking offset: %20lu %9.3f MiO %9.3f GiO (buffer: %zd) %c\r", 
              offset, mega, giga, n, progress_char(offset));              
    }
    fflush(stdout);

    if (zeroed) {
      printf("\npath [%s] is zero-ed.\n", path);
    } else {
      printf("\npath [%s] is not zero-ed.\n", path);
    }

    if (-1 == n) {
      fprintf(stderr, "error: could not read from path [%s]: %s\n", path, strerror(errno));
    }
    close(fd);
  }


  return EXIT_SUCCESS;
}
