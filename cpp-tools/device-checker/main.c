#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <math.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/fs.h> 

#include <getopt.h>
                          
typedef int bool;
#define false (0)
#define true  (!(false))

#define COLOR_RED     "\e[1;31m"
#define COLOR_GREEN   "\e[1;32m"
#define COLOR_NONE    "\e[0m"   
#define COLOR_CYAN    "\e[1;36m"
#define COLOR_BLUE    "\e[1;34m"
#define COLOR_YELLOW  "\e[1;33m"
#define COLOR_HL      "\e[1m"   

#define DEFAULT_SECTOR_SIZE ((unsigned long)512)

char progress_char(unsigned long offset) {
  static const char progression[] = {'/', '-', '\\'};
  return progression[offset %sizeof(progression)/sizeof(char)];
}
double progress(unsigned long offset, unsigned long size) {
  return 100 * ((double) offset / (double) size);
} 

const char* to_string0(unsigned long size, bool si)  {
  static char buffer_ring[2][12][128] = {};
  static int  buffer_ring_index[2] = {};
  static const char* multiplier_format_bi[] = {"%.3f KiB", "%.3f MiB", "%.3f GiB", "%.3f TiB", NULL};
  static const char* multiplier_format_si[] = {"%.3f KB", "%.3f MB", "%.3f GB", "%.3f TB", NULL};
  
  int bri = si ? 0:1;
  unsigned long multiplier = si ? 1000:1024;
  char* buffer = buffer_ring[bri][(buffer_ring_index[bri]++ % 12)];
  
  if (size < multiplier) {
    snprintf(buffer, 128, "%lu B", size);
  } else {
    const char* format = NULL;
    const char** table = si ? multiplier_format_si : multiplier_format_bi;
    unsigned long m = multiplier;
    
    double size_f = size;
    for (int i = 0; table[i] != NULL; ++i) {
      unsigned long n = m*multiplier;
      if (table[i+1] == NULL || size < n) {
        format = table[i];
        size_f /= m;
        break;
      }
      m = n;
    }
    if (size_f == INFINITY) {
      fprintf(stderr, "error: size_f = inf -- multiplier: %lu, m: %lu\n", multiplier, m);
    }
    snprintf(buffer, 128, format, size_f);
  }
  buffer[127] = 0;
  return buffer;
}

const char* to_si_string(unsigned long size)  {
  return to_string0(size, true);
}

const char* to_bi_string(unsigned long size)  {
  return to_string0(size, false);
}

// typedefs -- {{{
typedef enum {
  EXECUTION_MODE_INVALID,
  EXECUTION_MODE_ZERO,
  EXECUTION_MODE_ONE,
  EXECUTION_MODE_FIND
} ExecutionMode; 

typedef struct {
  FILE* output;
  unsigned long sector_size;
  unsigned long input_position;
  unsigned long size;
  char* buffer;
  unsigned long buffer_size;
} param_t;

typedef struct {
  int fd;
  const char* path;
} file_t;
// }}}

unsigned long ulong_min(unsigned long a, unsigned long b) {
  return a < b ? a:b;
}

// parsing the command line -- {{{
bool parse_arg_as_size0(const char* value, unsigned long* position) {
  char* endptr = NULL;
  *position = strtoul(value, &endptr, 10);
  return *endptr == '\0';
}

unsigned long parse_arg_as_size(const char* argname, bool* fail_indicator, const char* value, const unsigned long default_value) {
  if (NULL == value) {
    return default_value;
  }
  unsigned long result = default_value;
  if (!parse_arg_as_size0(value, &result)) {
    *fail_indicator = true;
    fprintf(stderr, "%serror:%s could not parse arg %s%s%s option value %s%s%s", 
      COLOR_RED, COLOR_NONE, COLOR_YELLOW, argname, COLOR_NONE, COLOR_HL, value, COLOR_NONE);
    return default_value;
  }
  return result;  
}



