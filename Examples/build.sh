ROOT=$(pwd)
java -jar build/ReBuilder.jar --build-dir $ROOT/build/out \
                        --source-dir $ROOT \
                        --original-app $ROOT/build/apks/base.apk \
                        --keystore-path /opt/bin/apksigner-keystore/android_tangled_bytes.keystore \
                        --install \
                        --keystore-password 201r11q41213 \
                        --install-with $ROOT/build/apks/split_config.arm64_v8a.apk \
                        --install-with $ROOT/build/apks/split_config.bn.apk \
                        --install-with $ROOT/build/apks/split_config.mdpi.apk \
                        --install-with $ROOT/build/apks/split_config.te.apk \
                        --install-with $ROOT/build/apks/split_config.gu.apk \
                        --install-with $ROOT/build/apks/split_config.ta.apk \
                        --install-with $ROOT/build/apks/split_config.en.apk \
                        --install-with $ROOT/build/apks/split_config.hi.apk \
                        --install-with $ROOT/build/apks/split_config.kn.apk \
                        --smali-dirs $ROOT/classes \
                        $@ 

cd build
./run
# Prevent original apk from being included in install with
# In help add a line informing about running 'run' file
# warn if not provided absolute paths 
# handle multiple devices
# Add tips section
    # Sign all apks with same key
    # Every path must be absolute
# Sign all apks option with new key
# --smali-dirs to --smali-dir
