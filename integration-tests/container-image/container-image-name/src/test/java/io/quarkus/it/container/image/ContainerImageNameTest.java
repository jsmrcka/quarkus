package io.quarkus.it.container.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;

import io.quarkus.container.image.deployment.ContainerImageConfig;
import io.quarkus.container.image.deployment.ContainerImageProcessor;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.configuration.BuildTimeConfigurationReader;
import io.quarkus.deployment.configuration.DefaultValuesConfigurationSource;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class ContainerImageNameTest {

    private static final String APP_NAME = "repo/name";
    private static final String APP_VERSION = "v1.0.1";

    private static final String APP_PROPERTY = "quarkus.application.name";
    private static final String VERSION_PROPERTY = "quarkus.application.version";
    private static final String GROUP_PROPERTY = "quarkus.container-image.group";
    private static final String USER_NAME_PROPERTY = "user.name";

    private static final Properties initialSystemProperties = new Properties();

    Properties testProperties = new Properties();

    ContainerImageInfoBuildItem actualContainerImageInfo;

    @BeforeAll
    public static void backupSystemProperties() {
        System.setProperty(APP_PROPERTY, APP_NAME);
        System.setProperty(VERSION_PROPERTY, APP_VERSION);
        initialSystemProperties.putAll(System.getProperties());
    }

    @AfterAll
    public static void restoreSystemProperties() {
        System.getProperties().remove(APP_PROPERTY);
        System.getProperties().remove(VERSION_PROPERTY);
    }

    @BeforeEach
    public void setup() {
        testProperties.clear();
    }

    @AfterEach
    public void tearDown() {
        System.getProperties().clear();
        System.getProperties().putAll(initialSystemProperties);
    }

    @Test
    public void shouldUseAppNameAndVersionWhenNoUserName() {
        givenNoUserName();
        whenPublishImageInfo();
        thenImageIs(APP_NAME + ":" + APP_VERSION);
    }

    @Test
    public void shouldUseAppNameAndVersionWhenUserName() {
        givenUserName("user");
        whenPublishImageInfo();
        thenImageIs("user/" + APP_NAME + ":" + APP_VERSION);
    }

    @Test
    public void shouldRemoveSpacesInUserName() {
        givenUserName("user surname");
        whenPublishImageInfo();
        thenImageIs("user-surname/" + APP_NAME + ":" + APP_VERSION);
    }

    @Test
    public void shouldFailWhenSpacesInGroupProperty() {
        givenProperty(GROUP_PROPERTY, "group with space");
        thenImagePublicationFails();
    }

    private void givenNoUserName() {
        givenProperty(USER_NAME_PROPERTY, StringUtils.EMPTY);
    }

    private void givenUserName(String value) {
        givenProperty(USER_NAME_PROPERTY, value);
    }

    private void givenProperty(String key, String value) {
        System.setProperty(key, value);
    }

    private void whenPublishImageInfo() throws RuntimeException {
        BuildTimeConfigurationReader reader = new BuildTimeConfigurationReader(
                Collections.singletonList(ContainerImageConfig.class));
        SmallRyeConfigBuilder builder = ConfigUtils.configBuilder(false);

        DefaultValuesConfigurationSource ds = new DefaultValuesConfigurationSource(
                reader.getBuildTimePatternMap());
        PropertiesConfigSource pcs = new PropertiesConfigSource(testProperties, "Test Properties");
        builder.withSources(ds, pcs);

        SmallRyeConfig src = builder.build();
        BuildTimeConfigurationReader.ReadResult readResult = reader.readConfiguration(src);
        ContainerImageConfig containerImageConfig = (ContainerImageConfig) readResult
                .requireRootObjectForClass(ContainerImageConfig.class);

        ApplicationInfoBuildItem app = new ApplicationInfoBuildItem(Optional.of(APP_NAME), Optional.of(APP_VERSION));
        Capabilities capabilities = new Capabilities(Collections.emptySet());
        BuildProducer<ContainerImageInfoBuildItem> containerImage = actualImageConfig -> actualContainerImageInfo = actualImageConfig;
        ContainerImageProcessor processor = new ContainerImageProcessor();
        processor.publishImageInfo(app, containerImageConfig, capabilities, containerImage);
    }

    private void thenImageIs(String expectedImage) {
        assertEquals(expectedImage, actualContainerImageInfo.getImage());
    }

    private void thenImagePublicationFails() {
        assertThrows(IllegalArgumentException.class, this::whenPublishImageInfo);
    }
}