void usage(const char* program, const char* p_file, const char* p_sector_size) { // {{{
  const char* device      = "device or file";     
  const char* file        = NULL == p_file        ? "file"   : p_file;       
  const char* sector_size = NULL == p_sector_size ? "size"   : p_sector_size;
  
  fprintf(stderr, "usage (0): %s%s%s %s%s%s --check-zero %s# verify that device contains only 0x00000000 filled bytes.%s\n", COLOR_CYAN, program, COLOR_NONE, COLOR_HL, device, COLOR_NONE, COLOR_BLUE, COLOR_NONE);
  fprintf(stderr, "usage (1): %s%s%s %s%s%s --check-one  %s# verify that device contains only 0x11111111 filled bytes.%s\n", COLOR_CYAN, program, COLOR_NONE, COLOR_HL, device, COLOR_NONE, COLOR_BLUE, COLOR_NONE);
  fprintf(stderr, "usage (2): %s%s%s %s%s%s --find %s%s%s %s# find a block of %ssector-size%s%s containing content of %s%s%s%s\n",
    COLOR_CYAN, program, COLOR_NONE, COLOR_HL, device, COLOR_NONE
    COLOR_YELLOW, file, COLOR_NONE, COLOR_BLUE, COLOR_NONE, COLOR_YELLOW, sector_size, COLOR_BLUE, COLOR_NONE, COLOR_YELLOW, file, COLOR_NONE);
  fprintf(stderr, "           %s--sector-size (-b)%s can be used to force the device sector size (example: for a non device).\n", COLOR_HL, COLOR_NONE);
  fprintf(stderr, "           %s--size (-s)%s can be used to force the device size or to limit to scope (example: for a non device or /dev/zero).\n", COLOR_HL, COLOR_NONE);
  fprintf(stderr, "           %s--input-position (-i)%s can be used to start from some position in device/file.\n", COLOR_HL, COLOR_NONE);
} // }}} [usage]

// }}}

bool match_all(const file_t* file, const unsigned long size, const unsigned long sector_size, char* buffer, const unsigned long buffer_size, const int what) {
  
  char* match_pattern = calloc(sector_size, sizeof(char));
  if (NULL == match_pattern) {
    fprintf(stderr, "%serror:%s could not allocate %lu bytes for the pattern block (error message: %s%s%s).\n", 
      COLOR_RED, COLOR_NONE, sector_size, COLOR_HL, strerror(errno), COLOR_NONE);
    return false;
  }
  memset(match_pattern, what, sector_size);
  
  bool matched = true;
  ssize_t n = 0;
  unsigned long chunk = 0;
  for (ssize_t size_read = 0; matched && (n = read(file->fd, buffer, buffer_size)) > 0 && size_read < size; size_read += n) {
    unsigned long limit = size_read + n > size ? ulong_min(n, size - size_read):n;
    for (unsigned long i = 0; i < limit; i += sector_size, ++chunk) {
      unsigned long chunk_read = size_read + i;
      unsigned long chunk_size = (i + sector_size) > limit ? limit - i :sector_size;
      fprintf(stdout, "checking chunk %12lu: [%s0x%016lx 0x%08lx%s] [%12s] [%12s] %c %s%6.2f%%%s\r", 
        chunk, COLOR_HL, chunk_read, chunk_size, COLOR_NONE,
        to_si_string(chunk_read), to_bi_string(chunk_read), 
        progress_char(chunk_read), 
        COLOR_YELLOW, progress(chunk_read, size), COLOR_NONE
      );
      
      if (0 != memcmp(buffer + i, match_pattern, chunk_size)) {
        matched = false;
        break;
      }
    }
  }
  free(match_pattern);
  if (matched) {
    fprintf(stdout, "\n%ssuccess:%sfile [%s%s%s] passed test.\n", COLOR_GREEN, COLOR_NONE, COLOR_HL, file->path, COLOR_NONE);  
  } else {
    fprintf(stdout, "\n%serror:%s file [%s%s%s] failed test.\n", COLOR_RED, COLOR_NONE, COLOR_HL, file->path, COLOR_NONE);  
  }
  return matched;
}

