
#include <stdio.h>
#include <limits.h>
#include <string.h>


void produceXML(char * filename) {

  FILE *fd = fopen(filename, "w");

  if (fd == NULL) {
    printf("Error while trying to write to file %s.", filename);
    return;
  }

  char * line;

  line = "<?xml version=\"1.0\" ?>\n";
  fwrite(line, strlen(line), 1, fd);

  char * endianness = "Big Endian";
  if (isLittleEndian())
    endianness = "Little Endian";

  fprintf(fd, "<Platform endianness=\"%s\">\n", endianness);

  fprintf(fd, "\t<char>\n\t\t<size>%d</size>\n\t\t", sizeof(char));
  fprintf(fd, "<minValue>%d</minValue>\n\t\t<maxValue>%d</maxValue>\n\t</char>\n", CHAR_MIN, CHAR_MAX);

  fprintf(fd, "\t<short>\n\t\t<size>%d</size>\n\t\t", sizeof(short));
  fprintf(fd, "<minValue>%d</minValue>\n\t\t<maxValue>%d</maxValue>\n\t</short>\n", SHRT_MIN, SHRT_MAX);

  fprintf(fd, "\t<int>\n\t\t<size>%d</size>\n\t\t", sizeof(int));
  fprintf(fd, "<minValue>%d</minValue>\n\t\t<maxValue>%d</maxValue>\n\t</int>\n", INT_MIN, INT_MAX);

  fprintf(fd, "\t<long>\n\t\t<size>%d</size>\n\t\t", sizeof(long));
  fprintf(fd, "<minValue>%d</minValue>\n\t\t<maxValue>%d</maxValue>\n\t</long>\n", LONG_MIN, LONG_MAX);

  fprintf(fd, "\t<float>\n\t\t<size>%d</size>\n\t</float>\n", sizeof(float));
  fprintf(fd, "\t<double>\n\t\t<size>%d</size>\n\t</double>\n", sizeof(double));
  fprintf(fd, "</Platform>\n");

  fclose(fd);



}

int isLittleEndian(){
  union{ int a; char b; } u;
  u.a = 1;
  return u.b;
}


int main(int argc, char* argv[])
{

  char * filename = "config.xml";

  if (argc == 2)
    filename = argv[1];

  produceXML(filename);
  return 0;
}
