#!/bin/bash

ROOTPATH="target_files-package"
mkdir -p $ROOTPATH
#build
mkdir -p  $ROOTPATH/build/target/product/
cp -a build/target/product/security/  $ROOTPATH/build/target/product/
mkdir -p $ROOTPATH/build/tools/
cp -ur build/tools/releasetools/  $ROOTPATH/build/tools/
#out
mkdir -p $ROOTPATH/out/host/linux-x86/bin/
cp -u out/host/linux-x86/bin/minigzip  out/host/linux-x86/bin/mkbootfs out/host/linux-x86/bin/mkbootimg out/host/linux-x86/bin/fs_config out/host/linux-x86/bin/zipalign  out/host/linux-x86/bin/bsdiff out/host/linux-x86/bin/imgdiff out/host/linux-x86/bin/mkuserimg.sh  out/host/linux-x86/bin/make_ext4fs  out/host/linux-x86/bin/aapt  out/host/linux-x86/bin/simg2img out/host/linux-x86/bin/e2fsck $ROOTPATH/out/host/linux-x86/bin/
cp -u out/host/linux-x86/bin/build_verity_tree  out/host/linux-x86/bin/verity_signer  out/host/linux-x86/bin/append2simg  out/host/linux-x86/bin/boot_signer out/host/linux-x86/bin/img2simg $ROOTPATH/out/host/linux-x86/bin/
mkdir -p $ROOTPATH/out/host/linux-x86/framework
cp -u out/host/linux-x86/framework/signapk.jar  out/host/linux-x86/framework/dumpkey.jar out/host/linux-x86/framework/BootSignature.jar out/host/linux-x86/framework/VeritySigner.jar $ROOTPATH/out/host/linux-x86/framework/
mkdir -p $ROOTPATH/out/target/product/$3/
cp -u $1/ota_scatter.txt  $ROOTPATH/out/target/product/$3/
mkdir -p $ROOTPATH/$4/
cp -u $4/libc++$5 $4/liblog$5 $4/libcutils$5 $4/libselinux$5 $4/libcrypto-host$5 $4/libext2fs_host$5 $4/libext2_blkid_host$5 $4/libext2_com_err_host$5 $4/libext2_e2p_host$5 $4/libext2_profile_host$5 $4/libext2_quota_host$5 $4/libext2_uuid_host$5  $ROOTPATH/$4/
mkdir -p $ROOTPATH/system/extras/verity/
cp -u system/extras/verity/build_verity_metadata.py $ROOTPATH/system/extras/verity/
#mkdir -p $ROOTPATH/out/host/linux-x86/lib64/
#vendor
mkdir -p $ROOTPATH/vendor/mediatek/proprietary/custom/$3/
cp -ur vendor/mediatek/proprietary/custom/$3/security/  $ROOTPATH/vendor/mediatek/proprietary/custom/$3/
#device
mkdir -p $ROOTPATH/device/mediatek/common/
cp -ur device/mediatek/common/security/  $ROOTPATH/device/mediatek/common/
mkdir -p $ROOTPATH/device/mediatek/build/
cp -ur device/mediatek/build/releasetools/  $ROOTPATH/device/mediatek/build/
#system_image_info
mkdir -p $ROOTPATH/out/target/product/$3/root/
cp -u $1/obj/PACKAGING/systemimage_intermediates/system_image_info.txt $ROOTPATH/
cp -u $1/root/file_contexts  $ROOTPATH/out/target/product/$3/root/
#target_files
if [ $# -ge 6 ] && [ $6 = IMG ] ; then
cp -u $1/img-target-files.zip $ROOTPATH/ota_target_files.zip
mkdir -p IMAGES/
cp -u $1/obj/PACKAGING/systemimage_intermediates/system.map IMAGES/
zip -q $ROOTPATH/ota_target_files.zip IMAGES/system.map
rm -rf IMAGES
else
echo `ls -lrt $1/obj/PACKAGING/target_files_intermediates/*target_files*.zip|tail -n 1|awk '{print $NF}'`
cp -u `ls -lrt $1/obj/PACKAGING/target_files_intermediates/*target_files*.zip|tail -n 1|awk '{print $NF}'`  $ROOTPATH/ota_target_files.zip
#mkdir -p IMAGES/
#touch IMAGES/system.img
#touch IMAGES/userdata.img
#zip -q $ROOTPATH/ota_target_files.zip IMAGES/system.img IMAGES/userdata.img
#rm -rf IMAGES
fi

#build.prop
cp -u $1/system/build.prop $ROOTPATH/build.prop

cp -u $1/lk.bin $ROOTPATH/lk.bin
cp -u $1/logo.bin $ROOTPATH/logo.bin

#configure.xml
echo "">$ROOTPATH/configure.xml
echo "<root>">>$ROOTPATH/configure.xml

#buildnumber
var=$(grep  "ro.fota.version=" "$1/system/build.prop" )
buildnumber=${var##"ro.fota.version="}
echo "<buildnumber>$buildnumber</buildnumber>">>$ROOTPATH/configure.xml

#language
if [ -n "`grep "ro.product.locale.language=" $1/system/build.prop`" ] ; then
    var=$(grep  "ro.product.locale.language=" "$1/system/build.prop" )
    echo "<language>${var##"ro.product.locale.language="}</language>">>$ROOTPATH/configure.xml
elif [ -n "`grep "ro.product.locale=" $1/system/build.prop`" ] ; then
    var=$(grep  "ro.product.locale=" "$1/system/build.prop" )
    echo "<language>${var##"ro.product.locale="}</language>">>$ROOTPATH/configure.xml
else
    echo "<language>en</language>">>$ROOTPATH/configure.xml
fi

#oem
var=$(grep  "ro.fota.oem=" "$1/system/build.prop" )
echo "<oem>${var##"ro.fota.oem="}</oem>">>$ROOTPATH/configure.xml

#operator
var=$(grep  "ro.operator.optr=" "$1/system/build.prop")
if [ "$var" = "" ] ; then
  var=other
else
var=$(echo $var|tr A-Z a-z)
if [ ${var##"ro.operator.optr="} = op01 ] ; then
var=CMCC
elif [ ${var##"ro.operator.optr="} = op02 ] ; then
var=CU
else
var=other
fi
fi
echo "<operator>${var##"ro.operator.optr="}</operator>">>$ROOTPATH/configure.xml

#model
var=$(grep  "ro.fota.device=" "$1/system/build.prop" )
product=${var##"ro.fota.device="}
echo "<product>$product</product>">>$ROOTPATH/configure.xml

#publishtime
echo "<publishtime>$(date +20%y%m%d%H%M%S)</publishtime>">>$ROOTPATH/configure.xml

#versionname
echo "<versionname>$buildnumber</versionname>">>$ROOTPATH/configure.xml
#key
echo "<key>$2</key>">>$ROOTPATH/configure.xml
echo "</root>">>$ROOTPATH/configure.xml

if [ -f $1/target_files-package.zip ]; then
echo "delete exist file:$1/target_files-package"
rm -f $1/target_files-package.zip
fi

if [ -f $1/img-target-files.zip ]; then
echo "delete exist file:$1/img-target-files.zip"
rm -f $1/img-target-files.zip
fi

cd target_files-package
zip -rq target_files-package.zip build device out vendor system configure.xml build.prop lk.bin logo.bin ota_target_files.zip system_image_info.txt
cd ..
mv target_files-package/target_files-package.zip $1/target_files-package.zip
rm -rf target_files-package

