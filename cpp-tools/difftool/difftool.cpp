#include <cstdio>
#include <cstdlib>
#include <cctype>
#include <cstring>

#include <iostream>
#include <fstream>
#include <vector>
#include <algorithm>
#include <iomanip>

constexpr size_t size_per_row = 8; // don't change

const std::string COLOR_RED    = "\e[1;31m";
const std::string COLOR_GREEN  = "\e[1;32m";
const std::string COLOR_NONE   = "\e[0m"   ;
const std::string COLOR_CYAN   = "\e[1;36m";
const std::string COLOR_HL     = "\e[1m"   ;
const std::string COLOR_YELLOW = "\e[1;33m";

std::ostream& debug() { return std::cerr << COLOR_RED    << "error|" << COLOR_NONE << " "; }
std::ostream& info()  { return std::cerr << COLOR_CYAN   << "info |" << COLOR_NONE << " "; }
std::ostream& warn()  { return std::cerr << COLOR_YELLOW << "warn |" << COLOR_NONE << " "; }
std::ostream& error() { return std::cerr << COLOR_GREEN  << "debug|" << COLOR_NONE << " "; }

char printable(const char c) {
  return isprint(c) ? c:'.';
}

class file {
  private:
    std::string   path;
    std::ifstream in;

  public:

    file(const char* file) : path(file), in(file, std::ifstream::in | std::ifstream::binary) {
    }

    inline const std::string& getPath() const {return path;}
    inline const bool         is_open() const {return in.is_open(); }

    void read(std::vector<char>& buffer, size_t buffer_length) {
      buffer.reserve(buffer_length);
      char c;
      for (size_t i = 0; i < buffer_length && in.get(c); ++i) {
        buffer.push_back(c);
      }
    }
};

std::ostream& operator << (std::ostream& out, const file& file) {
  out << file.getPath();
  return out;
}

struct center {
  const char* text;
  size_t length;

  center(const char* text, size_t length) : text(text), length(length) {}
};


std::ostream& operator << (std::ostream& out, const center& c) {
  size_t n = c.length - strlen(c.text);
  for (size_t i = 0, p = (n % 2) + (n / 2); i < p; ++i) {
    out << ' ';
  }
  out << c.text;
  for (size_t i = 0, p = (n / 2) ; i < p; ++i) {
    out << ' ';
  }
  return out;
}

std::ostream& print_position(std::ostream& out, size_t left_offset, size_t right_offset) {
  return out << std::setfill(' ') << std::setw(8) << std::right << std::hex << left_offset  << " "
             << std::setfill(' ') << std::setw(8) << std::right << std::hex << right_offset
             << " | " << std::setiosflags (std::ios::showbase);
}

std::ostream& print_hex0(std::ostream& out, const std::array<char, size_per_row>& left, const std::array<char, size_per_row>& right) {
  std::ios oldState(nullptr);
  oldState.copyfmt(out);
  for (size_t i = 0; i < left.size(); ++i) {
    char a = left[i], b = right[i];
    if (a != b) {
      out.copyfmt(oldState);
      out << COLOR_RED;
      out << std::setfill('0') << std::setw(2) << std::hex << static_cast<unsigned>(a);
      out.copyfmt(oldState);
      out << COLOR_NONE;
    } else {
      out << std::setfill('0') << std::setw(2) << std::hex << static_cast<unsigned>(a);
    }
    if (i % 2 && left.size() != (i+1) ) {
      out << ' ';
    }
  }
  out.copyfmt(oldState);
}
std::ostream& print_hex(std::ostream& out, const std::array<char, size_per_row>& left, const std::array<char, size_per_row>& right) {
  print_hex0(out, left, right);
  out << " | ";
  print_hex0(out, right, left);
  out << " | ";
  return out;
}

std::ostream& print_char0(std::ostream& out, const std::array<char, size_per_row>& left, const std::array<char, size_per_row>& right) {
 for (size_t i = 0; i < left.size(); ++i) {
    char a = left[i], b = right[i];
    if (a != b) {
      out << COLOR_RED << printable(a) << COLOR_NONE;
    } else {
      out << printable(a);
    }
  }
}

std::ostream& print_char(std::ostream& out, const std::array<char, size_per_row>& left, const std::array<char, size_per_row>& right) {
  print_char0(out, left, right); // print left
  out << " | ";
  print_char0(out, right, left); // print right
  out << " | ";
  return out;
}

