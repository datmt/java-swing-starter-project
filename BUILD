load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_test")

package(default_visibility = ["//visibility:public"])

MAIN_SRCS = glob(["src/main/java/com/binarycarpenter/spreadsheet/**/*.java"])
TEST_SRCS = glob(["src/test/java/com/binarycarpenter/spreadsheet/**/*.java"])
RESOURCES = glob(["src/main/resources/**/*"])

java_library(
    name = "bc-spreadsheet-lib",
    srcs = MAIN_SRCS,
    resources = RESOURCES,
    deps = [
        "@maven//:com_formdev_flatlaf",
        "@maven//:com_miglayout_miglayout_swing",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_opencsv_opencsv",
        "@maven//:org_apache_poi_poi",
        "@maven//:org_apache_poi_poi_ooxml",
        "@maven//:org_jopendocument_jOpenDocument",
        "@maven//:org_projectlombok_lombok",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
    ],
)

java_binary(
    name = "bc-spreadsheet",
    main_class = "com.binarycarpenter.spreadsheet.MainApp",
    runtime_deps = [":bc-spreadsheet-lib"],
)

java_test(
    name = "bc-spreadsheet-tests",
    srcs = TEST_SRCS,
    test_class = "com.binarycarpenter.spreadsheet.tools.ExcelEditorTest",
    deps = [
        ":bc-spreadsheet-lib",
        "@maven//:org_junit_jupiter_junit_jupiter",
        "@maven//:org_assertj_assertj_core",
        "@maven//:org_spockframework_spock_core",
        "@maven//:org_apache_groovy_groovy",
    ],
)
