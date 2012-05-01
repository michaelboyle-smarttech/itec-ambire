APPVER = 5.02
TARGETOS = WINNT
INCLUDE = $(INCLUDE);C:\Developer\SDK\Flex\Include
LIB = $(LIB);C:\Developer\SDK\Flex\lib\win
!include <win32.mak>

AmbireCapture.ane: library.swf AmbireCapture.dll AmbireCapture.xml
	adt -package -target ane AmbireCapture.ane AmbireCapture.xml -swc AmbireCapture.swc -platform Windows-x86 library.swf AmbireCapture.dll 

library.swf: org/eun/itec/ambire/student/AmbireCapture.as
	acompc -source-path . -include-classes org.eun.itec.ambire.student.AmbireCapture -swf-version=14 -output AmbireCapture-library.swc
	unzip AmbireCapture-library.swc library.swf

AmbireCapture.dll: AmbireCapture-Win32.obj AmbireCapture-Win32.def
	$(link) $(ldebug) $(dlllflags) $(guilibsmt) AmbireCapture-Win32.obj FlashRuntimeExtensions.lib /DEF:AmbireCapture-Win32.def /OUT:AmbireCapture.dll

AmbireCapture-Win32.obj: AmbireCapture-Win32.cpp
	$(cc) $(cdebug) $(cflags) $(cvarsmt) /c AmbireCapture-Win32.cpp

clean:
	del /q AmbireCapture.ane library.swf AmbireCapture.dll AmbireCapture-library.swc AmbireCapture.exp AmbireCapture.lib AmbireCapture.pdb vc100.pdb AmbireCapture-Win32.obj AmbireCapture.swc