bool execute(const file_t* file, const ExecutionMode mode, param_t* param) { // {{{
  // [find info] {{{ 
  int fd           = file->fd;
  const char* path = file->path;
  
  struct stat device_stat;
  if (-1 == fstat(fd, &device_stat)) {
    fprintf(stderr, "%serror:%s could not get stat on device (error message is %s%s%s). \n", 
      COLOR_RED, COLOR_NONE, COLOR_HL, strerror(errno), COLOR_NONE);
    return false;
  }  
  
  bool is_block_device = S_ISBLK(device_stat.st_mode);
  
  /*
   * try to get information about the device (if it is one)
   */
  unsigned long sector_size = param->sector_size;
  if (sector_size == 0) {
    if (!is_block_device) {
      sector_size = DEFAULT_SECTOR_SIZE;
    } else if (-1 == ioctl(fd, BLKSSZGET, &sector_size)) {
      fprintf(stderr, "%swarning:%s could not get sector size (error message is %s%s%s). Defaulting to %lu.\n", 
        COLOR_YELLOW, COLOR_NONE, COLOR_HL, strerror(errno), COLOR_NONE, DEFAULT_SECTOR_SIZE);
      sector_size = DEFAULT_SECTOR_SIZE;
    }
  } 
  
  /*
   * get device size (fails if it is not a  device). 
   */
  unsigned long file_or_device_size = 0;
  bool file_or_device_size_known = true;
  if (!is_block_device) {
    file_or_device_size = device_stat.st_size;
  } else {
    if (-1 == ioctl(fd, BLKGETSIZE64, &file_or_device_size)) {
      fprintf(stderr, "%serror:%s could not get device size (error message is %s%s%s). \n", 
        COLOR_RED, COLOR_NONE, COLOR_HL, strerror(errno), COLOR_NONE);    
      file_or_device_size_known = false;
    }
  }
  
  unsigned long size = param->size;
  if (size == 0 && file_or_device_size_known) {
    size = file_or_device_size; 
  }
  
  if (size == 0) {
    fprintf(stderr, "%serror:%s could not get determine the maximum size of data to process. Try --size some-size\n", 
      COLOR_RED, COLOR_NONE);
    return false;
  }
  
  if (0 != param->input_position) {
    if (-1 == lseek(fd, param->input_position, SEEK_SET)) {
      fprintf(stderr, "%serror:%s could not seek first %s%lu%s bytes (error message is %s%s%s). \n", 
        COLOR_RED, COLOR_NONE, COLOR_HL, param->input_position, COLOR_NONE,  COLOR_HL, strerror(errno), COLOR_NONE);
      return false;
    }
  }  
  
  const size_t chunks_per_sector = 16*16;
  const size_t buffer_size = chunks_per_sector*sector_size;
  
  // }}} [find info]

  // [display info] {{{
  /*
   * now print some fact:
   */
  const char* file_type = is_block_device ? "device":"file";
                                                              
  fprintf(stdout, "%s path: %s%s%s\n", file_type, COLOR_HL, path, COLOR_NONE);
  if (file_or_device_size_known) {
    fprintf(stdout, "%s size: %s%9lu B%s (%s%s%s, %s%s%s).\n", 
      file_type, COLOR_HL, file_or_device_size, COLOR_NONE, 
      COLOR_HL, to_si_string(file_or_device_size), COLOR_NONE, 
      COLOR_HL, to_bi_string(file_or_device_size), COLOR_NONE
    ); 
  }
  if (param->input_position != 0) {
    fprintf(stdout, "  starting from position: %s%9lu B%s (%s%s%s, %s%s%s).\n", 
      COLOR_HL, param->input_position, COLOR_NONE, 
      COLOR_HL, to_si_string(param->input_position), COLOR_NONE, 
      COLOR_HL, to_bi_string(param->input_position), COLOR_NONE
    );
  }
  if (size != file_or_device_size) {
    fprintf(stdout, "  reading up to.........: %s%9lu B%s (%s%s%s, %s%s%s).\n", 
      COLOR_HL, size, COLOR_NONE, 
      COLOR_HL, to_si_string(size), COLOR_NONE, 
      COLOR_HL, to_bi_string(size), COLOR_NONE
    );
  }  
  fprintf(stdout, "  reading by chunks %s%lu%s sectors of size %s%lu%s.\n", 
    COLOR_HL, chunks_per_sector, COLOR_NONE,
    COLOR_HL, sector_size, COLOR_NONE
  );
  
  // }}} 

  // [buffer] {{{
  if (param->buffer != NULL && param->buffer_size < buffer_size) {
    free(param->buffer);
    param->buffer = NULL;
    param->buffer_size = 0;
  }
  
  char* buffer = param->buffer;
  if (param->buffer == NULL) {
    buffer = calloc(buffer_size, sizeof(char));
    if (NULL == buffer) {
      fprintf(stderr, "%serror:%s could not allocate buffer (error message is %s%s%s). \n", 
        COLOR_RED, COLOR_NONE, COLOR_HL, strerror(errno), COLOR_NONE);
      return EXIT_FAILURE;
    }  
    param->buffer = buffer; 
    param->buffer_size = buffer_size;
  }
  // }}}
  
  if (mode == EXECUTION_MODE_ZERO || mode == EXECUTION_MODE_ONE) {
    int what = mode == EXECUTION_MODE_ZERO ? 0:~0;
    if (match_all(file, size, sector_size, buffer, buffer_size, what)) {
      fprintf(param->output, "%s: OK\n", path); 
    } else {
      fprintf(param->output, "%s: KO\n", path);
    }
    return true;
  }
  
  // find is a little different
  ssize_t n = 0;
  for (ssize_t size_read = 0; (n = read(fd, buffer, buffer_size)) > 0 && size_read < size; size_read += n) {
    unsigned long limit = size_read + n > size ? ulong_min(n, size - size_read):n;
    fprintf(stdout, "n: %lu size_read: %lu size: %lu -- limit %lu\n", n, size_read, size, limit);
    
    for (unsigned long i = 0, chunk = 0; i < limit; i += sector_size, ++chunk) {
      unsigned long max_offset = ulong_min(i + sector_size, limit);
    }
  }
  return true;
} // }}} 

