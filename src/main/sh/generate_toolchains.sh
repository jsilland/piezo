#! /bin/bash
PROTOC_PATH=$(which protoc)
cat > toolchains.xml <<EOF
<toolchains>
        <toolchain>
                <type>protobuf</type>
                <provides>
                        <version>2.5.0</version>
                </provides>
                <configuration>
                        <protocExecutable>$PROTOC_PATH</protocExecutable>
                </configuration>
        </toolchain>
</toolchains>
EOF
