#ifndef MSBTIME_H
#define MSBTIME_H

#include <cstddef>
#include <sys/time.h>
//#include <iostream.h>
#include <stdint.h>

//typedef unsigned int MsbTime;
typedef uint64_t MsbTime;

class MsbTimeUtil {
public:
  static bool greaterThan( MsbTime a, MsbTime b ) {return ((b-a) > 0x80000000);} 
  static MsbTime sysTime() ; 
};

inline 
MsbTime MsbTimeUtil::sysTime()
{
  struct timeval tv;
  unsigned int seconds, useconds;
  MsbTime sysTime;
  gettimeofday (&tv, NULL);

  seconds = tv.tv_sec;
  useconds = tv.tv_usec;
  sysTime = seconds;
  sysTime = sysTime * 1000000;
  sysTime = sysTime + useconds;
  //std::cout<<seconds<<" "<<useconds<<" "<<sysTime<<endl; 
  return sysTime;
}

#endif