int main(const int argc, char* const argv[]) {
  const char* device_path        = NULL;
  const char* find_file          = NULL;
  const char* device_size_str    = NULL;
  const char* sector_size_str    = NULL;
  const char* input_position_str = NULL;
  const char* status_output      = NULL;
  
  ExecutionMode mode = EXECUTION_MODE_INVALID;
  
  for (;;) {
    // use same name than ddrescue.
    static struct option long_options[] = {
      {"check-zero",     no_argument,       0, '0' },
      {"check-one",      no_argument,       0, '1' },
      {"find",           required_argument, 0, 'f' },
      {"sector-size",    required_argument, 0, 'b' },
      {"input-position", required_argument, 0, 'i' },
      {"size",           required_argument, 0, 's' },
      {"output",         required_argument, 0, 'o' },
      {0,                0,                 0, 0   } 
    };  
    int c = getopt_long(argc, argv, "01f:b:i:s:o:", long_options, NULL);
    if (c == -1) {
      break;
    }      
    switch (c) {     
      case '0': mode = EXECUTION_MODE_ZERO; break;            
      case '1': mode = EXECUTION_MODE_ONE; break;            
      case 'f': mode = EXECUTION_MODE_FIND; find_file = optarg; break;
      case 'b': sector_size_str    = optarg; break;
      case 'i': input_position_str = optarg; break;
      case 's': device_size_str    = optarg; break;
      case 'o': status_output      = optarg; break;
      case '?': case ':':
        return EXIT_FAILURE;
    }
  }
  
  if (optind != argc) {
    device_path = argv[optind];   
  }

  /*
   * check our stuff:
   */
  bool fail = false;
  if (mode == EXECUTION_MODE_FIND) {
    if (find_file == NULL) {        
      fail = true;
      fprintf(stderr, "%serror:%s missing file [--file].\n", COLOR_RED, COLOR_NONE);
    }
  }
  
  if (mode == EXECUTION_MODE_INVALID) {
    fail = true;
    fprintf(stderr, "%serror:%s missing execution mode [--zero, --one or --find].\n", COLOR_RED, COLOR_NONE);    
  }
  
  param_t param = {};
  param.sector_size    = parse_arg_as_size("sector-size",    &fail, sector_size_str,    0);
  param.input_position = parse_arg_as_size("input-position", &fail, input_position_str, 0);
  param.size           = parse_arg_as_size("size",           &fail, device_size_str,    0);
  
  if (fail) {                               
    usage(argv[0], find_file, sector_size_str);
    return EXIT_FAILURE;
  }
  
  FILE* out;
  if (status_output != NULL) {
    out = fopen(status_output, "w");
    if (NULL == out) {
      fprintf(stderr, "%serror:%s could not open output file [%s%s%s] in write mode.", 
                      COLOR_RED, COLOR_NONE, COLOR_YELLOW, status_output, COLOR_NONE);
      return EXIT_FAILURE;
    }
  } else {
    out = stdout;
  }
  
  param.output = out;
  
  file_t file = {-1, NULL};
  
  int error_count = 0;
  for (int i = optind; i < argc; ++i) {
    const char* input_path = argv[i];
    
    int fd = open(input_path, O_RDONLY);
    if (-1 == fd) {
      fprintf(stderr, "%serror:%s could not open file or device [%s%s%s], got some error: %s%s%s.\n", 
                      COLOR_RED, COLOR_NONE, COLOR_YELLOW, input_path, COLOR_NONE, COLOR_HL, strerror(errno), COLOR_NONE);
      ++error_count;
      continue;
    }
    
    file.fd = fd;
    file.path = input_path;
    
    if (!execute(&file, mode, &param)) {
      ++error_count;
    }
    
    close(fd);
  }
  if (param.buffer != NULL) {
    free(param.buffer);
  }
  if (status_output != NULL) {
    fclose(param.output);
  }
  
  return error_count;
}
