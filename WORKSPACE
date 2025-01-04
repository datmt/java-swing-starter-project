workspace(name = "bc18_spreadsheet_tools")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "4.5"
RULES_JVM_EXTERNAL_SHA = "b17d7388feb9bfa7f2fa09031b32707df529f26c91ab9e5d909eb1676badd9a6"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")
rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")
rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "com.formdev:flatlaf:3.2.5",
        "com.miglayout:miglayout-swing:11.3",
        "com.fasterxml.jackson.core:jackson-databind:2.15.2",
        "com.opencsv:opencsv:5.8",
        "org.apache.poi:poi:5.2.5",
        "org.apache.poi:poi-ooxml:5.2.5",
        "org.jopendocument:jOpenDocument:1.3",
        "org.projectlombok:lombok:1.18.36",
        "org.slf4j:slf4j-api:2.0.16",
        "ch.qos.logback:logback-classic:1.5.15",
        "ch.qos.logback:logback-core:1.5.15",
        # Test dependencies
        "org.junit.jupiter:junit-jupiter:5.9.2",
        "org.assertj:assertj-core:3.24.2",
        "org.spockframework:spock-core:2.4-M4-groovy-4.0",
        "org.apache.groovy:groovy:4.0.24",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    fetch_sources = True,
)