void print_data(
  std::ostream& out,
  const size_t left_offset,
  const std::array<char, size_per_row>& left,
  const size_t right_offset,
  const std::array<char, size_per_row>& right
) {
  // print_position( out, left_offset, right_offset);
  print_hex(  out, left, right );
  print_char( out, left, right );
  if (left != right) {
    out << COLOR_RED   << center("Different", 10) << COLOR_NONE;
  } else {
    out << COLOR_GREEN << center("Same", 10) << COLOR_NONE;
  }
  out << std::endl;
}

void print_header(std::ostream& out) {
  std::cout << center("Offset", size_per_row * 2 + 1) << " | "
            << center("Left"  , size_per_row * 2) << " | "
            << center("Right" , size_per_row * 2) << " | "
            << center("Left"  , size_per_row    ) << " | "
            << center("Right" , size_per_row    ) << " | "
            << center("Status", 10)
            << std::endl
            ;
}


class diff_printer {
  private:
    std::ostream& out;

    size_t printer_length;
    std::vector<char> left_printer;
    std::vector<char> right_printer;

    size_t buffer_length;
    std::vector<char> left_buffer;
    std::vector<char> right_buffer;

    void print_header();
    void print_data();
    void print_position();
    void print_hex0();
    void print_hex();
    void print_char0();
    void print_char();

  public:
    diff_printer(std::ostream& out, size_t printer_length = 8, size_t buffer_length = 8192) : out(out), printer_length(printer_length), buffer_length(buffer_length)  {}

    bool print(file& left, file& right);

};




bool diff_printer::print(file& left, file& right) {

  // prepare our vector
  left_printer.reserve(printer_length);
  right_printer.reserve(printer_length);


  /*
   * we read as much as possible but we keep difference altogether: if we got a set of
   * identical bytes, we bufferize (until buffer_length is reached) in one side.
   */

  bool in_diff_mode = false; // are we still reading diff ?
  for (;;) {
    /*
     * the buffer
     */
    left.read(left_buffer, buffer_length);
    right.read(right_buffer, buffer_length);

    if (left_buffer.empty() && right_buffer.empty()) {
      break;
    }

    const size_t n = left_buffer.size();
    const size_t m = right_buffer.size();
    for (size_t i = 0; i < n && i < m; ++i) {
      char a = left_buffer[i];
      char b = right_buffer[i];
      left_printer.push_back(a);
      right_printer.push_back(b);
    }

    // remove the first values
    const size_t rem = std::min(n, m);

    left_buffer.erase(left_buffer.begin(),   left_buffer.begin()  + rem);
    right_buffer.erase(right_buffer.begin(), right_buffer.begin() + rem);

  }
  return true;

  //
  //
  //
  // size_t left_offset  = 0;
  // size_t right_offset = 0;
  // size_t small_offset = 0;
  //
  // bool diff = false;
  // size_t rows = 0;
  // for (;;) {
  //   bool a = left.read(lb);
  //   bool b = right.read(rb);
  //
  //
  //   for (size_t big_offset = 0; big_offset < lb.size(); ++big_offset, ++left_offset, ++right_offset) {
  //     if (lb[big_offset] != rb[big_offset]) {
  //       diff = true;
  //     }
  //     lpb[small_offset] = lb[big_offset];
  //     rpb[small_offset] = rb[big_offset];
  //     ++small_offset;
  //
  //     if (small_offset == lpb.size()) {
  //       if (0 == (rows % 30)) {
  //         print_header(out);
  //       }
  //       print_data(out, left_offset, lpb, right_offset, rpb, diff);
  //       lpb.fill(0);
  //       rpb.fill(0);
  //       small_offset = 0;
  //       ++rows;
  //     }
  //
  //   }
  //
  //   if (!a && !b) {
  //     break; // stop the infernal loop
  //   }
  // }
  //
  // if (small_offset > 0) {
  //   print_data(out, left_offset, lpb, right_offset, rpb, diff);
  //   out << std::endl;
  //   small_offset = 0;
  //   ++rows;
  // }
  // return diff;
}




int main(int argc, char* argv[]) {
  if (argc != 3) {
    std::cerr << "usage: " << argv[0] << " <left> <right>" << std::endl;
    return EXIT_FAILURE;
  }

  file left{argv[1]};
  if (!left.is_open()) {
    error() << " could not open [" << COLOR_HL << left << COLOR_NONE << "] for reading." << std::endl;
    return EXIT_FAILURE;
  }

  file right{argv[2]};
  if (!right.is_open()) {
    error() << " could not open [" << COLOR_HL << right << COLOR_NONE <<  "] for reading." << std::endl;
    return EXIT_FAILURE;
  }


  diff_printer dp{std::cout};

  dp.print(left, right);

  return EXIT_SUCCESS;
}
