Summary: SnpViewer
Name: SnpViewer
Version: 1.0
Release: 1
License: unknown
Vendor: David Parry
Prefix: /opt
Provides: SnpViewer
Requires: ld-linux.so.2 libX11.so.6 libXext.so.6 libXi.so.6 libXrender.so.1 libXtst.so.6 libasound.so.2 libc.so.6 libdl.so.2 libgcc_s.so.1 libm.so.6 libpthread.so.0 libthread_db.so.1
Autoprov: 0
Autoreq: 0

#avoid ARCH subfolder
%define _rpmfilename %%{NAME}-%%{VERSION}-%%{RELEASE}.%%{ARCH}.rpm

#comment line below to enable effective jar compression
#it could easily get your package size from 40 to 15Mb but 
#build time will substantially increase and it may require unpack200/system java to install
%define __jar_repack %{nil}

%description
SnpViewer

%prep

%build

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt
cp -r %{_sourcedir}/SnpViewer %{buildroot}/opt

%files

/opt/SnpViewer

%post
cp /opt/SnpViewer/SnpViewer.desktop /usr/share/applications/
echo Detecting architecture and adding libjvm.so to runtime
MACHINE_TYPE=`uname -m`
if [ "$MACHINE_TYPE" = 'x86_64' ]; then
       echo is 64 bit
       ARCH_FOLDER='amd64';
else
       echo is 32 bit
       ARCH_FOLDER='i386';
fi

if [ ! -d /opt/SnpViewer/runtime/jre/lib/$ARCH_FOLDER/client ]
then 
	mkdir -p /opt/SnpViewer/runtime/jre/lib/$ARCH_FOLDER/client
fi
if [ ! -f /opt/SnpViewer/runtime/jre/lib/$ARCH_FOLDER/client/libjvm.so ]
	then
	cp /opt/SnpViewer/app/libjvm.so /opt/SnpViewer/runtime/jre/lib/$ARCH_FOLDER/client
fi


%preun
rm -f /usr/share/applications/SnpViewer.desktop

%clean
