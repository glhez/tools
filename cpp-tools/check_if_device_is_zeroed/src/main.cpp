

#include <cstdlib>
#include <iomanip>
#include <iostream>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>


int main(const int argc, const char* const argv[]) {
  if (argc < 2) {
    std::cerr << "error: please provide one path." << std::endl;
    return EXIT_FAILURE;
  }

  char buffer[4*8192];
  const size_t count = sizeof(buffer) / sizeof(char);

  for (int i = 1; i < argc; ++i) {
    const char* path = argv[i];

    std::cout << "checking [" << path << "]" << std::endl;


    int fd = open(path, O_RDONLY);

    if (-1 == fd) {
      std::cerr << "error: could not open path [" << path << "]" << std::endl;
      continue;
    }

    ssize_t n = 0;
    size_t offset = 0;
    bool zeroed = true;
    while ((n = read(fd, buffer, count)) > 0) {
      double mega = static_cast<double>(offset) / (1024*1024);
      double giga = mega / 1024;
      std::cout << "\e\033[Kchecking offset " 
                << std::setw(20) << std::setprecision(0) << offset 
                << " " 
                << std::setw(9) << std::fixed << std::setprecision(3) << mega << " MiO "
                << std::setw(9) << std::fixed << std::setprecision(3) << giga << " GiO "
                << "(buffer: " << n << ")\r"
                << std::flush
                ;
      for (ssize_t j = 0; j < n; ++j, ++offset) {
        if (buffer[j]) {
          zeroed = false;
          break;
        }
      }
    }
    if (zeroed) {
      std::cout << "path [" << path << "] is zero-ed." << std::endl;    
    } else {
      std::cout << "path [" << path << "] is not zero-ed." << std::endl;
    }

    if (-1 == n) {
      std::cerr << "error: could not read from path [" << path << "]" << std::endl;
    }

    close(fd);
  }


  return EXIT_SUCCESS;
}
