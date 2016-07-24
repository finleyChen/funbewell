
#ifndef DEBUG_H
#define DEBUG_H

// Define macros used for debugging

//#define DEBUGN900
#ifdef DEBUGN900
  #define DebugN900( COMMAND ) COMMAND; 
#else
  #define DebugN900( COMMAND )
#endif

//#define DEBUGTIMEPROFILE
#ifdef DEBUGTIMEPROFILE
  #define DebugTimeProfile( COMMAND ) COMMAND; 
#else
  #define DebugTimeProfile( COMMAND )
#endif

//#define DEBUGGPS
#ifdef DEBUGGPS
  #define DebugGps( COMMAND ) COMMAND; 
#else
  #define DebugGps( COMMAND )
#endif

//#define DEBUGWRITEFILE
#ifdef DEBUGWRITEFILE
  #define DebugWriteFile( COMMAND ) COMMAND; 
#else
  #define DebugWriteFile( COMMAND )
#endif

#define DEBUGAUDIO
#ifdef DEBUGAUDIO
  #define DebugAudio( COMMAND ) COMMAND; 
#else
  #define DebugAudio( COMMAND )
#endif

#define DEBUGLABEL
#ifdef DEBUGLABEL
  #define DebugLabel( COMMAND ) COMMAND; 
#else
  #define DebugLabel( COMMAND )
#endif

#endif
