package org.main.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

@Suite
@SelectClasspathResource("features")
@ConfigurationParameter(
        key = GLUE_PROPERTY_NAME,
        value = "org.main.cucumber"
)
@ConfigurationParameter(
        key = io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty, summary"
)
@ConfigurationParameter(
        key = io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME,
        value = "not @ignore"
)


public class CucumberTest {

}
